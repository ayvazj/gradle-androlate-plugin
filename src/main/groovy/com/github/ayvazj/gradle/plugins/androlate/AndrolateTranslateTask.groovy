package com.github.ayvazj.gradle.plugins.androlate

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.translate.Translate
import com.google.api.services.translate.model.TranslationsResource
import groovy.xml.Namespace
import groovy.xml.XmlUtil
import org.apache.commons.lang.StringEscapeUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException
import org.gradle.api.tasks.TaskAction

class AndrolateTranslateTask extends DefaultTask {

    /** Global instance of the HTTP transport. */
    NetHttpTransport httpTransport

    /** Global instance of the JSON factory. */
    JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance()

    /** Global instance of Androlate Extension */
    AndrolatePluginExtension androlate

    /** Global instance of Translate client */
    Translate translateClient


    AndrolateTranslateTask() {
        super()
        this.description = 'Translates the default string resources using Google Translate'
        this.androlate = project.androlate
        def httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        // set up global Translate instance
        this.translateClient = new Translate.Builder(
                httpTransport,
                JSON_FACTORY,
                null
        ).setApplicationName("${androlate.appName}").build()
    }

    /**
     * Send given set of string for translation
     * @param xstrings the strings to be translated
     * @param xlated_strings the translated strings are returned here
     * @param lang the targetLanguage
     */
    void executeTranslationRequest(List<String> xstrings, List<TranslationsResource> xlated_strings, String lang) {
        Translate.Translations.List translateRequest = this.translateClient.translations().list(
                xstrings,
                "${lang}"
        )
        translateRequest.setFormat('text')
        translateRequest.setKey("${androlate.apiKey}")
        def translateResponse = translateRequest.execute()
        xlated_strings.addAll(translateResponse.getTranslations())
    }

    /**
     * Google translate requires requests be < 2K for GET and <5K for post.  This method should conver the list
     * of strings to be translated into allowable chunks, send them for translation, then assemble the results
     *
     * @param stringsdirty the strings to be translated
     * @param xlated_strings the translated strings returned from Google
     * @param lang
     */
    void getChunkedTranslations(List<AndrolateBaseElement> elements, List<TranslationsResource> xlated_strings, String lang) {

        List<String> xstrings = new ArrayList<String>();

        // read all strings into a map [ dirname => [ name, value ]]
        def charcount = 0
        elements.each { elem ->
            def elem_texts = elem.text()
            if (elem_texts) {
                elem_texts.each { string_text ->
                    // request has to be less than 2K GET or 5K POST
                    if (charcount < 2048) {
                        xstrings.add(string_text)
                        charcount += StringEscapeUtils.escapeHtml(string_text).length() + 3
                    } else {
                        executeTranslationRequest(xstrings, xlated_strings, lang)
                        xstrings.clear()
                        xstrings.add(string_text)
                        charcount += string_text.length()
                    }
                }
            }
        }

        if (xstrings.size() > 0) {
            executeTranslationRequest(xstrings, xlated_strings, lang)
        }
    }

    /**
     * Performs the androlate translation process on an individual resource file
     */
    void androlateFile(File file) {

        def dir = file.getParentFile()
        def dirname = dir.getName()
        def resource_qualifiers = AndrolateUtils.getResourceQualifiers(dirname)

        // ignore XML files that are not in the default resource directory
        if (resource_qualifiers && resource_qualifiers.size() > 0) {
            return;
        }

        def srcparser = new XmlParser()
        def srcxml = srcparser.parse(file)

        // ignore XML files that are not resource files
        if (!srcxml.name().equals('resources')) {
            return
        }

        def List<AndrolateBaseElement> stringsdirty = new ArrayList<AndrolateBaseElement>()
        srcxml.children().each { child ->
            if ("string".equals(child.name()) || "string-array".equals(child.name())) {
                AndrolateBaseElement abelem = AndrolateBaseElement.newInstance(child)
                if (abelem.isDirty()) {
                    abelem.updateMd5()
                    stringsdirty.add(abelem)
                }
            }
        }

        if (!stringsdirty || stringsdirty.size() == 0) {
            return;
        }

        // Add the md5 sum to the sources to avoid resending unchanged strings
        srcxml.'@xmlns:androlate' = 'http://com.github.androlate'

        // save the modified source file
        def outwriter = new FileWriter('out.xml')
        XmlUtil.serialize(srcxml, outwriter)

        androlate.targetLanguages.each { lang ->
            def destdir = new File("${dir.path}-${lang}")
            def destfile = new File("${destdir.path}/${file.getName()}")
            def destxml = null

            destdir.mkdirs()
            def destparser = new XmlParser()
            if (destfile.exists()) {
                destxml = destparser.parse(destfile)
            } else {
                // create the destination document
                destxml = destparser.parseText('''<?xml version='1.0' encoding='utf-8'?><resources></resources>''')
            }

            List<TranslationsResource> xlated_strings = new ArrayList<TranslationsResource>()
            getChunkedTranslations(stringsdirty, xlated_strings, lang)

            println("  Target Language : ${lang}")

            def i = 0
            while (i < stringsdirty.size()) {
                def incby = stringsdirty[i].getTextCount()
                stringsdirty[i].updateDestXml(destxml, xlated_strings[i..<(i+incby)])
                i += incby
            }

            //Save File
            def destwriter = new FileWriter(destfile)
            XmlUtil.serialize(destxml, destwriter)
        }

    }

    @TaskAction
    void androlate() {

        if (!androlate.apiKey) {
            throw new GradleScriptException("androlate.apiKey is not defined", null)
        }

        if (!androlate.appName) {
            throw new GradleScriptException("androlate.appName is not defined", null)
        }


        println('  Default Language : ' + androlate.defaultLanguage)

        // TODO revist to find a more optimal way of locating strings.xml
        project.fileTree(dir: 'src', include: '**/*.xml').each { File file ->
            androlateFile(file)
        }
    }
}
