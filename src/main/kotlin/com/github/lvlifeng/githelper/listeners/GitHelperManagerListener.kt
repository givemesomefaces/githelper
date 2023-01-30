package com.github.lvlifeng.githelper.listeners

import com.github.lvlifeng.githelper.services.GitHelperProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

internal class GitHelperManagerListener : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        project.service<GitHelperProjectService>()
    }
}
