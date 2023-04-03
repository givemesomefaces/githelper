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
        var errorMsg = ""
        val gitLabSettingsState = GitLabSettingsState.getInstance()
        gitLabSettingsState.apply {
            this.gitlabServers.stream().forEach {
                try {
                    this.isApiValid(it.apiUrl, it.apiToken)
                } catch (e: Exception) {
                    it.validFlag = false
                    invalidServerList.add(it.apiUrl)
                    errorMsg += "GitLab server \"${it.apiUrl}\" is invalid. The reason is '${e.message}' \n"
                }
            }
            if (errorMsg.isNotEmpty()) {
                errorMsg += "Please click the button below to configure."
            }
        }
        if (gitLabSettingsState.gitlabServers.isEmpty()) {
            Notifications.Bus.notify(
                NotificationGroupManager.getInstance().getNotificationGroup(message("notifierGroup"))
                    .createNotification(
                        message("gitlabSettings"),
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
                        message("gitlabSettings"),
                        errorMsg,
                        NotificationType.WARNING,
                        null
                    ).addAction(
                        OpenGitLabSettingsAction()
                    )
            )

        }


    }
}
