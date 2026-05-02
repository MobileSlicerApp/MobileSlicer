package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.ProfileStoreRepository
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuetConnectionClientTest {
    @Test
    fun rrfUploadUsesRrUploadAndStartsWithRrGcodeM32() = runBlocking {
        val requestUrls = mutableListOf<String>()
        val rawUploads = mutableListOf<RawUploadCall>()
        val client = DuetConnectionClient(
            requestText = { url, _, _ ->
                requestUrls += url
                NetworkResult.Success
            },
            requestTextBody = { url, _, _ ->
                requestUrls += url
                if (url.contains("/rr_connect?")) TextNetworkResult.Success("""{"err":0}""") else TextNetworkResult.Failure("unexpected")
            },
            sendRawBody = { _, _, _, _ -> NetworkResult.Failure("unused") },
            uploadRawFile = { url, method, _, _, _, _, contentType ->
                rawUploads += RawUploadCall(url.substringBefore("&time="), method, contentType)
                NetworkResult.Success
            }
        )

        val result = client.uploadGcode(
            profile = ProfileStoreRepository.fallbackPrinterProfile().copy(printHostApiKey = "reprap"),
            baseUrl = "http://duet.local",
            file = tempGcodeFile(),
            remoteFileName = "Benchy test.gcode",
            action = PrinterUploadAction.UploadAndStart,
            onProgress = {}
        )

        assertTrue(result.success)
        assertEquals(
            listOf(RawUploadCall("http://duet.local/rr_upload?name=0:/gcodes/Benchy%20test.gcode", "POST", "application/octet-stream")),
            rawUploads
        )
        assertTrue(requestUrls.any { it == "http://duet.local/rr_gcode?gcode=M32%20%220:/gcodes/Benchy%20test.gcode%22" })
        assertTrue(requestUrls.any { it == "http://duet.local/rr_disconnect" })
    }

    @Test
    fun dsfUploadUsesMachineFileAndStartsWithMachineCode() = runBlocking {
        val rawUploads = mutableListOf<RawUploadCall>()
        val startBodies = mutableListOf<String>()
        val client = DuetConnectionClient(
            requestText = { url, _, _ ->
                if (url.endsWith("/machine/status")) NetworkResult.Success else NetworkResult.Failure("unexpected")
            },
            requestTextBody = { url, _, _ ->
                if (url.contains("/rr_connect?")) TextNetworkResult.Failure("rrf unavailable") else TextNetworkResult.Failure("unexpected")
            },
            sendRawBody = { url, method, _, body ->
                assertEquals("http://duet.local/machine/code", url)
                assertEquals("POST", method)
                startBodies += body
                NetworkResult.Success
            },
            uploadRawFile = { url, method, _, _, _, _, contentType ->
                rawUploads += RawUploadCall(url, method, contentType)
                NetworkResult.Success
            }
        )

        val result = client.uploadGcode(
            profile = ProfileStoreRepository.fallbackPrinterProfile(),
            baseUrl = "http://duet.local",
            file = tempGcodeFile(),
            remoteFileName = "Benchy test.gcode",
            action = PrinterUploadAction.UploadAndStart,
            onProgress = {}
        )

        assertTrue(result.success)
        assertEquals(
            listOf(RawUploadCall("http://duet.local/machine/file/gcodes/Benchy%20test.gcode", "PUT", "application/octet-stream")),
            rawUploads
        )
        assertEquals(listOf("M32 \"0:/gcodes/Benchy test.gcode\""), startBodies)
    }

    private fun tempGcodeFile(): File =
        File.createTempFile("mobile-slicer-duet", ".gcode").apply {
            writeText("G28\n")
            deleteOnExit()
        }

    private data class RawUploadCall(
        val url: String,
        val method: String,
        val contentType: String
    )
}
