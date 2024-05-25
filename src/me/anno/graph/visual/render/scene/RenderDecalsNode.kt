package me.anno.graph.visual.render.scene

import me.anno.ecs.components.mesh.material.shaders.DecalShader.Companion.srcBuffer
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.pipeline.PipelineStage

/**
 * Copies the input data into a temporary buffer, so we read from different buffers to where we write to;
 * This is required for WebGL, might improve performance, and might improve race-conditions.
 *
 * todo when a decal overrides all attributes, we can even apply it to forward-rendering :3
 * */
class RenderDecalsNode : RenderDeferredNode() {

    init {
        name = "RenderDecals"
        setInput(4, PipelineStage.DECAL) // stage
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
        srcBuffer = srcBufferI
        if (framebuffer is Framebuffer) {
            // ensure size; binding isn't really necessary
            srcBufferI.ensureSize(getIntInput(1), getIntInput(2), 0)
            framebuffer.copyTo(srcBufferI, copyColor = true, copyDepth = true)
        } else super.copyInputsOrClear(srcBufferI)
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