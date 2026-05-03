package com.mobileslicer.storage

import android.content.SharedPreferences
import com.mobileslicer.profiles.ProfileStore
import com.mobileslicer.profiles.ProfileStoreRepository
import com.mobileslicer.profiles.newProcessProfileUnchecked
import com.mobileslicer.viewer.MeshBounds
import com.mobileslicer.viewer.ViewerModelTransform
import com.mobileslicer.workspace.ImportedModelFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavedProjectRepositoryTest {
    @Test
    fun persistAndLoadPreservesFullObjectTransform() {
        val preferences = FakeSharedPreferences()
        val transform = ViewerModelTransform(
            centerXmm = 42.5f,
            centerYmm = 84.25f,
            rotationXDegrees = 12f,
            rotationYDegrees = 24f,
            rotationZDegrees = 36f,
            uniformScale = 1.25f,
            orientationMatrix = listOf(
                0f, -1f, 0f,
                1f, 0f, 0f,
                0f, 0f, 1f
            )
        )
        val project = savedProject(transform = transform)

        SavedProjectRepository.persist(preferences, listOf(project))

        val loaded = SavedProjectRepository.load(preferences).single()
        val loadedTransform = loaded.plateObjects.single().transform
        assertEquals(transform.centerXmm, loadedTransform.centerXmm, 0.0001f)
        assertEquals(transform.centerYmm, loadedTransform.centerYmm, 0.0001f)
        assertEquals(transform.rotationXDegrees, loadedTransform.rotationXDegrees, 0.0001f)
        assertEquals(transform.rotationYDegrees, loadedTransform.rotationYDegrees, 0.0001f)
        assertEquals(transform.rotationZDegrees, loadedTransform.rotationZDegrees, 0.0001f)
        assertEquals(transform.uniformScale, loadedTransform.uniformScale, 0.0001f)
        assertEquals(transform.orientationMatrix, loadedTransform.orientationMatrix)
    }

    @Test
    fun persistDropsInvalidOrientationMatrix() {
        val preferences = FakeSharedPreferences()
        val project = savedProject(
            transform = ViewerModelTransform(
                centerXmm = 42.5f,
                centerYmm = 84.25f,
                orientationMatrix = listOf(1f, Float.NaN, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
            )
        )

        SavedProjectRepository.persist(preferences, listOf(project))

        val loadedTransform = SavedProjectRepository.load(preferences).single().plateObjects.single().transform
        assertNull(loadedTransform.orientationMatrix)
    }

    private fun savedProject(transform: ViewerModelTransform): SavedProject =
        SavedProject(
            id = "project_orientation_roundtrip",
            name = "Orientation Roundtrip",
            updatedAtEpochMs = 123L,
            profileStore = defaultStore(),
            plateObjects = listOf(
                SavedProjectPlateObject(
                    label = "Oriented Model",
                    filePath = "/tmp/oriented-model.stl",
                    nativeSourceKey = "native-source-1",
                    filamentSlotIndex = 1,
                    format = ImportedModelFormat.Stl,
                    bounds = MeshBounds(
                        minX = 0f,
                        maxX = 10f,
                        minY = 0f,
                        maxY = 20f,
                        minZ = 0f,
                        maxZ = 30f
                    ),
                    transform = transform
                )
            )
        )

    private fun defaultStore(): ProfileStore {
        val printers = listOf(ProfileStoreRepository.fallbackPrinterProfile())
        val filaments = ProfileStoreRepository.defaultFilamentProfiles()
        val processes = listOf(
            newProcessProfileUnchecked(
                0 to "process_saved_project_repository",
                1 to "Saved Project Repository Process",
                3 to false,
                5 to 0.20f,
                259 to printers.first().id,
                261 to printers.first().nozzleDiameterMm
            )
        )
        return ProfileStore(
            printers = printers,
            filaments = filaments,
            processes = processes,
            selectedPrinterId = printers.first().id,
            selectedFilamentId = filaments.first().id,
            selectedProcessId = processes.first().id
        )
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, String?>()

        override fun getString(key: String?, defValue: String?): String? = values[key] ?: defValue
        override fun edit(): SharedPreferences.Editor = FakeEditor(values)

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String?, defValue: Int): Int = defValue
        override fun getLong(key: String?, defValue: Long): Long = defValue
        override fun getFloat(key: String?, defValue: Float): Float = defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = defValue
        override fun contains(key: String?): Boolean = values.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    }

    private class FakeEditor(
        private val values: MutableMap<String, String?>
    ) : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, String?>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = value
        }

        override fun clear(): SharedPreferences.Editor = apply { clear = true }

        override fun apply() {
            commit()
        }

        override fun commit(): Boolean {
            if (clear) values.clear()
            values.putAll(pending)
            pending.clear()
            clear = false
            return true
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
        override fun remove(key: String?): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = null
        }
    }
}
