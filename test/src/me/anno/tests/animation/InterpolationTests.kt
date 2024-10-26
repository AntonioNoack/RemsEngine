package me.anno.tests.animation

import me.anno.animation.Interpolation
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class InterpolationTests {
    @Test
    fun testOutTypes() {
        val e = 1e-6f
        for (type in Interpolation.entries) {
            assertEquals(type.getIn(0f), 0f, e)
            assertEquals(type.getIn(1f), 1f, e)
            assertEquals(type.getOut(0f), 0f, e)
            assertEquals(type.getOut(1f), 1f, e)
            assertEquals(type.getIn(0.0f), 1f - type.getOut(1.0f), e)
            assertEquals(type.getIn(0.1f), 1f - type.getOut(0.9f), e)
            assertEquals(type.getIn(0.2f), 1f - type.getOut(0.8f), e)
            assertEquals(type.getIn(0.3f), 1f - type.getOut(0.7f), e)
            assertEquals(type.getIn(0.4f), 1f - type.getOut(0.6f), e)
            assertEquals(type.getIn(0.5f), 1f - type.getOut(0.5f), e)
            assertEquals(type.getIn(0.6f), 1f - type.getOut(0.4f), e)
            assertEquals(type.getIn(0.7f), 1f - type.getOut(0.3f), e)
            assertEquals(type.getIn(0.8f), 1f - type.getOut(0.2f), e)
            assertEquals(type.getIn(0.9f), 1f - type.getOut(0.1f), e)
            assertEquals(type.getIn(1.0f), 1f - type.getOut(0.0f), e)
        }
    }

    @Test
    fun testInOutTypes() {
        for (type in Interpolation.entries) {
            assertEquals(type.getInOut(0.5f), 0.5f, 1e-5f)
        }
        for (type in Interpolation.entries) {
            val e = 1e-7f
            assertEquals(type.getInOut(0.5f - e), type.getInOut(0.5f + e), 1e-3f)
        }
    }

    @Test
    fun testReverseType() {
        val typeByName = Interpolation.entries.associateBy { it.name }
        for (type in Interpolation.entries) {
            val name = type.name
            val expectedSuffix = when {
                name.endsWith("_IN") -> "_OUT"
                name.endsWith("_OUT") -> "_IN"
                name.endsWith("_SYM") -> "_SYM"
                else -> continue
            }
            val expectedTypeName = name.substring(0, name.lastIndexOf('_')) + expectedSuffix
            val expectedType = typeByName[expectedTypeName]
            assertEquals(expectedType, type.getReversedType())
        }
    }

    @Test
    fun testClampedFunctions() {
        for (type in Interpolation.entries) {
            assertEquals(type.getIn(0f), type.getInClamped(-1f))
            assertEquals(type.getOut(0f), type.getOutClamped(-1f))
            assertEquals(type.getInOut(0f), type.getInOutClamped(-1f))
            assertEquals(type.getIn(1f), type.getInClamped(2f))
            assertEquals(type.getOut(1f), type.getOutClamped(2f))
            assertEquals(type.getInOut(1f), type.getInOutClamped(2f))
        }
    }
}