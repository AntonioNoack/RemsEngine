package me.anno.tests.structures

import me.anno.io.Saveable.Companion.registerCustomClass
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.utils.StringMap
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StringMapTest {

    @Test
    fun getSetClone() {

        registerCustomClass(StringMap())

        val map = StringMap()
        assertTrue(map["debug.ui.enableVsync", true])
        assertEquals(map["debug.ui.enableVsync"], true)
        map["debug.ui.enableVsync"] = false
        assertFalse(map["debug.ui.enableVsync", true])

        val clone = JsonStringReader.clone(map) as StringMap
        assertEquals(clone["debug.ui.enableVsync"], false)
    }
}