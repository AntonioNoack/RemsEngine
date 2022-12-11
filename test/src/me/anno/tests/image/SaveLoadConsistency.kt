package me.anno.tests.image

import me.anno.Engine
import me.anno.cache.instances.VideoCache
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.image.ImageGPUCache
import me.anno.image.raw.GPUImage
import me.anno.io.files.FileReference
import me.anno.utils.Color.toHexColor
import me.anno.utils.OS.desktop
import me.anno.utils.OS.pictures
import me.anno.video.VideoCreator
import me.anno.video.ffmpeg.FFMPEGMetadata.Companion.getMeta

fun main() {
    HiddenOpenGLContext.createOpenGL(2048)
    GFX.check()
    // load image from drive onto gpu
    // store image onto drive
    // repeat / compare
    val image = pictures.getChild("RemsStudio/colortest.jpg")
    // testImage(image)
    testVideo(image)
}

fun testVideo(image: FileReference) {
    val tmp0 = desktop.getChild("x/tmp0.mp4")
    val tmp1 = desktop.getChild("x/tmp1.mp4")
    var loadSum = 0L
    var storeSum = 0L
    val meta = getMeta(image, false)!!
    val w = meta.videoWidth
    val h = meta.videoHeight
    val dst1 = Framebuffer("dst", w, h, TargetType.UByteTarget4, DepthBufferType.NONE)
    var ctr = 0
    fun process(src: FileReference, dst: FileReference) {
        ImageGPUCache.clear()
        val t0 = System.nanoTime()
        VideoCache.clear()
        val gpu = VideoCache.getFrame(src, 1, 0, 0, 1, 1.0, 50_000L, false, needsToBeCreated = true)
            ?: throw NullPointerException("Missing $src")
        if (gpu.w != w || gpu.h != h) throw IllegalStateException()
        useFrame(dst1) {
            dst1.clearColor(0)
            drawTexture(gpu)
        }
        val t1 = System.nanoTime()
        val img = GPUImage(dst1.getTexture0(), 4, hasAlphaChannel = true, hasOwnership = false)
        println("out: " + img.createIntImage().data[0].toHexColor())
        VideoCreator.renderVideo2(w, h, 1.0, dst, 1) { img }
        img.write(tmp0.getSibling("${ctr++}.png"))
        val t2 = System.nanoTime()
        loadSum += t1 - t0
        storeSum += t2 - t0
    }
    process(image, tmp0)
    // ImageCPUCache[tmp0, false]!!.write(tmp0.getSibling("tmp0.png"))
    for (i in 0 until 50) {
        process(tmp0, tmp1)
        process(tmp1, tmp0)
    }
    // ImageCPUCache[tmp0, false]!!.write(tmp0.getSibling("tmp2.png"))
    println(loadSum)
    println(storeSum)
    Engine.requestShutdown()
}

fun testImage(image: FileReference) {
    // no change was noticeable after many, many iterations -> fine :)
    val tmp0 = desktop.getChild("tmp0.jpg")
    val tmp1 = desktop.getChild("tmp1.jpg")
    var loadSum = 0L
    var storeSum = 0L
    fun process(src: FileReference, dst: FileReference) {
        ImageGPUCache.clear()
        val t0 = System.nanoTime()
        val gpu = ImageGPUCache[src, false]!!
        val t1 = System.nanoTime()
        gpu.write(dst)
        val t2 = System.nanoTime()
        loadSum += t1 - t0
        storeSum += t2 - t0
    }
    process(image, tmp0)
    for (i in 0 until 350) {
        process(tmp0, tmp1)
        process(tmp1, tmp0)
    }
    println(loadSum)
    println(storeSum)
}