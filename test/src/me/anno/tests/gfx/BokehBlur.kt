package me.anno.tests.gfx

import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.effects.BokehBlur
import me.anno.gpu.texture.TextureCache
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.OS.pictures
import kotlin.math.max

fun main() {
    testDrawing("Bokeh Blur") { p, canvas ->
        val dst = FBStack["bokeh", p.width, p.height, 3, true, 1, DepthBufferType.NONE]
        val src = TextureCache[pictures.getChild("4k.jpg")].waitFor()!!
        val window = p.window!!
        canvas.drawTexture(p.x, p.y, p.width, p.height, src) // no idea why that's needed :/
        canvas.custom {
            BokehBlur.draw(src, dst, 0.1f * max(window.mouseX - p.x, 0f) / p.width, true)
        }
        canvas.drawTexture(p.x, p.y, p.width, p.height, dst.getTexture0())
    }
}