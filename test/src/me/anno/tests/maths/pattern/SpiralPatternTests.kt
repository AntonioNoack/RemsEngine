package me.anno.tests.maths.pattern

import me.anno.maths.patterns.SpiralPattern.spiral2d
import me.anno.utils.assertions.assertEquals
import org.joml.Vector3i
import org.junit.jupiter.api.Test

class SpiralPatternTests {

    val spiral0 get() = listOf(
        Vector3i(0, 17, 0)
    )

    val spiral1 get() = listOf(
        Vector3i(0, 17, 1),
        Vector3i(1, 17, 1),
        Vector3i(1, 17, 0),
        Vector3i(1, 17, -1),
        Vector3i(0, 17, -1),
        Vector3i(-1, 17, -1),
        Vector3i(-1, 17, 0),
        Vector3i(-1, 17, 1),
    )

    val spiral2 get() = listOf(
        Vector3i(0, 17, 2),
        Vector3i(1, 17, 2),
        Vector3i(2, 17, 2),
        Vector3i(2, 17, 1),
        Vector3i(2, 17, 0),
        Vector3i(2, 17, -1),
        Vector3i(2, 17, -2),
        Vector3i(1, 17, -2),
        Vector3i(0, 17, -2),
        Vector3i(-1, 17, -2),
        Vector3i(-2, 17, -2),
        Vector3i(-2, 17, -1),
        Vector3i(-2, 17, 0),
        Vector3i(-2, 17, 1),
        Vector3i(-2, 17, 2),
        Vector3i(-1, 17, 2),
    )

    @Test
    fun testSpiral2dRadius0() {
        assertEquals(spiral0, spiral2d(0, 17, false))
        assertEquals(spiral0, spiral2d(0, 17, true))
    }

    @Test
    fun testSpiral2dRadius1() {
        assertEquals(spiral1, spiral2d(1, 17, false))
        assertEquals(spiral0 + spiral1, spiral2d(1, 17, true))
    }

    @Test
    fun testSpiral2dRadius2() {
        assertEquals(spiral2, spiral2d(2, 17, false))
        assertEquals(spiral0 + spiral1 + spiral2, spiral2d(2, 17, true))
    }
}