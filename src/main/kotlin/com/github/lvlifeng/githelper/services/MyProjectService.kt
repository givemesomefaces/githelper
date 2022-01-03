package com.github.lvlifeng.githelper.services

import com.intellij.openapi.project.Project
import com.github.lvlifeng.githelper.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
