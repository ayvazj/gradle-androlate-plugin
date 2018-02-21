package com.github.ayvazj.gradle.plugins.androlate

import com.google.api.services.translate.model.TranslationsResource
import org.w3c.dom.Element


abstract class AndrolateBaseElement {
    def Element node = null
    def md5txt = null

    public AndrolateBaseElement(Element node) {
        this.node = node
    }

    def public String name() {
        return this.node.getAttribute('name')
    }

    def public String md5() {
        if (this.node && !this.md5txt) {
            this.md5txt = AndrolateUtils.md5sum(this.node.getTextContent())
        }
        return this.md5txt
    }

    /**
     * Add / Update the androlate:md5 attribute on the node
     */
    def public void updateMd5() {
        if (this.node) {
            this.node.setAttributeNS(Androlate.NAMESPACE.uri, "${Androlate.NAMESPACE.prefix}:md5", md5())
            this.node.setAttribute('name', this.node.getAttribute('name'))
        }
    }

    def public boolean isTranslatable() {
        // If translatable is false then it isn't - otherwise it is
        return this.node.getAttribute('translatable') != "false"
    }

    def public boolean isDirty() {
        if (this.node) {
            def md5attr = this.node.getAttributeNS(Androlate.NAMESPACE.uri, 'md5')
            return md5() != md5attr
        }
        return true
    }

    def public String[] text() {
        if (this.node) {
            return [AndrolateUtils.getMixedContent(this.node)]
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
    def public void updateDestXml(Element destxml, List<TranslationsResource> translatedResources) {
        if ("string".equals(this.node.getNodeName())) {
            def string_name = this.node.getAttribute('name')
            def existing = null
            def string_elems = destxml.getElementsByTagName('string')
            if (string_elems && string_elems.length > 0) {
                existing = string_elems.findAll { Element deststring -> deststring.getAttribute('name') == string_name }
            }

            if (existing) {
                existing.each { Element existstring ->
                    existstring.setNodeValue(AndrolateUtils.googleTranslateResolve(translatedResources[0].getTranslatedText()))
                }
            } else {
                def Element newelem = destxml.getOwnerDocument().createElement('string')
                newelem.setAttribute('name', string_name)
                if (this.node.hasChildNodes() && this.node.getFirstChild().getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE) {
                    def cdata = destxml.getOwnerDocument().createCDATASection(AndrolateUtils.googleTranslateResolve(translatedResources[0].getTranslatedText()))
                    newelem.appendChild(cdata)
                } else {
                    def alltext = AndrolateUtils.googleTranslateResolve(translatedResources[0].getTranslatedText())
                    def Element mixedNodes = null

                    try {
                        mixedNodes = AndrolateUtils.getMixedNodes(destxml, alltext)
                    }
                    catch(Exception ex) {
                        System.err << "Unable to translate string: name=${string_name}"
                        System.err << ex
                    }
                    if (mixedNodes) {
                        mixedNodes.getChildNodes().each { node ->
                            def importedNode = destxml.getOwnerDocument().importNode(node, true)
                            newelem.appendChild(importedNode)
                        }
                    }
                    else {
                        System.err << "Unable to translate string: name=${string_name}"
                    }
                }

                destxml.appendChild(newelem)

            }
        }
        return
    }

    def public static newInstance(Element elem) {
        if ('string'.equals(elem.getNodeName())) {
            return new AndrolateStringElement(elem)
        } else if ('string-array'.equals(elem.getNodeName())) {
            return new AndrolateStringArrayElement(elem)
        }
        return null
    }
}
