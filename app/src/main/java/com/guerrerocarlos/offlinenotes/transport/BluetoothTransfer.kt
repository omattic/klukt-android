package com.guerrerocarlos.offlinenotes.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import com.guerrerocarlos.offlinenotes.model.SignedNote
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.security.MessageDigest
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable private data class Hello(val key: String, val nonce: String, val receiveNonce: String)
@Serializable private data class WireNote(val note: SignedNote)

/** Paired-device RFCOMM transport. Bluetooth pairing is done in Android Settings, not silently by this app. */
class BluetoothTransfer(context: Context) {
    private val maxHandshakeBytes = 8 * 1024
    private val maxPaymentBytes = 256 * 1024
    private val adapter = (context.getSystemService(BluetoothManager::class.java)).adapter
    private val json = Json
    private val serviceId = UUID.fromString("5c9b6ee0-a9a3-4c56-ae13-8b3fe6a58c32")
    @SuppressLint("MissingPermission") fun pairedDevices(): List<BluetoothDevice> = adapter?.bondedDevices?.sortedBy { it.name ?: it.address } ?: emptyList()
    fun available(): Boolean = adapter?.isEnabled == true
    private fun code(one: Hello, two: Hello): String {
        val material = listOf(one.key, two.key, one.nonce, two.nonce).sorted().joinToString("|")
        val raw = MessageDigest.getInstance("SHA-256").digest(material.toByteArray())
        val n = ((raw[0].toInt() and 255) shl 16 or ((raw[1].toInt() and 255) shl 8) or (raw[2].toInt() and 255)) % 1_000_000
        return "%06d".format(n)
    }
    private fun readFrame(input: InputStream, limit: Int): String {
        val bytes = ArrayList<Byte>(minOf(limit, 1024)); while (true) { val b = input.read(); check(b >= 0) { "Peer disconnected" }; if (b == '\n'.code) break; check(bytes.size < limit) { "Peer frame too large" }; bytes += b.toByte() }
        return bytes.toByteArray().toString(Charsets.UTF_8)
    }
    @SuppressLint("MissingPermission") suspend fun receive(myKey: String, receiveNonce: String, confirmCode: suspend (String) -> Boolean): SignedNote = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        requireNotNull(adapter) { "Bluetooth unavailable" }.listenUsingRfcommWithServiceRecord("Offline Notes", serviceId).use { server ->
            server.accept(120_000).use { socket ->
                val writer = OutputStreamWriter(socket.outputStream)
                val peer = json.decodeFromString<Hello>(readFrame(socket.inputStream, maxHandshakeBytes)); val me = Hello(myKey, UUID.randomUUID().toString(), receiveNonce)
                writer.write(json.encodeToString(me) + "\n"); writer.flush()
                check(confirmCode(code(peer, me))) { "Pairing code was not confirmed" }
                writer.write("OK\n"); writer.flush()
                json.decodeFromString<WireNote>(readFrame(socket.inputStream, maxPaymentBytes)).note
            }
        }
    }
    @SuppressLint("MissingPermission") suspend fun send(device: BluetoothDevice, noteFor: suspend (String, String) -> SignedNote, myKey: String, confirmCode: suspend (String) -> Boolean) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        device.createRfcommSocketToServiceRecord(serviceId).use { socket ->
            socket.connect()
            val writer = OutputStreamWriter(socket.outputStream)
            val me = Hello(myKey, UUID.randomUUID().toString(), ""); writer.write(json.encodeToString(me) + "\n"); writer.flush()
            val peer = json.decodeFromString<Hello>(readFrame(socket.inputStream, maxHandshakeBytes))
            check(confirmCode(code(me, peer))) { "Pairing code was not confirmed" }
            check(readFrame(socket.inputStream, maxHandshakeBytes) == "OK") { "Receiver did not confirm the pairing code" }
            writer.write(json.encodeToString(WireNote(noteFor(peer.key, peer.receiveNonce))) + "\n"); writer.flush()
        }
    }
}
