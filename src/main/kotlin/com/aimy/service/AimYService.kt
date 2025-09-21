// kotlin
package com.aimy.service

import com.aimy.game.GameMode
import com.aimy.game.GameStats
import com.aimy.settings.AimYSettings
import com.aimy.stats.StatsManager
import com.aimy.ui.GameWindow
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.MessageBusConnection
import javax.swing.Timer
import com.intellij.openapi.ui.DialogWrapper

@Service(Service.Level.APP)
class AimYService {
    private var idleTimer: Timer? = null
    private var gameWindow: GameWindow? = null
    private var isGameActive = false
    private val settings = AimYSettings.getInstance()
    private val statsManager = StatsManager()
    private var connection: MessageBusConnection? = null

    init {
        setupIdleDetection()
    }

    private fun setupIdleDetection() {
        connection = ApplicationManager.getApplication().messageBus.connect()

        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                resetIdleTimer()
            }
        })

        connection?.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                resetIdleTimer()
            }

            override fun selectionChanged(event: FileEditorManagerEvent) {
                resetIdleTimer()
            }
        })

        resetIdleTimer()
    }

    private fun resetIdleTimer() {
        idleTimer?.stop()

        if (!settings.enableExtension || isGameActive) return

        idleTimer = Timer(settings.idleTimer) {
            startGame()
        }.apply {
            isRepeats = false
            start()
        }
    }

    fun startGame() {
        if (isGameActive) return

        isGameActive = true
        val gameMode = when (settings.gameMode.lowercase().replace(" ", "_")) {
            "time_frenzy" -> GameMode.TIME_FRENZY
            "hydra_targets" -> GameMode.HYDRA_TARGETS
            else -> GameMode.TARGET_RUSH
        }

        val project = com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull() ?: run {
            isGameActive = false
            return
        }

        if (settings.closeWorkspaceOnGameStart) {
            val fem = FileEditorManager.getInstance(project)
            for (open in fem.openFiles) {
                try {
                    fem.closeFile(open)
                } catch (_: Exception) { /* ignore close errors */ }
            }
        }

        val file = com.intellij.testFramework.LightVirtualFile("AimY - Game.aimy", "")
        file.putUserData(com.aimy.ui.AimYFileKeys.GAME_MODE_KEY, gameMode)
        file.putUserData(com.aimy.ui.AimYFileKeys.ON_COMPLETE_KEY, { stats ->
            ApplicationManager.getApplication().invokeLater {
                handleGameComplete(stats)
            }
        })

        FileEditorManager.getInstance(project).openFile(file, true)
    }

    private fun handleGameComplete(stats: GameStats) {
        isGameActive = false
        gameWindow?.close(DialogWrapper.OK_EXIT_CODE)
        gameWindow = null

        if (settings.enableStatsSave) {
            statsManager.saveStats(stats, settings)
        }

        resetIdleTimer()
    }

    fun toggleExtension() {
        settings.enableExtension = !settings.enableExtension
        if (settings.enableExtension) {
            resetIdleTimer()
        } else {
            idleTimer?.stop()
        }
    }

    fun dispose() {
        idleTimer?.stop()
        gameWindow?.close(DialogWrapper.OK_EXIT_CODE)
        connection?.disconnect()
    }

    companion object {
        fun getInstance(): AimYService = ApplicationManager.getApplication().getService(AimYService::class.java)
    }
}