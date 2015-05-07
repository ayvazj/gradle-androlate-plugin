package com.github.ayvazj.gradle.plugins.androlate

import groovy.xml.DOMBuilder
import groovy.xml.XmlUtil
import org.gradle.api.GradleScriptException
import org.w3c.dom.Document
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Element
import org.w3c.dom.Node

import java.security.MessageDigest


class AndrolateUtils {

    def private static final REPLACE_MAP = [
            "\\n": "<span class=\"notranslate\">axlate_N</span>",
            "\\t": "<span class=\"notranslate\">axlate_T</span>",
            "\\r": "<span class=\"notranslate\">axlate_R</span>",
            "%"  : "<span class=\"notranslate\">axlate_percent</span>",
            "\$" : "<span class=\"notranslate\">axlate_dollar</span>"
    ]

    def private static final IOS_REPLACE_MAP = [
            "\\%.*?s": "%@"
    ]

    def public static getResourceName(dirname) { dirname.tokenize("-")[0] }

    def public static getResourceQualifiers(dirname) {
        def tokens = dirname.tokenize("-")
        if (tokens.size() > 1) {
            return tokens[1..-1]
        }
        return []
    }

    /**
     * MD5SUM
     */
    def public static String md5sum(String s) {
        MessageDigest digest = MessageDigest.getInstance("MD5")
        digest.update(s.bytes);
        return new BigInteger(1, digest.digest()).toString(16).padLeft(32, '0')
    }

    /**
     * Google translate takes some liberties with formatting for the sake of
     * meaningful human translations, we don't need things to be that smart
     * so these two methods attempt to modify prevent Google' NLP from
     * modifying the string too much
     *
     * @param instr
     * @param lang
     * @return
     */
    def public static String googleTranslateEscape(String instr, String lang) {
        String result = instr
        REPLACE_MAP.each { String k, String v ->
            result = result.replace(k, "${v}")
        }
        result = "<span lang=\"" + lang + "\">" + result + "</span>"
        return result
    }

    def public static String googleTranslateResolve(String instr) {
        String result = replaceLast(instr, "</span>", "")
        REPLACE_MAP.each { k, v ->
            result = result.replace(" ${v} ", k)
            result = result.replace(v, k)
        }
        result = result.replaceFirst("\\<span.*?\\>", "")
        return result
    }

    def private static String replaceLast(String string, String substring, String replacement) {
        int index = string.lastIndexOf(substring);
        if (index == -1)
            return string;
        return string.substring(0, index) + replacement
        +string.substring(index + substring.length());
    }

    /**
     * Try to convert Java/Android string formatting to Mac/iOS style
     * @param instr
     * @return
     */
    def public static String convertToAppleFormatting(String instr) {
        String result = instr
        IOS_REPLACE_MAP.each { k, v ->
            result = result.replaceAll(k, v)
        }
        return result
    }

    def public static String findBackupFilename(File file) {
        def (String basename, String ext) = file.getName().split("\\.(?=[^\\.]+\$)");
        def i = ''
        def result = null
        while (true) {
            result = file.getParentFile().getPath() + System.getProperty("file.separator") + basename + ".bak" + i
            def File resFile = new File(result)
            if (!resFile.exists()) {
                return result
            }
            if (!i) {
                i = 1
            } else {
                i++
            }
        }
        return result
    }

    /**
     * Returns a string representenation of all the textContent including mixed content
     * @param elem
     * @return
     */
    def public static String getMixedContent(Node node) {
        if (node.hasChildNodes()) {
            def result = ''
            def children = node.getChildNodes()
            if (children && children.length) {
                children.each { org.w3c.dom.Node child ->
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        def xmlOutput = new StringWriter()
                        XmlUtil.serialize(child, xmlOutput)
                        result += xmlOutput.toString().replaceAll("<\\?.*\\?>", "").trim()
                    } else {
                        result += child.getNodeValue()
                    }
                }
            }
            return result
        }
        return node.getNodeValue()
    }

    def public static Element getMixedNodes(Element element, String s) {

        def parser = null

        // create the destination document
        try {
            parser = DOMBuilder.newInstance(false, true).parseText('''<?xml version='1.0' encoding='utf-8'?>\n<androlate>''' + s + '''</androlate>''')
        }
        catch (Exception e) {
            throw new GradleScriptException("Error parsing getMixedNodes", e)
        }

        if (parser == null) {
            return null
        }

        return parser.documentElement
    }
}
