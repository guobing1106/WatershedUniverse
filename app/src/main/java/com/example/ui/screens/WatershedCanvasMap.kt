package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.LandType
import com.example.data.models.WatershedCell
import kotlin.math.sin

@Composable
fun WatershedCanvasMap(
    grid: List<WatershedCell>,
    isSimulating: Boolean,
    simulationStep: Int,
    selectedCell: WatershedCell?,
    onCellSelected: (WatershedCell?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Infinite transition for fluid ambient animations (flowing river, rain, forest swaying)
    val infiniteTransition = rememberInfiniteTransition(label = "watershed_ambient")
    
    val timeState by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val rainOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_offset"
    )

    val selectionPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "selection_pulse"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("watershed_canvas_map_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "2D 生态高程沙盘 (Topological Map)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "点击地块直接选中 · 3D 叠层水文视角",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Active indicators
                if (isSimulating) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⛈️ 降水流失流动中...",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🛰️ 卫星实时测绘",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Canvas
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF131A22), // Deep night sky / space bg
                                Color(0xFF1B2431)
                            )
                        ),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                val density = LocalDensity.current
                val width = constraints.maxWidth.toFloat()
                val height = constraints.maxHeight.toFloat()

                // Layout parameters
                // Center-bottom projection anchor
                val centerX = width / 2f
                val centerY = height * 0.28f // Start near the upper-middle of the screen

                // Scale factors based on canvas width
                val tileWidth = width / 7.8f
                val tileHeight = tileWidth * 0.5f // 2:1 ratio for nice isometric view

                // Coordinates holder to help with tap detection
                val projectedCells = remember(grid, width, height) {
                    grid.map { cell ->
                        val r = cell.row
                        val c = cell.col
                        
                        // Elevation offset (higher rows are physically rendered higher)
                        val elevOffset = (4 - r) * (tileHeight * 0.45f)
                        
                        // Isometric calculation
                        // c flows to the right, r flows to the left
                        val x = centerX + (c - r) * tileWidth * 0.85f
                        val y = centerY + (r + c) * tileHeight * 0.75f - elevOffset

                        ProjectedCell(cell, x, y, tileWidth, tileHeight)
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(projectedCells) {
                            detectTapGestures { offset ->
                                // Hit-testing: Find closest cell center within tile distance
                                var closestCell: ProjectedCell? = null
                                var minDistance = Float.MAX_VALUE
                                
                                for (p in projectedCells) {
                                    val dx = offset.x - p.x
                                    val dy = offset.y - p.y
                                    val dist = dx * dx + dy * dy
                                    // Make sure it's within a bounding range roughly equivalent to tile size
                                    val maxHitDistance = (tileWidth * 0.8f) * (tileWidth * 0.8f)
                                    if (dist < maxHitDistance && dist < minDistance) {
                                        minDistance = dist
                                        closestCell = p
                                    }
                                }
                                onCellSelected(closestCell?.cell)
                            }
                        }
                ) {
                    // 1. Draw Background grid and contour layers
                    drawBackgroundValley(width, height)

                    // 2. Draw 3D cells in painter's order (back-to-front, row 0 to 4, column 0 to 4)
                    // We sort cells so that upper elements (lowest r+c) are drawn first, then front-most elements (highest r+c).
                    val sortedProjected = projectedCells.sortedWith(
                        compareBy<ProjectedCell> { it.cell.row + it.cell.col }
                            .thenBy { it.cell.row }
                    )

                    for (p in sortedProjected) {
                        val isCurrentActiveRow = isSimulating && p.cell.row == simulationStep
                        val isSelected = selectedCell?.row == p.cell.row && selectedCell?.col == p.cell.col

                        draw3DTile(
                            p = p,
                            isSelected = isSelected,
                            isSimulationActive = isCurrentActiveRow,
                            selectionPulse = selectionPulse,
                            time = timeState
                        )
                    }

                    // 3. Draw Beautiful central flowing river stream overlapping/winding down
                    drawFlowingRiver(projectedCells, timeState, isSimulating)

                    // 4. Draw Atmospheric Effects (Rain / Sunrays)
                    if (isSimulating) {
                        drawRainfall(width, height, rainOffset)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stylish interactive Map Legend
            MapLegendRow()
        }
    }
}

// Projection metadata helper
private data class ProjectedCell(
    val cell: WatershedCell,
    val x: Float,
    val y: Float,
    val tileWidth: Float,
    val tileHeight: Float
)

