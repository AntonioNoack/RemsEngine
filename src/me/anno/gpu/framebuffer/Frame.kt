package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import org.lwjgl.opengl.GL11.glViewport
import java.lang.Exception

class Frame(val x: Int, val y: Int, val w: Int, val h: Int, val changeSize: Boolean, val buffer: Framebuffer?, render: () -> Unit) {

    constructor(w: Int, h: Int,
                changeSize: Boolean, buffer: Framebuffer?, render: () -> Unit): this(0, 0, w, h, changeSize, buffer, render)
    constructor(x: Int, y: Int, w: Int, h: Int,
                changeSize: Boolean, render: () -> Unit): this(x, y, w, h, changeSize, currentFrame!!.buffer, render)
    constructor(buffer: Framebuffer?, render: () -> Unit): this(0, 0, buffer?.w ?: GFX.width, buffer?.h ?: GFX.height, false, buffer, render)

    init {
        val lastFrame = currentFrame
        currentFrame = this
        try {
            GFX.check()
            render()
            GFX.check()
            currentFrame = lastFrame
        } catch (e: Throwable){
            currentFrame = lastFrame
            throw e
        }
    }

    fun bind(){
        if(this != lastBoundFrame){

            if(buffer != null){
                if(changeSize){
                    buffer.bindDirectly(w, h, false)
                } else {
                    buffer.bindDirectly(false)
                }
            } else {
                Framebuffer.bindNullDirectly()
            }

            glViewport(x - GFX.deltaX,y - GFX.deltaY, w, h)
            lastBoundFrame = this

            GFX.windowX = x
            GFX.windowY = GFX.height - (y + h)
            GFX.windowWidth = w
            GFX.windowHeight = h

        }
    }

    companion object {

        fun bind(){
            currentFrame!!.bind()
        }

        fun bindMaybe(){
            currentFrame?.bind()
        }

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