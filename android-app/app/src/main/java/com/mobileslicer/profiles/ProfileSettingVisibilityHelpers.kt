package com.mobileslicer.profiles

internal fun ProfileEditorSetting.isVisible(showAdvancedProfileSettings: Boolean): Boolean =
    visibility == ProfileSettingVisibility.Simple || showAdvancedProfileSettings

internal fun profileEditorOrcaKeysForVisibility(visibility: ProfileSettingVisibility): Set<String> =
    ProfileEditorSetting.entries
        .filter { it.visibility == visibility }
        .flatMap { it.orcaKeys }
        .toSet()
