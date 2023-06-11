package me.anno.tests.gfx

import me.anno.config.DefaultConfig
import me.anno.gpu.drawing.DrawTextures
import me.anno.image.ImageGPUCache
import me.anno.input.Input
import me.anno.ui.Panel
import me.anno.ui.debug.TestStudio
import me.anno.utils.OS

fun main() {
    TestStudio.testUI3 {
        // todo test Bokeh blur
        // val dst = Framebuffer("tmp", 512, 512, 1, 1, false, DepthBufferType.NONE)
        object : Panel(DefaultConfig.style) {
            override fun onUpdate() {
                invalidateDrawing()
            }

            override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                super.onDraw(x0, y0, x1, y1)
                val src = ImageGPUCache[OS.pictures.getChild("BricksColor.png"), false]!!
                if (Input.isShiftDown) {
                    println("yes")
                    //draw(src, dst, 0.05f, true)
                    //drawTexture(x, y, w, h, dst.getTexture0())
                } else {
                    println("no")
                    DrawTextures.drawTexture(x, y, w, h, src)
                }
            }
        }
    }
}