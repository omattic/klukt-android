package com.guerrerocarlos.offlinenotes

import android.annotation.SuppressLint
import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.guerrerocarlos.offlinenotes.crypto.WalletKey
import com.guerrerocarlos.offlinenotes.data.NoteEntity
import com.guerrerocarlos.offlinenotes.data.WalletDatabase
import com.guerrerocarlos.offlinenotes.data.WalletRepository
import com.guerrerocarlos.offlinenotes.model.SignedNote
import com.guerrerocarlos.offlinenotes.transport.BluetoothTransfer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
    private val vm by viewModels<WalletViewModel> {
        WalletViewModel.Factory(applicationContext)
    }
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        vm.status("Nearby devices: " + if (result.values.all { it }) "granted" else "denied")
    }
    private val importNote = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selected -> contentResolver.openInputStream(selected)?.bufferedReader()?.use { vm.importNote(it.readText()) } }
    }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
        setContent { MaterialTheme { WalletScreen(vm, { requestBluetoothPermissions() }, { importNote.launch("application/json") }) } }
    }
    private fun requestBluetoothPermissions() {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN) else emptyArray()
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) vm.status("Nearby devices already granted") else this.permissions.launch(permissions)
    }
}

class WalletViewModel(private val key: WalletKey, private val repo: WalletRepository, private val bluetooth: BluetoothTransfer) : ViewModel() {
    val notes = repo.held().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _status = MutableStateFlow("TEST VALUE ONLY. No network, cloud, or double-spend protection.")
    val status: StateFlow<String> = _status
    val diagnostic = MutableStateFlow(key.diagnostic())
    val paired = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairingCode = MutableStateFlow<String?>(null)
    private var confirmation: CompletableDeferred<Boolean>? = null
    fun status(value: String) { _status.value = value }
    fun refreshPaired() { paired.value = bluetooth.pairedDevices(); status(if (paired.value.isEmpty()) "No paired phones. Pair in Android Settings first." else "Choose a paired phone to send.") }
    private suspend fun confirmCode(code: String): Boolean {
        val deferred = CompletableDeferred<Boolean>(); confirmation = deferred; pairingCode.value = code
        status("Compare $code on both phones, then tap Confirm matching code.")
        val accepted = withTimeoutOrNull(60_000) { deferred.await() } ?: false
        confirmation = null; pairingCode.value = null; return accepted
    }
    fun confirmPairing() { confirmation?.complete(true) }
    fun receive() = viewModelScope.launch { try {
        val note = bluetooth.receive(key.publicKey(), repo.testNonce(), ::confirmCode)
        repo.receive(note); status("Received ${note.issued.denomination} test units. It is now transferable offline.")
    } catch (e: Exception) { status("Receive failed: ${e.message}") } }
    fun send(note: NoteEntity, device: BluetoothDevice) = viewModelScope.launch { try {
        bluetooth.send(device, { recipient, nonce -> repo.prepareTransfer(note.noteId, recipient, nonce) }, key.publicKey(), ::confirmCode)
        repo.markTransferred(note.noteId); status("Sent test note. Local copy is marked transferred.")
    } catch (e: Exception) { status("Send failed: ${e.message}") } }
    fun experiment() { diagnostic.value = key.tryCreateSingleUseExperiment() }
    fun trustIssuer(publicKey: String) = viewModelScope.launch { try { repo.trustIssuer(publicKey, "Local test issuer"); status("Issuer explicitly trusted") } catch (e: Exception) { status("Issuer import failed: ${e.message}") } }
    fun importNote(raw: String) = viewModelScope.launch { try { repo.importProvisionedNote(raw); status("Provisioned test note imported") } catch (e: Exception) { status("Note import rejected: ${e.message}") } }
    class Factory(private val context: android.content.Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val key = WalletKey(context); return WalletViewModel(key, WalletRepository(WalletDatabase.open(context), key), BluetoothTransfer(context)) as T
        }
    }
}

@SuppressLint("MissingPermission")
@Composable private fun WalletScreen(vm: WalletViewModel, askPermissions: () -> Unit, pickNote: () -> Unit) {
    val notes by vm.notes.collectAsState(); val status by vm.status.collectAsState(); val diagnostic by vm.diagnostic.collectAsState(); val paired by vm.paired.collectAsState(); val pairingCode by vm.pairingCode.collectAsState()
    var selection by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<NoteEntity?>(null) }
    var issuerKey by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Offline Notes", style = MaterialTheme.typography.headlineMedium)
        Text("Research prototype. TEST VALUE ONLY.", color = MaterialTheme.colorScheme.error)
        Text(status)
        pairingCode?.let { code -> Button(onClick = vm::confirmPairing) { Text("Confirm matching code: $code") } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = askPermissions) { Text("Grant Nearby") } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = vm::receive) { Text("Receive") }; OutlinedButton(onClick = vm::refreshPaired) { Text("Send to paired phone") } }
        Text("Spendable test notes: ${notes.size}", style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(1f)) { items(notes, key = { it.noteId }) { note ->
            Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${note.noteId.take(8)}  •  test note")
                TextButton(onClick = { selection = note }) { Text("Send") }
            } }
        } }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
            Text("Hardware diagnostic", style = MaterialTheme.typography.titleMedium); Text(diagnostic)
            TextButton(onClick = vm::experiment) { Text("Create experimental single-use key") }
            OutlinedTextField(value = issuerKey, onValueChange = { issuerKey = it }, label = { Text("Trusted test issuer public key") }, modifier = Modifier.fillMaxWidth())
            Row { TextButton(onClick = { vm.trustIssuer(issuerKey) }) { Text("Trust issuer") }; TextButton(onClick = pickNote) { Text("Import provisioned note") } }
        } }
    }
    selection?.let { note -> AlertDialog(onDismissRequest = { selection = null }, title = { Text("Choose paired receiver") }, text = {
        Column { if (paired.isEmpty()) Text("Tap Send to paired phone first, then select a device.") else paired.forEach { device -> TextButton(onClick = { vm.send(note, device); selection = null }) { Text(device.name ?: device.address) } } }
    }, confirmButton = { TextButton(onClick = { selection = null }) { Text("Cancel") } }) }
}