// Canvas Drawing Extensions
private fun DrawScope.drawBackgroundValley(width: Float, height: Float) {
    // Draw subtle mountain silhouette in the background
    val mountainPath = Path().apply {
        moveTo(0f, height * 0.35f)
        lineTo(width * 0.25f, height * 0.15f)
        lineTo(width * 0.5f, height * 0.3f)
        lineTo(width * 0.75f, height * 0.1f)
        lineTo(width, height * 0.4f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(
        path = mountainPath,
        color = Color(0xFF0F151F),
        alpha = 0.5f
    )

    // Draw contour elevation guidelines
    for (i in 1..3) {
        val y = height * (0.2f + i * 0.2f)
        drawLine(
            color = Color.White.copy(alpha = 0.04f),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private fun DrawScope.draw3DTile(
    p: ProjectedCell,
    isSelected: Boolean,
    isSimulationActive: Boolean,
    selectionPulse: Float,
    time: Float
) {
    val cell = p.cell
    val x = p.x
    val y = p.y
    val w = p.tileWidth
    val h = p.tileHeight

    // 1. Core Palette Definitions based on land use
    val (topColor, leftColor, rightColor) = when (cell.landType) {
        LandType.FOREST -> Triple(
            Color(0xFF2E6F40), // rich forest green
            Color(0xFF235531),
            Color(0xFF1B4125)
        )
        LandType.WETLAND -> Triple(
            Color(0xFF197278), // beautiful oceanic teal
            Color(0xFF13575C),
            Color(0xFF0E4044)
        )
        LandType.GRASSLAND -> Triple(
            Color(0xFF639A42), // lively grass green
            Color(0xFF4C7632),
            Color(0xFF395926)
        )
        LandType.AGRICULTURE -> Triple(
            Color(0xFFDCAE53), // crop golden gold
            Color(0xFFB08C42),
            Color(0xFF8C6F34)
        )
        LandType.URBAN -> Triple(
            Color(0xFF708090), // modern slate gray
            Color(0xFF56626E),
            Color(0xFF424B54)
        )
    }

    // Adjust color for active simulated row or pollution density
    val animatedTopColor = if (isSimulationActive) {
        // Bright shimmer pulse on the current row
        val pulse = (sin(time * 3f) + 1f) / 2f
        topColor.lerp(Color(0xFF00E5FF), 0.25f + pulse * 0.15f)
    } else if (cell.pollution > 0.15f) {
        // Blend in high toxic violet / dirty gray warning mud
        val amount = (cell.pollution / 100f).coerceIn(0.1f, 0.75f)
        topColor.lerp(Color(0xFF9C27B0), amount)
    } else {
        topColor
    }

    // Compute tile corners (Rhombus shape)
    val pTop = Offset(x, y - h / 2)
    val pRight = Offset(x + w / 2, y)
    val pBottom = Offset(x, y + h / 2)
    val pLeft = Offset(x - w / 2, y)

    // Compute column depth height (3D block thickness)
    val blockThickness = h * 0.7f

    // LEFT 3D Side face
    val leftFacePath = Path().apply {
        moveTo(pLeft.x, pLeft.y)
        lineTo(pBottom.x, pBottom.y)
        lineTo(pBottom.x, pBottom.y + blockThickness)
        lineTo(pLeft.x, pLeft.y + blockThickness)
        close()
    }
    drawPath(path = leftFacePath, color = leftColor)

    // RIGHT 3D Side face
    val rightFacePath = Path().apply {
        moveTo(pBottom.x, pBottom.y)
        lineTo(pRight.x, pRight.y)
        lineTo(pRight.x, pRight.y + blockThickness)
        lineTo(pBottom.x, pBottom.y + blockThickness)
        close()
    }
    drawPath(path = rightFacePath, color = rightColor)

    // TOP 3D Rhombus face
    val topFacePath = Path().apply {
        moveTo(pTop.x, pTop.y)
        lineTo(pRight.x, pRight.y)
        lineTo(pBottom.x, pBottom.y)
        lineTo(pLeft.x, pLeft.y)
        close()
    }
    drawPath(path = topFacePath, color = animatedTopColor)

    // Add subtle grid lines/borders to make top faces stand out
    drawPath(
        path = topFacePath,
        color = Color.White.copy(alpha = 0.08f),
        style = Stroke(width = 1.dp.toPx())
    )

    // 2. Draw land-use micro-illustrations inside the tile face
    val sizeScale = w * 0.14f
    drawMicroIcon(cell.landType, Offset(x, y - h * 0.1f), sizeScale, time, cell.row)

    // 3. Render pollution cloud or hazard dots
    if (cell.pollution > 0.2f) {
        val dotPulse = (sin(time * 2f + cell.col) + 1f) / 2f
        drawCircle(
            color = Color(0xFFFF5252).copy(alpha = 0.4f + dotPulse * 0.3f),
            radius = (w * 0.08f) * (0.8f + dotPulse * 0.2f),
            center = Offset(x - w * 0.15f, y + h * 0.15f)
        )
    }

    // 4. Highlight selection outline
    if (isSelected) {
        // Double pulsing neon border
        val pulseColor = Color(0xFF00E5FF).copy(alpha = selectionPulse)
        drawPath(
            path = topFacePath,
            color = pulseColor,
            style = Stroke(width = 2.5.dp.toPx())
        )
        
        // Dynamic neon halo circles at bottom point
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = selectionPulse * 0.3f),
            radius = w * 0.45f * (0.9f + selectionPulse * 0.1f),
            center = pBottom,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

private fun DrawScope.drawMicroIcon(
    type: LandType,
    center: Offset,
    scale: Float,
    time: Float,
    row: Int
) {
    when (type) {
        LandType.FOREST -> {
            // Draw beautiful evergreen pine trees with gentle wind sway
            val sway = sin(time + row) * 2.5f
            
            val tree1 = Path().apply {
                moveTo(center.x + sway, center.y - scale * 1.5f)
                lineTo(center.x - scale * 0.8f + sway, center.y + scale * 0.3f)
                lineTo(center.x + scale * 0.8f + sway, center.y + scale * 0.3f)
                close()
            }
            val tree2 = Path().apply {
                moveTo(center.x - scale * 0.6f + sway, center.y - scale * 0.5f)
                lineTo(center.x - scale * 1.2f + sway, center.y + scale * 0.8f)
                lineTo(center.x + scale * 0.1f + sway, center.y + scale * 0.8f)
                close()
            }
            drawPath(tree1, Color(0xFF1B4125))
            drawPath(tree2, Color(0xFF122C19))
        }
        LandType.WETLAND -> {
            // Draw marsh water ripples and dynamic cattail grass
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                radius = scale * 0.8f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            
            // Reed blade
            val sway = sin(time * 1.5f) * 2f
            drawLine(
                color = Color(0xFF2E6F40),
                start = Offset(center.x, center.y + scale * 0.5f),
                end = Offset(center.x - scale * 0.3f + sway, center.y - scale * 0.6f),
                strokeWidth = 2.dp.toPx()
            )
            // Reed brown tip
            drawCircle(
                color = Color(0xFF5C4033),
                radius = scale * 0.15f,
                center = Offset(center.x - scale * 0.3f + sway, center.y - scale * 0.5f)
            )
        }
        LandType.GRASSLAND -> {
            // Little grass shoots
            val sway = sin(time + row * 1.5f) * 1.5f
            drawLine(
                color = Color(0xFF81C784),
                start = Offset(center.x - scale * 0.4f, center.y + scale * 0.4f),
                end = Offset(center.x - scale * 0.6f + sway, center.y - scale * 0.4f),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = Color(0xFF81C784),
                start = Offset(center.x, center.y + scale * 0.4f),
                end = Offset(center.x + scale * 0.1f + sway, center.y - scale * 0.5f),
                strokeWidth = 1.5.dp.toPx()
            )
        }
        LandType.AGRICULTURE -> {
            // Tilled neat rows
            for (i in -1..1) {
                val dx = i * scale * 0.6f
                drawLine(
                    color = Color(0xFFE5C158).copy(alpha = 0.7f),
                    start = Offset(center.x - scale * 0.5f + dx, center.y + scale * 0.2f),
                    end = Offset(center.x + scale * 0.5f + dx, center.y - scale * 0.4f),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
        LandType.URBAN -> {
            // Draw modern block/cube houses with neon light windows
            val rectPath = Path().apply {
                moveTo(center.x - scale * 0.6f, center.y + scale * 0.5f)
                lineTo(center.x - scale * 0.6f, center.y - scale * 0.6f)
                lineTo(center.x + scale * 0.1f, center.y - scale * 0.9f)
                lineTo(center.x + scale * 0.7f, center.y - scale * 0.4f)
                lineTo(center.x + scale * 0.7f, center.y + scale * 0.5f)
                close()
            }
            drawPath(rectPath, Color(0xFF2D3748))
            
            // Tiny glowing light window
            val windowPulse = if (sin(time * 4f) > 0f) Color(0xFFFFEB3B) else Color(0xFF718096)
            drawCircle(
                color = windowPulse,
                radius = scale * 0.15f,
                center = Offset(center.x, center.y - scale * 0.2f)
            )
        }
    }
}

private fun DrawScope.drawFlowingRiver(
    projectedCells: List<ProjectedCell>,
    time: Float,
    isSimulating: Boolean
) {
    // Generate the path of the river following a central natural canyon route
    // Connecting center column cells downstream (c=2 and winding downstream)
    val riverKeypoints = listOf(
        Offset(0.1f, 0.1f), // Upstream peak
        Offset(0.2f, 0.2f)
    )

    // Let's dynamically map the precise physical coordinates of the center spine of the watershed
    val riverPathPoints = mutableListOf<Offset>()
    
    // Mountain water feed 1 (top side)
    projectedCells.find { it.cell.row == 0 && it.cell.col == 2 }?.let {
        riverPathPoints.add(Offset(it.x, it.y - it.tileHeight * 0.5f))
        riverPathPoints.add(Offset(it.x, it.y))
    }
    
    // Connect winding pathway through middle rows
    val pathRows = listOf(
        Pair(1, 2),
        Pair(2, 2),
        Pair(3, 2),
        Pair(4, 2)
    )
    for ((r, c) in pathRows) {
        projectedCells.find { it.cell.row == r && it.cell.col == c }?.let {
            riverPathPoints.add(Offset(it.x, it.y))
        }
    }

    // Coastal Delta discharge point (below row 4 estuary)
    projectedCells.find { it.cell.row == 4 && it.cell.col == 2 }?.let {
        riverPathPoints.add(Offset(it.x - it.tileWidth * 0.4f, it.y + it.tileHeight * 0.6f))
        riverPathPoints.add(Offset(it.x, it.y + it.tileHeight * 1.1f))
        riverPathPoints.add(Offset(it.x + it.tileWidth * 0.4f, it.y + it.tileHeight * 0.6f))
    }

    if (riverPathPoints.size < 2) return

    val riverPath = Path().apply {
        moveTo(riverPathPoints[0].x, riverPathPoints[0].y)
        for (i in 1 until riverPathPoints.size) {
            val prev = riverPathPoints[i - 1]
            val curr = riverPathPoints[i]
            // Draw organic smooth Bezier curve instead of hard straight lines
            val controlX = (prev.x + curr.x) / 2f
            val controlY = (prev.y + curr.y) / 2f
            quadraticTo(prev.x, prev.y, controlX, controlY)
        }
    }

    // Draw bottom shadow/glow of the river
    drawPath(
        path = riverPath,
        color = Color(0xFF00B0FF).copy(alpha = 0.3f),
        style = Stroke(width = 8.dp.toPx())
    )

    // Flowing water animated pulse stroke
    // A shifting dash pattern creates a moving downstream animation effect
    val dashPhase = -time * 30f
    drawPath(
        path = riverPath,
        color = if (isSimulating) Color(0xFF00E5FF) else Color(0xFF0091EA),
        style = Stroke(
            width = 4.5.dp.toPx(),
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                intervals = floatArrayOf(40f, 30f),
                phase = dashPhase
            )
        )
    )

    // Core central stream line
    drawPath(
        path = riverPath,
        color = Color.White.copy(alpha = 0.8f),
        style = Stroke(width = 1.5.dp.toPx())
    )
}

private fun DrawScope.drawRainfall(width: Float, height: Float, offset: Float) {
    // Elegant falling thin storm/rain lines
    val count = 20
    val spacing = width / count
    for (i in 0 until count) {
        val xStart = (i * spacing + offset * 0.15f) % width
        val yStart = (offset + i * 25f) % height
        
        drawLine(
            color = Color(0xFF4FC3F7).copy(alpha = 0.45f),
            start = Offset(xStart, yStart),
            end = Offset(xStart - 10f, yStart + 35f),
            strokeWidth = 1.2f.dp.toPx()
        )
    }
}

@Composable
fun MapLegendRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(name = "森林", color = Color(0xFF2E6F40), icon = "🌲")
        LegendItem(name = "草地", color = Color(0xFF639A42), icon = "🌱")
        LegendItem(name = "湿地", color = Color(0xFF197278), icon = "🪷")
        LegendItem(name = "农田", color = Color(0xFFDCAE53), icon = "🌾")
        LegendItem(name = "城镇", color = Color(0xFF708090), icon = "🏢")
    }
}

@Composable
fun LegendItem(name: String, color: Color, icon: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$icon $name",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Inline color helper extension
private fun Color.lerp(target: Color, fraction: Float): Color {
    val r = this.red + (target.red - this.red) * fraction
    val g = this.green + (target.green - this.green) * fraction
    val b = this.blue + (target.blue - this.blue) * fraction
    val a = this.alpha + (target.alpha - this.alpha) * fraction
    return Color(r, g, b, a)
}
