package me.anno.tests.utils

import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.AnyToDouble.getDouble
import me.anno.utils.types.AnyToLong.getLong
import org.joml.Vector3d
import org.junit.jupiter.api.Test

class AnyToDoubleTests {
    @Test
    fun testAnyToDouble() {
        assertEquals(0.0, getDouble(false))
        assertEquals(1.0, getDouble(true))
        assertEquals(1.5, getDouble(1.5))
        assertEquals(3.0, getDouble(1.5, 1, 3.0)) // index 1 of Number is default
        assertEquals(1.0, getDouble(Vector3d(1.0, 2.0, 3.0)))
        assertEquals(2.0, getDouble(Vector3d(1.0, 2.0, 3.0), 1, 0.0))
        assertEquals(3.0, getDouble(Vector3d(1.0, 2.0, 3.0), 2, 0.0))
        assertEquals(5.5, getDouble("5.5"))
        assertEquals(17.0, getDouble(Unit, 17.0)) // unknown
    }

    @Test
    fun testAnyToLong() {
        assertEquals(0L, getLong(false))
        assertEquals(1L, getLong(true))
        assertEquals(1L, getLong(1.5))
        assertEquals(3L, getLong(1.5, 1, 3)) // index 1 of Number is default
        assertEquals(1L, getLong(Vector3d(1.0, 2.0, 3.0)))
        assertEquals(2L, getLong(Vector3d(1.0, 2.0, 3.0), 1, 0))
        assertEquals(3L, getLong(Vector3d(1.0, 2.0, 3.0), 2, 0))
        assertEquals(5L, getLong("5")) // decimal
        assertEquals(Long.MIN_VALUE, getLong(Long.MIN_VALUE.toString())) // min
        assertEquals(Long.MAX_VALUE, getLong(Long.MAX_VALUE.toString())) // max
        assertEquals(0x17L, getLong("0x17")) // hex
        assertEquals(0xff123456.toInt().toLong(), getLong("#123456")) // color
        assertEquals(17L, getLong(Unit, 17)) // unknown
    }
}