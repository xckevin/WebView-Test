package com.xckevin.android.app.webview.test.i18n

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StringResourceParityTest {
    private val resourceRoot = File("src/main/res")
    private val expectedLocaleDirs = listOf(
        "values",
        "values-zh",
        "values-ja",
        "values-es",
        "values-pt",
        "values-de",
        "values-fr",
        "values-ru",
    )

    @Test
    fun allSupportedLocalesDeclareTheSameStringKeys() {
        val defaultKeys = stringKeys(resourceRoot.resolve("values/strings.xml"))

        expectedLocaleDirs.forEach { localeDir ->
            val stringsFile = resourceRoot.resolve("$localeDir/strings.xml")
            assertTrue("Missing $localeDir/strings.xml", stringsFile.isFile)
            assertEquals(
                "String keys differ for $localeDir",
                defaultKeys,
                stringKeys(stringsFile),
            )
        }
    }

    private fun stringKeys(file: File): Set<String> {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
        val strings = document.getElementsByTagName("string")
        return buildSet {
            for (index in 0 until strings.length) {
                val node = strings.item(index)
                val name = node.attributes.getNamedItem("name")?.nodeValue
                if (name != null) add(name)
            }
        }
    }
}
