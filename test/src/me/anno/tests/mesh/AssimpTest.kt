package me.anno.tests.mesh

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.jvm.HiddenOpenGLContext
import me.anno.image.ImageCache
import me.anno.gpu.texture.TextureCache
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads

fun main() {
    OfficialExtensions.initForTests()
    // done: why is the engine sometimes complaining, that it can't read that texture?? :/
    // the following code works fine, and confirms that GPU and CPU methods are working correctly
    HiddenOpenGLContext.createOpenGL()
    val source = downloads.getChild("3d/DamagedHelmet.glb/textures/1.jpg")
    TextureCache[source, false]!!
        .createImage(flipY = false, withAlpha = false)
        .write(desktop.getChild(source.nameWithoutExtension + ".gpu.png"))
    ImageCache[source, false]!!.write(desktop.getChild(source.nameWithoutExtension + ".cpu.png"))
    Engine.requestShutdown()
}