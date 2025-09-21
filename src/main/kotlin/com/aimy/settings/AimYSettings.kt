package com.aimy.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*

@State(
    name = "AimYSettings",
    storages = [Storage("aimy.xml")]
)
@Service(Service.Level.APP)
class AimYSettings : PersistentStateComponent<AimYSettings.State> {

    data class State(
        var enableExtension: Boolean = true,
        var idleTimer: Int = 60000,
        var difficulty: String = "Normal",
        var gameMode: String = "Target Rush",
        var targetGoals: Int = 5,
        var targetSize: Int = 100,
        var targetMove: Boolean = false,
        var targetSpeed: Int = 3000,
        var targetTimeExists: Int = 3000,
        var timeFrenzyDuration: Int = 60000,
        var hydraMode: String = "Target Count",
        var hydraTargetCount: Int = 20,
        var hydraTotalTime: Int = 60000,
        var enableSoundEffects: Boolean = true,
        var soundVolume: Int = 80,
        var enableEffects: Boolean = true,
        var enableStatsSave: Boolean = true,
        var statsFormat: String = "json",
        var statsDirectory: String = "",
        var closeWorkspaceOnGameStart: Boolean = true
    )

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) { myState = state }

    // Convenience properties
    var enableExtension: Boolean
        get() = myState.enableExtension
        set(value) { myState.enableExtension = value }

    var idleTimer: Int
        get() = myState.idleTimer
        set(value) { myState.idleTimer = value.coerceIn(1000, 3600000) }

    var difficulty: String
        get() = myState.difficulty
        set(value) { myState.difficulty = value }

    var gameMode: String
        get() = myState.gameMode
        set(value) { myState.gameMode = value }

    var targetGoals: Int
        get() = myState.targetGoals
        set(value) { myState.targetGoals = value.coerceIn(1, 1000) }

    var targetSize: Int
        get() = myState.targetSize
        set(value) { myState.targetSize = value.coerceIn(10, 1000) }

    var targetMove: Boolean
        get() = myState.targetMove
        set(value) { myState.targetMove = value }

    var targetSpeed: Int
        get() = myState.targetSpeed
        set(value) { myState.targetSpeed = value.coerceIn(100, 60000) }

    var targetTimeExists: Int
        get() = myState.targetTimeExists
        set(value) { myState.targetTimeExists = value.coerceIn(0, 60000) }

    var timeFrenzyDuration: Int
        get() = myState.timeFrenzyDuration
        set(value) { myState.timeFrenzyDuration = value.coerceIn(1000, 3600000) }

    var hydraMode: String
        get() = myState.hydraMode
        set(value) { myState.hydraMode = value }

    var hydraTargetCount: Int
        get() = myState.hydraTargetCount
        set(value) { myState.hydraTargetCount = value.coerceIn(1, 1000) }

    var hydraTotalTime: Int
        get() = myState.hydraTotalTime
        set(value) { myState.hydraTotalTime = value.coerceIn(1000, 3600000) }

    var enableSoundEffects: Boolean
        get() = myState.enableSoundEffects
        set(value) { myState.enableSoundEffects = value }

    var soundVolume: Int
        get() = myState.soundVolume
        set(value) { myState.soundVolume = value.coerceIn(0, 100) }

    var enableEffects: Boolean
        get() = myState.enableEffects
        set(value) { myState.enableEffects = value }

    var enableStatsSave: Boolean
        get() = myState.enableStatsSave
        set(value) { myState.enableStatsSave = value }

    var statsFormat: String
        get() = myState.statsFormat
        set(value) { myState.statsFormat = value }

    var statsDirectory: String
        get() = myState.statsDirectory
        set(value) { myState.statsDirectory = value }

    var closeWorkspaceOnGameStart: Boolean
        get() = myState.closeWorkspaceOnGameStart
        set(value) { myState.closeWorkspaceOnGameStart = value }

    companion object {
        fun getInstance(): AimYSettings = ApplicationManager.getApplication().getService(AimYSettings::class.java)
    }
}