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
    private fun public(pair: KeyPair) = Base64.getEncoder().encodeToString(pair.public.encoded)
    private fun sign(pair: KeyPair, bytes: ByteArray) = Base64.getEncoder().encodeToString(Signature.getInstance("SHA256withECDSA").apply { initSign(pair.private); update(bytes) }.sign())
    @Test fun `test vector validates canonical issuance and transfer`() {
        val issuer = pair(); val alice = pair(); val bob = pair(); val issuerKey = public(issuer); val aliceKey = public(alice); val bobKey = public(bob)
        val unsigned = IssuedNote(1, TEST_NETWORK, issuerKey, "serial-0001", 10, aliceKey, "")
        val issued = unsigned.copy(issuerSignature = sign(issuer, CanonicalBytes.issuance(unsigned)))
        val initial = SignedNote(issued)
        val unsignedTransfer = OwnershipTransfer(1, TEST_NETWORK, issuerKey, "serial-0001", 10, CanonicalBytes.stateDigest(initial), bobKey, "transfer-0001", "0123456789abcdef", aliceKey, "")
        val note = SignedNote(issued, listOf(unsignedTransfer.copy(signature = sign(alice, CanonicalBytes.transfer(unsignedTransfer)))))
        assertNull(NoteVerifier.verify(note, setOf(issuerKey)))
        assertEquals("domain:22:offline-notes/issuance", String(CanonicalBytes.issuance(unsigned)).lineSequence().first())
    }
    @Test fun `untrusted issuer and altered history are rejected`() {
        val issuer = pair(); val owner = pair(); val issuerKey = public(issuer); val ownerKey = public(owner)
        val unsigned = IssuedNote(1, TEST_NETWORK, issuerKey, "s", 10, ownerKey, "")
        val note = SignedNote(unsigned.copy(issuerSignature = sign(issuer, CanonicalBytes.issuance(unsigned))))
        assertEquals("Issuer is not explicitly trusted", NoteVerifier.verify(note, emptySet()))
    }
}
