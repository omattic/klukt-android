package com.guerrerocarlos.offlinenotes.data

import androidx.room.withTransaction
import com.guerrerocarlos.offlinenotes.crypto.WalletKey
import com.guerrerocarlos.offlinenotes.model.CanonicalBytes
import com.guerrerocarlos.offlinenotes.model.NoteVerifier
import com.guerrerocarlos.offlinenotes.model.OwnershipTransfer
import com.guerrerocarlos.offlinenotes.model.SignedNote
import com.guerrerocarlos.offlinenotes.model.PROTOCOL_VERSION
import com.guerrerocarlos.offlinenotes.model.TEST_NETWORK
import java.security.SecureRandom
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WalletRepository(private val db: WalletDatabase, private val key: WalletKey) {
    private val json = Json { ignoreUnknownKeys = false }
    fun held(): Flow<List<NoteEntity>> = db.notes().held()
    suspend fun trustIssuer(publicKey: String, label: String) {
        check(NoteVerifier.verify(publicKey, "probe".toByteArray(), "invalid") == false) { "Impossible" }
        db.notes().trustIssuer(TrustedIssuerEntity(publicKey.trim(), label.ifBlank { "Imported test issuer" }, System.currentTimeMillis()))
    }
    suspend fun importProvisionedNote(rawJson: String) {
        val note = json.decodeFromString<SignedNote>(rawJson)
        receive(note)
    }
    suspend fun prepareTransfer(id: String, recipientKey: String, recipientNonce: String): SignedNote = db.withTransaction {
        val source = requireNotNull(db.notes().byId(id)) { "Unknown note" }; check(source.held) { "Note is outgoing or already transferred" }
        val note = json.decodeFromString<SignedNote>(source.json); val trusted = db.notes().trustedIssuerKeys().toSet()
        check(NoteVerifier.verify(note, trusted) == null) { "Stored note fails validation" }; check(note.currentOwnerKey == key.publicKey()) { "This wallet is not the owner" }
        require(note.transfers.size < 64) { "Note history limit reached" }
        val t = OwnershipTransfer(PROTOCOL_VERSION, TEST_NETWORK, note.issued.issuerId, note.issued.serial, note.issued.denomination, CanonicalBytes.stateDigest(note), recipientKey, UUID.randomUUID().toString(), recipientNonce, key.publicKey(), "")
        val signed = t.copy(signature = key.sign(CanonicalBytes.transfer(t)))
        val payload = note.copy(transfers = note.transfers + signed)
        val wire = json.encodeToString(payload)
        val digest = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(wire.toByteArray()))
        check(db.notes().markTransferred(id) == 1) { "Note changed while reserving" }
        db.notes().insertOutbox(OutboxEntity(signed.transferId, wire, digest, "SIGNED", 0, System.currentTimeMillis()))
        payload
    }
    suspend fun markTransferred(id: String) = db.withTransaction { check(db.notes().markTransferred(id) == 1) { "Note changed while sending" } }
    suspend fun receive(note: SignedNote) = db.withTransaction {
        val trusted = db.notes().trustedIssuerKeys().toSet(); check(NoteVerifier.verify(note, trusted) == null) { "Rejected note: untrusted issuer or invalid history" }
        check(note.currentOwnerKey == key.publicKey()) { "Rejected note: addressed to a different wallet" }
        db.notes().insert(NoteEntity(note.noteId, json.encodeToString(note), true, System.currentTimeMillis()))
    }
    fun testNonce(): String = ByteArray(32).also { SecureRandom().nextBytes(it) }.let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
}
