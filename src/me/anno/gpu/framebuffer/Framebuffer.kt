package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30.*
import java.awt.Frame
import java.lang.RuntimeException
import java.util.*

class Framebuffer(var w: Int, var h: Int, val targetCount: Int, val fpTargets: Boolean, val createDepthBuffer: Boolean){

    var pointer = -1
    var depthRenderBuffer = -1

    lateinit var textures: Array<Texture2D>

    fun bind(){
        if(pointer < 0) create()
        currentFramebuffer = this
        glBindFramebuffer(GL_FRAMEBUFFER, pointer)
        glViewport(0,0, w, h)
    }

    fun bind(newWidth: Int, newHeight: Int){
        if(newWidth != w || newHeight != h){
            w = newWidth
            h = newHeight
            destroy()
            create()
        }
        bind()
    }

    fun create(){
        pointer = glGenFramebuffers()
        if(pointer < 0) throw RuntimeException()
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
            if(depthRenderBuffer < 0) throw RuntimeException()
            glBindRenderbuffer(GL_RENDERBUFFER, depthRenderBuffer)
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, w, h)
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRenderBuffer)
        }
        check()
    }

    fun check(){
        val state = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        if(state != GL_FRAMEBUFFER_COMPLETE){
            throw RuntimeException("framebuffer is incomplete: $state")
        }
    }

    fun bindTextures(nearest: Boolean){
        textures.forEachIndexed { index, texture ->
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + index)
            texture.bind(nearest)
        }
    }

    fun destroy(){
        if(pointer > -1){
            glDeleteFramebuffers(pointer)
            pointer = -1
            textures.forEach {
                it.destroy()
            }
        }
        if(depthRenderBuffer > -1){
            glDeleteRenderbuffers(depthRenderBuffer)
            depthRenderBuffer = -1
        }
    }

    companion object {
        private var currentFramebuffer: Framebuffer? = null
        val stack = Stack<Framebuffer>()
        fun bindNull(){
            currentFramebuffer = null
        }
        fun bindNullTemporary(){
            stack.push(currentFramebuffer)
            bindNull()
        }
        fun unbindNull(){
            if(stack.isEmpty()) throw RuntimeException("No framebuffer was found!")
            stack.pop().bind()
        }
    }

    fun bindTemporary(newWidth: Int, newHeight: Int){
        stack.push(currentFramebuffer)
        bind(newWidth, newHeight)
    }

    fun bindTemporary(){
        stack.push(currentFramebuffer)
        bind()
    }

    fun unbind(){
        if(stack.isEmpty()) throw RuntimeException("No framebuffer was found!")
        stack.pop().bind()
    }

}