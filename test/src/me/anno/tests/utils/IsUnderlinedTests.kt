package me.anno.tests.utils

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.apache.logging.log4j.PrintColor.ESC
import org.apache.logging.log4j.UnderlineDetector.getUnderlinedRegions
import org.apache.logging.log4j.UnderlineDetector.isUnderlined
import org.junit.jupiter.api.Test

class IsUnderlinedTests {

    @Test
    fun testIsUnderlinedSimple() {
        val text = "${ESC}4mHELLO${ESC}0m"

        for (i in text.indices) {
            if (text[i] in 'A'..'Z') {
                assertTrue(isUnderlined(text, i), "Expected underlined at index $i")
            }
        }
    }

    @Test
    fun testIsUnderlinedReset() {
        val text = "${ESC}4mHELLO${ESC}0m WORLD"
        val split = text.indexOf(" ")

        for (i in text.indices) {
            if (text[i] in 'A'..'Z') {
                if (i < split) {
                    assertTrue(isUnderlined(text, i), "Expected underlined at $i")
                } else {
                    assertFalse(isUnderlined(text, i), "Expected NOT underlined at $i")
                }
            }
        }
    }

    @Test
    fun testIsUnderlinedNestedStyles() {
        val text = "${ESC}31;4mSTYLED${ESC}0m"

        for (i in text.indices) {
            if (text[i] in 'A'..'Z') {
                assertTrue(isUnderlined(text, i))
            }
        }
    }

    @Test
    fun testIsUnderlinedUnderlineOff() {
        val text = "${ESC}4mHELLO${ESC}24m WORLD"
        val offIndex = text.indexOf(" WORLD")
        for (i in text.indices) {
            if (text[i] in 'A'..'Z') {
                if (i < offIndex) {
                    assertTrue(isUnderlined(text, i))
                } else {
                    assertFalse(isUnderlined(text, i))
                }
            }
        }
    }

    @Test
    fun testGetUnderlinedRegionsSimple() {
        val text = "${ESC}4mhello${ESC}0m"
        val regions = getUnderlinedRegions(text)

        assertEquals(1, regions.size)

        val region = regions[0]
        assertEquals("hello", text.substring(region))
    }

    @Test
    fun testGetUnderlinedRegionsMultiple() {
        val text = buildString {
            append("${ESC}4mhello${ESC}0m ")
            append("${ESC}4mworld${ESC}0m")
        }

        val regions = getUnderlinedRegions(text)
        assertEquals(2, regions.size)
        assertEquals("hello", text.substring(regions[0]))
        assertEquals("world", text.substring(regions[1]))
    }

    @Test
    fun testGetUnderlinedRegionsMixed() {
        val text = buildString {
            append("normal ")
            append("${ESC}4mlink${ESC}0m ")
            append("text")
        }

        val regions = getUnderlinedRegions(text)

        assertEquals(1, regions.size)
        assertEquals("link", text.substring(regions[0]))
    }

    @Test
    fun testGetUnderlinedRegionsUnderlineOff() {
        val text = "${ESC}4mhello${ESC}24mworld"
        val regions = getUnderlinedRegions(text)

        assertEquals(1, regions.size)
        assertEquals("hello", text.substring(regions[0]))
    }

    @Test
    fun testGetUnderlinedRegionsCombinedCodes() {
        val text = "${ESC}1;4;31mhello${ESC}0m"
        val regions = getUnderlinedRegions(text)

        assertEquals(1, regions.size)
        assertEquals("hello", text.substring(regions[0]))
    }
}