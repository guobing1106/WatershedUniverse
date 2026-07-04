package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.models.LandType
import com.example.data.models.SimulationLog
import com.example.data.models.WatershedCell
import com.example.data.models.WeatherScenario
import com.example.ui.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    // Collect game state from viewModel
    val grid by viewModel.grid.collectAsStateWithLifecycle()
    val budget by viewModel.budget.collectAsStateWithLifecycle()
    val seasonIndex by viewModel.seasonIndex.collectAsStateWithLifecycle()
    val currentWeather by viewModel.currentWeather.collectAsStateWithLifecycle()
    val estuaryRunoff by viewModel.estuaryRunoff.collectAsStateWithLifecycle()
    val waterQualityIndex by viewModel.waterQualityIndex.collectAsStateWithLifecycle()
    val biodiversityScore by viewModel.biodiversityScore.collectAsStateWithLifecycle()
    val netRevenue by viewModel.netRevenue.collectAsStateWithLifecycle()
    val selectedCell by viewModel.selectedCell.collectAsStateWithLifecycle()
    val isSimulating by viewModel.isSimulating.collectAsStateWithLifecycle()
    val simulationStep by viewModel.simulationStep.collectAsStateWithLifecycle()
    val aiResponse by viewModel.aiResponse.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val disasterEvent by viewModel.disasterEvent.collectAsStateWithLifecycle()
    val historicalLogs by viewModel.historicalLogs.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Sandbox & Simulator, 1: AI Advisor, 2: Records & Analytics

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "流域乾坤 (Watershed Universe)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.resetGame() },
                        modifier = Modifier.testTag("reset_game_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Game",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Hero Dashboard Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_watershed_banner),
                    contentDescription = "Watershed Hero Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Backdrop scrim for legibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xAA000000)),
                                startY = 0f
                            )
                        )
                )
                // App Title Overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "生态综合治理模拟系统",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "季候周期: 第 $seasonIndex 季度  •  天气: ${currentWeather.displayName}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Material 3 Segmented Navigation Tabs
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("流域沙盘", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("AI 科学诊断", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("治理台账", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Disaster notifications if any
            disasterEvent?.let { event ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Disaster",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = event,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Dynamic Tab Contents
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    0 -> SandboxTab(
                        grid = grid,
                        budget = budget,
                        estuaryRunoff = estuaryRunoff,
                        waterQualityIndex = waterQualityIndex,
                        biodiversityScore = biodiversityScore,
                        netRevenue = netRevenue,
                        currentWeather = currentWeather,
                        selectedCell = selectedCell,
                        isSimulating = isSimulating,
                        simulationStep = simulationStep,
                        onCellSelected = { viewModel.selectCell(it) },
                        onTransformCell = { cell, type -> viewModel.transformCell(cell, type) },
                        onSimulate = { viewModel.runSeasonalSimulation() },
                        getRestorationCost = { viewModel.getRestorationCost(it) }
                    )
                    1 -> AiAdvisorTab(
                        aiResponse = aiResponse,
                        isAiLoading = isAiLoading,
                        onTriggerConsult = { viewModel.consultAiHydrologist() }
                    )
                    2 -> HistoricalLogsTab(
                        logs = historicalLogs,
                        onClearLogs = { viewModel.clearAllHistory() }
                    )
                }
            }
        }
    }
}

// ======================== TAB 1: SANDBOX & SIMULATOR ========================

