package com.aimy.ui

import com.aimy.service.AimYService
import com.aimy.settings.AimYSettings
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.util.Consumer
import org.jetbrains.annotations.Nls
import java.awt.Component
import java.awt.event.MouseEvent

class AimYStatusBarWidget : StatusBarWidget, StatusBarWidget.TextPresentation {
    private val settings = AimYSettings.getInstance()
    private var statusBarRef: StatusBar? = null

    override fun ID(): String = "AimYStatusBar"

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getText(): String = if (settings.enableExtension) "AimY: On" else "AimY: Off"

    override fun getClickConsumer(): Consumer<MouseEvent>? = Consumer {
        AimYService.getInstance().toggleExtension()
        // refresh widget text/tooltip after toggling
        statusBarRef?.updateWidget(ID())
    }

    override fun install(statusBar: StatusBar) {
        statusBarRef = statusBar
    }

    override fun dispose() {
        statusBarRef = null
    }

    override fun getAlignment(): Float = Component.CENTER_ALIGNMENT

    override fun getTooltipText(): @Nls String? =
        if (settings.enableExtension) "AimY is enabled" else "AimY is disabled"
}