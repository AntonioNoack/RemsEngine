package me.anno.gpu.framebuffer

import me.anno.gpu.texture.Texture2D
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30.*
import java.lang.RuntimeException

class Framebuffer(val w: Int, val h: Int, val targetCount: Int, val fpTargets: Boolean, val createDepthBuffer: Boolean){

    var pointer = -1
    var depthRenderBuffer = -1

    lateinit var textures: Array<Texture2D>

    fun bind(){
        if(pointer < 0) create()
        glBindFramebuffer(GL_FRAMEBUFFER, pointer)
        glViewport(0,0, w, h)
    }

    fun create(){
        pointer = glGenFramebuffers()
        glBindFramebuffer(GL_FRAMEBUFFER, pointer)
        textures = Array(targetCount){
            val texture = Texture2D(w, h)
            if(fpTargets) texture.createFP32()
            else texture.create()
            texture.filtering(true)
            texture
        }
        textures.forEachIndexed { index, texture ->
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index, GL11.GL_TEXTURE_2D, texture.pointer, 0)
        }
        GL20.glDrawBuffers(textures.indices.map { it + GL_COLOR_ATTACHMENT0 }.toIntArray())
        if(createDepthBuffer){
            depthRenderBuffer = glGenRenderbuffers()
            glBindRenderbuffer(GL_RENDERBUFFER, depthRenderBuffer)
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, w, h)
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRenderBuffer)
        }
    }

    fun check(){
        val state = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        if(state != GL_FRAMEBUFFER_COMPLETE){
            throw RuntimeException("framebuffer is incomplete: $state")
        }
    }

    fun bindTextures(){
        textures.forEachIndexed { index, texture ->
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + index)
            texture.bind()
        }
    }

    fun destroy(){
        if(pointer > -1){
            glDeleteFramebuffers(pointer)
            textures.forEach {
                it.destroy()
            }
        }
        if(depthRenderBuffer > -1){
            glDeleteRenderbuffers(depthRenderBuffer)
        }
    }


}