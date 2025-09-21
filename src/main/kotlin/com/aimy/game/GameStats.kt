package com.aimy.game

data class GameStats(
    val score: Int,
    val time: Int,
    val accuracy: Int,
    val bestStreak: Int,
    val gameMode: String,
    val timestamp: String = java.time.Instant.now().toString()
)