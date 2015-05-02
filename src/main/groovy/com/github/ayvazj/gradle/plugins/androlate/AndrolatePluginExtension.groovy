package com.github.ayvazj.gradle.plugins.androlate

import org.gradle.api.Project

class AndrolatePluginExtension {
    def String defaultLanguage
    def targetLanguages
    def String apiKey
    def String appName
    private final Project project

    public AndrolatePluginExtension(Project project) {
        this.project = project
        this.defaultLanguage = 'en'
        this.targetLanguages = []
        this.apiKey = ''
        this.appName = null
    }
}
