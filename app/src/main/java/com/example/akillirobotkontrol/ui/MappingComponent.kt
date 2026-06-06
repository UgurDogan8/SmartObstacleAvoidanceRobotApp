package com.example.akillirobotkontrol.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.akillirobotkontrol.robot.mapping.CellState
import com.example.akillirobotkontrol.robot.mapping.OccupancyGrid
import com.example.akillirobotkontrol.robot.mapping.OdometryTracker

@Composable
fun MappingComponent(
    occupancyGrid: OccupancyGrid,
    odometryTracker: OdometryTracker,
    gridUpdateVersion: Long,
    modifier: Modifier = Modifier
) {
    // Recompose when version changes
    val currentVersion by rememberUpdatedState(gridUpdateVersion)
    
    // Pan and zoom state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    val cellSizePx = 20f // pixels per cell

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0A0A0A))
                .border(1.dp, Color.DarkGray, RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.2f, 5f)
                        offset += pan
                    }
                }
        ) {
            // Trigger recomposition explicitly via state read
            currentVersion.let { 
                // no-op
            }
            
            translate(
                left = size.width / 2f + offset.x,
                top = size.height / 2f + offset.y
            ) {
                // Draw origin axes (faint)
                drawLine(
                    color = Color.DarkGray.copy(alpha = 0.5f),
                    start = Offset(-1000f, 0f),
                    end = Offset(1000f, 0f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.DarkGray.copy(alpha = 0.5f),
                    start = Offset(0f, -1000f),
                    end = Offset(0f, 1000f),
                    strokeWidth = 2f
                )

                // Draw Grid Cells
                occupancyGrid.grid.forEach { (coord, state) ->
                    val (gridX, gridY) = coord
                    val pxX = gridX * cellSizePx * scale
                    val pxY = -gridY * cellSizePx * scale // Invert Y so positive is UP
                    
                    val color = when (state) {
                        CellState.FREE -> Color(0xFF26A69A).copy(alpha = 0.3f) // Teal, faint
                        CellState.OBSTACLE -> Color(0xFF00E5FF) // Neon Cyan for obstacles
                        CellState.UNKNOWN -> Color.Transparent
                    }
                    
                    if (state != CellState.UNKNOWN) {
                        drawRect(
                            color = color,
                            topLeft = Offset(pxX - (cellSizePx*scale)/2, pxY - (cellSizePx*scale)/2),
                            size = Size(cellSizePx * scale, cellSizePx * scale)
                        )
                    }
                }
                
                // Draw Robot
                val robotPxX = (odometryTracker.xCm / 5f) * cellSizePx * scale
                val robotPxY = -(odometryTracker.yCm / 5f) * cellSizePx * scale
                
                translate(left = robotPxX, top = robotPxY) {
                    rotate(degrees = odometryTracker.headingDegrees) {
                        val path = Path().apply {
                            moveTo(0f, -15f * scale) // Tip (Up)
                            lineTo(-10f * scale, 10f * scale) // Bottom Left
                            lineTo(10f * scale, 10f * scale) // Bottom Right
                            close()
                        }
                        drawPath(
                            path = path,
                            color = Color(0xFFFF1744) // Neon Red
                        )
                        // Inner glow/core
                        drawCircle(
                            color = Color.White,
                            radius = 3f * scale
                        )
                    }
                }
            }
        }

        // Overlay controls
        SmallFloatingActionButton(
            onClick = {
                scale = 1f
                offset = Offset.Zero
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Center Map")
        }
        
        // Clear Map Button
        Button(
            onClick = {
                occupancyGrid.clear()
                odometryTracker.reset()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
        ) {
            Text("Haritayı Temizle", style = MaterialTheme.typography.labelSmall)
        }
    }
}
