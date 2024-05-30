package me.anno.tests.mesh.hexagons

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.gpu.texture.Texture2DArray
import me.anno.image.ImageCache
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.desktop
import me.anno.utils.OS.pictures

// textures for HexagonSphereMC2 were broken because of our introduction of offset and stride in images
fun main() {
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()
    val source = pictures.getChild("textures/atlas.webp")
    val image = ImageCache[source, false]!!
    val images = image.split(16, 16)
    val texture = Texture2DArray("tex", 1, 1, 1)
    texture.create(images, false)
    val folder = desktop.getChild("test")
    folder.deleteRecursively()
    folder.tryMkdirs()
    for (i in images.indices) {
        images[i].write(folder.getChild("tex$i.png"))
    }
    // todo why is that result black???
    texture.write(folder.getChild("tex2image.png"))
    Engine.requestShutdown()
}