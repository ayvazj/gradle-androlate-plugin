package com.github.ayvazj.gradle.plugins.androlate

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.translate.Translate
import com.google.api.services.translate.model.TranslationsResource
import groovy.xml.DOMBuilder
import groovy.xml.Namespace
import groovy.xml.XmlUtil
import org.apache.commons.lang.StringEscapeUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Element

class AndrolateTranslateTask extends DefaultTask {

    /** Global instance of the HTTP transport. */
    NetHttpTransport httpTransport

    /** Global instance of the JSON factory. */
    JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance()

    /** Global instance of Androlate Extension */
    AndrolatePluginExtension androlate

    /** Global instance of Translate client */
    Translate translateClient

    @Option(option = "force", description = "Force translation even for non-dirty strings.")
    boolean force = false


    AndrolateTranslateTask() {
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
        translateRequest.setFormat('html')
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
                    def procstring = AndrolateUtils.googleTranslateEscape(string_text, lang)
                    // request has to be less than 2K GET or 5K POST
                    if (charcount < 2048) {
                        xstrings.add(procstring)
                        charcount += StringEscapeUtils.escapeHtml(procstring).length() + 3
                    } else {
                        executeTranslationRequest(xstrings, xlated_strings, lang)
                        xstrings.clear()
                        xstrings.add(procstring)
                        charcount += procstring.length()
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
    void androlateFile(File file, boolean force) {

        def dir = file.getParentFile()
        def dirname = dir.getName()
        def resource_qualifiers = AndrolateUtils.getResourceQualifiers(dirname)

        // ignore XML files that are not in the default resource directory
        if (resource_qualifiers && resource_qualifiers.size() > 0) {
            return;
        }

        def srcreader = new FileReader(file)
        def srcparser
        try {
            srcparser = DOMBuilder.newInstance(false, true).parse(srcreader)
        }
        catch (Exception e) {
            throw new GradleScriptException("Error parsing ${file.getName()}", e)
        }

        if (!srcparser) {
            return
        }

        def Element srcxml = srcparser.documentElement
        // ignore XML files that are not resource files
        if (!srcxml.getNodeName().equals('resources')) {
            return
        }

        def List<AndrolateBaseElement> stringsdirty = new ArrayList<AndrolateBaseElement>()
        def children = srcxml.getChildNodes()
        if (children && children.length) {
            children.each { org.w3c.dom.Node child ->
                if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    def Element elem = child as org.w3c.dom.Element
                    if ("string".equals(elem.getTagName()) || "string-array".equals(elem.getTagName())) {

                        AndrolateBaseElement abelem = AndrolateBaseElement.newInstance(child)
                        boolean dirty = abelem.isDirty() || force

                        if (dirty && abelem.isTranslatable()) {
                            abelem.updateMd5()
                            stringsdirty.add(abelem)
                        }
                    }
                }
            }
        }

        if (!stringsdirty || stringsdirty.size() == 0) {
            return;
        }

        // Add the md5 sum to the sources to avoid resending unchanged strings
        srcxml.setAttribute("xmlns:${Androlate.NAMESPACE.prefix}", Androlate.NAMESPACE.getUri().toString())

        if (androlate.backup) {
            // save the modified source file
            def backfn = AndrolateUtils.findBackupFilename(file)
            if (!backfn) {
                throw new GradleScriptException("Unable to create backup file")
            }

            File backFile = new File(backfn)
            logger.log(LogLevel.INFO, "Renaming ${file.getName()} to ${backFile.getName()}")

            file.renameTo(backfn)
        }

        def outwriter = new FileWriter(file.getPath())
        def fos = new FileOutputStream(file.getPath());
        def osw = new OutputStreamWriter(fos,"UTF-8");
        XmlUtil.serialize(srcxml, outwriter)
        osw.close()

        androlate.targetLanguages.each { lang ->
            def destdir = new File("${dir.path}-${lang}")
            def destfile = new File("${destdir.path}/${file.getName()}")

            destdir.mkdirs()
            def destparser

            if (destfile.exists()) {
                def destreader = new FileReader(destfile)
                try {
                    destparser = DOMBuilder.newInstance(false, true).parse(destreader)
                }
                catch (Exception e) {
                    throw new GradleScriptException("Error parsing ${destfile}", e)
                }
            } else {
                // create the destination document
                try {
                    destparser = DOMBuilder.newInstance(false, true).parseText('''<?xml version='1.0' encoding='utf-8'?>\n<resources></resources>''')
                }
                catch (Exception e) {
                    throw new GradleScriptException("Error parsing ${destfile}", e)
                }
            }

            def destxml = destparser.documentElement

            List<TranslationsResource> xlated_strings = new ArrayList<TranslationsResource>()
            getChunkedTranslations(stringsdirty, xlated_strings, lang)

            def i = 0
            def pos = 0
            while (i < stringsdirty.size()) {
                def incby = stringsdirty[i].getTextCount()
                if (pos <= xlated_strings.size()) {
                    stringsdirty[i].updateDestXml(destxml, xlated_strings[pos..<(pos + incby)])
                }
                i++
                pos += incby
            }

            //Save File
            def destfos = new FileOutputStream(destfile);
            def destosw = new OutputStreamWriter(destfos,"UTF-8");
            XmlUtil.serialize(destxml, destosw)
            destosw.close()
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
            androlateFile(file, force)
        }
    }
}
