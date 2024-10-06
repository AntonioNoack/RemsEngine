package me.anno.tests.export

import me.anno.Engine
import me.anno.Time
import me.anno.cache.CacheSection
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.Events
import me.anno.engine.OfficialExtensions
import me.anno.gpu.GPUTasks
import me.anno.gpu.texture.TextureCache
import me.anno.image.ImageCache
import me.anno.jvm.HiddenOpenGLContext
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.OS.downloads
import me.anno.utils.Sleep
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotNull

// can't reproduce the issue :(
fun main() {
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()
    TextureCache.timeoutMillis = 50
    ImageCache.timeoutMillis = 50
    PrefabCache.timeoutMillis = 50
    val src = downloads.getChild("3d/DamagedHelmet.glb/textures/1.jpg/rgb.png")
    assertNotNull(TextureCache[src, false])
    val waitTime = (TextureCache.timeoutMillis + 15000) * MILLIS_TO_NANOS
    val endWaitTime = Time.nanoTime + waitTime
    Sleep.waitUntil(true, {
        Time.nanoTime > endWaitTime
    }, {
        CacheSection.updateAll()
        val image = assertNotNull(ImageCache[src, false])
        val asIntImage = image.asIntImage()
        assertEquals(image.width, asIntImage.width)
        println("Passed test")
        Engine.requestShutdown()
    })

    while (!Engine.shutdown) {
        Events.workEventTasks()
        GPUTasks.workGPUTasks(false)
        CacheSection.updateAll()
    }
}