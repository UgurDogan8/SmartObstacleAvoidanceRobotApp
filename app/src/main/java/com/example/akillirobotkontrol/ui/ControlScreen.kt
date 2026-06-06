package com.example.akillirobotkontrol.ui

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.akillirobotkontrol.robot.RobotStatus
import com.example.akillirobotkontrol.robot.WarningLevel

@Composable
fun ControlScreen(
    viewModel: ControlViewModel,
    onDiagnosticsClick: () -> Unit
) {
    val robotStatus by viewModel.connectionService.robotStatus.collectAsState()

    val connectionState by viewModel.connectionService.connectionState.collectAsState()
    val currentSpeed by viewModel.currentSpeed.collectAsState()
    val gridUpdateVersion by viewModel.gridUpdateVersion.collectAsState()
    val isDisconnected = connectionState != com.example.akillirobotkontrol.bluetooth.ConnectionState.CONNECTED
    
    val context = LocalContext.current
    
    // Ekranın kapanmasını engelle (Keep Screen On)
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Geri tuşuna basıldığında bağlantıyı kes
    BackHandler {
        viewModel.disconnect()
    }

    // Uygulama arka plana atıldığında 'S' (Dur) komutu gönder
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.sendStop()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ControlScreenContent(
        robotStatus = robotStatus,
        isDisconnected = isDisconnected,
        currentSpeed = currentSpeed,
        occupancyGrid = viewModel.occupancyGrid,
        odometryTracker = viewModel.odometryTracker,
        gridUpdateVersion = gridUpdateVersion,
        onDiagnosticsClick = onDiagnosticsClick,
        onDisconnect = { viewModel.disconnect() },
        onSendStop = { viewModel.sendStop() },
        onSendForward = { viewModel.sendForward() },
        onSendBackward = { viewModel.sendBackward() },
        onSendLeft = { viewModel.sendLeft() },
        onSendRight = { viewModel.sendRight() },
        onSendForwardLeft = { viewModel.sendForwardLeft() },
        onSendForwardRight = { viewModel.sendForwardRight() },
        onSendBackwardLeft = { viewModel.sendBackwardLeft() },
        onSendBackwardRight = { viewModel.sendBackwardRight() },
        onSendEmergency = { viewModel.sendEmergency() },
        onResetEmergency = { viewModel.resetEmergency() },
        onSetAutoMode = { viewModel.setAutoMode() },
        onSetProtectedManualMode = { viewModel.setProtectedManualMode() },
        onSetFreeManualMode = { viewModel.setFreeManualMode() },
        onSetSpeed = { viewModel.setSpeed(it) },
        onReconnectToLastDevice = { adapter -> viewModel.reconnectToLastDevice(adapter) }
    )
}

