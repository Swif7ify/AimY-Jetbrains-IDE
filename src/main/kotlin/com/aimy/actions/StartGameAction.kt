package com.aimy.actions

import com.aimy.service.AimYService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class StartGameAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        AimYService.getInstance().startGame()
    }
}