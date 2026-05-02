package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.ProfileStoreRepository
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OctoKlipperConnectionClientTest {
    @Test
    fun octoPrintUploadAndStartUsesOrcaFileEndpointAndPrintField() = runBlocking {
        val uploads = mutableListOf<UploadCall>()
        val client = OctoKlipperConnectionClient(
            requestText = { url, _, _ ->
                when {
                    url.endsWith("/api/version") -> NetworkResult.Success
                    else -> NetworkResult.Failure("unexpected request")
                }
            },
            requestTextBody = { _, _, _ -> TextNetworkResult.Failure("unused") },
            uploadMultipart = { url, headers, fields, _, fileFieldName, remoteFileName, _ ->
                uploads += UploadCall(url, headers, fields, fileFieldName, remoteFileName)
                NetworkResult.Success
            }
        )

        val result = client.uploadGcode(
            profile = ProfileStoreRepository.fallbackPrinterProfile(),
            baseUrl = "http://octopi.local:80",
            file = tempGcodeFile(),
            remoteFileName = "orca-parity.gcode",
            action = PrinterUploadAction.UploadAndStart,
            onProgress = {}
        )

        assertTrue(result.success)
        assertEquals(
            listOf(
                UploadCall(
                    url = "http://octopi.local:80/api/files/local",
                    headers = emptyMap(),
                    fields = mapOf("print" to "true"),
                    fileFieldName = "file",
                    remoteFileName = "orca-parity.gcode"
                )
            ),
            uploads
        )
    }

    @Test
    fun moonrakerUploadUsesServerFilesUploadRootAndPrintFalse() = runBlocking {
        val uploads = mutableListOf<UploadCall>()
        val client = OctoKlipperConnectionClient(
            requestText = { url, _, _ ->
                when {
                    url.endsWith("/api/version") -> NetworkResult.Failure("not octoprint")
                    url.endsWith("/server/info") -> NetworkResult.Success
                    else -> NetworkResult.Failure("unexpected request")
                }
            },
            requestTextBody = { _, _, _ -> TextNetworkResult.Failure("unused") },
            uploadMultipart = { url, headers, fields, _, fileFieldName, remoteFileName, _ ->
                uploads += UploadCall(url, headers, fields, fileFieldName, remoteFileName)
                NetworkResult.Success
            }
        )

        val result = client.uploadGcode(
            profile = ProfileStoreRepository.fallbackPrinterProfile(),
            baseUrl = "http://192.168.1.42:7125",
            file = tempGcodeFile(),
            remoteFileName = "orca-parity.gcode",
            action = PrinterUploadAction.UploadOnly,
            onProgress = {}
        )

        assertTrue(result.success)
        assertEquals(
            listOf(
                UploadCall(
                    url = "http://192.168.1.42:7125/server/files/upload",
                    headers = emptyMap(),
                    fields = mapOf("root" to "gcodes", "print" to "false"),
                    fileFieldName = "file",
                    remoteFileName = "orca-parity.gcode"
                )
            ),
            uploads
        )
    }

    @Test
    fun moonrakerUploadAndStartQuotesFilenameInStartCommand() = runBlocking {
        val requestUrls = mutableListOf<String>()
        val uploadedNames = mutableListOf<String>()
        val client = OctoKlipperConnectionClient(
            requestText = { url, _, _ ->
                requestUrls += url
                when {
                    url.endsWith("/api/version") -> NetworkResult.Failure("not octoprint")
                    url.endsWith("/server/info") -> NetworkResult.Success
                    "/printer/gcode/script?" in url -> NetworkResult.Success
                    else -> NetworkResult.Failure("unexpected request")
                }
            },
            requestTextBody = { _, _, _ -> TextNetworkResult.Failure("unused") },
            uploadMultipart = { _, _, _, _, _, remoteFileName, _ ->
                uploadedNames += remoteFileName
                NetworkResult.Success
            }
        )
        val result = client.uploadGcode(
            profile = ProfileStoreRepository.fallbackPrinterProfile(),
            baseUrl = "http://192.168.1.42:7125",
            file = tempGcodeFile(),
            remoteFileName = "3DBenchy test.gcode",
            action = PrinterUploadAction.UploadAndStart,
            onProgress = {}
        )

        assertTrue(result.success)
        assertEquals(listOf("3DBenchy test.gcode"), uploadedNames)
        val startUrl = requestUrls.first { "/printer/gcode/script?" in it }
        assertTrue(startUrl.contains("SDCARD_PRINT_FILE%20FILENAME%3D%223DBenchy%20test.gcode%22"))
    }

    @Test
    fun moonrakerStatusParsesStructuredRuntimeFields() {
        val client = OctoKlipperConnectionClient(
            requestText = { _, _, _ -> NetworkResult.Failure("unused") },
            requestTextBody = { url, _, _ ->
                when {
                    url.contains("/printer/objects/query") -> TextNetworkResult.Success(
                        """
                        {
                          "result": {
                            "status": {
                              "print_stats": {
                                "state": "printing",
                                "filename": "3DBenchy.gcode"
                              },
                              "virtual_sdcard": { "progress": 0.42 },
                              "extruder": { "temperature": 214.7, "target": 215.0 },
                              "heater_bed": { "temperature": 59.9, "target": 60.0 }
                            }
                          }
                        }
                        """.trimIndent()
                    )
                    else -> TextNetworkResult.Failure("unexpected request")
                }
            },
            uploadMultipart = { _, _, _, _, _, _, _ -> NetworkResult.Failure("unused") }
        )

        val result = client.fetchStatus(
            profile = ProfileStoreRepository.fallbackPrinterProfile(),
            baseUrl = "http://192.168.1.42:7125"
        )

        assertTrue(result.success)
        assertEquals("State: printing • File: 3DBenchy.gcode • Progress: 42% • Nozzle: 214C • Bed: 59C", result.detail)
        val status = result.runtimeStatus
        assertNotNull(status)
        checkNotNull(status)
        assertEquals(PrinterState.Printing, status.state)
        assertEquals(42, status.progressPercent)
        assertEquals("3DBenchy.gcode", status.currentFile)
        assertEquals("moonraker", status.hostType)
        assertEquals(2, status.temperatures.size)
        assertEquals(215.0, checkNotNull(status.temperatures.first().targetCelsius), 0.01)
    }

    @Test
    fun octoPrintStatusParsesStructuredRuntimeFieldsAfterMoonrakerProbeFails() {
        val client = OctoKlipperConnectionClient(
            requestText = { _, _, _ -> NetworkResult.Failure("unused") },
            requestTextBody = { url, _, _ ->
                when {
                    url.contains("/printer/objects/query") -> TextNetworkResult.Failure("not moonraker")
                    url.endsWith("/api/job") -> TextNetworkResult.Success(
                        """
                        {
                          "state": "Operational",
                          "job": {
                            "file": { "name": "calibration_cube.gcode" }
                          },
                          "progress": { "completion": 12.8 }
                        }
                        """.trimIndent()
                    )
                    else -> TextNetworkResult.Failure("unexpected request")
                }
            },
            uploadMultipart = { _, _, _, _, _, _, _ -> NetworkResult.Failure("unused") }
        )

        val result = client.fetchStatus(
            profile = ProfileStoreRepository.fallbackPrinterProfile(),
            baseUrl = "http://octopi.local"
        )

        assertTrue(result.success)
        assertEquals("State: Operational • File: calibration_cube.gcode • Progress: 12%", result.detail)
        val status = result.runtimeStatus
        assertNotNull(status)
        checkNotNull(status)
        assertEquals(PrinterState.Idle, status.state)
        assertEquals(12, status.progressPercent)
        assertEquals("calibration_cube.gcode", status.currentFile)
        assertEquals("octoprint", status.hostType)
    }

    private fun tempGcodeFile(): File =
        File.createTempFile("mobile-slicer-printer-upload", ".gcode").apply {
            writeText("G28\n")
            deleteOnExit()
        }

    private data class UploadCall(
        val url: String,
        val headers: Map<String, String>,
        val fields: Map<String, String>,
        val fileFieldName: String,
        val remoteFileName: String
    )
}
