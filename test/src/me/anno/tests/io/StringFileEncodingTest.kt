package me.anno.tests.io

import me.anno.engine.projects.FileEncoding
import me.anno.io.files.InvalidRef
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.io.saveable.UnknownSaveable
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.lists.Lists.firstInstance2
import org.junit.jupiter.api.Test

/**
 * tests special symbols in strings that could cause issues
 * */
class StringFileEncodingTest {

    private fun testStrings(strings: List<String>) {
        var ex: Exception? = null
        for (encoding in FileEncoding.entries) {
            try {
                testStrings(encoding, strings)
            } catch (e: Exception) {
                ex?.printStackTrace()
                ex = e
            }
        }
        throw ex ?: return
    }

    private fun testStrings(encoding: FileEncoding, strings: List<String>) {

        // register classes
        registerCustomClass(UnknownSaveable())

        // prepare mega-instance will all properties
        val instance = UnknownSaveable()
        for ((idx, v) in strings.withIndex()) {
            instance.setProperty("p$idx", v)
        }

        // serialization
        val bytes = encoding.encode(instance, InvalidRef)
        if (encoding != FileEncoding.BINARY) println(bytes.decodeToString())

        // deserialization
        val clone = encoding.decode(bytes, InvalidRef, false)
            .firstInstance2(UnknownSaveable::class)

        // equality check
        for ((idx, v) in strings.withIndex()) {
            assertEquals(v, clone["p$idx"], "p$idx @ $encoding")
        }
    }

    @Test
    fun testEmojis() {
        testStrings(listOf("😊🦘", "\uD83C\uDDE9\uD83C\uDDEA"))
    }

    @Test
    fun testUmlauts() {
        testStrings(listOf("ÄÖÜäöüß"))
    }

    @Test
    fun testMultiline() {
        testStrings(listOf("\n", "\n\n"))
    }

    @Test
    fun testEvilLineBreaks() {
        testStrings(listOf("\r", "\r\n"))
    }

    @Test
    fun testXMLSpecific() {
        testStrings(listOf("<>", "&lt;&gt;", "<!-- lol -->", "<?xml ?>"))
    }

    @Test
    fun testYamlSpecific() {
        // line breaks in YAML are insane
        testStrings(listOf("", " ", "- ", " -", "# comment"))
    }

    @Test
    fun testJsonSpecific() {
        testStrings(listOf("[]", "{}", "\"", "'", "null", "false", "true", "\"Hi!\""))
    }
}