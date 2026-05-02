package com.mobileslicer.printerconnection

import android.util.Base64
import com.mobileslicer.profiles.PrintHostAuthorizationType
import com.mobileslicer.profiles.PrinterProfile
import java.nio.charset.StandardCharsets

internal enum class PrinterProtocol {
    OctoPrint,
    Moonraker
}

internal fun PrinterProfile.authHeaders(protocol: PrinterProtocol): Map<String, String> {
    val headers = mutableMapOf<String, String>()
    when (printHostAuthorizationType) {
        PrintHostAuthorizationType.Key -> {
            val key = printHostApiKey.trim()
            if (key.isNotBlank()) {
                headers["X-Api-Key"] = key
            }
        }
        PrintHostAuthorizationType.User -> {
            val user = printHostUser.trim()
            if (user.isNotBlank() || printHostPassword.isNotBlank()) {
                val token = "$user:$printHostPassword"
                headers["Authorization"] = "Basic " + Base64.encodeToString(
                    token.toByteArray(StandardCharsets.UTF_8),
                    Base64.NO_WRAP
                )
            }
        }
    }
    if (protocol == PrinterProtocol.Moonraker && printHostApiKey.isNotBlank()) {
        headers["X-Api-Key"] = printHostApiKey.trim()
    }
    return headers
}
