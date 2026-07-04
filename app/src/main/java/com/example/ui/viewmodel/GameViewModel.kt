package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.models.LandType
import com.example.data.models.SimulationLog
import com.example.data.models.WatershedCell
import com.example.data.models.WeatherScenario
import com.example.data.repository.GameRepository
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.Part
import com.example.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(private val repository: GameRepository) : ViewModel() {

    // --- Observable states ---
    private val _grid = MutableStateFlow<List<WatershedCell>>(emptyList())
    val grid: StateFlow<List<WatershedCell>> = _grid.asStateFlow()

    private val _budget = MutableStateFlow(300) // Starting budget
    val budget: StateFlow<Int> = _budget.asStateFlow()

    private val _seasonIndex = MutableStateFlow(1)
    val seasonIndex: StateFlow<Int> = _seasonIndex.asStateFlow()

    private val _currentWeather = MutableStateFlow(WeatherScenario.NORMAL_RAIN)
    val currentWeather: StateFlow<WeatherScenario> = _currentWeather.asStateFlow()

    // --- Active metrics ---
    private val _estuaryRunoff = MutableStateFlow(0.0f)
    val estuaryRunoff: StateFlow<Float> = _estuaryRunoff.asStateFlow()

    private val _waterQualityIndex = MutableStateFlow(100)
    val waterQualityIndex: StateFlow<Int> = _waterQualityIndex.asStateFlow()

    private val _biodiversityScore = MutableStateFlow(100)
    val biodiversityScore: StateFlow<Int> = _biodiversityScore.asStateFlow()

    private val _netRevenue = MutableStateFlow(0)
    val netRevenue: StateFlow<Int> = _netRevenue.asStateFlow()

    // --- UI State managers ---
    private val _selectedCell = MutableStateFlow<WatershedCell?>(null)
    val selectedCell: StateFlow<WatershedCell?> = _selectedCell.asStateFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _simulationStep = MutableStateFlow(0)
    val simulationStep: StateFlow<Int> = _simulationStep.asStateFlow()

    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _disasterEvent = MutableStateFlow<String?>(null)
    val disasterEvent: StateFlow<String?> = _disasterEvent.asStateFlow()

    // Observe historical simulation logs reactively from Room
    val historicalLogs: StateFlow<List<SimulationLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        resetGame()
    }

    // Initialize/Reset the watershed board with a degraded setup
    fun resetGame() {
        val initialGrid = mutableListOf<WatershedCell>()
        // elevation ranges from 500m (upstream row 0) to 10m (estuary row 4)
        val elevations = listOf(500, 350, 200, 100, 10)

        for (r in 0 until 5) {
            for (c in 0 until 5) {
                // Pre-configure a realistic partially-degraded watershed layout
                val type = when {
                    r == 0 && c % 2 == 0 -> LandType.FOREST // Some mountain forests
                    r == 0 -> LandType.GRASSLAND
                    r == 1 && c == 2 -> LandType.URBAN // Upstream high-elevation urban core sprawl
                    r == 1 -> LandType.AGRICULTURE // Agricultural expanding slopes
                    r == 2 && c % 2 == 1 -> LandType.URBAN // Midstream urban sprawl
                    r == 2 -> LandType.GRASSLAND
                    r == 3 && c == 1 -> LandType.WETLAND // Single degraded wetland
                    r == 3 -> LandType.AGRICULTURE // Intense midstream cropping
                    r == 4 && c == 4 -> LandType.URBAN // Coastal delta urban
                    else -> LandType.GRASSLAND
                }
                initialGrid.add(WatershedCell(r, c, elevations[r], type))
            }
        }
        _grid.value = initialGrid
        _budget.value = 350
        _seasonIndex.value = 1
        _currentWeather.value = WeatherScenario.NORMAL_RAIN
        _selectedCell.value = null
        _disasterEvent.value = null
        _aiResponse.value = null
        recalculateStaticMetrics()
    }

    fun selectCell(cell: WatershedCell?) {
        _selectedCell.value = cell
    }

    // Change a cell's land cover
    fun transformCell(cell: WatershedCell, newType: LandType) {
        val currentBudget = _budget.value
        val cost = getRestorationCost(newType)

        if (currentBudget >= cost) {
            _budget.value = currentBudget - cost
            val updatedGrid = _grid.value.map {
                if (it.row == cell.row && it.col == cell.col) {
                    it.copy(landType = newType)
                } else {
                    it
                }
            }
            _grid.value = updatedGrid
            _selectedCell.value = updatedGrid.find { it.row == cell.row && it.col == cell.col }
            recalculateStaticMetrics()
        }
    }

    // Get restoration costs, which are affected by Green Tech subsidy scenario
    fun getRestorationCost(type: LandType): Int {
        val baseCost = type.restorationCost
        return if (_currentWeather.value == WeatherScenario.GREEN_TECH && 
            (type == LandType.FOREST || type == LandType.WETLAND)) {
            (baseCost * 0.7f).toInt() // 30% discount
        } else {
            baseCost
        }
    }

    // Recalculates eco-scores and budget outputs prior to dynamic rain
    private fun recalculateStaticMetrics() {
        val forestCount = _grid.value.count { it.landType == LandType.FOREST }
        val wetlandCount = _grid.value.count { it.landType == LandType.WETLAND }
        val grasslandCount = _grid.value.count { it.landType == LandType.GRASSLAND }
        val agricultureCount = _grid.value.count { it.landType == LandType.AGRICULTURE }
        val urbanCount = _grid.value.count { it.landType == LandType.URBAN }

        // Compute biodiversity score out of 100
        val rawBio = (forestCount * 12) + (wetlandCount * 16) + (grasslandCount * 5) + 15
        _biodiversityScore.value = rawBio.coerceIn(10, 100)

        // Compute net season economic revenue (taxes & crops minus reservation upkeep)
        val agriYield = (agricultureCount * LandType.AGRICULTURE.economicBenefit * _currentWeather.value.agriculturalPremium).toInt()
        val urbanTax = urbanCount * LandType.URBAN.economicBenefit
        val wetlandEcoEco = wetlandCount * LandType.WETLAND.economicBenefit
        val forestryCost = (forestCount * 5) + (wetlandCount * 10) // Small conservation maintenance cost

        _netRevenue.value = agriYield + urbanTax + wetlandEcoEco - forestryCost
    }

    // Clean simulation visual data fields
    private fun clearFlowStates() {
        _grid.value = _grid.value.map { it.copy(waterFlow = 0.0f, pollution = 0.0f) }
    }

    // PHYSICAL HYDROLOGY SIMULATION LOOP
    fun runSeasonalSimulation() {
        if (_isSimulating.value) return
        _isSimulating.value = true
        _disasterEvent.value = null

        viewModelScope.launch {
            clearFlowStates()
            val weather = _currentWeather.value
            val baseRain = 2.0f * weather.rainfallIntensity

            // Run step-by-step physical water progression downstream
            // There are 5 elevations (rows 0, 1, 2, 3, 4)
            for (step in 0..5) {
                _simulationStep.value = step
                delay(800) // Delay for high-fidelity visual animation progression

                val nextGrid = _grid.value.map { it.copy() } // Deep copy grid states

                // 1. Process local rainfall and pollution inputs at current row
                if (step < 5) {
                    val targetRow = step
                    for (col in 0 until 5) {
                        val cell = nextGrid.find { it.row == targetRow && it.col == col } ?: continue
                        
                        // Water input based on land coefficient
                        val runoffAmount = baseRain * cell.landType.runoffCoefficient
                        cell.waterFlow += runoffAmount

                        // Local pollution based on land type and weather factor
                        val pollutionAmount = cell.landType.pollutionRate * weather.basePollutionFactor
                        cell.pollution += pollutionAmount
                    }
                }

                // 2. Downstream flow progression (water flows from row to row + 1)
                // Water from row r splits among row r+1 neighbors
                for (r in 0..4) {
                    for (c in 0 until 5) {
                        val cell = nextGrid.find { it.row == r && it.col == c } ?: continue
                        if (cell.waterFlow > 0.05f) {
                            val waterToFlow = cell.waterFlow * 0.7f // 70% flows downstream
                            val pollutionToFlow = cell.pollution * 0.7f

                            // Retain 30% in the current cell
                            cell.waterFlow -= waterToFlow
                            cell.pollution -= pollutionToFlow

                            if (r < 4) {
                                // Downstream neighbors to distribute water
                                val nextNeighbors = mutableListOf<WatershedCell>()
                                nextGrid.find { it.row == r + 1 && it.col == c }?.let { nextNeighbors.add(it) }
                                if (c > 0) nextGrid.find { it.row == r + 1 && it.col == c - 1 }?.let { nextNeighbors.add(it) }
                                if (c < 4) nextGrid.find { it.row == r + 1 && it.col == c + 1 }?.let { nextNeighbors.add(it) }

                                if (nextNeighbors.isNotEmpty()) {
                                    val waterPerNeighbor = waterToFlow / nextNeighbors.size
                                    val pollutionPerNeighbor = pollutionToFlow / nextNeighbors.size

                                    for (neighbor in nextNeighbors) {
                                        // Forests and Wetlands filter and absorb!
                                        var finalWaterInput = waterPerNeighbor
                                        var finalPollutionInput = pollutionPerNeighbor

                                        when (neighbor.landType) {
                                            LandType.WETLAND -> {
                                                finalWaterInput *= 0.5f // Wetland absorbs 50% of incoming water volume
                                                finalPollutionInput *= 0.3f // Wetland filters 70% of pollution
                                            }
                                            LandType.FOREST -> {
                                                finalWaterInput *= 0.7f // Forest absorbs 30% of incoming water volume
                                                finalPollutionInput *= 0.5f // Forest filters 50% of pollution
                                            }
                                            LandType.GRASSLAND -> {
                                                finalWaterInput *= 0.9f
                                                finalPollutionInput *= 0.8f
                                            }
                                            else -> { /* Urban / Ag do not absorb or filter */ }
                                        }

                                        neighbor.waterFlow += finalWaterInput
                                        neighbor.pollution += finalPollutionInput
                                    }
                                }
                            } else {
                                // If already in estuary (Row 4), remaining water flows out to river mouth
                                _estuaryRunoff.value = (_estuaryRunoff.value + waterToFlow).coerceAtLeast(0f)
                            }
                        }
                    }
                }

                _grid.value = nextGrid
            }

            // --- Post Simulation Calculations ---
            // Aggregate Estuary total runoff and remaining pollution
            val estuaryCells = _grid.value.filter { it.row == 4 }
            val accumulatedRunoff = estuaryCells.sumOf { it.waterFlow.toDouble() }.toFloat() + _estuaryRunoff.value
            val averagePollution = estuaryCells.map { it.pollution }.average().toFloat()

            _estuaryRunoff.value = accumulatedRunoff
            
            // Water Quality Index: 100 - (pollution penalty). Min 0, Max 100
            val calculatedWQI = (100 - (averagePollution * 150)).toInt().coerceIn(0, 100)
            _waterQualityIndex.value = calculatedWQI

            // Re-eval net business metrics
            recalculateStaticMetrics()

            // Apply economy outcome
            val netRev = _netRevenue.value
            _budget.value = (_budget.value + netRev).coerceAtLeast(0)

            // Trigger natural disasters or events based on results
            var disasterMsg: String? = null
            var finalDeduction = 0

            if (accumulatedRunoff > 6.2f) {
                disasterMsg = "⚠️ 洪涝灾害 (Severe Flood Disaster)! " +
                        "下游累积径流值高达 ${String.format("%.1f", accumulatedRunoff)} (安全阈值: 6.2)。" +
                        "由于上中游保水林地不足，洪水冲毁了下游防洪堤及部分农田，紧急抢修扣除 $60 资金，并导致生态急剧恶化。"
                finalDeduction += 60
                _biodiversityScore.value = (_biodiversityScore.value - 25).coerceAtLeast(0)
            } else if (calculatedWQI < 40) {
                disasterMsg = "⚠️ 蓝藻爆发与鱼类死亡 (Severe Eutrophication)! " +
                        "下游水质指数 WQI 跌至 $calculatedWQI (安全阈值: 40)。" +
                        "由于城镇及农田过多，氮磷残留严重超标。政府环保部门开出 $40 罚单，生态多样性丧失严重。"
                finalDeduction += 40
                _biodiversityScore.value = (_biodiversityScore.value - 20).coerceAtLeast(0)
            }

            if (finalDeduction > 0) {
                _budget.value = (_budget.value - finalDeduction).coerceAtLeast(0)
                _disasterEvent.value = disasterMsg
            }

            // Save records to Room database
            saveSeasonLog(weather, accumulatedRunoff, calculatedWQI)

            // Increment Season Counter and Roll Next Weather Event
            _seasonIndex.value += 1
            rollNextWeather()

            _isSimulating.value = false
        }
    }

    private fun rollNextWeather() {
        val scenarios = WeatherScenario.values()
        // Randomly roll, but bias towards normal rain
        val randomRoll = (0..9).random()
        _currentWeather.value = when (randomRoll) {
            in 0..4 -> WeatherScenario.NORMAL_RAIN
            5, 6 -> WeatherScenario.HEAVY_STORM
            7 -> WeatherScenario.DROUGHT
            8 -> WeatherScenario.ACID_RAIN
            else -> WeatherScenario.GREEN_TECH
        }
    }

    private suspend fun saveSeasonLog(weather: WeatherScenario, runoff: Float, wqi: Int) {
        val forestCount = _grid.value.count { it.landType == LandType.FOREST }
        val wetlandCount = _grid.value.count { it.landType == LandType.WETLAND }
        val grasslandCount = _grid.value.count { it.landType == LandType.GRASSLAND }
        val agricultureCount = _grid.value.count { it.landType == LandType.AGRICULTURE }
        val urbanCount = _grid.value.count { it.landType == LandType.URBAN }

        val log = SimulationLog(
            seasonIndex = _seasonIndex.value,
            weatherName = weather.displayName,
            rainfallIntensity = weather.rainfallIntensity,
            totalRunoff = runoff,
            waterQualityIndex = wqi,
            biodiversityScore = _biodiversityScore.value,
            netRevenue = _netRevenue.value,
            budgetAfter = _budget.value,
            forestCount = forestCount,
            wetlandCount = wetlandCount,
            urbanCount = urbanCount,
            agricultureCount = agricultureCount,
            grasslandCount = grasslandCount
        )
        repository.insertLog(log)
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllLogs()
            resetGame()
        }
    }

    // AI SCIENTIFIC CONSULTATION ENGINE
    fun consultAiHydrologist() {
        if (_isAiLoading.value) return
        _isAiLoading.value = true
        _aiResponse.value = "AI 水文学家正在深入诊断您的流域生态，并准备学术优化方案..."

        viewModelScope.launch {
            val forestCount = _grid.value.count { it.landType == LandType.FOREST }
            val wetlandCount = _grid.value.count { it.landType == LandType.WETLAND }
            val grasslandCount = _grid.value.count { it.landType == LandType.GRASSLAND }
            val agricultureCount = _grid.value.count { it.landType == LandType.AGRICULTURE }
            val urbanCount = _grid.value.count { it.landType == LandType.URBAN }

            // Construct compact visual representation of the grid for Gemini
            val gridRepresentation = StringBuilder()
            for (r in 0 until 5) {
                gridRepresentation.append("行 $r (高程: ${listOf(500, 350, 200, 100, 10)[r]}m): ")
                for (c in 0 until 5) {
                    val cell = _grid.value.find { it.row == r && it.col == c }
                    val typeChar = when (cell?.landType) {
                        LandType.FOREST -> "🌲森林"
                        LandType.WETLAND -> "🌾湿地"
                        LandType.GRASSLAND -> "🌱草地"
                        LandType.AGRICULTURE -> "🌾农田"
                        LandType.URBAN -> "🏢城镇"
                        null -> "❓"
                    }
                    gridRepresentation.append("[$typeChar]")
                }
                gridRepresentation.append("\n")
            }

            val prompt = """
                您是《流域乾坤 (Watershed Universe)》游戏中的资深学术级水文与生态策略学顾问。
                请针对当前第 ${_seasonIndex.value} 季度的流域模拟数据和格局进行一次极为专业、富有洞察力的中文分析和诊断。

                【当前流域格局 (行0为上游山顶，行4为下游出海口/河口湾)】:
                ${gridRepresentation.toString()}

                【最新模拟环境与KPI指标】:
                - 天气状况: ${_currentWeather.value.displayName} (${_currentWeather.value.description})
                - 下游河口累积径流量 (洪涝风险): ${String.format("%.2f", _estuaryRunoff.value)} (安全上限: 6.20)
                - 水质指数 (WQI): ${_waterQualityIndex.value} / 100 (低质阈值: <40)
                - 生物多样性分值: ${_biodiversityScore.value} / 100
                - 本季净收益: ${_netRevenue.value} 金币 (当前金币存量: ${_budget.value})

                【土地分类统计】:
                - 森林: $forestCount 个单元格
                - 湿地: $wetlandCount 个单元格
                - 草地: $grasslandCount 个单元格
                - 农田: $agricultureCount 个单元格
                - 城镇: $urbanCount 个单元格

                请严格按以下三板块进行学术化、精简的 markdown 中文撰写，语气应严谨且对玩家富启发性，避免套话：
                1. **🔍 流域诊断**: 剖析空间格局上的病灶（例如：“上游大量水泥城镇硬化阻碍入渗造成径流洪峰”或“农业集中连片缺乏河岸带森林过滤”等）。
                2. **💡 水文学原理科普**: 针对当前问题，科普 1 个核心学术概念（如：“海绵城市/海绵流域效应 (Sponge Watershed)”，“非点源污染 (Non-point Source Pollution)”，“河岸缓冲带 (Riparian Buffer Zones)” 或 “不透水表面径流系数”），生动解释其物理机理。
                3. **🛠️ 流域综合治理提案**: 给出 2 条针对性的空间治理操作指令（指明行列和转换，例如：建议将 [Row 1, Col 2] 的城镇拆迁恢复为森林以延缓地表径流；或在 [Row 3, Col 1] 农业区边缘新建湿地缓冲）。

                字数控制在 450 字以内。
            """.trimIndent()

            try {
                val systemInstructionText = "You are an expert watershed hydrologist advising a player on eco-restoration. Give an academic, precise, highly encouraging and beautiful markdown response in Chinese."
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))
                )

                val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                _aiResponse.value = responseText ?: "AI顾问暂未获取到答复，请检查您的Gemini API Key设置。"
            } catch (e: Exception) {
                _aiResponse.value = "⚠️ 诊断失败: ${e.localizedMessage}\n\n提示：请确保在 AI Studio 侧边栏的「Secrets」面板中配置了合法的 `GEMINI_API_KEY`，并保持设备网络连通。"
            } finally {
                _isAiLoading.value = false
            }
        }
    }
}

// Factory to inject repository into ViewModel without complex frameworks
class GameViewModelFactory(private val repository: GameRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
