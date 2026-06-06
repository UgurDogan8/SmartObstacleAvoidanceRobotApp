package com.example.akillirobotkontrol

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.akillirobotkontrol.bluetooth.BluetoothDeviceModel
import com.example.akillirobotkontrol.bluetooth.ConnectionState
import com.example.akillirobotkontrol.ui.ControlScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.SmartToy

import com.example.akillirobotkontrol.ui.theme.AkilliRobotKontrolTheme
import com.example.akillirobotkontrol.utils.PermissionHelper

import com.example.akillirobotkontrol.ui.ConnectionViewModel
import com.example.akillirobotkontrol.ui.ControlViewModel

class MainActivity : ComponentActivity() {
    private val connectionViewModel: ConnectionViewModel by viewModels()
    private val controlViewModel: ControlViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager?.adapter

        val enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                connectionViewModel.updatePermissionState(true)
                connectionViewModel.loadPairedDevices(this)
            } else {
                connectionViewModel.setPermissionErrorMessage("Bluetooth'u açmanız gerekmektedir.")
            }
        }

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            connectionViewModel.updatePermissionState(allGranted)
            
            if (allGranted) {
                if (bluetoothAdapter?.isEnabled == false) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(enableBtIntent)
                } else {
                    connectionViewModel.loadPairedDevices(this)
                }
            } else {
                connectionViewModel.setPermissionErrorMessage("Bluetooth cihazlarını taramak ve bağlanmak için izin vermeniz gerekmektedir.")
            }
        }

        setContent {
            AkilliRobotKontrolTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val connectionState by connectionViewModel.connectionService.connectionState.collectAsState()
                    val context = LocalContext.current
                    
                    // Toast handling for connection states
                    LaunchedEffect(connectionState) {
                        when (connectionState) {
                            ConnectionState.CONNECTED -> {
                                Toast.makeText(context, "Bağlantı başarılı", Toast.LENGTH_SHORT).show()
                            }
                            ConnectionState.ERROR -> {
                                Toast.makeText(context, "Bağlantı kurulamadı", Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }

                    var showSettings by remember { mutableStateOf(false) }
                    var showDiagnostics by remember { mutableStateOf(false) }

                    if (showSettings) {
                        com.example.akillirobotkontrol.ui.SettingsScreen(
                            viewModel = controlViewModel,
                            onBack = { showSettings = false }
                        )
                    } else if (showDiagnostics) {
                        com.example.akillirobotkontrol.ui.DiagnosticsScreen(
                            viewModel = controlViewModel,
                            onBack = { showDiagnostics = false }
                        )
                    } else if (connectionState == ConnectionState.CONNECTED) {
                        ControlScreen(
                            viewModel = controlViewModel,
                            onDiagnosticsClick = { showDiagnostics = true }
                        )
                    } else {
                        ConnectionScreen(
                            viewModel = connectionViewModel,
                            bluetoothAdapter = bluetoothAdapter,
                            onRefreshClick = {
                                val requiredPermissions = PermissionHelper.getRequiredBluetoothPermissions()
                                if (PermissionHelper.hasPermissions(this@MainActivity, requiredPermissions)) {
                                    connectionViewModel.updatePermissionState(true)
                                    if (bluetoothAdapter?.isEnabled == false) {
                                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                        enableBluetoothLauncher.launch(enableBtIntent)
                                    } else {
                                        connectionViewModel.loadPairedDevices(this@MainActivity)
                                    }
                                } else {
                                    requestPermissionLauncher.launch(requiredPermissions)
                                }
                            },
                            onSettingsClick = { showSettings = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionScreen(
    viewModel: ConnectionViewModel,
    bluetoothAdapter: BluetoothAdapter?,
    onRefreshClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val permissionGranted by viewModel.permissionGranted.collectAsState()
    val errorMessage by viewModel.permissionErrorMessage.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val connectionState by viewModel.connectionService.connectionState.collectAsState()
    val lastMac by viewModel.lastConnectedDeviceMac.collectAsState()
    val lastName by viewModel.lastConnectedDeviceName.collectAsState()
    
    val isBluetoothEnabled = bluetoothAdapter?.isEnabled == true

    ConnectionScreenContent(
        permissionGranted = permissionGranted,
        errorMessage = errorMessage,
        pairedDevices = pairedDevices,
        selectedDevice = selectedDevice,
        connectionState = connectionState,
        lastMac = lastMac,
        lastName = lastName,
        isBluetoothEnabled = isBluetoothEnabled,
        onRefreshClick = onRefreshClick,
        onSettingsClick = onSettingsClick,
        onConnectToLastDevice = { 
            if (lastMac != null && lastName != null) {
                viewModel.connectToMacAddress(lastMac!!, lastName!!, bluetoothAdapter)
            }
        },
        onSelectDevice = { viewModel.selectDevice(it) },
        onConnectToSelectedDevice = { viewModel.connectToSelectedDevice(bluetoothAdapter) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreenContent(
    permissionGranted: Boolean,
    errorMessage: String?,
    pairedDevices: List<BluetoothDeviceModel>,
    selectedDevice: BluetoothDeviceModel?,
    connectionState: ConnectionState,
    lastMac: String?,
    lastName: String?,
    isBluetoothEnabled: Boolean,
    onRefreshClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onConnectToLastDevice: () -> Unit,
    onSelectDevice: (BluetoothDeviceModel) -> Unit,
    onConnectToSelectedDevice: () -> Unit
) {
    val isConnecting = connectionState == ConnectionState.CONNECTING
    val isConnected = connectionState == ConnectionState.CONNECTED

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Üst Kısım (Kaydırılabilir alan)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Üst Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "AKILLI ROBOT MERKEZİ",
                    style = MaterialTheme.typography.titleLarge.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = MaterialTheme.colorScheme.primary,
                            blurRadius = 12f
                        )
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Ayarlar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Merkez: Pulsing Radar ve Robot İkonu
            PulsingRadar(isConnecting = isConnecting, isConnected = isConnected)

            Spacer(modifier = Modifier.height(16.dp))

            // Mini Terminal (Siber Konsol)
            MiniTerminal(pairedDeviceCount = pairedDevices.size, isBluetoothEnabled = isBluetoothEnabled)

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Son Bağlanılan Cihaz
            if (lastMac != null && lastName != null && permissionGranted && isBluetoothEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.5f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Son Bağlanılan Cihaz: $lastName", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onConnectToLastDevice,
                            enabled = !isConnecting,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.secondary)
                            } else {
                                Text(text = "Son Cihaza Tekrar Bağlan")
                            }
                        }
                    }
                }
            }
        }

        // Ana Aksiyon Butonu (Her zaman en altta sabit kalacak)
        Button(
            onClick = { 
                onRefreshClick()
                if (permissionGranted && isBluetoothEnabled) {
                    showBottomSheet = true 
                }
            }, 
            enabled = !isConnecting,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(text = "CİHAZLARI GÖSTER / YENİLE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }

    // Modal Bottom Sheet (Alt Çekmece)
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Eşleşmiş Cihazlar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (pairedDevices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Eşleşmiş cihaz bulunamadı.\nLütfen telefonunuzun Bluetooth ayarlarından HC-05'i eşleştirin.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(pairedDevices) { device ->
                            DeviceCard(
                                device = device,
                                isSelected = selectedDevice?.macAddress == device.macAddress,
                                onClick = { if (!isConnecting) onSelectDevice(device) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Morphing Bağlan Butonu
                if (selectedDevice != null) {
                    val buttonWidth by animateDpAsState(
                        targetValue = if (isConnecting) 56.dp else 300.dp,
                        animationSpec = tween(500)
                    )

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = onConnectToSelectedDevice,
                            enabled = !isConnecting,
                            shape = if (isConnecting) CircleShape else RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .width(buttonWidth)
                                .height(56.dp)
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.secondary)
                            } else {
                                Text(text = "BAĞLAN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PulsingRadar(isConnecting: Boolean, isConnected: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")
    
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarScale1"
    )
    
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAlpha1"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing, delayMillis = 1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarScale2"
    )
    
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing, delayMillis = 1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAlpha2"
    )

    val color = when {
        isConnected -> MaterialTheme.colorScheme.secondary
        isConnecting -> Color(0xFFFFD600) // Sarı / Uyarı
        else -> MaterialTheme.colorScheme.primary
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale1)
                .border(2.dp, color.copy(alpha = alpha1), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale2)
                .border(2.dp, color.copy(alpha = alpha2), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(color.copy(alpha = 0.2f), CircleShape)
                .border(1.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SmartToy,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(72.dp)
            )
        }
    }
}

@Composable
fun DeviceCard(
    device: BluetoothDeviceModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isHC05 = device.name.contains("HC-05", ignoreCase = true)
    
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.background
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isHC05) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Target",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp).padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isHC05) FontWeight.Bold else FontWeight.Normal,
                        color = if (isHC05) MaterialTheme.colorScheme.primary else Color.Unspecified
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = device.macAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            if (device.isPaired) {
                Text(
                    text = "Eşleşmiş",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
fun MiniTerminal(pairedDeviceCount: Int, isBluetoothEnabled: Boolean) {
    var logs by remember { mutableStateOf(listOf<String>()) }
    var showCursor by remember { mutableStateOf(true) }

    LaunchedEffect(isBluetoothEnabled, pairedDeviceCount) {
        logs = emptyList() // Reset logs
        val bootSequence = listOf(
            "> AKILLI ROBOT SİSTEMİ v1.0",
            "> Çekirdek modüller yükleniyor... [OK]",
            "> Bluetooth adaptörü... [${if (isBluetoothEnabled) "OK" else "HATA"}]",
            "> Eşleşmiş cihazlar... [$pairedDeviceCount BULUNDU]",
            "> Sistem Hazır. Komut bekleniyor"
        )
        
        for (log in bootSequence) {
            kotlinx.coroutines.delay((300..500).random().toLong())
            logs = logs + log
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            showCursor = !showCursor
            kotlinx.coroutines.delay(500)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha=0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            logs.forEach { log ->
                Text(
                    text = log,
                    color = Color(0xFF00E676),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = "> " + if (showCursor) "_" else "",
                color = Color(0xFF00E676),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ConnectionScreenPreview() {
    val dummyDevices = listOf(
        BluetoothDeviceModel("Robot HC-05", "00:11:22:33:44:55", true),
        BluetoothDeviceModel("TV", "AA:BB:CC:DD:EE:FF", false),
        BluetoothDeviceModel("Headphones", "11:22:33:44:55:66", true)
    )
    
    AkilliRobotKontrolTheme {
        ConnectionScreenContent(
            permissionGranted = true,
            errorMessage = null,
            pairedDevices = dummyDevices,
            selectedDevice = dummyDevices[0],
            connectionState = ConnectionState.DISCONNECTED,
            lastMac = "00:11:22:33:44:55",
            lastName = "Robot HC-05",
            isBluetoothEnabled = true,
            onRefreshClick = {},
            onSettingsClick = {},
            onConnectToLastDevice = {},
            onSelectDevice = {},
            onConnectToSelectedDevice = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun DeviceCardPreview() {
    AkilliRobotKontrolTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DeviceCard(
                device = BluetoothDeviceModel("Robot HC-05", "00:11:22:33:44:55", true),
                isSelected = true,
                onClick = {}
            )
            DeviceCard(
                device = BluetoothDeviceModel("TV", "AA:BB:CC:DD:EE:FF", false),
                isSelected = false,
                onClick = {}
            )
        }
    }
}
