package com.mobileslicer.profiles

internal fun PrinterProfile.nativeConfigSignatureHash(): Int =
    toJson().canonicalConfigSignatureHash()

internal fun FilamentProfile.nativeConfigSignatureHash(): Int =
    toJson().canonicalConfigSignatureHash()

internal fun ProcessProfile.nativeConfigSignatureHash(): Int =
    toJson().canonicalConfigSignatureHash()
