package com.guerrerocarlos.offlinenotes.model

import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import kotlinx.serialization.Serializable

const val PROTOCOL_VERSION = 1
const val TEST_NETWORK = "offline-notes-test-v1"
const val TRANSFER_DOMAIN = "offline-notes/ownership-transfer"

@Serializable data class IssuedNote(val protocolVersion: Int, val network: String, val issuerId: String, val serial: String, val denomination: Long, val initialOwnerKey: String, val issuerSignature: String)
@Serializable data class OwnershipTransfer(val protocolVersion: Int, val network: String, val issuerId: String, val serial: String, val denomination: Long, val previousStateDigest: String, val recipientKey: String, val transferId: String, val recipientNonce: String, val senderKey: String, val signature: String)
@Serializable data class SignedNote(val issued: IssuedNote, val transfers: List<OwnershipTransfer> = emptyList()) {
    val noteId: String get() = "${issued.issuerId}:${issued.serial}"
    val currentOwnerKey: String get() = transfers.lastOrNull()?.recipientKey ?: issued.initialOwnerKey
}

/** Canonical UTF-8 bytes, never JSON. Values are byte-length prefixed to avoid delimiter ambiguity. */
object CanonicalBytes {
    private fun field(name: String, value: String) = "$name:${value.toByteArray(StandardCharsets.UTF_8).size}:$value\n"
    private fun record(domain: String, fields: List<Pair<String, String>>) = buildString { append(field("domain", domain)); fields.forEach { append(field(it.first, it.second)) } }.toByteArray(StandardCharsets.UTF_8)
    fun issuance(n: IssuedNote) = record("offline-notes/issuance", listOf("protocolVersion" to n.protocolVersion.toString(), "network" to n.network, "issuerId" to n.issuerId, "serial" to n.serial, "denomination" to n.denomination.toString(), "initialOwnerKey" to n.initialOwnerKey))
    fun transfer(t: OwnershipTransfer) = record(TRANSFER_DOMAIN, listOf("protocolVersion" to t.protocolVersion.toString(), "network" to t.network, "issuerId" to t.issuerId, "serial" to t.serial, "denomination" to t.denomination.toString(), "previousStateDigest" to t.previousStateDigest, "recipientKey" to t.recipientKey, "transferId" to t.transferId, "recipientNonce" to t.recipientNonce, "senderKey" to t.senderKey))
    fun stateDigest(note: SignedNote): String = Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest((Base64.getEncoder().encodeToString(issuance(note.issued)) + "|" + note.issued.issuerSignature + "|" + note.transfers.joinToString("|") { it.signature }).toByteArray(StandardCharsets.UTF_8)))
}

object NoteVerifier {
    fun verify(note: SignedNote, trustedIssuerKeys: Set<String>): String? {
        val n = note.issued
        if (n.protocolVersion != PROTOCOL_VERSION || n.network != TEST_NETWORK) return "Wrong protocol or test network"
        if (n.serial.isBlank() || n.denomination <= 0 || n.denomination > 1_000_000L) return "Malformed note"
        if (n.issuerId !in trustedIssuerKeys) return "Issuer is not explicitly trusted"
        if (!verify(n.issuerId, CanonicalBytes.issuance(n), n.issuerSignature)) return "Invalid issuer signature"
        var owner = n.initialOwnerKey; var prefix = SignedNote(n); val seen = mutableSetOf<String>()
        note.transfers.forEachIndexed { index, t ->
            if (!seen.add(t.transferId) || t.protocolVersion != PROTOCOL_VERSION || t.network != TEST_NETWORK || t.issuerId != n.issuerId || t.serial != n.serial || t.denomination != n.denomination) return "Malformed transfer $index"
            if (t.senderKey != owner || t.previousStateDigest != CanonicalBytes.stateDigest(prefix) || t.recipientNonce.length < 16) return "Altered ownership history at transfer $index"
            if (!verify(owner, CanonicalBytes.transfer(t), t.signature)) return "Invalid transfer $index signature"
            owner = t.recipientKey; prefix = SignedNote(n, note.transfers.take(index + 1))
        }
        return null
    }
    fun verify(publicKeyBase64: String, bytes: ByteArray, signatureBase64: String): Boolean = try {
        val publicKey: PublicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)))
        Signature.getInstance("SHA256withECDSA").apply { initVerify(publicKey); update(bytes) }.verify(Base64.getDecoder().decode(signatureBase64))
    } catch (_: Exception) { false }
}
