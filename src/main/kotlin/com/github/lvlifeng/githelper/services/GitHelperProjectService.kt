package com.github.lvlifeng.githelper.services

import com.github.lvlifeng.githelper.Bundle
import com.github.lvlifeng.githelper.Bundle.message
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import gitlab.actions.OpenGitLabSettingsAction
import gitlab.settings.GitLabSettingsState

class GitHelperProjectService(project: Project) {

    init {
        println(Bundle.message("projectService", project.name))
        validateGitlabServer()
    }

    private fun validateGitlabServer() {
        var invalidServerList = mutableListOf<String>()
        var errotMsg = ""
        val gitLabSettingsState = GitLabSettingsState.getInstance()
        gitLabSettingsState.apply {
            this.gitlabServers.stream().forEach {
                try {
                    this.isApiValid(it.apiUrl, it.apiToken)
                } catch (e: Exception) {
                    invalidServerList.add(it.apiUrl)
                    errotMsg += "GitLab server \"${it.apiUrl}\" is invalid. The reason is '${e.message}' \n"
                }
            }
            if (errotMsg.isNotEmpty()) {
                errotMsg += "Please click the button below to configure."
            }
        }
        if (gitLabSettingsState.gitlabServers.isEmpty()) {
            Notifications.Bus.notify(
                NotificationGroupManager.getInstance().getNotificationGroup(message("notifierGroup"))
                    .createNotification(
                        message("notifierGroup"),
                        "GitLab cannot be used without configuring GitLab server. Please click the button below to configure.",
                        NotificationType.WARNING,
                        null
                    ).addAction(
                        OpenGitLabSettingsAction()
                    )
            )
        }
        if (invalidServerList.isNotEmpty()) {
            Notifications.Bus.notify(
                NotificationGroupManager.getInstance().getNotificationGroup(message("notifierGroup"))
                    .createNotification(
                        message("notifierGroup"),
                        errotMsg,
                        NotificationType.WARNING,
                        null
                    ).addAction(
                        OpenGitLabSettingsAction()
                    )
            )

        }


    }
}
