package me.anno.graph.visual.render.scene

import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer

/**
 * Copies the input data into a temporary buffer, so we read from different buffers to where we write to;
 * This is required for WebGL, might improve performance, and might improve race-conditions.
 *
 * todo when a decal overrides all attributes, we can even apply it to forward-rendering :3
 * */
class RenderDecalsNode : RenderDeferredNode() {

    init {
        name = "RenderDecals"
    }

    companion object {
        var srcBuffer: IFramebuffer? = null
    }

    override fun createNewFramebuffer(settings: DeferredSettings, samples: Int) {
        super.createNewFramebuffer(settings, samples)
        srcBufferI = settings.createBaseBuffer("srcBuffer", 1)
    }

    private var srcBufferI: IFramebuffer? = null
    override fun copyInputsOrClear(framebuffer: IFramebuffer) {
        super.copyInputsOrClear(framebuffer)
        if (!needsRendering()) return // small optimization, we should do better though...
        val srcBufferI = srcBufferI!!
        if (framebuffer is Framebuffer) {
            // ensure size; binding isn't really necessary
            useFrame(getIntInput(1), getIntInput(2), true, srcBufferI) {
                framebuffer.copyTo(srcBufferI, copyColor = true, copyDepth = true)
            }
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