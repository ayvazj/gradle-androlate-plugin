package com.github.ayvazj.gradle.plugins.androlate

import com.google.api.services.translate.model.TranslationsResource


class AndrolateStringArrayElement extends AndrolateBaseElement {
    AndrolateStringArrayElement(Node node) {
        super(node)
    }

    @Override
    def public String md5() {
        if (this.node && !this.md5txt) {
            def item_concat = ''
            this.node.children().each { item ->
                item_concat += "${item.text()}"
            }
            this.md5txt = AndrolateUtils.md5sum(item_concat)
            return this.md5txt
        }
        return null;
    }

    @Override
    def public String[] text() {
        if (this.node) {
            def result = []
            this.node.children().each { item ->
                result.add("${item.text()}")
            }
            return result
        }
        return null
    }

    /**
     * Used to query how many strings this element has
     * @return
     */
    @Override
    def public int getTextCount() {
        if (this.node) {
            return this.node.children().size()
        }
        return 0
    }

    @Override
    def public void updateDestXml(Node destxml, List<TranslationsResource> translatedResources) {
        if ("string-array".equals(this.name())) {
            def string_name = this.node.'@name'
            def existing = null
            if (destxml.'string-array' && destxml.'string-array'.size() > 0) {
                existing = destxml.'string-array'.findAll { deststring -> deststring.'@name' == string_name }
            }

            if (existing) {
                // updating string arrays are tricky, so just replace them for now
                existing.each { exist ->
                    destxml.remove(exist)
                }
            }

            def newnode = destxml.appendNode('string-array')
            newnode.'@name' = string_name
            translatedResources.each { tr ->
                newnode.appendNode('item', tr.getTranslatedText())
            }
        }
    }
}
