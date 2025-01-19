package me.anno.tests.structures

import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.io.utils.StringMap
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test

class StringMapTest {

    @Test
    fun getSetClone() {

        registerCustomClass(StringMap())

        val map = StringMap()
        assertTrue(map["debug.ui.enableVsync", true])
        assertEquals(map["debug.ui.enableVsync"], true)
        map["debug.ui.enableVsync"] = false
        assertFalse(map["debug.ui.enableVsync", true])

        val clone = JsonStringReader.clone(map)
        assertEquals(clone["debug.ui.enableVsync"], false)
    }
}