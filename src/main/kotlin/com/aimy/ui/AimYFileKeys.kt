package com.aimy.ui

import com.aimy.game.GameStats
import com.aimy.game.GameMode
import com.intellij.openapi.util.Key

object AimYFileKeys {
    val GAME_MODE_KEY: Key<GameMode> = Key.create("AimY.GameMode")
    val ON_COMPLETE_KEY: Key<(GameStats) -> Unit> = Key.create("AimY.OnComplete")
}