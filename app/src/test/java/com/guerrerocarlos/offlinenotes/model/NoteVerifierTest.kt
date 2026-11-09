package com.guerrerocarlos.offlinenotes.model

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteVerifierTest {
    private fun pair() = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
    private fun encoded(pair: KeyPair) = Base64.getEncoder().encodeToString(pair.public.encoded)
    private fun sign(pair: KeyPair, text: String) = Base64.getEncoder().encodeToString(Signature.getInstance("SHA256withECDSA").apply { initSign(pair.private); update(text.toByteArray()) }.sign())
    @Test fun `valid ownership history verifies`() {
        val alice = pair(); val bob = pair(); val a = encoded(alice); val b = encoded(bob)
        val g = Genesis("n1", 10, 1, a, sign(alice, NotePayload.genesis("n1", 10, 1, a)))
        val unsigned = SignedNote(g); val t = Transfer(a, b, 2, "nonce", sign(alice, NotePayload.transfer(unsigned, a, b, 2, "nonce")))
        assertNull(NoteVerifier.verify(SignedNote(g, listOf(t))))
    }
    @Test fun `tampered recipient is rejected`() {
        val alice = pair(); val a = encoded(alice); val g = Genesis("n1", 10, 1, a, sign(alice, NotePayload.genesis("n1", 10, 1, a)))
        val t = Transfer(a, "tampered", 2, "nonce", sign(alice, NotePayload.transfer(SignedNote(g), a, "original", 2, "nonce")))
        assertEquals("Bad transfer 0 signature", NoteVerifier.verify(SignedNote(g, listOf(t))))
    }
}
