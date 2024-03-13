package me.anno.tests.shader

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.extensions.ExtensionLoader
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.Reduction
import me.anno.gpu.texture.TextureCache
import me.anno.utils.Color.toHexColor
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager

/**
 * this is a test & sample on how to compute the average color of an image
 * */
fun main() {
    OfficialExtensions.register()
    ExtensionLoader.load()
    HiddenOpenGLContext.createOpenGL()
    val fileReference = OS.pictures.getChild("4k.jpg")
    val image = TextureCache[fileReference, false]!!
    LogManager.getLogger("Reduction").info(Reduction.reduce(image, Reduction.AVG).toHexColor())
    Engine.requestShutdown()
}