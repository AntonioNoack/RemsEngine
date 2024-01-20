package me.anno.tests.gfx

import me.anno.engine.ECSRegistry
import me.anno.gpu.GFXState
import me.anno.gpu.drawing.DrawGradients
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.maths.Maths
import me.anno.utils.OS

fun main() {
    // test odd widths
    // fixed the bug :)
    val s = 31
    ECSRegistry.initWithGFX(s + s.and(1))
    val fb = FBStack["", s, s, 4, false, 1, DepthBufferType.NONE]
    GFXState.useFrame(fb) {
        val black = 255 shl 24
        // random color, so we can observe changes in the preview icon
        val color = (Maths.random() * (1 shl 24)).toInt() or 0x333333
        DrawGradients.drawRectGradient(
            0, 0, s, s, color or black, black
        )
    }
    val image = fb.createImage(false, withAlpha = false)
    image.write(OS.desktop.getChild("odd.png"))
}