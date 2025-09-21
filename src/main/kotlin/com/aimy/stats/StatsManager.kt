package com.aimy.stats

import com.aimy.game.GameStats
import com.aimy.settings.AimYSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.time.Instant

@Serializable
data class StatsEntry(
    val score: Int,
    val time: Int,
    val accuracy: Int,
    val bestStreak: Int,
    val gameMode: String,
    val timestamp: String,
    val settings: Map<String, Any>
)

class StatsManager {
    private val json = Json { prettyPrint = true }

    fun saveStats(stats: GameStats, settings: AimYSettings) {
        try {
            val statsDir = getStatsDirectory(settings)
            statsDir.mkdirs()

            val settingsMap = mapOf(
                "difficulty" to settings.difficulty,
                "gameMode" to settings.gameMode,
                "targetGoals" to settings.targetGoals,
                "targetSize" to settings.targetSize,
                "targetMove" to settings.targetMove,
                "targetSpeed" to settings.targetSpeed,
                "targetTimeExists" to settings.targetTimeExists,
                "timeFrenzyDuration" to settings.timeFrenzyDuration,
                "hydraMode" to settings.hydraMode,
                "hydraTargetCount" to settings.hydraTargetCount,
                "hydraTotalTime" to settings.hydraTotalTime,
                "enableSoundEffects" to settings.enableSoundEffects,
                "soundVolume" to settings.soundVolume,
                "enableEffects" to settings.enableEffects
            )

            if (settings.statsFormat == "csv") {
                saveCsvStats(stats, settingsMap, statsDir)
            } else {
                saveJsonStats(stats, settingsMap, statsDir)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveJsonStats(stats: GameStats, settingsMap: Map<String, Any>, statsDir: File) {
        val entry = StatsEntry(
            score = stats.score,
            time = stats.time,
            accuracy = stats.accuracy,
            bestStreak = stats.bestStreak,
            gameMode = stats.gameMode,
            timestamp = stats.timestamp,
            settings = settingsMap
        )

        val filename = "aimy-stats-${Instant.now().toString().replace(":", "-")}.json"
        val file = File(statsDir, filename)
        file.writeText(json.encodeToString(entry))
    }

    private fun saveCsvStats(stats: GameStats, settingsMap: Map<String, Any>, statsDir: File) {
        val file = File(statsDir, "aimy-stats.csv")
        val exists = file.exists()

        val headers = listOf(
            "timestamp", "score", "time", "accuracy", "bestStreak", "gameMode",
            "difficulty", "targetGoals", "targetMove", "targetSpeed", "targetSize", "targetTimeExists",
            "configuredGameMode", "timeFrenzyDuration", "hydraMode", "hydraTargetCount", "hydraTotalTime",
            "enableSoundEffects", "soundVolume", "enableEffects"
        )

        val values = listOf(
            stats.timestamp, stats.score, stats.time, stats.accuracy, stats.bestStreak, stats.gameMode,
            settingsMap["difficulty"], settingsMap["targetGoals"], settingsMap["targetMove"],
            settingsMap["targetSpeed"], settingsMap["targetSize"], settingsMap["targetTimeExists"],
            settingsMap["gameMode"], settingsMap["timeFrenzyDuration"], settingsMap["hydraMode"],
            settingsMap["hydraTargetCount"], settingsMap["hydraTotalTime"],
            settingsMap["enableSoundEffects"], settingsMap["soundVolume"], settingsMap["enableEffects"]
        )

        if (!exists) {
            file.writeText(headers.joinToString(",") + "\n")
        }

        file.appendText(values.joinToString(",") { csvEscape(it.toString()) } + "\n")
    }

    private fun csvEscape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun getStatsDirectory(settings: AimYSettings): File {
        return if (settings.statsDirectory.isNotEmpty()) {
            File(settings.statsDirectory)
        } else {
            File(System.getProperty("user.home"), ".aimy")
        }
    }
}