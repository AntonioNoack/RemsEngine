package me.anno.tests.utils.terminal

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.apache.logging.log4j.PrintColor
import org.apache.logging.log4j.UnderlineDetector
import org.junit.jupiter.api.Test

class IsUnderlinedTests {

    @Test
    fun testIsUnderlinedSimple() {
        val text = "${PrintColor.ESC}4mHELLO${PrintColor.ESC}0m"

        for (i in text.indices) {
            if (text[i] in 'A'..'Z') {
                assertTrue(UnderlineDetector.isUnderlined(text, i), "Expected underlined at index $i")
            }
        }
    }

    @Test
    fun testIsUnderlinedReset() {
        val text = "${PrintColor.ESC}4mHELLO${PrintColor.ESC}0m WORLD"
        val split = text.indexOf(" ")

        for (i in text.indices) {
            if (text[i] in 'A'..'Z') {
                if (i < split) {
                    assertTrue(UnderlineDetector.isUnderlined(text, i), "Expected underlined at $i")
                } else {
                    assertFalse(UnderlineDetector.isUnderlined(text, i), "Expected NOT underlined at $i")
                }
            }
        }
    }

    @Test
    fun testIsUnderlinedNestedStyles() {
        val text = "${PrintColor.ESC}31;4mSTYLED${PrintColor.ESC}0m"

        for (i in text.indices) {
            if (text[i] in 'A'..'Z') {
                assertTrue(UnderlineDetector.isUnderlined(text, i))
            }
        }
    }

    @Test
    fun testIsUnderlinedUnderlineOff() {
        val text = "${PrintColor.ESC}4mHELLO${PrintColor.ESC}24m WORLD"
        val offIndex = text.indexOf(" WORLD")
        for (i in text.indices) {
            if (text[i] in 'A'..'Z') {
                if (i < offIndex) {
                    assertTrue(UnderlineDetector.isUnderlined(text, i))
                } else {
                    assertFalse(UnderlineDetector.isUnderlined(text, i))
                }
            }
        }
    }

    @Test
    fun testGetUnderlinedRegionsSimple() {
        val text = "${PrintColor.ESC}4mhello${PrintColor.ESC}0m"
        val regions = UnderlineDetector.getUnderlinedRegions(text)

        assertEquals(1, regions.size)

        val region = regions[0]
        assertEquals("hello", text.substring(region))
    }

    @Test
    fun testGetUnderlinedRegionsMultiple() {
        val text = buildString {
            append("${PrintColor.ESC}4mhello${PrintColor.ESC}0m ")
            append("${PrintColor.ESC}4mworld${PrintColor.ESC}0m")
        }

        val regions = UnderlineDetector.getUnderlinedRegions(text)
        assertEquals(2, regions.size)
        assertEquals("hello", text.substring(regions[0]))
        assertEquals("world", text.substring(regions[1]))
    }

    @Test
    fun testGetUnderlinedRegionsMixed() {
        val text = buildString {
            append("normal ")
            append("${PrintColor.ESC}4mlink${PrintColor.ESC}0m ")
            append("text")
        }

        val regions = UnderlineDetector.getUnderlinedRegions(text)

        assertEquals(1, regions.size)
        assertEquals("link", text.substring(regions[0]))
    }

    @Test
    fun testGetUnderlinedRegionsUnderlineOff() {
        val text = "${PrintColor.ESC}4mhello${PrintColor.ESC}24mworld"
        val regions = UnderlineDetector.getUnderlinedRegions(text)

        assertEquals(1, regions.size)
        assertEquals("hello", text.substring(regions[0]))
    }

    @Test
    fun testGetUnderlinedRegionsCombinedCodes() {
        val text = "${PrintColor.ESC}1;4;31mhello${PrintColor.ESC}0m"
        val regions = UnderlineDetector.getUnderlinedRegions(text)

        assertEquals(1, regions.size)
        assertEquals("hello", text.substring(regions[0]))
    }
}