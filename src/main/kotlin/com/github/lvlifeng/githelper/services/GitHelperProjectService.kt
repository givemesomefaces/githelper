package com.github.lvlifeng.githelper.services

import com.github.lvlifeng.githelper.Bundle
import com.intellij.openapi.project.Project

class GitHelperProjectService(project: Project) {

    init {
        println(Bundle.message("projectService", project.name))
    }
}