@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun ControlScreenContent(
    robotStatus: RobotStatus,
    isDisconnected: Boolean,
    currentSpeed: Int,
    occupancyGrid: com.example.akillirobotkontrol.robot.mapping.OccupancyGrid,
    odometryTracker: com.example.akillirobotkontrol.robot.mapping.OdometryTracker,
    gridUpdateVersion: Long,
    onDiagnosticsClick: () -> Unit,
    onDisconnect: () -> Unit,
    onSendStop: () -> Unit,
    onSendForward: () -> Unit,
    onSendBackward: () -> Unit,
    onSendLeft: () -> Unit,
    onSendRight: () -> Unit,
    onSendForwardLeft: () -> Unit,
    onSendForwardRight: () -> Unit,
    onSendBackwardLeft: () -> Unit,
    onSendBackwardRight: () -> Unit,
    onSendEmergency: () -> Unit,
    onResetEmergency: () -> Unit,
    onSetAutoMode: () -> Unit,
    onSetProtectedManualMode: () -> Unit,
    onSetFreeManualMode: () -> Unit,
    onSetSpeed: (Int) -> Unit,
    onReconnectToLastDevice: (BluetoothAdapter?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val warningLevel = robotStatus.warningLevel

    // Mesafe uyarısı durumunda dokunsal geri bildirim (Vibrasyon)
    LaunchedEffect(warningLevel) {
        if (warningLevel == WarningLevel.CAUTION || warningLevel == WarningLevel.CRITICAL) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(150.milliseconds)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val isEmergency = robotStatus.isEmergency
    val isAutoMode = robotStatus.mode == "A1"
    val batteryLevel = robotStatus.batteryLevel
    val areDirectionsEnabled = !isEmergency && !isAutoMode && !isDisconnected

    var hasShownBatteryWarning by remember { mutableStateOf(false) }
    var showBatteryWarning by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(batteryLevel) {
        if (batteryLevel != null && batteryLevel < 20 && !hasShownBatteryWarning) {
            showBatteryWarning = true
            hasShownBatteryWarning = true
            delay(5.seconds) // 5 saniye ekranda kal
            showBatteryWarning = false
        } else if (batteryLevel != null && batteryLevel >= 20) {
            hasShownBatteryWarning = false // Şarj edilirse uyarı hakkını sıfırla
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // Kaydırılabilir Üst Alan (Sürüş Kontrolleri)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Modern Dashboard
            Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Glassmorphism HIZ Kartı
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = Color(0x1A00E5FF)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("HIZ", style = MaterialTheme.typography.labelMedium, color = Color(0xFF00E5FF).copy(alpha = 0.7f))
                    Text(
                        text = "${robotStatus.speedLevel}",
                        style = MaterialTheme.typography.displayMedium.copy(
                            shadow = Shadow(color = Color(0xFF00E5FF), blurRadius = 15f)
                        ),
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Glassmorphism MOD Kartı
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = Color(0x1A00E676)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("MOD", style = MaterialTheme.typography.labelMedium, color = Color(0xFF00E676).copy(alpha = 0.7f))
                    Text(
                        text = if (isAutoMode) "OTO" else if (robotStatus.manualSubMode == "K") "KORUMALI" else "SERBEST",
                        style = MaterialTheme.typography.titleMedium.copy(
                            shadow = Shadow(color = Color(0xFF00E676), blurRadius = 15f)
                        ),
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Glassmorphism PIL Kartı
            val batteryColor = when {
                batteryLevel == null -> Color.Gray
                batteryLevel > 50 -> Color(0xFF00E676) // Green
                batteryLevel > 20 -> Color(0xFFFFEA00) // Yellow
                else -> Color(0xFFFF1744) // Red
            }
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = batteryColor.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, batteryColor.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("PİL", style = MaterialTheme.typography.labelMedium, color = batteryColor.copy(alpha = 0.7f))
                    Text(
                        text = if (batteryLevel != null) "%$batteryLevel" else "--",
                        style = MaterialTheme.typography.displayMedium.copy(
                            shadow = Shadow(color = batteryColor, blurRadius = 15f)
                        ),
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sekmeler (SÜRÜŞ / HARİTA)
        val tabTitles = listOf("SÜRÜŞ", "HARİTA")
        
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }
        
        androidx.compose.animation.AnimatedContent(
            targetState = selectedTabIndex,
            label = "tab_switch",
            transitionSpec = {
                @Suppress("DEPRECATION")
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width } + fadeIn()).with(slideOutHorizontally { width -> -width } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).with(slideOutHorizontally { width -> width } + fadeOut())
                }.using(SizeTransform(clip = false))
            }
        ) { targetTab ->
            when (targetTab) {
                0 -> {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        // Radar / Mesafe Widget
                        val distanceColor by animateColorAsState(
            targetValue = when (warningLevel) {
                WarningLevel.CRITICAL -> Color(0xFFFF1744) // Neon Red
                WarningLevel.CAUTION -> Color(0xFFFFEA00) // Neon Yellow
                WarningLevel.SAFE -> Color(0xFF00E676) // Neon Green
                else -> Color.Gray
            },
            animationSpec = tween(500)
        )

        val distanceText = when (warningLevel) {
            WarningLevel.UNKNOWN -> "BEKLENİYOR"
            WarningLevel.SAFE -> "GÜVENLİ"
            WarningLevel.CAUTION -> "DİKKAT"
            WarningLevel.CRITICAL -> "KRİTİK"
        }
        
        // Pulsing animation
        val infiniteTransition = rememberInfiniteTransition()
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = distanceColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = distanceColor.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp).fillMaxWidth()
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp).scale(pulseScale)) {
                    CircularProgressIndicator(
                        progress = ((robotStatus.distanceCm?.toFloat() ?: 0f) / 100f).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxSize(),
                        color = distanceColor,
                        strokeWidth = 6.dp
                    )
                    Text(
                        text = "${robotStatus.distanceCm ?: 0}", 
                        fontWeight = FontWeight.Bold, 
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp,
                        color = Color.White,
                        style = TextStyle(
                            shadow = Shadow(color = distanceColor, blurRadius = 15f)
                        )
                    )
                }
                Spacer(modifier = Modifier.width(24.dp))
                Column {
                    Text("SENSÖR DURUMU", style = MaterialTheme.typography.labelMedium, color = distanceColor.copy(alpha = 0.8f))
                    Text(
                        text = distanceText, 
                        color = Color.White, 
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.titleLarge.copy(
                            shadow = Shadow(color = distanceColor, blurRadius = 15f)
                        ), 
                        fontWeight = FontWeight.Bold
                    )
                    Text("> ${robotStatus.movementState}", fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

            // Dinamik Orta Alan (Kaydırılabilir içerik olduğu için weight yerine padding kullanıyoruz)
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
            // Normal Kontroller (Sadece Joystick)
            androidx.compose.animation.AnimatedVisibility(
                visible = !isEmergency && !isDisconnected,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxHeight().padding(top = 16.dp, bottom = 32.dp)
                ) {
                    // 8 Yönlü Siber Joystick
                    CyberJoystick(
                        modifier = Modifier.padding(vertical = 8.dp),
                        enabled = areDirectionsEnabled,
                        onDirectionChanged = { dir ->
                            when(dir) {
                                "FORWARD" -> onSendForward()
                                "BACKWARD" -> onSendBackward()
                                "LEFT" -> onSendLeft()
                                "RIGHT" -> onSendRight()
                                "FORWARD_LEFT" -> onSendForwardLeft()
                                "FORWARD_RIGHT" -> onSendForwardRight()
                                "BACKWARD_LEFT" -> onSendBackwardLeft()
                                "BACKWARD_RIGHT" -> onSendBackwardRight()
                                "STOP" -> onSendStop()
                            }
                        }
                    )
                }
            }

                        }
                    }
                } // SÜRÜŞ sekmesinin sonu
                1 -> {
                    // HARİTA sekmesi
                    Box(modifier = Modifier.fillMaxWidth().height(450.dp).padding(vertical = 8.dp)) {
                        MappingComponent(
                            occupancyGrid = occupancyGrid,
                            odometryTracker = odometryTracker,
                            gridUpdateVersion = gridUpdateVersion
                        )
                    }
                }
            } // when sonu
        } // AnimatedContent sonu

        // Acil Durum Kartı
            androidx.compose.animation.AnimatedVisibility(
                visible = isEmergency && !isDisconnected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Rounded.Warning, contentDescription = "Acil", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "ACİL DURUM AKTİF",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onResetEmergency,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("SİSTEMİ SIFIRLA", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Bağlantı Koptu Kartı
            androidx.compose.animation.AnimatedVisibility(
                visible = isDisconnected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "BAĞLANTI KOPTU",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val bluetoothAdapter: BluetoothAdapter? = LocalContext.current.getSystemService(BluetoothManager::class.java)?.adapter
                        
                        Button(
                            onClick = { onReconnectToLastDevice(bluetoothAdapter) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("YENİDEN BAĞLAN", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } // Kaydırılabilir alanın sonu

        Spacer(modifier = Modifier.height(8.dp))

        // Alt Sabit Kontroller (Hız ve Mod) - Sadece Acil Durum / Kopukluk yoksa ve SÜRÜŞ sekmesindeyse göster
        androidx.compose.animation.AnimatedVisibility(
            visible = !isEmergency && !isDisconnected && selectedTabIndex == 0,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                // Hız Kaydırıcısı
                var sliderPosition by remember(currentSpeed) { mutableFloatStateOf(currentSpeed.toFloat()) }
                
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = 0f..9f,
                    steps = 8,
                    onValueChangeFinished = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSetSpeed(sliderPosition.toInt())
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    enabled = !isEmergency,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color.DarkGray
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Mod Seçimi
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), 
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isProtected = !isAutoMode && robotStatus.manualSubMode == "K"
                    val isFree = !isAutoMode && robotStatus.manualSubMode == "S"

                    Button(
                        onClick = { 
                            if (!isAutoMode || isEmergency) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSetAutoMode() 
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAutoMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isAutoMode) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("OTO", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = { 
                            if (!isProtected || isEmergency) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSetProtectedManualMode() 
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isProtected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isProtected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("KORUMALI", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = { 
                            if (!isFree || isEmergency) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSetFreeManualMode() 
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFree) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isFree) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("SERBEST", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Acil Durdurma Butonu
        Button(
            onClick = { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSendEmergency() 
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(elevation = 15.dp, shape = RoundedCornerShape(16.dp), ambientColor = Color.Red, spotColor = Color.Red),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
        ) {
            Icon(imageVector = Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.padding(end = 8.dp), tint = Color.White)
            Text(
                text = "ACİL DURDUR", 
                style = MaterialTheme.typography.titleLarge.copy(
                    letterSpacing = 2.sp,
                    shadow = Shadow(color = Color.Black, blurRadius = 5f)
                ), 
                fontWeight = FontWeight.ExtraBold, 
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Alt Butonlar
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(
                onClick = onDisconnect,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text("BAĞLANTIYI KES")
            }
            OutlinedButton(
                onClick = onDiagnosticsClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("TANILAMA")
            }
        }
    }

    // Düşük Pil Overlay (Ekranda diğer elementleri kaydırmaması için Box'ın direkt içine eklendi)
            androidx.compose.animation.AnimatedVisibility(
        visible = showBatteryWarning && !isDisconnected,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 120.dp, start = 16.dp, end = 16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().shadow(elevation = 20.dp, shape = RoundedCornerShape(16.dp), spotColor = Color.Red),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xE6210005)), // Çok koyu şeffaf kırmızı
            border = BorderStroke(1.dp, Color(0xFFFF1744).copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Rounded.Warning, contentDescription = "Düşük Pil", tint = Color(0xFFFF1744), modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("DÜŞÜK PİL UYARISI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFF1744))
                    Text("Pil seviyesi çok düşük (%$batteryLevel). Lütfen aracı şarj edin.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
    }
}
}

@Composable
fun ControlButton(
    icon: ImageVector,
    enabled: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f)
    val neonBlue = Color(0xFF00E5FF)

    Box(
        modifier = Modifier
            .padding(8.dp)
            .size(80.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 20.dp else 0.dp, 
                shape = RoundedCornerShape(24.dp), 
                ambientColor = neonBlue, 
                spotColor = neonBlue
            )
            .background(
                color = if (!enabled) Color.Gray.copy(alpha=0.1f) else if (isPressed) neonBlue.copy(alpha=0.8f) else Color.Transparent,
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 2.dp,
                color = if (!enabled) Color.Transparent else neonBlue.copy(alpha = if (isPressed) 1f else 0.4f),
                shape = RoundedCornerShape(24.dp)
            )
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPress()
                            try {
                                tryAwaitRelease()
                            } finally {
                                isPressed = false
                                onRelease()
                            }
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (!enabled) Color.DarkGray else if (isPressed) Color.White else neonBlue,
            modifier = Modifier.size(48.dp)
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun ControlScreenPreview() {
    com.example.akillirobotkontrol.ui.theme.AkilliRobotKontrolTheme {
        ControlScreenContent(
            robotStatus = RobotStatus(
                distanceCm = 35,
                warningLevel = WarningLevel.SAFE,
                speedLevel = 5,
                movementState = "İleri Gidiyor",
                mode = "A0",
                isEmergency = false
            ),
            isDisconnected = false,
            currentSpeed = 5,
            onDiagnosticsClick = {},
            onDisconnect = {},
            onSendStop = {},
            onSendForward = {},
            onSendBackward = {},
            onSendLeft = {},
            onSendRight = {},
            onSendForwardLeft = {},
            onSendForwardRight = {},
            onSendBackwardLeft = {},
            onSendBackwardRight = {},
            onSendEmergency = {},
            onResetEmergency = {},
            onSetAutoMode = {},
            onSetProtectedManualMode = {},
            onSetFreeManualMode = {},
            onSetSpeed = {},
            onReconnectToLastDevice = {},
            occupancyGrid = com.example.akillirobotkontrol.robot.mapping.OccupancyGrid(),
            odometryTracker = com.example.akillirobotkontrol.robot.mapping.OdometryTracker(),
            gridUpdateVersion = 0L
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun ControlScreenEmergencyPreview() {
    com.example.akillirobotkontrol.ui.theme.AkilliRobotKontrolTheme {
        ControlScreenContent(
            robotStatus = RobotStatus(
                distanceCm = 12,
                warningLevel = WarningLevel.CRITICAL,
                speedLevel = 0,
                movementState = "KİLİTLİ",
                mode = "A0",
                isEmergency = true,
                batteryLevel = 45,
                encoderTicks = 1200
            ),
            isDisconnected = false,
            currentSpeed = 0,
            onDiagnosticsClick = {},
            onDisconnect = {},
            onSendStop = {},
            onSendForward = {},
            onSendBackward = {},
            onSendLeft = {},
            onSendRight = {},
            onSendForwardLeft = {},
            onSendForwardRight = {},
            onSendBackwardLeft = {},
            onSendBackwardRight = {},
            onSendEmergency = {},
            onResetEmergency = {},
            onSetAutoMode = {},
            onSetProtectedManualMode = {},
            onSetFreeManualMode = {},
            onSetSpeed = {},
            onReconnectToLastDevice = {},
            occupancyGrid = com.example.akillirobotkontrol.robot.mapping.OccupancyGrid(),
            odometryTracker = com.example.akillirobotkontrol.robot.mapping.OdometryTracker(),
            gridUpdateVersion = 0L
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun ControlScreenLowBatteryPreview() {
    com.example.akillirobotkontrol.ui.theme.AkilliRobotKontrolTheme {
        ControlScreenContent(
            robotStatus = RobotStatus(
                distanceCm = 45,
                warningLevel = WarningLevel.SAFE,
                speedLevel = 3,
                movementState = "S",
                mode = "A0",
                isEmergency = false,
                batteryLevel = 15, // Düşük pil senaryosu
                encoderTicks = 5400
            ),
            isDisconnected = false,
            currentSpeed = 3,
            onDiagnosticsClick = {},
            onDisconnect = {},
            onSendStop = {},
            onSendForward = {},
            onSendBackward = {},
            onSendLeft = {},
            onSendRight = {},
            onSendForwardLeft = {},
            onSendForwardRight = {},
            onSendBackwardLeft = {},
            onSendBackwardRight = {},
            onSendEmergency = {},
            onResetEmergency = {},
            onSetAutoMode = {},
            onSetProtectedManualMode = {},
            onSetFreeManualMode = {},
            onSetSpeed = {},
            onReconnectToLastDevice = {},
            occupancyGrid = com.example.akillirobotkontrol.robot.mapping.OccupancyGrid(),
            odometryTracker = com.example.akillirobotkontrol.robot.mapping.OdometryTracker(),
            gridUpdateVersion = 0L
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun ControlButtonPreview() {
    com.example.akillirobotkontrol.ui.theme.AkilliRobotKontrolTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            ControlButton(
                icon = Icons.Rounded.KeyboardArrowUp,
                enabled = true,
                onPress = {},
                onRelease = {}
            )
            ControlButton(
                icon = Icons.Rounded.KeyboardArrowDown,
                enabled = false,
                onPress = {},
                onRelease = {}
            )
        }
    }
}

@Composable
fun CyberJoystick(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onDirectionChanged: (String) -> Unit
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var lastDirection by remember { mutableStateOf("STOP") }
    val maxRadius = 260f
    val neonBlue = Color(0xFF00E5FF)
    val neonRed = Color(0xFFFF1744)

    val density = LocalDensity.current.density
    val boxSizeDp = (maxRadius * 2) / density

    // Esnek yay animasyonu
    val animatedX by animateFloatAsState(targetValue = dragOffset.x)
    val animatedY by animateFloatAsState(targetValue = dragOffset.y)

    Box(
        modifier = modifier
            .size(boxSizeDp.dp)
            .background(Color.DarkGray.copy(alpha = 0.1f), CircleShape)
            .border(2.dp, if (enabled) neonBlue.copy(alpha = 0.3f) else Color.Gray, CircleShape)
            .pointerInput(enabled) {
                if (enabled) {
                    detectDragGestures(
                        onDragStart = { },
                        onDragEnd = {
                            dragOffset = Offset.Zero
                            if (lastDirection != "STOP") {
                                lastDirection = "STOP"
                                onDirectionChanged("STOP")
                            }
                        },
                        onDragCancel = {
                            dragOffset = Offset.Zero
                            if (lastDirection != "STOP") {
                                lastDirection = "STOP"
                                onDirectionChanged("STOP")
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = dragOffset + dragAmount
                            val distance = newOffset.getDistance()
                            
                            // Joystick topunu sınırla
                            dragOffset = if (distance <= maxRadius) {
                                newOffset
                            } else {
                                newOffset / distance * maxRadius
                            }
                            
                            // Yönü 8 dilime ayır
                            val angle = Math.toDegrees(kotlin.math.atan2(dragOffset.y.toDouble(), dragOffset.x.toDouble()))
                            val mappedDir = when {
                                angle in -22.5..22.5 -> "RIGHT"
                                angle in 22.5..67.5 -> "BACKWARD_RIGHT"
                                angle in 67.5..112.5 -> "BACKWARD"
                                angle in 112.5..157.5 -> "BACKWARD_LEFT"
                                angle >= 157.5 || angle <= -157.5 -> "LEFT"
                                angle in -157.5..-112.5 -> "FORWARD_LEFT"
                                angle in -112.5..-67.5 -> "FORWARD"
                                else -> "FORWARD_RIGHT"
                            }
                            
                            // Deadzone (Merkeze yakınsa dur)
                            val finalDir = if (dragOffset.getDistance() < 40f) "STOP" else mappedDir
                            
                            // Sadece yön değiştiğinde tetikle (Spam engelleme)
                            if (lastDirection != finalDir) {
                                lastDirection = finalDir
                                onDirectionChanged(finalDir)
                            }
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Merkez belirteci
        Box(modifier = Modifier.size(16.dp).background(if(enabled) neonRed else Color.Gray, CircleShape).shadow(10.dp, CircleShape, spotColor = neonRed))
        
        // Joystick topu
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedX.toInt(), animatedY.toInt()) }
                .size(96.dp)
                .shadow(20.dp, CircleShape, ambientColor = neonBlue, spotColor = neonBlue)
                .background(if(enabled) neonBlue.copy(alpha=0.8f) else Color.Gray, CircleShape)
                .border(2.dp, neonBlue, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp, 
                contentDescription = null, 
                tint = Color.White.copy(alpha=0.5f), 
                modifier = Modifier.align(Alignment.TopCenter)
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown, 
                contentDescription = null, 
                tint = Color.White.copy(alpha=0.5f), 
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
