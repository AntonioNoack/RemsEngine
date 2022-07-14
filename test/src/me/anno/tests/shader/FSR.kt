package me.anno.tests

import me.anno.ecs.components.shaders.effects.FSR
import me.anno.gpu.OpenGL
import me.anno.gpu.copying.FramebufferToMemory
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.ShaderLib
import me.anno.image.ImageGPUCache
import me.anno.io.files.FileReference
import me.anno.utils.OS

fun main() {

    // testing to upscale and sharpen an image

    HiddenOpenGLContext.createOpenGL()

    val src = FileReference.getReference(OS.pictures, "rem-original.jpg")
    val texture = ImageGPUCache.getImage(src, 10000, false)!!

    ShaderLib.init()

    val size = 3

    val ow = texture.w * size
    val oh = texture.h * size

    val upscaled = FBStack["", ow, oh, 4, false, 1, false]
    OpenGL.useFrame(upscaled) { FSR.upscale(texture, 0, 0, ow, oh, true, applyToneMapping = false) }
    FramebufferToMemory.createImage(upscaled, false, withAlpha = false)
        .write(src.getSibling("${src.nameWithoutExtension}-${size}x.png"))

    val sharpened = FBStack["", ow, oh, 4, false, 1, false]
    OpenGL.useFrame(sharpened) { FSR.sharpen(upscaled.textures.first(), 1f, 0, 0, ow, oh, true) }

    FramebufferToMemory.createImage(sharpened, false, withAlpha = false)
        .write(src.getSibling("${src.nameWithoutExtension}-${size}x-s.png"))

}