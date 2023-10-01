package me.anno.tests.gfx

import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.effects.BokehBlur
import me.anno.image.ImageGPUCache
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.OS.pictures
import kotlin.math.max

fun main() {
    testDrawing("Bokeh Blur") {
        val dst = FBStack["bokeh", it.width, it.height, 3, true, 1, DepthBufferType.NONE]
        val src = ImageGPUCache[pictures.getChild("4k.jpg"), false]!!
        val window = it.window!!
        DrawTextures.drawTexture(it.x, it.y, it.width, it.height, src) // no idea why that's needed :/
        BokehBlur.draw(src, dst, 0.1f * max(window.mouseX - it.x, 0f) / it.width, true)
        DrawTextures.drawTexture(it.x, it.y, it.width, it.height, dst.getTexture0())
    }
}