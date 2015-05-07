# Androlate Gradle Plugin

This plugin translates the string resources in your Android project using the
[Google Translate] (https://translate.google.com/).  It also provides a few extras tasks to help with common translation tasks.

LEGAL INFORMATION
---------------------

    THIS PLUGIN MAY CONTAIN TRANSLATIONS POWERED BY GOOGLE.  GOOGLE DISCLAIMS ALL
    WARRANTIES RELATED TO THE TRANSLATIONS, EXPRESS OR IMPLIED, INCLUDING ANY
    WARRANTIES OF ACCURACY, RELIABILITY, AND ANY IMPLIED WARRANTIES OF
    MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.

    The Androlate plugin performs translations for your convenience
    using translation software powered by Google Translate.  Reasonable efforts have
    been made to provide an accurate translation, however, no automated translation
    is perfect nor is it intended to replace human translators. Translations are provided
    as a service to users of the Androlate tool, and are provided "as is." No warranty of
    any kind, either expressed or implied, is made as to the accuracy, reliability,
    or correctness of any translations made from <source language> into any other
    language. Some content (such as images, videos, Flash, etc.) may not be accurately
    translated due to the limitations of the translation software.
    
    Any discrepancies or differences created in the translation are not binding and 
    have no legal effect for compliance or enforcement purposes.

IMPORTANT INFORMATION
---------------------
 * [Google Translate] (https://translate.google.com/) is a **PAID** service.
   I am in no way responsible for any fees incurred as a result of using this tool
   *including bugs*.  **IT IS YOUR RESPONSIBILITY TO MONITOR USAGE AND BILLING**.

 * In order for translations to work you **MUST** have the Google Translate API enabled in the
   [Google Developer Console] (https://console.developers.google.com/).

 * YOU **MUST** HAVE BILLIING ENABLED IN ORDER FOR THE TRANSLATE API TO WORK.
   This means you must add a credit card to your Google Developer Console account and enable
   for the google translate API.


KNOWN ISSUES
------------

 * [Google Translate] (https://translate.google.com/) may jumble words around as it was designed for natural language.
 
     Example: Line1 nl Line2 nl Line3 => Línea 1 Línea 2 nl nl Line3

 * Some escape and formatting characters may not work (\\n)

 * Embedded html markup may be stripped by the Google Translate API (i.e. Hello <b>World!</b>)
 * <![CDATA[]]> sections inside <string-array> item elements won't be translated.

## Examples

An example projects can be found in [/examples](examples).

## Usage

Add the androlate plug to your *modules* build.gradle file below your android plugin

    buildscript {
        repositories {
            jcenter()
            maven {
                url  "https://dl.bintray.com/ayvazj/maven"
            }
        }
        dependencies {
            classpath('com.github.ayvazj.gradle.plugins.androlate:androlate:0.1')
        }
    }

    apply plugin: 'com.android.application'
    apply plugin: 'androlate'

Add the following section at the end of your build.gradle file

    androlate {
        appName 'API Project'
    	apiKey '********************************'
    	defaultLanguage 'en'
    	targetLanguages = ['es']
    }

Where:

    appName
        The name of the app you entered into the [Google Developer Console] (https://console.developers.google.com/)

    apiKey
        The API key obtained when you enable the Translate API key in the
        [Google Developer Console] (https://console.developers.google.com/).

    defaultLanguage
        The source language or the language that values/strings.xml is written in.  This value will be passed to
        [Google Translate] (https://translate.google.com/) as the input language.

    targetLanguages
        A list of languages you are targeting.  Keep in mind androlate will submit each string to
        [Google Translate] (https://translate.google.com/) so complete translations may take a while and result in fees.


Run the androlate target to translate the string resources

    > ./gradlew -q androlate

Your strings files will be parsed and sent to [Google Translate] (https://translate.google.com/) for translation.  The result will be stored in the values-<language> directory with the same name as the source file.

## Additional information

####Unnecessary Translations *(and fees)*
In order to avoid unnecessary translations androlate generates an md5 checksum for all of the strings in your default language.  Only when the strings value does not match the md5sum is the string sent for translation.  This ensures that on repeated runs only strings that have changed are sent for translation.

    <string name="hello_world" xandrolate:md5="86f269d190d2c85f6e0468ceca42a20">Hello world!</string>

## Bonus features

Extras tasks were added to help with common translation tasks.

####Export to Microsoft Excel spreadsheet.
The resulting file will be located under the export/excel directory.

    > ./gradlew -q androlate-export-excel

####Export strings to iOS / Mac strings.
The resulting files will be located under the export/apple directory.

    > ./gradlew -q androlate-export-apple


[doc]: http://ayvazj.github.io/gradle-androlate-plugin/doc/latest/
[javadoc]: http://ayvazj.github.io/gradle-androlate-plugin/doc/latest/javadoc/
