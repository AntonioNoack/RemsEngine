package me.anno.tests.shader

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.gpu.GFXState
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.effects.FSR
import me.anno.gpu.texture.TextureCache
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.pictures

fun main() {

    // testing to upscale and sharpen an image

    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()

    val src = pictures.getChild("Anime/90940211_p0_master1200.jpg")
    val texture = TextureCache[src, 10000].waitFor()!!

    val size = 3

    val ow = texture.width * size
    val oh = texture.height * size

    val upscaled = FBStack["", ow, oh, 4, false, 1, DepthBufferType.NONE] as Framebuffer
    GFXState.useFrame(upscaled) {
        FSR.upscale(
            texture, 0, 0, ow, oh, 0,
            flipY = true, applyToneMapping = false, withAlpha = false
        )
    }
    upscaled.createImage(false, withAlpha = false)
        ?.write(src.getSibling("${src.nameWithoutExtension}-${size}x.png"))

    val sharpened = FBStack["", ow, oh, 4, false, 1, DepthBufferType.NONE]
    GFXState.useFrame(sharpened) { FSR.sharpen(upscaled.getTexture0(), 1f, 0, 0, ow, oh, true) }

    sharpened.createImage(false, withAlpha = false)
        ?.write(src.getSibling("${src.nameWithoutExtension}-${size}x-s.png"))

    Engine.requestShutdown()
}