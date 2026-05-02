package com.mobileslicer.printerconnection

import android.util.Base64
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal data class SimplyPrintOAuthResult(
    val success: Boolean,
    val message: String,
    val accessToken: String = "",
    val refreshToken: String = ""
)

internal class SimplyPrintOAuthClient {
    suspend fun login(openUrl: suspend (String) -> Unit): SimplyPrintOAuthResult = withContext(Dispatchers.IO) {
        val verifier = randomUrlToken()
        val state = randomUrlToken()
        val authUrl = "$SIMPLYPRINT_OAUTH_HOME/panel/oauth2/authorize?" + formEncode(
            mapOf(
                "client_id" to SIMPLYPRINT_OAUTH_CLIENT_ID,
                "redirect_uri" to SIMPLYPRINT_OAUTH_CALLBACK_URL,
                "scope" to SIMPLYPRINT_OAUTH_SCOPES,
                "response_type" to "code",
                "state" to state,
                "code_challenge" to sha256Base64Url(verifier),
                "code_challenge_method" to "S256"
            )
        )

        val server = try {
            ServerSocket(SIMPLYPRINT_OAUTH_CALLBACK_PORT).apply { soTimeout = 180_000 }
        } catch (exception: Exception) {
            return@withContext SimplyPrintOAuthResult(false, "SimplyPrint login failed\nCould not open local callback port ${SIMPLYPRINT_OAUTH_CALLBACK_PORT}: ${exception.message}")
        }

        server.use {
            openUrl(authUrl)
            val params = try {
                val socket = it.accept()
                socket.use { accepted ->
                    val requestLine = BufferedReader(accepted.getInputStream().reader()).readLine().orEmpty()
                    val query = requestLine.substringAfter("GET ", "")
                        .substringBefore(" HTTP/")
                        .substringAfter('?', "")
                    val response = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nConnection: close\r\n\r\n" +
                        "<html><body><h2>SimplyPrint login complete.</h2><p>You can return to Mobile Slicer.</p></body></html>"
                    accepted.getOutputStream().write(response.toByteArray(StandardCharsets.UTF_8))
                    parseQuery(query)
                }
            } catch (exception: Exception) {
                return@withContext SimplyPrintOAuthResult(false, "SimplyPrint login failed\nNo OAuth callback was received: ${exception.message}")
            }

            if (params["state"] != state) {
                return@withContext SimplyPrintOAuthResult(false, "SimplyPrint login failed\nOAuth state did not match.")
            }
            val code = params["code"].orEmpty()
            if (code.isBlank()) {
                val error = params["error_description"] ?: params["error"] ?: "SimplyPrint did not return an authorization code."
                return@withContext SimplyPrintOAuthResult(false, "SimplyPrint login failed\n$error")
            }

            exchangeCode(code, verifier)
        }
    }

    private fun exchangeCode(code: String, verifier: String): SimplyPrintOAuthResult {
        return try {
            val body = formEncode(
                mapOf(
                    "grant_type" to "authorization_code",
                    "client_id" to SIMPLYPRINT_OAUTH_CLIENT_ID,
                    "code" to code,
                    "redirect_uri" to SIMPLYPRINT_OAUTH_CALLBACK_URL,
                    "code_verifier" to verifier
                )
            )
            val connection = (URL(SIMPLYPRINT_OAUTH_TOKEN_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 7_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Accept", "application/json")
            }
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
            val response = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                return SimplyPrintOAuthResult(false, "SimplyPrint login failed\nHTTP ${connection.responseCode}: ${error.ifBlank { "token exchange failed" }}")
            }
            val json = JSONObject(response)
            val accessToken = json.optString("access_token", "")
            val refreshToken = json.optString("refresh_token", "")
            if (accessToken.isBlank() || refreshToken.isBlank()) {
                SimplyPrintOAuthResult(false, "SimplyPrint login failed\nToken response did not include access and refresh tokens.")
            } else {
                SimplyPrintOAuthResult(true, "SimplyPrint login complete", accessToken, refreshToken)
            }
        } catch (exception: Exception) {
            SimplyPrintOAuthResult(false, "SimplyPrint login failed\n${exception.message ?: "Token exchange failed."}")
        }
    }

    private fun randomUrlToken(byteCount: Int = 32): String {
        val bytes = ByteArray(byteCount)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun sha256Base64Url(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun formEncode(values: Map<String, String>): String =
        values.entries.joinToString("&") { (key, value) ->
            "${urlEncode(key)}=${urlEncode(value)}"
        }

    private fun parseQuery(query: String): Map<String, String> =
        query.split('&')
            .filter { it.isNotBlank() }
            .associate { part ->
                val key = part.substringBefore('=')
                val value = part.substringAfter('=', "")
                urlDecode(key) to urlDecode(value)
            }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

    private fun urlDecode(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}

internal const val SIMPLYPRINT_OAUTH_HOME = "https://simplyprint.io"
internal const val SIMPLYPRINT_OAUTH_API = "https://api.simplyprint.io"
internal const val SIMPLYPRINT_OAUTH_CLIENT_ID = "simplyprintorcaslicer"
internal const val SIMPLYPRINT_OAUTH_CALLBACK_PORT = 21328
internal const val SIMPLYPRINT_OAUTH_CALLBACK_URL = "http://localhost:21328/callback"
internal const val SIMPLYPRINT_OAUTH_SCOPES = "user.read files.temp_upload"
internal const val SIMPLYPRINT_OAUTH_TOKEN_URL = "$SIMPLYPRINT_OAUTH_API/oauth2/Token"
