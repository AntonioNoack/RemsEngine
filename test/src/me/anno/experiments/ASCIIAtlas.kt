package me.anno.experiments

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.jvm.HiddenOpenGLContext
import me.anno.maths.Maths.ceilDiv
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.Color.black
import me.anno.utils.Color.white
import me.anno.utils.OS.desktop

fun main() {
    // create a texture for the most important symbols in ASCII, so 32 - 128
    val min = 32
    val max = 128
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()
    val height = 32
    val perRow = 16
    val width = height * 2 / 3
    val framebuffer = Framebuffer("font", width * perRow, height * ceilDiv(max - min, perRow), TargetType.UInt8x4)
    val font = monospaceFont.withSize(height * 0.8f)
    GFX.loadTexturesSync.push(true)
    useFrame(framebuffer) {
        framebuffer.clearColor(black)
        for (c in min until max) {
            val y = (height * ((c - min) / perRow)) + height.shr(1)
            val x = (width * ((c - min) % perRow)) + width.shr(1)
            DrawTexts.drawText(
                x, y, font, c.toChar().toString(), white, black,
                -1, -1, AxisAlignment.CENTER, AxisAlignment.CENTER
            )
        }
    }
    framebuffer.getTexture0()
        .write(desktop.getChild("ASCIIAtlas.png"), flipY = true, withAlpha = false)
    Engine.requestShutdown()
}