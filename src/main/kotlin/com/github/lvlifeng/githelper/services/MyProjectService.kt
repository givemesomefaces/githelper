package com.github.lvlifeng.githelper.services

import com.intellij.openapi.project.Project
import com.github.lvlifeng.githelper.Bundle

class MyProjectService(project: Project) {

    init {
        println(Bundle.message("projectService", project.name))
    }
}
