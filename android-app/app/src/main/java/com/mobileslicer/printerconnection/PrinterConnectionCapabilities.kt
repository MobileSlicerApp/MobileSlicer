package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.PrintHostType
import com.mobileslicer.profiles.PrinterProfile

internal enum class PrinterConnectionUploadRoute {
    HttpPrintHost,
    BambuLanAgent,
    ExternalImport
}

internal enum class PrinterConnectionField {
    PrinterAgent,
    Host,
    WebUi,
    Authorization,
    ApiKey,
    PathOrPort,
    Group,
    CaFile,
    User,
    Password,
    SslRevoke,
    BambuBedType,
    BambuUseAms,
    BambuAmsMapping,
    BambuNozzleMapping,
    BambuBedLeveling,
    BambuFlowCalibration,
    BambuVibrationCalibration,
    BambuTimelapse
}

internal data class PrinterConnectionFieldSpec(
    val field: PrinterConnectionField,
    val label: String,
    val visible: Boolean = true,
    val required: Boolean = false,
    val helperText: String = ""
)

internal data class PrinterConnectionCapabilities(
    val canTest: Boolean = true,
    val canFetchStatus: Boolean = true,
    val canUpload: Boolean = true,
    val canUploadAndStart: Boolean = true,
    val canQueue: Boolean = false,
    val canBrowsePrinters: Boolean = false,
    val canBrowseStorage: Boolean = false,
    val canBrowseGroups: Boolean = false,
    val browseTargetsLabel: String? = null,
    val browseGroupsLabel: String? = null,
    val requiresApiKeyOrToken: Boolean = false,
    val requiresUserPassword: Boolean = false,
    val requiresDeviceSerial: Boolean = false,
    val uploadRoute: PrinterConnectionUploadRoute = PrinterConnectionUploadRoute.HttpPrintHost,
    val implementationNote: String = ""
)

internal fun PrinterProfile.connectionCapabilities(): PrinterConnectionCapabilities =
    printHostType.connectionCapabilities()

internal fun PrintHostType.connectionCapabilities(): PrinterConnectionCapabilities =
    when (this) {
        PrintHostType.PrusaLink -> PrinterConnectionCapabilities(
            canBrowseStorage = true,
            browseTargetsLabel = "Browse Storage",
            requiresApiKeyOrToken = true,
            requiresUserPassword = true,
            implementationNote = "Orca PrusaLink path: version probe, storage browse, PUT or multipart upload."
        )
        PrintHostType.PrusaConnect -> PrinterConnectionCapabilities(
            canQueue = true,
            requiresApiKeyOrToken = true,
            implementationNote = "Orca PrusaConnect path: PrusaLink family upload with to_print/to_queue fields."
        )
        PrintHostType.OctoPrint -> PrinterConnectionCapabilities(
            requiresApiKeyOrToken = true,
            implementationNote = "Orca OctoPrint host plus Mobile Slicer Moonraker probe fallback."
        )
        PrintHostType.Duet -> PrinterConnectionCapabilities(
            requiresApiKeyOrToken = true,
            implementationNote = "Orca Duet path: classic RRF plus DSF fallback."
        )
        PrintHostType.FlashAir -> PrinterConnectionCapabilities(
            canUploadAndStart = false,
            implementationNote = "Orca FlashAir path supports upload to card, not remote print start."
        )
        PrintHostType.AstroBox -> PrinterConnectionCapabilities(
            requiresApiKeyOrToken = true,
            implementationNote = "Orca AstroBox path: OctoPrint-style local API."
        )
        PrintHostType.Repetier -> PrinterConnectionCapabilities(
            canBrowsePrinters = true,
            canBrowseGroups = true,
            browseTargetsLabel = "Browse Printers",
            browseGroupsLabel = "Browse Groups",
            requiresApiKeyOrToken = true,
            implementationNote = "Orca Repetier path: selected printer slug plus optional model group."
        )
        PrintHostType.Mks -> PrinterConnectionCapabilities(
            implementationNote = "Orca MKS path: HTTP upload plus TCP console start."
        )
        PrintHostType.Esp3d -> PrinterConnectionCapabilities(
            implementationNote = "Orca ESP3D path: upload_serial plus M23/M24 start."
        )
        PrintHostType.CrealityPrint -> PrinterConnectionCapabilities(
            requiresApiKeyOrToken = true,
            implementationNote = "Orca CrealityPrint path: bearer auth, multipart upload, WebSocket start."
        )
        PrintHostType.Obico -> PrinterConnectionCapabilities(
            canBrowsePrinters = true,
            browseTargetsLabel = "Browse Printers",
            requiresApiKeyOrToken = true,
            implementationNote = "Orca Obico path: cloud API token and selected printer id."
        )
        PrintHostType.Flashforge -> PrinterConnectionCapabilities(
            implementationNote = "Orca Flashforge path: TCP console file transfer and start commands."
        )
        PrintHostType.SimplyPrint -> PrinterConnectionCapabilities(
            canUploadAndStart = false,
            canQueue = true,
            requiresApiKeyOrToken = true,
            uploadRoute = PrinterConnectionUploadRoute.ExternalImport,
            implementationNote = "Orca SimplyPrint path: temp upload then import URL, no direct local start."
        )
        PrintHostType.ElegooLink -> PrinterConnectionCapabilities(
            requiresApiKeyOrToken = true,
            implementationNote = "Orca Elegoo Link path: OctoPrint fallback or native chunk/WebSocket flow."
        )
        PrintHostType.BambuLan -> PrinterConnectionCapabilities(
            requiresApiKeyOrToken = true,
            requiresDeviceSerial = true,
            uploadRoute = PrinterConnectionUploadRoute.BambuLanAgent,
            implementationNote = "Orca BBL path: Bambu device agent using secure local MQTT plus FTP/FTPS."
        )
    }

