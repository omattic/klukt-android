package com.guerrerocarlos.offlinenotes.crypto

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class WalletKey(private val context: Context) {
    private val alias = "offline_notes_identity_v1"
    private fun store() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun entry(): KeyStore.PrivateKeyEntry {
        val keyStore = store()
        if (!keyStore.containsAlias(alias)) {
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
                initialize(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256).build())
            }.generateKeyPair()
        }
        return keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry
    }
    fun publicKey(): String = Base64.getEncoder().encodeToString(entry().certificate.publicKey.encoded)
    fun sign(payload: String): String = Base64.getEncoder().encodeToString(Signature.getInstance("SHA256withECDSA").apply {
        initSign(entry().privateKey); update(payload.toByteArray(StandardCharsets.UTF_8))
    }.sign())

    fun diagnostic(): String {
        val strongBoxFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        return try {
            val factory = KeyFactory.getInstance(entry().privateKey.algorithm, "AndroidKeyStore")
            val info = factory.getKeySpec(entry().privateKey, KeyInfo::class.java) as KeyInfo
            val level = if (Build.VERSION.SDK_INT >= 31) when (info.securityLevel) {
                KeyProperties.SECURITY_LEVEL_STRONGBOX -> "StrongBox"
                KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> "TEE"
                else -> "software/unknown"
            } else if (info.isInsideSecureHardware) "secure hardware" else "software"
            "API ${Build.VERSION.SDK_INT}; StrongBox feature: $strongBoxFeature; identity key: $level. No single-use claim."
        } catch (e: Exception) { "API ${Build.VERSION.SDK_INT}; StrongBox feature: $strongBoxFeature; key detail unavailable: ${e.javaClass.simpleName}" }
    }

    /** An experiment only. A usage-limited key is neither a trusted balance nor network-wide double-spend prevention. */
    fun tryCreateSingleUseExperiment(): String {
        if (Build.VERSION.SDK_INT < 31) return "Unsupported: usage-count APIs require Android 12 (API 31)."
        return try {
        val experimentAlias = "offline_notes_single_use_probe"
        store().deleteEntry(experimentAlias)
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
            initialize(KeyGenParameterSpec.Builder(experimentAlias, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1")).setDigests(KeyProperties.DIGEST_SHA256)
                .setMaxUsageCount(1).build())
        }.generateKeyPair()
        "Experimental usage-limited key created. Sign once from a controlled test, then inspect KeyInfo. This cannot prevent a conflicting note history."
        } catch (e: StrongBoxUnavailableException) { "Experiment unavailable: ${e.message}" } catch (e: Exception) { "Experiment unsupported: ${e.javaClass.simpleName}: ${e.message}" }
    }
}
