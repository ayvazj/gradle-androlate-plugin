package com.github.ayvazj.gradle.plugins.androlate

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndrolatePlugin implements Plugin<Project> {

    final static String GROUP_NAME = 'Androlate'

    void apply(Project project) {
        applyExtensions(project)
        applyTasks(project)
    }

    static void applyExtensions(final Project project) {
        project.extensions.create('androlate', AndrolatePluginExtension, project)
    }

    static void applyTasks(final Project project) {
        project.task('androlate', type: AndrolateTranslateTask, group: GROUP_NAME)
        project.task('androlate-export-apple', type: AndrolateExportAppleTask, group: GROUP_NAME)
        // TODO project.task('androlate-export-xls', type: AndrolateExportExcelTask, group: GROUP_NAME)
    }
}
