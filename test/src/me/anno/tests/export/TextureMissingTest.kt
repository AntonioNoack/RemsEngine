package me.anno.tests.export

import me.anno.Engine
import me.anno.Time
import me.anno.cache.CacheSection
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.gpu.texture.TextureCache
import me.anno.image.ImageCache
import me.anno.jvm.HiddenOpenGLContext
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.OS.downloads
import me.anno.utils.Sleep
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotNull

fun main() {
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()
    TextureCache.timeoutMillis = 10
    ImageCache.timeoutMillis = 10
    PrefabCache.timeoutMillis = 10
    val src = downloads.getChild("3d/DamagedHelmet.glb/textures/1.jpg/b.png")
    assertNotNull(TextureCache[src].waitFor())
    val waitTime = (TextureCache.timeoutMillis + 20) * MILLIS_TO_NANOS
    val endWaitTime = Time.nanoTime + waitTime
    Sleep.waitUntil(true, {
        Time.nanoTime > endWaitTime
    }, {
        CacheSection.updateAll()
        val image = assertNotNull(ImageCache[src].waitFor())
        val asIntImage = image.asIntImage()
        assertEquals(image.width, asIntImage.width)
        Engine.requestShutdown()
    })
}