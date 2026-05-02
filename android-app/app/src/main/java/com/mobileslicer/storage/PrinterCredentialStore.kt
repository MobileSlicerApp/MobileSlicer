package com.mobileslicer.storage

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.mobileslicer.profiles.ProfileStore
import com.mobileslicer.profiles.PrinterProfile
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

internal class PrinterCredentialStore private constructor(
    private val preferences: SharedPreferences
) {
    fun hydrate(store: ProfileStore): ProfileStore =
        store.copy(printers = store.printers.map(::hydratePrinter))

    fun hydrateProjects(projects: List<SavedProject>): List<SavedProject> =
        projects.map { project -> project.copy(profileStore = hydrate(project.profileStore)) }

    fun persistSecrets(store: ProfileStore) {
        val editor = preferences.edit()
        store.printers.forEach { printer ->
            val key = preferenceKey(printer.id)
            if (printer.hasPrinterSecret()) {
                val secretJson = JSONObject()
                    .put("apiKey", printer.printHostApiKey)
                    .put("user", printer.printHostUser)
                    .put("password", printer.printHostPassword)
                encrypt(secretJson.toString())?.let { encrypted ->
                    editor.putString(key, encrypted)
                } ?: editor.remove(key)
            } else {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    fun persistProjectSecrets(projects: List<SavedProject>) {
        projects.forEach { persistSecrets(it.profileStore) }
    }

    fun stripSecrets(store: ProfileStore): ProfileStore =
        store.copy(printers = store.printers.map { it.withoutPrinterSecrets() })

    fun stripProjectSecrets(projects: List<SavedProject>): List<SavedProject> =
        projects.map { project -> project.copy(profileStore = stripSecrets(project.profileStore)) }

    private fun hydratePrinter(printer: PrinterProfile): PrinterProfile {
        val encrypted = preferences.getString(preferenceKey(printer.id), null) ?: return printer
        val secretJson = decrypt(encrypted)?.let(::JSONObject) ?: return printer
        return printer.copy(
            printHostApiKey = secretJson.optString("apiKey", printer.printHostApiKey),
            printHostUser = secretJson.optString("user", printer.printHostUser),
            printHostPassword = secretJson.optString("password", printer.printHostPassword)
        )
    }

    private fun encrypt(plainText: String): String? =
        runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            JSONObject()
                .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .put("cipherText", Base64.encodeToString(cipherText, Base64.NO_WRAP))
                .toString()
        }.getOrNull()

    private fun decrypt(encrypted: String): String? =
        runCatching {
            val json = JSONObject(encrypted)
            val iv = Base64.decode(json.getString("iv"), Base64.NO_WRAP)
            val cipherText = Base64.decode(json.getString("cipherText"), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        }.getOrNull()

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun preferenceKey(printerId: String): String = "printer_credentials:$printerId"

    companion object {
        private const val PREFERENCES = "mobile_slicer_printer_credentials"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "mobile_slicer_printer_credentials_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128

        fun from(context: Context): PrinterCredentialStore =
            PrinterCredentialStore(context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE))
    }
}

private fun PrinterProfile.hasPrinterSecret(): Boolean =
    printHostApiKey.isNotBlank() || printHostUser.isNotBlank() || printHostPassword.isNotBlank()

private fun PrinterProfile.withoutPrinterSecrets(): PrinterProfile =
    copy(
        printHostApiKey = "",
        printHostUser = "",
        printHostPassword = ""
    )
