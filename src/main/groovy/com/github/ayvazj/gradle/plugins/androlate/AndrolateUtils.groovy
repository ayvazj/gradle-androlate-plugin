package com.github.ayvazj.gradle.plugins.androlate

import java.security.MessageDigest


class AndrolateUtils {

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
}
