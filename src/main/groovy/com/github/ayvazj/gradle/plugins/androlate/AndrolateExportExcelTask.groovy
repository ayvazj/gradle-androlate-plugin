package com.github.ayvazj.gradle.plugins.androlate

import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException
import org.gradle.api.tasks.TaskAction

class AndrolateExportExcelTask extends DefaultTask {

    AndrolatePluginExtension androlate


    AndrolateExportExcelTask() {
        super()
        this.description = 'Export string resources to to MS Excel spreadsheet'
    }

    void extract_strings(File file, data) {
        def dir = file.getParentFile()
        def dirname = dir.getName()
        def resource_name = AndrolateUtils.getResourceName(dirname)
        def resource_qualifiers = AndrolateUtils.getResourceQualifiers(dirname)

        def srcparser = new XmlParser()
        def srcxml = srcparser.parse(file)

        // ignore XML files that are not resource files
        if (!srcxml.name().equals('resources')) {
            return
        }

        def stringElems = srcxml.string
        if (!stringElems || stringElems.size() == 0) {
            return;
        }

        stringElems.each { string ->
            def string_name = string.'@name'
            def string_text = string.text()
            if (!data.containsKey(resource_name)) {
                data[resource_name] = [:]
            }
            if (!data[resource_name].containsKey("__default__")) {
                data[resource_name]["__default__"] = [:]
            } else {
                for (qual in resource_qualifiers) {
                    if (!data[resource_name].containsKey(qual)) {
                        data[resource_name][qual] = [:]
                    }
                }
            }

            if (resource_qualifiers.size() == 0) {
                data[resource_name]["__default__"][string_name] = string_text
            } else {
                for (qual in resource_qualifiers) {
                    data[resource_name][qual][string_name] = string_text
                }
            }
        }
    }

    @TaskAction
    def export_excel() {
        androlate = project.androlate
        println('Exporting String resources')
        println('  Default Language : ' + androlate.defaultLanguage)


        def data = [:] // data is a map of files and strings
        // TODO revist to find a more optimal way of locating strings.xml
        project.fileTree(dir: 'src', include: '**/*.xml').each { File file ->
            extract_strings(file, data)
        }

        def fileName = 'export/xls/androlate.xls'
        def file = new File(fileName)
        if (file.getParentFile()) {
            file.getParentFile().mkdirs()
        }

        if (!file.exists() && !file.createNewFile()) {
            throw new GradleScriptException("Unable to create file '${fileName}", null)
        }

        // pivot the data so we can fill the table
        def tableData = [:]
        data.each { resource_name, qualifiers ->
            // each resource type (i.e. values-* )

            qualifiers.each { qualstr, values ->
                values.each { strkey, strval ->
                    // keep track of the keys we see

                    if (!tableData.containsKey(strkey)) {
                        tableData[strkey] = [:]
                    }
                    tableData[strkey][qualstr] = strval
                }
            }
        }

        // keep the languages in a sorted list since the keys may be in different orders
        def langkeys = []
        tableData.each { strkey, values ->
            values.each { key, value ->
                if (!langkeys.contains(key)) {
                    langkeys.add(key)
                }
            }
        }

        //println(tableData.toString().replace("[", "\n\t["))
        def writer = new FileWriter(fileName)
        def xml = new groovy.xml.MarkupBuilder(writer)
        xml.setDoubleQuotes(true)
        xml.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")
        xml."ss:Workbook"("xmlns:ss": "urn:schemas-microsoft-com:office:spreadsheet") {
            "ss:Worksheet"("ss:Name": "TODO Sheet name") {
                "ss:Table"() {
                    "ss:Row"() {
                        "ss:Cell"() {
                            "ss:Data"("ss:Type": "String", "STRING")
                        }
                        langkeys.each { langkey ->
                            "ss:Cell"() {
                                "ss:Data"("ss:Type": "String", "${langkey}")
                            }
                        }
                    }

                    tableData.each { strkey, values ->
                        "ss:Row"() {
                            "ss:Cell"() {
                                "ss:Data"("ss:Type": "String", strkey)
                            }
                            langkeys.each { langkey ->
                                values.each { valkey, valval ->
                                    if (valkey == langkey) {
                                        "ss:Cell"() {
                                            "ss:Data"("ss:Type": "String", valval)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

// write the list of string data to XML files in Android resource file format
    def write_missing_xlate(missing_list) {
        missing_list.each { item ->
            def resource_name = item["resource_name"]
            def qualstr_xlate = item["qualstr_xlate"]
            def dirname = "./xportlate/${resource_name}-${qualstr_xlate}"
            new File(dirname).mkdirs()
            def fileName = "${dirname}/strings.xml"

            def writer = new FileWriter(fileName)
            def xml = new groovy.xml.MarkupBuilder(writer)
            xml.setDoubleQuotes(true)
            xml.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")
            xml.resources() {
                missing_list.each { item_nested ->
                    if (item_nested["resource_name"] == resource_name && item_nested["qualstr_xlate"] == qualstr_xlate) {
                        def strkey = item_nested["strkey"]
                        def strval = item_nested["strval"]
                        string(name: strkey, strval)
                    }
                }
            }
        }
    }
}
