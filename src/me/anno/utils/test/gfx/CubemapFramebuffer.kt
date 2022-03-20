package me.anno.utils.test.gfx

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.texture.CubemapTexture
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL30

fun main() {

    HiddenOpenGLContext.createOpenGL()

    Frame.invalidate()

    val w = 512

    val layers = Array(6) { GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + it }

    // LOGGER.info("w: $w, h: $h, samples: $samples, targets: $targetCount x fp32? $fpTargets")
    GFX.check()
    val pointer = GL30.glGenFramebuffers()
    if (pointer < 0) throw RuntimeException()
    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, pointer)
    Frame.lastPtr = pointer
    //stack.push(this)
    GFX.check()
    /* for(index in textures.indices){
         val texture = textures[index]
         GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + index, tex2D, texture.pointer, 0)
     }*/
    GFX.check()
    /*if (targets.size > 1) {// skip array alloc otherwise
        GL30.glDrawBuffers(textures.indices.map { it + GL30.GL_COLOR_ATTACHMENT0 }.toIntArray())
    } else GL30.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0)*/
    GFX.check()

    // first try
    /*for(i in 0 until 6){
        val depthTexture = Texture2D("depth", w, h, samples)
        depthTexture.createDepth()
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, layers[i], depthTexture.pointer, 0)
    }*/

    // second try
    val depthTexture = CubemapTexture("depth", w, 1)
    depthTexture.createDepth()

    for (i in 0 until 6) {
        GL30.glFramebufferTexture2D(
            GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
            layers[i], depthTexture.pointer, 0
        )
    }

    GFX.check()
    val state = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)
    if (state != GL30.GL_FRAMEBUFFER_COMPLETE) {
        throw RuntimeException("Framebuffer is incomplete: ${GFX.getErrorTypeName(state)}")
    }

    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, pointer)

}