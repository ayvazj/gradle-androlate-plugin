package com.github.ayvazj.gradle.plugins.androlate

import java.security.MessageDigest


class AndrolateUtils {

    def private static final REPLACE_MAP = [
            "\\n": "<span class=\"notranslate\">axlate_N</span>",
            "\\t": "<span class=\"notranslate\">axlate_T</span>",
            "\\r": "<span class=\"notranslate\">axlate_R</span>",
            "%"  : "<span class=\"notranslate\">axlate_percent</span>",
            "\$" : "<span class=\"notranslate\">axlate_dollar</span>"
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
            result = result.replace(k, " ${v} ")
        }
        result = "<span lang=\"" + lang + "\">" + result + "</span>"
        return result
    }

    def public static String googleTranslateResolve(String instr) {
        String result = replaceLast(instr, "</span>", "")
        REPLACE_MAP.each { k, v ->
            result = result.replace(" ${v} ", k)
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
}