internal fun PrintHostType.connectionFieldSpecs(): List<PrinterConnectionFieldSpec> {
    val capabilities = connectionCapabilities()
    val isBambu = this == PrintHostType.BambuLan
    val isSimplyPrint = this == PrintHostType.SimplyPrint
    fun standardAuthVisible(): Boolean = !isBambu && !isSimplyPrint
    return listOf(
        PrinterConnectionFieldSpec(
            field = PrinterConnectionField.PrinterAgent,
            label = when (this) {
                PrintHostType.Mks,
                PrintHostType.Flashforge -> "Printer Agent / protocol hint"
                else -> "Printer Agent"
            },
            visible = !isBambu && !isSimplyPrint
        ),
        PrinterConnectionFieldSpec(
            field = PrinterConnectionField.Host,
            label = when (this) {
                PrintHostType.BambuLan -> "Bambu LAN IP or hostname"
                PrintHostType.SimplyPrint -> "SimplyPrint panel URL"
                else -> "Hostname, IP or URL"
            },
            required = this !in setOf(PrintHostType.PrusaConnect, PrintHostType.Obico, PrintHostType.SimplyPrint)
        ),
        PrinterConnectionFieldSpec(
            field = PrinterConnectionField.WebUi,
            label = "Printer web UI URL",
            visible = !isBambu
        ),
        PrinterConnectionFieldSpec(
            field = PrinterConnectionField.Authorization,
            label = "Authentication",
            visible = standardAuthVisible()
        ),
        PrinterConnectionFieldSpec(
            field = PrinterConnectionField.ApiKey,
            label = when (this) {
                PrintHostType.BambuLan -> "LAN access code"
                PrintHostType.SimplyPrint -> "Access token"
                else -> "API key / token"
            },
            visible = capabilities.requiresApiKeyOrToken || isBambu || isSimplyPrint,
            required = capabilities.requiresApiKeyOrToken
        ),
        PrinterConnectionFieldSpec(
            field = PrinterConnectionField.PathOrPort,
            label = when (this) {
                PrintHostType.BambuLan -> "Device serial / dev id"
                PrintHostType.PrusaLink -> "Storage path"
                PrintHostType.Repetier -> "Printer slug"
                PrintHostType.Obico -> "Printer id"
                PrintHostType.Mks,
                PrintHostType.Flashforge -> "TCP console port"
                else -> "Printer path or port"
            },
            visible = this !in setOf(PrintHostType.SimplyPrint),
            required = capabilities.requiresDeviceSerial
        ),
        PrinterConnectionFieldSpec(
            field = PrinterConnectionField.Group,
            label = "Model group",
            visible = capabilities.canBrowseGroups
        ),
        PrinterConnectionFieldSpec(
            field = PrinterConnectionField.CaFile,
            label = "HTTPS CA File",
            visible = standardAuthVisible()
        ),
        PrinterConnectionFieldSpec(
            field = PrinterConnectionField.User,
            label = "User",
            visible = standardAuthVisible() && (capabilities.requiresUserPassword || this == PrintHostType.PrusaLink)
        ),
        PrinterConnectionFieldSpec(
            field = PrinterConnectionField.Password,
            label = "Password",
            visible = standardAuthVisible() && (capabilities.requiresUserPassword || this == PrintHostType.PrusaLink)
        ),
        PrinterConnectionFieldSpec(
            field = PrinterConnectionField.SslRevoke,
            label = "Ignore HTTPS certificate revocation checks",
            visible = standardAuthVisible()
        ),
        PrinterConnectionFieldSpec(PrinterConnectionField.BambuBedType, "Bambu bed type", visible = isBambu),
        PrinterConnectionFieldSpec(PrinterConnectionField.BambuUseAms, "Use AMS", visible = isBambu),
        PrinterConnectionFieldSpec(PrinterConnectionField.BambuAmsMapping, "AMS mapping", visible = isBambu),
        PrinterConnectionFieldSpec(PrinterConnectionField.BambuNozzleMapping, "Nozzle mapping", visible = isBambu),
        PrinterConnectionFieldSpec(PrinterConnectionField.BambuBedLeveling, "Bed leveling", visible = isBambu),
        PrinterConnectionFieldSpec(PrinterConnectionField.BambuFlowCalibration, "Flow calibration", visible = isBambu),
        PrinterConnectionFieldSpec(PrinterConnectionField.BambuVibrationCalibration, "Vibration calibration", visible = isBambu),
        PrinterConnectionFieldSpec(PrinterConnectionField.BambuTimelapse, "Timelapse", visible = isBambu)
    )
}
