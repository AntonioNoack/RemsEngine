package me.anno.tools

import me.anno.engine.OfficialExtensions
import me.anno.fonts.Font
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.desktop

fun main() {

    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()

    val min = 32
    val sx = 10
    val sy = 10

    val cellSizeX = 64
    val cellSizeY = 64

    val font = Font("Ubuntu Mono", cellSizeY * 0.85f)
    val joined = Framebuffer("fontAtlas", cellSizeX * sx, cellSizeY * sy, 1, TargetType.UInt8x3, DepthBufferType.NONE)
    useFrame(joined) {
        for (y in 0 until sy) {
            for (x in 0 until sx) {
                val code = min + x + sx * y
                DrawTexts.drawText(x * cellSizeX, y * cellSizeY, 0, font, code.toChar().toString(), -1, 0)
            }
        }
    }

    joined.getTexture0()
        .write(desktop.getChild("${font.name}.webp"))

}