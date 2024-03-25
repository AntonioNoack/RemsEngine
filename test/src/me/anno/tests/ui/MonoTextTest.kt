package me.anno.tests.ui

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.extensions.ExtensionLoader
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.Color.black
import me.anno.utils.OS.desktop

fun main() {

    OfficialExtensions.register()
    ExtensionLoader.load()

    HiddenOpenGLContext.createOpenGL()

    val padding = 1
    val lineHeight = DrawTexts.monospaceFont.sizeInt + padding

    val lines = listOf(
        'A'..'Z',
        'a'..'z',
        '0'..'9',
        "!\\%&/=+-*.,(){}[]"
    ).map {
        if (it is CharRange) it.joinToString("")
        else it.toString()
    }

    val charWidth = DrawTexts.monospaceFont.sampleWidth
    if (charWidth <= 0) throw IllegalStateException()

    val image = Framebuffer("mono",
        (charWidth + padding) * lines.maxOf { it.length } + padding, lines.size * lineHeight + padding,
        1, 1, false,
        DepthBufferType.NONE)

    // write all letters as mono
    useFrame(image) {
        image.clearColor(0x777777 or black)
        for ((yi, line) in lines.withIndex()) {
            val y = yi * lineHeight + padding
            for ((xi, char) in line.withIndex()) {
                val x = padding + xi * (charWidth + padding)
                DrawTexts.drawSimpleTextCharByChar(
                    x, y, 0, char.toString(), -1, 0
                )
            }
        }
    }

    image.getTexture0().write(desktop.getChild("mono.png"), true)

    Engine.requestShutdown()
}