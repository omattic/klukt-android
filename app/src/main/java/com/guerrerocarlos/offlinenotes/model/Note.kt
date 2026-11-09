package com.guerrerocarlos.offlinenotes.model

import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import kotlinx.serialization.Serializable

@Serializable data class Genesis(
    val noteId: String,
    val value: Long,
    val issuedAt: Long,
    val ownerKey: String,
    val signature: String,
)

@Serializable data class Transfer(
    val fromKey: String,
    val toKey: String,
    val timestamp: Long,
    val nonce: String,
    val signature: String,
)

@Serializable data class SignedNote(val genesis: Genesis, val transfers: List<Transfer> = emptyList()) {
    val currentOwnerKey: String get() = transfers.lastOrNull()?.toKey ?: genesis.ownerKey
}

object NotePayload {
    fun genesis(noteId: String, value: Long, issuedAt: Long, owner: String) =
        "genesis|$noteId|$value|$issuedAt|$owner"
    fun transfer(note: SignedNote, from: String, to: String, timestamp: Long, nonce: String) =
        "transfer|${note.genesis.noteId}|${note.genesis.signature}|${note.transfers.lastOrNull()?.signature.orEmpty()}|$from|$to|$timestamp|$nonce"
}

object NoteVerifier {
    fun verify(note: SignedNote): String? {
        if (note.genesis.value <= 0) return "Value must be positive"
        if (!verify(note.genesis.ownerKey, NotePayload.genesis(note.genesis.noteId, note.genesis.value, note.genesis.issuedAt, note.genesis.ownerKey), note.genesis.signature)) return "Bad genesis signature"
        var owner = note.genesis.ownerKey
        note.transfers.forEachIndexed { index, transfer ->
            if (transfer.fromKey != owner) return "Transfer $index is not owned by its sender"
            if (!verify(owner, NotePayload.transfer(SignedNote(note.genesis, note.transfers.take(index)), transfer.fromKey, transfer.toKey, transfer.timestamp, transfer.nonce), transfer.signature)) return "Bad transfer $index signature"
            owner = transfer.toKey
        }
        return null
    }

    fun verify(publicKeyBase64: String, payload: String, signatureBase64: String): Boolean = try {
        val publicKey: PublicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)))
        Signature.getInstance("SHA256withECDSA").apply {
            initVerify(publicKey); update(payload.toByteArray(StandardCharsets.UTF_8))
        }.verify(Base64.getDecoder().decode(signatureBase64))
    } catch (_: Exception) { false }
}
