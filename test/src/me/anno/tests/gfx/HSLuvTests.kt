package me.anno.tests.gfx

import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.utils.assertions.assertEquals
import org.hsluv.HSLuvColorSpace
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test

class HSLuvTests {
    fun forAllColors(callback: (Vector3f) -> Unit) {
        val values = listOf(0f, 0.1f, 0.3f, 0.5f, 0.7f, 0.9f, 1f)
        val tmp = Vector3f()
        for (x in values) {
            for (y in values) {
                for (z in values) {
                    callback(tmp.set(x, y, z))
                }
            }
        }
    }

    @Test
    fun testInvertHSLuv() {
        val hsluv = Vector3f()
        val rgb2 = Vector3f()
        forAllColors { rgb ->
            HSLuv.fromRGB(rgb, hsluv)
            HSLuv.toRGB(hsluv, rgb2)
            assertEquals(rgb, rgb2, 1e-4)
        }
    }

    @Test
    fun testInvertHPLuv() {
        val hpluv = Vector3d()
        val rgb = Vector3d()
        val rgb2 = Vector3d()
        forAllColors { rgb0 ->
            rgb.set(rgb0)
            HSLuvColorSpace.rgbToHpluv(rgb, hpluv)
            HSLuvColorSpace.hpluvToRgb(hpluv, rgb2)
            assertEquals(rgb, rgb2, 1e-12)
        }
    }
}