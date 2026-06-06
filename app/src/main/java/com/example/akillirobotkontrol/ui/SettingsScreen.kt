package com.example.akillirobotkontrol.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.akillirobotkontrol.data.AppSettings

@Composable
fun SettingsScreen(
    viewModel: ControlViewModel,
    onBack: () -> Unit
) {
    val currentSettings by viewModel.appSettings.collectAsState()
    
    SettingsScreenContent(
        currentSettings = currentSettings,
        onSave = { newSettings ->
            viewModel.updateSettings(newSettings)
            onBack()
        },
        onBack = onBack
    )
}

@Composable
fun SettingsScreenContent(
    currentSettings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    var speedLevel by remember { mutableFloatStateOf(currentSettings.speedLevel.toFloat()) }
    var rememberLastDevice by remember { mutableStateOf(currentSettings.rememberLastDevice) }
    var commandRepeatMs by remember { mutableStateOf(currentSettings.commandRepeatMs.toString()) }
    var telemetryTimeoutMs by remember { mutableStateOf(currentSettings.telemetryTimeoutMs.toString()) }
    var criticalDistanceCm by remember { mutableStateOf(currentSettings.criticalDistanceCm.toString()) }
    var cautionDistanceCm by remember { mutableStateOf(currentSettings.cautionDistanceCm.toString()) }

    var showError by remember { mutableStateOf(false) }

    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Ayarlar",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Genel Ayarlar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Varsayılan Başlangıç Hızı: ${speedLevel.toInt()}")
                Slider(
                    value = speedLevel,
                    onValueChange = { speedLevel = it },
                    valueRange = 0f..9f,
                    steps = 8
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Son Cihaza Otomatik Bağlan")
                    Switch(
                        checked = rememberLastDevice,
                        onCheckedChange = { rememberLastDevice = it }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Haberleşme & Güvenlik", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = telemetryTimeoutMs,
                    onValueChange = { telemetryTimeoutMs = it },
                    label = { Text("Telemetri Zaman Aşımı (ms)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Veri gelmezse bağlantıyı kesecek süre (Varsayılan 1000ms)") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = commandRepeatMs,
                    onValueChange = { commandRepeatMs = it },
                    label = { Text("Komut Tekrar Aralığı (ms)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Ardışık komutlar arası min. süre (Varsayılan 150ms)") }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sensör Eşikleri", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = cautionDistanceCm,
                    onValueChange = { cautionDistanceCm = it },
                    label = { Text("Dikkat Eşiği (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = criticalDistanceCm,
                    onValueChange = { criticalDistanceCm = it },
                    label = { Text("Kritik Eşik (cm) [Fren]") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showError) {
            Text(
                text = "Lütfen geçerli değerler girin! Kritik mesafe, Dikkat mesafesinden küçük olmalıdır.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val telTimeout = telemetryTimeoutMs.toIntOrNull() ?: 1000
                val cmdRepeat = commandRepeatMs.toIntOrNull() ?: 150
                val critDist = criticalDistanceCm.toIntOrNull() ?: 20
                val cautDist = cautionDistanceCm.toIntOrNull() ?: 35

                if (critDist >= cautDist || telTimeout < 100 || cmdRepeat < 0) {
                    showError = true
                } else {
                    showError = false
                    val newSettings = AppSettings(
                        speedLevel = speedLevel.toInt(),
                        rememberLastDevice = rememberLastDevice,
                        commandRepeatMs = cmdRepeat,
                        telemetryTimeoutMs = telTimeout,
                        criticalDistanceCm = critDist,
                        cautionDistanceCm = cautDist
                    )
                    onSave(newSettings)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Ayarları Kaydet ve Geri Dön", style = MaterialTheme.typography.titleMedium)
        }
    }
}
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    com.example.akillirobotkontrol.ui.theme.AkilliRobotKontrolTheme {
        SettingsScreenContent(
            currentSettings = AppSettings(),
            onSave = {},
            onBack = {}
        )
    }
}