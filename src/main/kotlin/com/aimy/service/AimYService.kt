package com.aimy.service

import com.aimy.game.GameMode
import com.aimy.settings.AimYSettings
import com.aimy.ui.GameWindow
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import kotlinx.coroutines.*
import java.util.*
import javax.swing.Timer

@Service(Service.Level.APP)
class AimYService : Disposable {
    private var idleTimer: Timer? = null
    private var gameWindow: GameWindow? = null
    private var isGameActive = false
    private val settings = AimYSettings.getInstance()

    init {
        setupIdleDetection()
    }

    private fun setupIdleDetection() {
        val connection = ApplicationManager.getApplication().messageBus.connect()

        // Document changes
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                resetIdleTimer()
            }
        })

        // File editor changes
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
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
        gameWindow = GameWindow(
            gameMode = GameMode.valueOf(settings.gameMode.uppercase()),
            settings = settings,
            onGameComplete = { stats ->
                handleGameComplete(stats)
            }
        )
        gameWindow?.show()
    }

    private fun handleGameComplete(stats: GameStats) {
        isGameActive = false
        gameWindow?.dispose()
        gameWindow = null

        // Save stats
        if (settings.enableStatsSave) {
            saveGameStats(stats)
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

    override fun dispose() {
        idleTimer?.stop()
        gameWindow?.dispose()
    }

    companion object {
        fun getInstance(): AimYService = ApplicationManager.getApplication().getService(AimYService::class.java)
    }
}