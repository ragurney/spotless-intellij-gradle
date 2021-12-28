package com.github.ragurney.spotless.services

import com.intellij.openapi.project.Project
import com.github.ragurney.spotless.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
