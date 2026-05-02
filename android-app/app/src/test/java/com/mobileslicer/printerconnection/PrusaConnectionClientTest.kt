package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.PrintHostAuthorizationType
import com.mobileslicer.profiles.ProfileStoreRepository
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrusaConnectionClientTest {
    @Test
    fun prusaLinkPutUploadUsesV1FilesOverwriteAndPrintAfterUpload() = runBlocking {
        val rawUploads = mutableListOf<RawUploadCall>()
        val client = prusaClient(
            versionBody = """{"api":"2.0.0","text":"PrusaLink 0.8.1","capabilities":{"upload-by-put":true}}""",
            rawUploads = rawUploads
        )
        val profile = ProfileStoreRepository.fallbackPrinterProfile().copy(
            printHostAuthorizationType = PrintHostAuthorizationType.Key,
            printHostApiKey = "abc123",
            printHostPort = "/usb"
        )

        val result = client.uploadLinkGcode(
            profile = profile,
            baseUrl = "http://prusa.local",
            file = tempGcodeFile(),
            remoteFileName = "Folder/Benchy test.gcode",
            action = PrinterUploadAction.UploadAndStart,
            onProgress = {}
        )

        assertTrue(result.success)
        assertEquals(
            listOf(
                RawUploadCall(
                    url = "http://prusa.local/api/v1/files/usb/Benchy%20test.gcode",
                    method = "PUT",
                    headers = mapOf(
                        "X-Api-Key" to "abc123",
                        "Overwrite" to "?1",
                        "Print-After-Upload" to "?1"
                    ),
                    contentType = "text/x.gcode"
                )
            ),
            rawUploads
        )
    }

    @Test
    fun prusaConnectQueueUsesMultipartToQueueField() = runBlocking {
        val multipartUploads = mutableListOf<MultipartUploadCall>()
        val client = prusaClient(
            versionBody = """{"api":"2.0.0","text":"PrusaLink 0.8.1","capabilities":{"upload-by-put":true}}""",
            multipartUploads = multipartUploads
        )
        val profile = ProfileStoreRepository.fallbackPrinterProfile().copy(
            printHostAuthorizationType = PrintHostAuthorizationType.Key,
            printHostApiKey = "connect-token",
            printHostPort = "/local"
        )

        val result = client.uploadConnectGcode(
            profile = profile,
            baseUrl = "https://connect.prusa3d.com",
            file = tempGcodeFile(),
            remoteFileName = "Benchy.gcode",
            action = PrinterUploadAction.Queue,
            onProgress = {}
        )

        assertTrue(result.success)
        assertEquals(
            listOf(
                MultipartUploadCall(
                    url = "https://connect.prusa3d.com/api/files/local",
                    headers = mapOf("X-Api-Key" to "connect-token"),
                    fields = mapOf("path" to "", "to_queue" to "True"),
                    fileFieldName = "file",
                    remoteFileName = "Benchy.gcode"
                )
            ),
            multipartUploads
        )
    }

    private fun prusaClient(
        versionBody: String,
        multipartUploads: MutableList<MultipartUploadCall> = mutableListOf(),
        rawUploads: MutableList<RawUploadCall> = mutableListOf()
    ): PrusaConnectionClient =
        PrusaConnectionClient(
            requestTextBody = { url, method, _ ->
                if (method == "GET" && url.endsWith("/api/version")) {
                    TextNetworkResult.Success(versionBody)
                } else {
                    TextNetworkResult.Failure("unexpected request")
                }
            },
            uploadMultipart = { url, headers, fields, _, fileFieldName, remoteFileName, _ ->
                multipartUploads += MultipartUploadCall(url, headers, fields, fileFieldName, remoteFileName)
                NetworkResult.Success
            },
            uploadRawFile = { url, method, headers, _, _, _, contentType ->
                rawUploads += RawUploadCall(url, method, headers, contentType)
                NetworkResult.Success
            },
            fetchDigestChallenge = { null }
        )

    private fun tempGcodeFile(): File =
        File.createTempFile("mobile-slicer-prusa", ".gcode").apply {
            writeText("G28\n")
            deleteOnExit()
        }

    private data class MultipartUploadCall(
        val url: String,
        val headers: Map<String, String>,
        val fields: Map<String, String>,
        val fileFieldName: String,
        val remoteFileName: String
    )

    private data class RawUploadCall(
        val url: String,
        val method: String,
        val headers: Map<String, String>,
        val contentType: String
    )
}
