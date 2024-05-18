package me.anno.graph.visual.render.scene

import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer

/**
 * Copies the input data into a temporary buffer, so we read from different buffers to where we write to;
 * This is required for WebGL, might improve performance, and might improve race-conditions.
 * */
class RenderDecalsNode : RenderDeferredNode() {

    companion object {
        var srcBuffer: IFramebuffer? = null
    }

    override fun createNewFramebuffer(settings: DeferredSettings, samples: Int) {
        super.createNewFramebuffer(settings, samples)
        srcBufferI = settings.createBaseBuffer("srcBuffer", samples)
    }

    private var srcBufferI: IFramebuffer? = null
    override fun copyInputsOrClear(framebuffer: IFramebuffer) {
        super.copyInputsOrClear(framebuffer)
        val srcBufferI = srcBufferI!!
        if (framebuffer is Framebuffer) {
            framebuffer.copyTo(srcBufferI)
        } else {
            bind(srcBufferI) {
                super.copyInputsOrClear(srcBufferI)
            }
        }
        srcBuffer = srcBufferI
    }

    override fun executeAction() {
        super.executeAction()
        srcBuffer = null // clear it for GC
    }

    override fun destroy() {
        super.destroy()
        srcBufferI?.destroy()
    }
}