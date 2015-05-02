package com.github.ayvazj.gradle.plugins.androlate

import com.google.api.services.translate.model.TranslationsResource


abstract class AndrolateBaseElement {
    def node = null
    def md5txt = null

    public AndrolateBaseElement(Node node) {
        this.node = node
    }

    def public name() {
        return this.node.name()
    }

    def public String md5() {
        if (this.node && !this.md5txt) {
            this.md5txt = AndrolateUtils.md5sum("${this.node.text()}")
            return this.md5txt
        }
        return null;
    }

    /**
     * Add / Update the androlate:md5 attribute on the node
     */
    def public void updateMd5() {
        if (this.node && !this.md5txt) {
            this.node.attributes()[Androlate.NAMESPACE.md5] = this.md5txt.md5()
        }
    }

    def public boolean isDirty() {
        if (this.node) {
            def md5attr = this.node.attributes()[Androlate.NAMESPACE.md5]
            return (!md5().equals(md5attr))
        }
        return true;
    }

    def public String[] text() {
        if (this.node) {
            return [this.node.text()]
        }
        return null
    }

    /**
     * Used to query how many strings this element has
     * @return
     */
    def public int getTextCount() {
        return 1
    }

    /**
     * Update the destination
     * @param destxml
     * @param translatedText
     * @return
     */
    def public void updateDestXml(Node destxml, List<TranslationsResource> translatedResources) {
        if ("string".equals(this.name())) {
            def string_name = this.node.'@name'
            def existing = null
            if (destxml.string && destxml.string.size() > 0) {
                existing = destxml.string.findAll { deststring -> deststring.'@name' == string_name }
            }

            if (existing) {
                existing.each { existstring ->
                    existstring.value = translatedResources[0].getTranslatedText()
                }
            } else {
                def newnode = destxml.appendNode('string', translatedResources[0].getTranslatedText())
                newnode.'@name' = string_name
            }
        }
        return
    }

    def public static newInstance(Node elem) {
        if ('string'.equals(elem.name())) {
            return new AndrolateStringElement(elem)
        } else if ('string-array'.equals(elem.name())) {
            return new AndrolateStringArrayElement(elem)
        }
        return null
    }
}
