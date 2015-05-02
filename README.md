# Androlate Gradle Plugin

This plugin translates the string resources in your Android project using the
[Google Translate] (https://translate.google.com/).  It also provides a few extras tasks to export
your strings to Excel or iOS format.


IMPORTANT INFORMATION
---------------------

 * [Google Translate] (https://translate.google.com/) has usage limits.
   I am in no way responsible for any fees incurred while using androlate *including bugs*.
   IT IS YOUR RESPONSIBILITY TO SET QUOTAS.

 * In order for translations to work you MUST have the Google Translate API enabled in the
   [Google Developer Console] (https://console.developers.google.com/).

 * YOU MUST HAVE BILLIING ENABLED IN ORDER FOR THE TRANSLATE API TO WORK.
   This means you must add a credit card to your Google Developer Console account and enable
   for the google translate API.


## Examples

An example projects can be found in [/examples](examples).

## Usage

Add the androlate plug to your build.gradle file below your android plugin

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

Your strings files will be parsed and sent to [Google Translate] (https://translate.google.com/) for translation.  The
result will be stored in the values-<language> directory with the same name as the source file.

## Additional information

####Unnecessary Translations *(and fees)*
In order to avoid unnecessary translations androlate generates an md5 checksum for all of the strings in your default language.  Only when the strings value does not match the md5sum is the string sent for translation.  This ensures that strings are only sent to translation when they are changed.

    <string name="hello_world" androlate:md5="86f269d190d2c85f6e0468ceca42a20">Hello world!</string>

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
