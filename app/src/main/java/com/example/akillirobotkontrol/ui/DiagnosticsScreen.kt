package com.example.akillirobotkontrol.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DiagnosticsScreen(
    viewModel: ControlViewModel,
    onBack: () -> Unit
) {
    val allLogs by viewModel.logs.collectAsState()
    var blackboxSizeKb by remember { mutableStateOf(viewModel.blackboxLogger.getLogFileSizeKb()) }
    
    DiagnosticsScreenContent(
        allLogs = allLogs,
        blackboxSizeKb = blackboxSizeKb,
        onClearLogs = { viewModel.clearLogs() },
        onClearBlackbox = { 
            viewModel.blackboxLogger.clearLogs() 
            blackboxSizeKb = viewModel.blackboxLogger.getLogFileSizeKb()
        },
        onBack = onBack
    )
}

@Composable
fun DiagnosticsScreenContent(
    allLogs: List<LogEntry>,
    blackboxSizeKb: Long,
    onClearLogs: () -> Unit,
    onClearBlackbox: () -> Unit,
    onBack: () -> Unit
) {
    var hideRx by remember { mutableStateOf(false) }
    
    val filteredLogs = if (hideRx) {
        allLogs.filter { it.type != LogType.RX }
    } else {
        allLogs
    }

    val listState = rememberLazyListState()

    // Otomatik aşağı kaydırma (Auto-Scroll)
    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)) // Koyu terminal teması
            .padding(16.dp)
    ) {
        // Üst Kısım: Başlık ve Filtre
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tanılama / Terminal",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Telemetriyi Gizle (RX)", color = Color.LightGray, fontSize = 12.sp)
                Checkbox(
                    checked = hideRx,
                    onCheckedChange = { hideRx = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = Color.Gray,
                        checkmarkColor = Color.White
                    )
                )
            }
        }
        
        Divider(color = Color.DarkGray, thickness = 1.dp)

        // Orta Kısım: Log Listesi
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            items(filteredLogs) { log ->
                val color = when (log.type) {
                    LogType.TX -> Color(0xFF64B5F6)     // Mavi
                    LogType.RX -> Color(0xFFAED581)     // Yeşil
                    LogType.EVENT -> Color(0xFFFFD54F)  // Sarı
                    LogType.ERROR -> Color(0xFFE57373)  // Kırmızı
                }

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        text = "[${log.time}] ",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${log.type.name}: ${log.message}",
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Divider(color = Color.DarkGray, thickness = 1.dp)

        // Alt Kısım: Butonlar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = { onBack() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Geri Dön")
            }
            Button(
                onClick = onClearBlackbox,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))
            ) {
                Text("Kara Kutu (${blackboxSizeKb}KB)", color = Color.White)
            }
            Button(
                onClick = onClearLogs,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Temizle")
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun DiagnosticsScreenPreview() {
    com.example.akillirobotkontrol.ui.theme.AkilliRobotKontrolTheme {
        DiagnosticsScreenContent(
            allLogs = listOf(
                LogEntry("10:00:00.000", "Sisteme bağlanıldı", LogType.EVENT),
                LogEntry("10:00:01.000", "F komutu gönderildi", LogType.TX),
                LogEntry("10:00:01.100", "Mesafe: 20cm", LogType.RX),
                LogEntry("10:00:02.000", "Bağlantı koptu!", LogType.ERROR)
            ),
            blackboxSizeKb = 12L,
            onClearLogs = {},
            onClearBlackbox = {},
            onBack = {}
        )
    }
}
