package com.github.ayvazj.gradle.plugins.androlate

import com.google.api.services.translate.model.TranslationsResource
import org.w3c.dom.Element


class AndrolateStringArrayElement extends AndrolateBaseElement {
    AndrolateStringArrayElement(Element node) {
        super(node)
    }

    @Override
    def public String md5() {
        if (this.node && !this.md5txt) {
            def item_concat = ''
            def item_elems = this.node.getElementsByTagName('item')
            if (item_elems && item_elems.length > 0) {
                item_elems.each { Element item ->
                    item_concat += "${item.getTextContent()}"
                }
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
            def item_elems = this.node.getElementsByTagName('item')
            if (item_elems && item_elems.length > 0) {
                item_elems.each { Element item ->
                    result.add("${item.getTextContent()}")
                }
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
            def item_elems = this.node.getElementsByTagName('item')
            if (item_elems && item_elems.length > 0) {
                return item_elems.length
            }
        }
        return 0
    }

    @Override
    def public void updateDestXml(Element destxml, List<TranslationsResource> translatedResources) {
        if ("string-array".equals(this.node.getNodeName())) {
            def string_name = this.node.getAttribute('name')

            def existing = null
            def stringarray_elems = destxml.getElementsByTagName('string-array')
            if (stringarray_elems && stringarray_elems.length > 0) {
                existing = stringarray_elems.findAll { Element deststring -> deststring.getAttribute('name') == string_name }
            }

            if (existing) {
                // updating string arrays are tricky, so just replace them for now
                existing.each { Element exist ->
                    destxml.removeChild(exist)
                }
            }

            def Element newelem = destxml.getOwnerDocument().createElement('string-array')
            newelem.setAttribute('name', string_name)

            translatedResources.each { tr ->
                def Element newItem = destxml.getOwnerDocument().createElement('item')
                newItem.setTextContent(AndrolateUtils.googleTranslateResolve(tr.getTranslatedText()))
                newelem.appendChild(newItem)
            }

            destxml.appendChild(newelem)
        }
        return
    }
}