@Composable
fun SandboxTab(
    grid: List<WatershedCell>,
    budget: Int,
    estuaryRunoff: Float,
    waterQualityIndex: Int,
    biodiversityScore: Int,
    netRevenue: Int,
    currentWeather: WeatherScenario,
    selectedCell: WatershedCell?,
    isSimulating: Boolean,
    simulationStep: Int,
    onCellSelected: (WatershedCell?) -> Unit,
    onTransformCell: (WatershedCell, LandType) -> Unit,
    onSimulate: () -> Unit,
    getRestorationCost: (LandType) -> Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        // Core KPIs Row Cards
        KPIsDashboard(
            budget = budget,
            estuaryRunoff = estuaryRunoff,
            wqi = waterQualityIndex,
            biodiversity = biodiversityScore,
            netRevenue = netRevenue
        )

        Spacer(modifier = Modifier.height(12.dp))

        // View Mode Segmented Control (0 = Grid, 1 = 2D Map Canvas)
        var viewMode by remember { mutableStateOf(1) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { viewMode = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewMode == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (viewMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("2D 生态沙盘 (Canvas)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = { viewMode = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewMode == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (viewMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("规划数字网格 (Grid)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Animated Switch between 2D Canvas Map and 5x5 Grid Section
        AnimatedContent(
            targetState = viewMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
            },
            label = "view_mode_switch"
        ) { mode ->
            if (mode == 1) {
                // Render the interactive 2D Canvas Map of the watershed
                WatershedCanvasMap(
                    grid = grid,
                    isSimulating = isSimulating,
                    simulationStep = simulationStep,
                    selectedCell = selectedCell,
                    onCellSelected = onCellSelected,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Grid Elevation Headers & 5x5 Grid Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "流域高程沙盘 (5x5)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "山地山脊 (上游0行) ➔ 蔚蓝河口 (下游4行)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSimulating) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("正流动至第 ${simulationStep} 行...", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Text(" 部署季  ", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Render 5x5 grid cells
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (r in 0 until 5) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Elevation guide marker for each row
                                    val elevText = when (r) {
                                        0 -> "山脊\n500m"
                                        1 -> "山地\n350m"
                                        2 -> "中游\n200m"
                                        3 -> "河谷\n100m"
                                        4 -> "河口\n10m"
                                        else -> ""
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(42.dp)
                                            .height(58.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                                RoundedCornerShape(4.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = elevText,
                                            fontSize = 9.sp,
                                            lineHeight = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // 5 Columns
                                    for (c in 0 until 5) {
                                        val cell = grid.find { it.row == r && it.col == c }
                                        if (cell != null) {
                                            val isCurrentActiveRow = isSimulating && r == simulationStep
                                            val isSelected = selectedCell?.row == r && selectedCell?.col == c

                                            WatershedCellItem(
                                                cell = cell,
                                                isSelected = isSelected,
                                                isActiveSimulationRow = isCurrentActiveRow,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(58.dp)
                                                    .clickable(enabled = !isSimulating) {
                                                        onCellSelected(cell)
                                                    }
                                                    .testTag("cell_${r}_${c}")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Cell Strategy Inspector & Zoning Control Panel
        AnimatedVisibility(
            visible = selectedCell != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            selectedCell?.let { cell ->
                ZoningControlPanel(
                    cell = cell,
                    budget = budget,
                    onTransform = { onTransformCell(cell, it) },
                    getRestorationCost = getRestorationCost
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Giant filled action button to run simulator
        Button(
            onClick = { onSimulate() },
            enabled = !isSimulating,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("simulate_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isSimulating) Icons.Default.Cyclone else Icons.Default.WaterDrop,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSimulating) "正在运行地表水文流动模拟..." else "运行本季降水模拟 (Rainfall & Flow)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// Dashboard widgets displaying critical KPIs with colored indicators
@Composable
fun KPIsDashboard(
    budget: Int,
    estuaryRunoff: Float,
    wqi: Int,
    biodiversity: Int,
    netRevenue: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "本季实时流域健康指标",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 1st row: Budget, Revenue & Biodiversity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Budget Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("当前金币", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$budget", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = if (netRevenue >= 0) "+$netRevenue/季" else "$netRevenue/季",
                            color = if (netRevenue >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }

                // Water Quality (WQI) Card
                val wqiColor = when {
                    wqi >= 80 -> Color(0xFF2E7D32) // Good
                    wqi >= 50 -> Color(0xFFEF6C00) // Moderate
                    else -> Color(0xFFC62828) // Bad/Disaster
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = wqiColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("水质 WQI", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$wqi / 100", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = wqiColor)
                        Text(
                            text = when {
                                wqi >= 80 -> "优等水质"
                                wqi >= 50 -> "中度污染"
                                else -> "爆发红潮"
                            },
                            color = wqiColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Biodiversity Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Forest, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("生态多样性", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$biodiversity%", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.LightGray)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(biodiversity / 100f)
                                    .background(Color(0xFF2E7D32))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Estuary Runoff Flood Risk bar gauge
            val runoffColor = when {
                estuaryRunoff > 6.2f -> Color(0xFFC62828) // Critical flood
                estuaryRunoff > 4.5f -> Color(0xFFEF6C00) // Warning
                else -> Color(0xFF2E7D32) // Safe
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Flood,
                        contentDescription = null,
                        tint = runoffColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("下游河口累积地表径流 (Flood Level)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${String.format("%.2f", estuaryRunoff)} / 6.20",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = runoffColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Progress track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.LightGray.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((estuaryRunoff / 8.0f).coerceAtMost(1.0f))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF81C784), Color(0xFFFFB74D), Color(0xFFE57373))
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

// Single custom styled watershed cell in the grid
@Composable
fun WatershedCellItem(
    cell: WatershedCell,
    isSelected: Boolean,
    isActiveSimulationRow: Boolean,
    modifier: Modifier = Modifier
) {
    // Colors of cells
    val (bgColors, symbol, colorText) = when (cell.landType) {
        LandType.FOREST -> Triple(
            listOf(Color(0xFF1B5E20), Color(0xFF2E7D32)),
            "🌲",
            Color.White
        )
        LandType.WETLAND -> Triple(
            listOf(Color(0xFF006064), Color(0xFF00838F)),
            "🌾",
            Color.White
        )
        LandType.GRASSLAND -> Triple(
            listOf(Color(0xFF4CAF50), Color(0xFF81C784)),
            "🌱",
            Color(0xFF1B5E20)
        )
        LandType.AGRICULTURE -> Triple(
            listOf(Color(0xFFFBC02D), Color(0xFFFFF176)),
            "🌾",
            Color(0xFF5D4037)
        )
        LandType.URBAN -> Triple(
            listOf(Color(0xFF455A64), Color(0xFF78909C)),
            "🏢",
            Color.White
        )
    }

    // Interactive scale / animation when active
    val borderModifier = when {
        isSelected -> Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
        isActiveSimulationRow -> Modifier.border(2.dp, Color(0xFF2196F3), RoundedCornerShape(6.dp))
        else -> Modifier.border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.verticalGradient(bgColors))
            .then(borderModifier)
    ) {
        // Grid content
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = symbol, fontSize = 16.sp)
            Text(
                text = "${cell.row},${cell.col}",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = colorText.copy(alpha = 0.7f)
            )
        }

        // Active simulation dynamic flows visual overlay
        if (cell.waterFlow > 0.05f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x7F2196F3)) // Cyan water flow overlay
            ) {
                // Wave/ripple text symbol
                Text(
                    text = "💧 " + String.format("%.1f", cell.waterFlow),
                    fontSize = 8.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        if (cell.pollution > 0.05f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(Color(0x809C27B0)) // Purple toxic pollution top overlay
                    .align(Alignment.BottomCenter)
            ) {
                Text(
                    text = "☣️ " + String.format("%.1f", cell.pollution),
                    fontSize = 7.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

// Bottom sheet/inspection pane to transform selected cells
@Composable
fun ZoningControlPanel(
    cell: WatershedCell,
    budget: Int,
    onTransform: (LandType) -> Unit,
    getRestorationCost: (LandType) -> Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "坐标 [Row ${cell.row}, Col ${cell.col}]",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Badge(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(" 高程: ${cell.elevation}m ")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "当前覆被: ${cell.landType.displayName}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = cell.landType.description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            Text(
                text = "综合施策：改变土地用途或恢复生态 (Restoration)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Grid of conversion buttons
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LandType.values().forEach { targetType ->
                    if (targetType != cell.landType) {
                        val cost = getRestorationCost(targetType)
                        val isAffordable = budget >= cost

                        Button(
                            onClick = { onTransform(targetType) },
                            enabled = isAffordable,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (targetType) {
                                    LandType.FOREST -> Color(0xFF2E7D32)
                                    LandType.WETLAND -> Color(0xFF00838F)
                                    LandType.GRASSLAND -> Color(0xFF81C784)
                                    LandType.AGRICULTURE -> Color(0xFFFBC02D)
                                    LandType.URBAN -> Color(0xFF78909C)
                                },
                                contentColor = if (targetType == LandType.AGRICULTURE) Color.DarkGray else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("convert_${targetType.name}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when (targetType) {
                                            LandType.FOREST -> "🌲 恢复为森林"
                                            LandType.WETLAND -> "🌾 恢复为生态湿地"
                                            LandType.GRASSLAND -> "🌱 种植生态草地"
                                            LandType.AGRICULTURE -> "🌾 垦殖农田"
                                            LandType.URBAN -> "🏢 扩建城镇化商业区"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$${cost}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!isAffordable) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("(资金不足)", fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ======================== TAB 2: AI ACADEMIC ADVISOR ========================

@Composable
fun AiAdvisorTab(
    aiResponse: String?,
    isAiLoading: Boolean,
    onTriggerConsult: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Hydrologist Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI Advisor",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "资深学术水文学家",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "提供专业流域空间拓扑分析与面源污染学术治理诊断",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Diagnostic response panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔬 流域学术诊断书 (Scientific Advice)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (isAiLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                if (aiResponse != null) {
                    AcademicMarkdownText(text = aiResponse)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "本季尚未生成诊断。请点击下方按钮，水文AI将对您当前规划进行深度学术解析！",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onTriggerConsult() },
                    enabled = !isAiLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("consult_ai_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAiLoading) "水文AI运算中..." else "请求水文学家进行学术诊断",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Professional Academic Glossary cards
        Text(
            text = "📚 流域生态科学名词科普 (Syllabus)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        GlossaryCard(
            title = "海绵流域效应 (Sponge Watershed)",
            concept = "不透水地表蓄纳水分能力差，而湿地和森林通过发达的根系与有机质土壤层，能像海绵一样在高水分时吸水，在缺水时缓慢吐水，从而起到‘削峰补枯’的作用。"
        )
        Spacer(modifier = Modifier.height(6.dp))
        GlossaryCard(
            title = "面源污染 (Non-Point Source Pollution)",
            concept = "与工厂排污口等点源污染不同，农田化肥残留流失、城市雨水冲刷地表垃圾等随地表径流大面积汇入河道的污染。河岸带森林和湿地是拦截过滤此类污染的核心生态设施。"
        )
        Spacer(modifier = Modifier.height(6.dp))
        GlossaryCard(
            title = "径流系数 (Runoff Coefficient)",
            concept = "地表流出的地表径流量与该区域降雨量的比值。城镇硬化地表系数通常在0.8-0.9以上，而森林草地则可低至0.1-0.2，是衡量流域洪峰负荷的核心参数。"
        )
    }
}

// Basic markdown text renderer for clean AI outputs in Compose
@Composable
fun AcademicMarkdownText(text: String) {
    val lines = text.split("\n")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("###") -> {
                    val header = trimmed.replace("###", "").trim()
                    Text(
                        text = header,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("**") && trimmed.endsWith("**") -> {
                    val bold = trimmed.replace("**", "").trim()
                    Text(
                        text = bold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                trimmed.startsWith("1.") || trimmed.startsWith("2.") || trimmed.startsWith("3.") -> {
                    Text(
                        text = trimmed,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("-") || trimmed.startsWith("*") -> {
                    val bullet = trimmed.substring(1).trim()
                    Row(
                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = bullet,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                trimmed.isEmpty() -> {
                    Spacer(modifier = Modifier.height(2.dp))
                }
                else -> {
                    // Standard text block
                    Text(
                        text = trimmed,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun GlossaryCard(title: String, concept: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = concept,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


// ======================== TAB 3: HISTORICAL LOGS ========================

@Composable
fun HistoricalLogsTab(
    logs: List<SimulationLog>,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📋 历季生态治理台账",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "系统保存的每一季度水文与生态模拟记录",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (logs.isNotEmpty()) {
                TextButton(
                    onClick = onClearLogs,
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重置历史", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (logs.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = Color.Gray.copy(alpha = 0.4f),
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "暂无治理历史账单。\n请在“流域沙盘”中运行降雨模拟！",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        } else {
            // Summary progress indicator card
            EcosystemProgressReportCard(logs = logs)
            Spacer(modifier = Modifier.height(10.dp))

            // Logs stream
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    SimulationLogItem(log = log)
                }
            }
        }
    }
}

@Composable
fun EcosystemProgressReportCard(logs: List<SimulationLog>) {
    val firstLog = logs.lastOrNull() // Earliest record
    val latestLog = logs.firstOrNull() // Latest record

    if (firstLog != null && latestLog != null) {
        val wqiImprovement = latestLog.waterQualityIndex - firstLog.waterQualityIndex
        val bioImprovement = latestLog.biodiversityScore - firstLog.biodiversityScore

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "📈 流域生态修复成效评估",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("河口水质净改善:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                        Text(
                            text = if (wqiImprovement >= 0) "+$wqiImprovement WQI" else "$wqiImprovement WQI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (wqiImprovement >= 0) Color(0xFF1B5E20) else Color(0xFFC62828)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("生物多样性增幅:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                        Text(
                            text = if (bioImprovement >= 0) "+$bioImprovement%" else "$bioImprovement%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (bioImprovement >= 0) Color(0xFF1B5E20) else Color(0xFFC62828)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        wqiImprovement > 20 && bioImprovement > 15 -> "🌟 卓越的水文战略！您成功地将该破碎流域打造为了“海绵流域示范区”。"
                        wqiImprovement >= 0 -> "👍 流域呈现稳步好转迹象，请继续增补湿地和林地缓冲。"
                        else -> "⚠️ 流域呈过度开发与退化态势，极易诱发下游洪涝与富营养化，请尽快撤并重度水泥硬化单元！"
                    },
                    fontSize = 9.5.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun SimulationLogItem(log: SimulationLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text(" 季度 ${log.seasonIndex} ", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "气候: ${log.weatherName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "预算结余: $${log.budgetAfter}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Metric values comparison inside card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Runoff Log
                Column {
                    Text("河口径流", fontSize = 9.sp, color = Color.Gray)
                    Text(
                        text = "${String.format("%.2f", log.totalRunoff)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (log.totalRunoff > 6.2f) Color.Red else Color.DarkGray
                    )
                }
                // Water Quality Log
                Column {
                    Text("水质 WQI", fontSize = 9.sp, color = Color.Gray)
                    Text(
                        text = "${log.waterQualityIndex}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (log.waterQualityIndex < 40) Color.Red else Color(0xFF2E7D32)
                    )
                }
                // Bio score
                Column {
                    Text("生物多样性", fontSize = 9.sp, color = Color.Gray)
                    Text(
                        text = "${log.biodiversityScore}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                // Land inventory count
                Column {
                    Text("格格局比例", fontSize = 9.sp, color = Color.Gray)
                    Text(
                        text = "🌲${log.forestCount} 🌾${log.wetlandCount} 🏢${log.urbanCount} 🚜${log.agricultureCount}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
