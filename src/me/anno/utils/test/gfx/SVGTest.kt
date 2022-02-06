package me.anno.utils.test.gfx

import me.anno.cache.data.ImageData.Companion.imageTimeout
import me.anno.cache.instances.MeshCache
import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle
import me.anno.gpu.SVGxGFX
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureLib
import me.anno.io.files.FileReference
import me.anno.ui.Panel
import me.anno.ui.debug.TestStudio
import me.anno.utils.OS
import org.joml.Matrix4fArrayList

fun main() {

    // what was the error?: 1) we renamed finalPosition to localPosition, but not everywhere...

    TestStudio {
        val srcFile = FileReference.getReference(OS.downloads, "tiger.svg")
        val panel = object : Panel(DefaultConfig.style) {
            override fun tickUpdate() {
                invalidateDrawing()
            }

            override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                val buffer = MeshCache.getSVG(srcFile, imageTimeout, false)!!
                val transform = Matrix4fArrayList()
                transform.scale((buffer.maxY / buffer.maxX).toFloat(), 1f, 1f)
                val white = TextureLib.whiteTexture
                SVGxGFX.draw3DSVG(
                    null, 1.0,
                    transform, buffer, white,
                    DefaultStyle.white4, Filtering.NEAREST,
                    white.clamping!!, null
                )
            }
        }
        panel
    }

}