package com.aimy.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.*

class AimYConfigurable : Configurable {
    private val settings = AimYSettings.getInstance()

    private val enableExtensionCheckBox = JBCheckBox("Enable AimY auto-start")
    private val idleTimerField = JBTextField()
    private val difficultyCombo = ComboBox(arrayOf("Easy", "Normal", "Hard", "Very Hard", "Custom"))
    private val gameModeCombo = ComboBox(arrayOf("Target Rush", "Time Frenzy", "Hydra Targets"))
    private val targetGoalsField = JBTextField()
    private val targetSizeField = JBTextField()
    private val targetMoveCheckBox = JBCheckBox("Enable moving targets")
    private val targetSpeedField = JBTextField()
    private val targetTimeExistsField = JBTextField()
    private val timeFrenzyDurationField = JBTextField()
    private val hydraModeCombo = ComboBox(arrayOf("Target Count", "Timed"))
    private val hydraTargetCountField = JBTextField()
    private val hydraTotalTimeField = JBTextField()
    private val enableSoundEffectsCheckBox = JBCheckBox("Enable sound effects")
    private val soundVolumeField = JBTextField()
    private val enableEffectsCheckBox = JBCheckBox("Enable visual effects")
    private val enableStatsSaveCheckBox = JBCheckBox("Enable stats saving")
    private val statsFormatCombo = ComboBox(arrayOf("json", "csv"))
    private val statsDirectoryField = JBTextField()
    private val closeWorkspaceCheckBox = JBCheckBox("Close workspace on game start")

    override fun getDisplayName(): String = "AimY Settings"

    override fun createComponent(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Enable Extension:", enableExtensionCheckBox)
            .addLabeledComponent("Idle Timer (ms):", idleTimerField)
            .addSeparator()
            .addLabeledComponent("Difficulty:", difficultyCombo)
            .addLabeledComponent("Game Mode:", gameModeCombo)
            .addLabeledComponent("Target Goals:", targetGoalsField)
            .addLabeledComponent("Target Size:", targetSizeField)
            .addLabeledComponent("Moving Targets:", targetMoveCheckBox)
            .addLabeledComponent("Target Speed (ms):", targetSpeedField)
            .addLabeledComponent("Target Time Exists (ms):", targetTimeExistsField)
            .addSeparator()
            .addLabeledComponent("Time Frenzy Duration (ms):", timeFrenzyDurationField)
            .addLabeledComponent("Hydra Mode:", hydraModeCombo)
            .addLabeledComponent("Hydra Target Count:", hydraTargetCountField)
            .addLabeledComponent("Hydra Total Time (ms):", hydraTotalTimeField)
            .addSeparator()
            .addLabeledComponent("Sound Effects:", enableSoundEffectsCheckBox)
            .addLabeledComponent("Sound Volume (0-100):", soundVolumeField)
            .addLabeledComponent("Visual Effects:", enableEffectsCheckBox)
            .addSeparator()
            .addLabeledComponent("Save Stats:", enableStatsSaveCheckBox)
            .addLabeledComponent("Stats Format:", statsFormatCombo)
            .addLabeledComponent("Stats Directory:", statsDirectoryField)
            .addLabeledComponent("Close Workspace:", closeWorkspaceCheckBox)
            .panel
    }

