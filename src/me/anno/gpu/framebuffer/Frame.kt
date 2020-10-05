package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import org.lwjgl.opengl.GL11.glViewport

class Frame(val x: Int, val y: Int, val w: Int, val h: Int, val buffer: Framebuffer?, render: () -> Unit) {

    constructor(w: Int, h: Int, buffer: Framebuffer?, render: () -> Unit): this(0, 0, w, h, buffer, render)
    constructor(x: Int, y: Int, w: Int, h: Int, render: () -> Unit): this(x, y, w, h, currentFrame!!.buffer, render)
    constructor(buffer: Framebuffer?, render: () -> Unit): this(0, 0, buffer?.w ?: GFX.width, buffer?.h ?: GFX.height, buffer, render)

    init {
        val lastFrame = currentFrame
        currentFrame = this
        render()
        currentFrame = lastFrame
    }

    fun bind(){
        if(this != lastBoundFrame){
            buffer?.bindDirectly(x + w, y + h, false) ?: Framebuffer.bindNullDirectly()
            glViewport(x, y, w, h)
            lastBoundFrame = this

            GFX.windowX = x
            GFX.windowY = GFX.height - (y + h)
            GFX.windowWidth = w
            GFX.windowHeight = h
        }
    }

    companion object {

        fun reset(){
            lastBoundFrame = null
            currentFrame = null
        }

        fun invalidate(){
            lastBoundFrame = null
        }

        var lastBoundFrame: Frame? = null
        var currentFrame: Frame? = null

    }

}