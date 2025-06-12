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

/**
 * create a texture for the most important symbols in ASCII, so 32 - 128;
 * for use by AtlasFontGenerator
 * */
fun main() {
    val min = 32
    val max = 128
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()
    val height = 48
    val numTilesX = 16
    val width = height * 7 / 12
    val framebuffer = Framebuffer("font", width * numTilesX, height * ceilDiv(max - min, numTilesX), TargetType.UInt8x4)
    val font = monospaceFont.withSize(height.toFloat())
    GFX.loadTexturesSync.push(true)
    val dy = height / 12
    useFrame(framebuffer) {
        framebuffer.clearColor(black)
        for (c in min until max) {
            val y = (height * ((c - min) / numTilesX)) + height.shr(1)
            val x = (width * ((c - min) % numTilesX)) + width.shr(1)
            DrawTexts.drawText(
                x, y + dy, font, c.toChar().toString(), white, black,
                -1, -1, AxisAlignment.CENTER, AxisAlignment.CENTER
            )
        }
    }
    framebuffer.getTexture0()
        .createImage(flipY = false, withAlpha = false)
        .ref.getChild("grayscale.png")
        .copyTo(desktop.getChild("ASCIIAtlas.png"))
    Engine.requestShutdown()
}