package me.anno.gpu

import me.anno.engine.ui.render.RenderView
import me.anno.gpu.framebuffer.Framebuffer

interface VRRenderingRoutine {
    /**
     * returns true on success
     * */
    fun startSession(window: OSWindow, rv: RenderView): Boolean

    /**
     * returns whether the next frame still shall be drawn by this context
     * */
    fun drawFrame(window: OSWindow): Boolean

    val fb: Framebuffer
    val isActive: Boolean
}