    override fun isModified(): Boolean {
        return enableExtensionCheckBox.isSelected != settings.enableExtension ||
                idleTimerField.text.toIntOrNull() != settings.idleTimer ||
                difficultyCombo.selectedItem != settings.difficulty ||
                gameModeCombo.selectedItem != settings.gameMode ||
                targetGoalsField.text.toIntOrNull() != settings.targetGoals ||
                targetSizeField.text.toIntOrNull() != settings.targetSize ||
                targetMoveCheckBox.isSelected != settings.targetMove ||
                targetSpeedField.text.toIntOrNull() != settings.targetSpeed ||
                targetTimeExistsField.text.toIntOrNull() != settings.targetTimeExists ||
                timeFrenzyDurationField.text.toIntOrNull() != settings.timeFrenzyDuration ||
                hydraModeCombo.selectedItem != settings.hydraMode ||
                hydraTargetCountField.text.toIntOrNull() != settings.hydraTargetCount ||
                hydraTotalTimeField.text.toIntOrNull() != settings.hydraTotalTime ||
                enableSoundEffectsCheckBox.isSelected != settings.enableSoundEffects ||
                soundVolumeField.text.toIntOrNull() != settings.soundVolume ||
                enableEffectsCheckBox.isSelected != settings.enableEffects ||
                enableStatsSaveCheckBox.isSelected != settings.enableStatsSave ||
                statsFormatCombo.selectedItem != settings.statsFormat ||
                statsDirectoryField.text != settings.statsDirectory ||
                closeWorkspaceCheckBox.isSelected != settings.closeWorkspaceOnGameStart
    }

    override fun apply() {
        settings.enableExtension = enableExtensionCheckBox.isSelected
        settings.idleTimer = idleTimerField.text.toIntOrNull() ?: 60000
        settings.difficulty = difficultyCombo.selectedItem as String
        settings.gameMode = gameModeCombo.selectedItem as String
        settings.targetGoals = targetGoalsField.text.toIntOrNull() ?: 5
        settings.targetSize = targetSizeField.text.toIntOrNull() ?: 100
        settings.targetMove = targetMoveCheckBox.isSelected
        settings.targetSpeed = targetSpeedField.text.toIntOrNull() ?: 3000
        settings.targetTimeExists = targetTimeExistsField.text.toIntOrNull() ?: 3000
        settings.timeFrenzyDuration = timeFrenzyDurationField.text.toIntOrNull() ?: 60000
        settings.hydraMode = hydraModeCombo.selectedItem as String
        settings.hydraTargetCount = hydraTargetCountField.text.toIntOrNull() ?: 20
        settings.hydraTotalTime = hydraTotalTimeField.text.toIntOrNull() ?: 60000
        settings.enableSoundEffects = enableSoundEffectsCheckBox.isSelected
        settings.soundVolume = soundVolumeField.text.toIntOrNull() ?: 80
        settings.enableEffects = enableEffectsCheckBox.isSelected
        settings.enableStatsSave = enableStatsSaveCheckBox.isSelected
        settings.statsFormat = statsFormatCombo.selectedItem as String
        settings.statsDirectory = statsDirectoryField.text
        settings.closeWorkspaceOnGameStart = closeWorkspaceCheckBox.isSelected
    }

    override fun reset() {
        enableExtensionCheckBox.isSelected = settings.enableExtension
        idleTimerField.text = settings.idleTimer.toString()
        difficultyCombo.selectedItem = settings.difficulty
        gameModeCombo.selectedItem = settings.gameMode
        targetGoalsField.text = settings.targetGoals.toString()
        targetSizeField.text = settings.targetSize.toString()
        targetMoveCheckBox.isSelected = settings.targetMove
        targetSpeedField.text = settings.targetSpeed.toString()
        targetTimeExistsField.text = settings.targetTimeExists.toString()
        timeFrenzyDurationField.text = settings.timeFrenzyDuration.toString()
        hydraModeCombo.selectedItem = settings.hydraMode
        hydraTargetCountField.text = settings.hydraTargetCount.toString()
        hydraTotalTimeField.text = settings.hydraTotalTime.toString()
        enableSoundEffectsCheckBox.isSelected = settings.enableSoundEffects
        soundVolumeField.text = settings.soundVolume.toString()
        enableEffectsCheckBox.isSelected = settings.enableEffects
        enableStatsSaveCheckBox.isSelected = settings.enableStatsSave
        statsFormatCombo.selectedItem = settings.statsFormat
        statsDirectoryField.text = settings.statsDirectory
        closeWorkspaceCheckBox.isSelected = settings.closeWorkspaceOnGameStart
    }
}