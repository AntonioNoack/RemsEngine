package me.anno.tests.shader

import me.anno.engine.OfficialExtensions
import me.anno.gpu.shader.Reduction
import me.anno.gpu.texture.TextureCache
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.Color.toHexColor
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager

private val LOGGER = LogManager.getLogger("Reduction")

/**
 * this is a test & sample on how to compute the average color of an image
 * */
fun main() {
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()
    val fileReference = OS.pictures.getChild("4k.jpg")
    TextureCache[fileReference].waitFor { image, _ ->
        LOGGER.info(Reduction.reduce(image!!, Reduction.AVG).toHexColor())
    }
}