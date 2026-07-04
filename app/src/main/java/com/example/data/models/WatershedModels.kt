package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LandType(
    val displayName: String,
    val description: String,
    val runoffCoefficient: Float, // Higher value means less absorption, more surface runoff
    val pollutionRate: Float, // Higher value means more chemical runoff (fertilizer/industrial)
    val economicBenefit: Int, // Revenue generated per season
    val restorationCost: Int, // Cost to convert other land types to this type
    val ecoPoints: Int // Ecosystem health points (out of 100)
) {
    FOREST("森林 (Forest)", "Eco core: Excellent water retention and filtration, zero pollution.", 0.2f, 0.0f, 0, 150, 95),
    WETLAND("湿地 (Wetland)", "Water buffer: Absorbs major storm volume, filters toxic chemicals.", 0.1f, 0.0f, 5, 200, 100),
    GRASSLAND("草地 (Grassland)", "Eco balance: Natural low-cost ground cover with neutral effects.", 0.4f, 0.0f, 0, 80, 60),
    AGRICULTURE("农田 (Agriculture)", "Economic: Regular grain yields but causes nitrogen runoff.", 0.6f, 0.3f, 35, 50, 30),
    URBAN("城镇 (Urban)", "High yield: Large economic engine, but 100% impervious & high pollution.", 0.9f, 0.8f, 75, 100, 10)
}

data class WatershedCell(
    val row: Int,
    val col: Int,
    val elevation: Int, // Elevated area down to river delta (higher row flows to lower row)
    var landType: LandType,
    var waterFlow: Float = 0.0f, // Active water flowing through in the current step
    var pollution: Float = 0.0f // Active pollution density in the current step
)

enum class WeatherScenario(
    val displayName: String,
    val description: String,
    val rainfallIntensity: Float, // Base rainfall quantity multiplier
    val basePollutionFactor: Float, // Weather-induced runoff multiplier
    val agriculturalPremium: Float // Crop revenue multiplier
) {
    NORMAL_RAIN("正常雨季 (Normal Rain)", "Standard rainfall, balanced water runoff and agriculture.", 1.0f, 1.0f, 1.0f),
    HEAVY_STORM("特大暴雨 (Heavy Storm)", "Extreme rainfall! High flood risks. Tests forests and wetlands buffer capacity.", 2.5f, 1.5f, 0.8f),
    DROUGHT("干旱季节 (Drought Season)", "Almost no rain. Wet wetlands store buffer water, agriculture output halved.", 0.2f, 0.5f, 0.5f),
    ACID_RAIN("酸雨侵害 (Acid Rain)", "Industrial acid rainfall. Highly increases incoming pollutants.", 1.2f, 2.5f, 0.7f),
    GREEN_TECH("生态风向 (Green Tech Subsidy)", "Government sponsors ecological restoration. Cost of forests and wetlands -30%.", 0.9f, 0.8f, 1.1f)
}

@Entity(tableName = "simulation_logs")
data class SimulationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val seasonIndex: Int,
    val weatherName: String,
    val rainfallIntensity: Float,
    val totalRunoff: Float,
    val waterQualityIndex: Int,
    val biodiversityScore: Int,
    val netRevenue: Int,
    val budgetAfter: Int,
    val forestCount: Int,
    val wetlandCount: Int,
    val urbanCount: Int,
    val agricultureCount: Int,
    val grasslandCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)
