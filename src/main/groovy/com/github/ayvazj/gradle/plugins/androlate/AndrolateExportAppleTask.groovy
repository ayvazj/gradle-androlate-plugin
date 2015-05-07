package com.github.ayvazj.gradle.plugins.androlate

import groovy.xml.DOMBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Element

class AndrolateExportAppleTask extends DefaultTask {

    AndrolatePluginExtension androlate


    AndrolateExportAppleTask() {
        super()
        this.description = 'Export string resources to iOS / OS X format'
    }

    void extract_strings(File file, data) {
        def dir = file.getParentFile()
        def dirname = dir.getName()
        def resource_qualifiers = AndrolateUtils.getResourceQualifiers(dirname)

        def srcparser

        if (file.exists()) {
            def destreader = new FileReader(file)
            try {
                srcparser = DOMBuilder.newInstance(false, true).parse(destreader)
            }
            catch (Exception e) {
                throw new GradleScriptException("Error parsing ${destfile}", e)
            }
        }

        def Element srcxml = srcparser.documentElement

        // ignore XML files that are not resource files
        if (!srcxml.getTagName().equals('resources')) {
            return
        }

        def stringElems = srcxml.getElementsByTagName('string')
        if (!stringElems || stringElems.length == 0) {
            return;
        }

        stringElems.each { Element string ->
            def string_name = string.getAttribute('name')
            def string_text = AndrolateUtils.getMixedContent(string)

            if (!data.containsKey(string_name)) {
                data[string_name] = [:]
            }

            if (resource_qualifiers.size() == 0) {
                data[string_name]["__default__"] = string_text
            } else {
                for (qual in resource_qualifiers) {
                    data[string_name][qual] = string_text
                }
            }
        }
    }

    def appendLocalizableStrings(fileName, string_text, string_translation) {
        def file = new File(fileName)
        if (file.getParentFile()) {
            file.getParentFile().mkdirs()
        }

        if (!file.exists() && !file.createNewFile()) {
            throw new GradleScriptException("Unable to create file '${fileName}", null)
        }

        file.append("\"" + AndrolateUtils.convertToAppleFormatting(string_text) + "\" = \"" + AndrolateUtils.convertToAppleFormatting(string_translation) + "\"" + System.getProperty("line.separator"))
    }

    @TaskAction
    def export_apple() {
        androlate = project.androlate
        println('Exporting String resources')
        println('  Default Language : ' + androlate.defaultLanguage)


        def data = [:] // data is a map of files and strings
        // TODO revist to find a more optimal way of locating strings.xml
        project.fileTree(dir: 'src', include: '**/*.xml').each { File file ->
            extract_strings(file, data)
        }

        def fileName = 'export/apple/Localizable.strings'
        def file = new File(fileName)
        if (file.getParentFile()) {
            file.getParentFile().mkdirs()
        }

        if (!file.exists() && !file.createNewFile()) {
            throw new GradleScriptException("Unable to create file '${fileName}", null)
        }

        data.each { string_name, qualifiers ->
            // each resource type (i.e. values-* )
            qualifiers.each { qualkey, qualval ->
                if ("__default__".equals(qualkey)) {
                    appendLocalizableStrings("export/apple/Localizable.strings", data[string_name]['__default__'], data[string_name]["__default__"])
                } else {
                    appendLocalizableStrings("export/apple/${qualkey}/Localizable.strings", data[string_name]['__default__'], data[string_name][qualkey])
                }
            }
        }
    }
}
