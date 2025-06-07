package me.anno.tests.utils

import me.anno.io.utils.StringMap
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test

class ConfigTest {
    @Test
    fun testDefaultValueIsStored() {
        testDefaultValueIsStored(false, true) { m, v -> m["a", v] }
        testDefaultValueIsStored(0, 1) { m, v -> m["a", v] }
        testDefaultValueIsStored(0L, 1L) { m, v -> m["a", v] }
        testDefaultValueIsStored(0f, 1f) { m, v -> m["a", v] }
        testDefaultValueIsStored(0.0, 1.0) { m, v -> m["a", v] }
        testDefaultValueIsStored("0", "1") { m, v -> m["a", v] }
    }

    fun <V> testDefaultValueIsStored(value0: V, value1: V, getter: (StringMap, V) -> V) {
        testDefaultValueIsStoredI(value0, value1, getter)
        testDefaultValueIsStoredI(value1, value0, getter)
    }

    fun <V> testDefaultValueIsStoredI(value0: V, value1: V, getter: (StringMap, V) -> V) {
        val config = StringMap()
        assertFalse(config.wasChanged)
        assertEquals(value0, getter(config, value0))
        assertTrue(config.wasChanged)
        assertEquals(value0, getter(config, value1))
    }
}