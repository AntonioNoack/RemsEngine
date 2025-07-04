package me.anno.tests.image

import me.anno.Engine
import me.anno.Time
import me.anno.video.VideoCache
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.jvm.HiddenOpenGLContext
import me.anno.gpu.texture.TextureCache
import me.anno.image.raw.GPUImage
import me.anno.io.files.FileReference
import me.anno.utils.Color.toHexColor
import me.anno.utils.OS.desktop
import me.anno.utils.OS.pictures
import me.anno.video.VideoCreator
import me.anno.io.MediaMetadata.Companion.getMeta

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
    val meta = getMeta(image).waitFor()!!
    val w = meta.videoWidth
    val h = meta.videoHeight
    val dst1 = Framebuffer("dst", w, h, TargetType.UInt8x4, DepthBufferType.NONE)
    var ctr = 0
    fun process(src: FileReference, dst: FileReference) {
        TextureCache.clear()
        val t0 = Time.nanoTime
        VideoCache.clear()
        val gpu = VideoCache.getVideoFrame(src, 1, 0, 0, 1, 1.0, 50_000L,  needsToBeCreated = true)
            .waitFor() ?: throw NullPointerException("Missing $src")
        if (gpu.width != w || gpu.height != h) throw IllegalStateException()
        useFrame(dst1) {
            dst1.clearColor(0)
            drawTexture(0, 0, gpu.width, gpu.height, gpu)
        }
        val t1 = Time.nanoTime
        val img = GPUImage(dst1.getTexture0(), 4, hasAlphaChannel = true)
        println("out: " + img.asIntImage().data[0].toHexColor())
        VideoCreator.renderVideo2(w, h, 1.0, dst, 1) { img }
        img.write(tmp0.getSibling("${ctr++}.png"))
        val t2 = Time.nanoTime
        loadSum += t1 - t0
        storeSum += t2 - t0
    }
    process(image, tmp0)
    // ImageCPUCache[tmp0].waitFor()!!.write(tmp0.getSibling("tmp0.png"))
    for (i in 0 until 50) {
        process(tmp0, tmp1)
        process(tmp1, tmp0)
    }
    // ImageCPUCache[tmp0].waitFor()!!.write(tmp0.getSibling("tmp2.png"))
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
        TextureCache.clear()
        val t0 = Time.nanoTime
        val gpu = TextureCache[src].waitFor()!!
        val t1 = Time.nanoTime
        gpu.write(dst)
        val t2 = Time.nanoTime
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