package com.aimy.ui

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class AimYFileEditorProvider : FileEditorProvider {
    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.name.startsWith("AimY -") || file.extension == "aimy"
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return AimYFileEditor(project, file)
    }

    override fun getEditorTypeId(): String = "AimYFileEditor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR
}