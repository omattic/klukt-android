package com.guerrerocarlos.offlinenotes.data

import androidx.room.withTransaction
import com.guerrerocarlos.offlinenotes.crypto.WalletKey
import com.guerrerocarlos.offlinenotes.model.Genesis
import com.guerrerocarlos.offlinenotes.model.NotePayload
import com.guerrerocarlos.offlinenotes.model.NoteVerifier
import com.guerrerocarlos.offlinenotes.model.SignedNote
import com.guerrerocarlos.offlinenotes.model.Transfer
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class WalletRepository(private val db: WalletDatabase, private val key: WalletKey) {
    private val json = Json { ignoreUnknownKeys = false }
    fun held(): Flow<List<NoteEntity>> = db.notes().held()
    suspend fun mint(value: Long): SignedNote {
        require(value > 0)
        val owner = key.publicKey(); val now = System.currentTimeMillis(); val id = UUID.randomUUID().toString()
        val genesis = Genesis(id, value, now, owner, key.sign(NotePayload.genesis(id, value, now, owner)))
        val note = SignedNote(genesis)
        db.notes().insert(NoteEntity(id, json.encodeToString(note), true, now))
        return note
    }
    suspend fun prepareTransfer(id: String, recipientKey: String): SignedNote = db.withTransaction {
        val source = requireNotNull(db.notes().byId(id)) { "Unknown note" }
        check(source.held) { "Note has already been transferred locally" }
        val note = json.decodeFromString<SignedNote>(source.json)
        check(NoteVerifier.verify(note) == null) { "Stored note has an invalid history" }
        check(note.currentOwnerKey == key.publicKey()) { "This wallet is not current owner" }
        val now = System.currentTimeMillis(); val nonce = UUID.randomUUID().toString()
        val payload = NotePayload.transfer(note, key.publicKey(), recipientKey, now, nonce)
        note.copy(transfers = note.transfers + Transfer(key.publicKey(), recipientKey, now, nonce, key.sign(payload)))
    }
    suspend fun markTransferred(id: String) = db.withTransaction {
        check(db.notes().markTransferred(id) == 1) { "Note changed while sending" }
    }
    suspend fun receive(note: SignedNote) = db.withTransaction {
        check(NoteVerifier.verify(note) == null) { "Rejected note: invalid signature history" }
        check(note.currentOwnerKey == key.publicKey()) { "Rejected note: addressed to a different wallet" }
        db.notes().insert(NoteEntity(note.genesis.noteId, json.encodeToString(note), true, System.currentTimeMillis()))
    }
}
