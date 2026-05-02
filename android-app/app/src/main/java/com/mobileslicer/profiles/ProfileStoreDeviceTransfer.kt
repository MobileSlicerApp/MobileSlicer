package com.mobileslicer.profiles

import org.json.JSONObject

internal const val PROFILE_STORE_DEVICE_TRANSFER_MIME_TYPE = "application/json"
internal const val PROFILE_STORE_DEVICE_TRANSFER_FILE_NAME = "mobileslicer-profiles.json"

private const val PROFILE_STORE_DEVICE_TRANSFER_SCHEMA = "com.mobileslicer.profile-store"
private const val PROFILE_STORE_DEVICE_TRANSFER_VERSION = 1

internal data class ProfileStoreDeviceImportResult(
    val store: ProfileStore,
    val printerCount: Int,
    val filamentCount: Int,
    val processCount: Int
)

internal fun ProfileStore.toDeviceTransferJson(includePrinterSecrets: Boolean = false): String = JSONObject()
    .put("schema", PROFILE_STORE_DEVICE_TRANSFER_SCHEMA)
    .put("version", PROFILE_STORE_DEVICE_TRANSFER_VERSION)
    .put("includesPrinterSecrets", includePrinterSecrets)
    .put("profileStore", (if (includePrinterSecrets) this else withoutPrinterSecrets()).toJsonObject())
    .toString(2)

internal fun ProfileStore.withoutPrinterSecrets(): ProfileStore =
    copy(
        printers = printers.map { printer ->
            printer.copy(
                printHostApiKey = "",
                printHostUser = "",
                printHostPassword = ""
            )
        }
    )

internal fun profileStoreFromDeviceTransferJson(text: String): ProfileStoreDeviceImportResult {
    val root = JSONObject(text)
    val payload = when {
        root.optString("schema") == PROFILE_STORE_DEVICE_TRANSFER_SCHEMA -> {
            require(root.optInt("version", 0) == PROFILE_STORE_DEVICE_TRANSFER_VERSION) {
                "Unsupported MobileSlicer profile export version."
            }
            root.getJSONObject("profileStore")
        }
        root.has("printers") || root.has("filaments") || root.has("processes") -> root
        else -> error("This file does not look like a MobileSlicer profile export.")
    }
    val store = payload.toProfileStoreOrDefault()
    return ProfileStoreDeviceImportResult(
        store = store,
        printerCount = store.printers.size,
        filamentCount = store.filaments.size,
        processCount = store.processes.size
    )
}
