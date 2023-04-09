package com.github.lvlifeng.githelper

import com.github.lvlifeng.githelper.Bundle.message
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import org.jetbrains.plugins.gitlab.authentication.accounts.GitLabAccountManager
import org.jetbrains.plugins.gitlab.authentication.accounts.GitLabProjectDefaultAccountHolder

internal class GitLabSettingsConfigurable internal constructor(private val project: Project) : BoundConfigurable(message("gitLab"), "settings.gitlab") {
    override fun createPanel(): DialogPanel {
        val accountManager = service<GitLabAccountManager>()
        val defaultAccountHolder = project.service<GitLabProjectDefaultAccountHolder>()

        val scope = DisposingMainScope(disposable!!) + ModalityState.any().asContextElement()
        val accountsModel = GitLabAccountsListModel()
        val detailsProvider = GitLabAccountsDetailsProvider(scope) { account ->
            accountsModel.newCredentials.getOrElse(account) {
                accountManager.findCredentials(account)
            }?.let {
                service<GitLabApiManager>().getClient(it)
            }
        }
        val actionsController = GitLabAccountsPanelActionsController(project, accountsModel)
        val accountsPanelFactory = AccountsPanelFactory(scope, accountManager, defaultAccountHolder, accountsModel)

        return panel {
            row {
                accountsPanelFactory.accountsPanelCell(this, detailsProvider, actionsController)
                    .align(Align.FILL)
            }.resizableRow()
        }
    }
}