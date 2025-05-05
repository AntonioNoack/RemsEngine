package me.anno.tests.gfx.forwardplus

import me.anno.gpu.GFX
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.graph.visual.render.scene.RenderViewNode
import me.anno.maths.Maths.clamp

class ForwardPlusRenderSceneNode() : RenderViewNode(
    "Forward+ FillLights", listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Enum<me.anno.gpu.pipeline.PipelineStage>", "Stage",
        "Buffer", "LightBuckets"
    ), listOf(
        "Texture", "Illuminated",
        "Texture", "Depth",
    )
) {
    override fun executeAction() {
        val width = getIntInput(1)
        val height = getIntInput(2)
        val samples = clamp(getIntInput(3), 1, GFX.maxSamples)
        if (width <= 0 || height <= 0) return

        var framebuffer = framebuffer
        if (framebuffer !is Framebuffer ||
            framebuffer.width != width ||
            framebuffer.height != height ||
            framebuffer.samples != samples
        ) {
            framebuffer?.destroy()
            if (framebuffer !is Framebuffer || framebuffer.samples != samples) {
                framebuffer = Framebuffer(
                    "Forward+ Render", width, height, samples,
                    TargetType.Float16x4, DepthBufferType.TEXTURE
                )
            } else {
                framebuffer.width = width
                framebuffer.height = height
            }
        }

        useFrame(framebuffer) {

        }

        // todo render the scene
        //  - render geometry; when shading, apply light as post-processing stages using the lights in the buffer
        //  - bind all lights, and bind the buckets,
        //    then use the current bucket to iterate over all lights in that tile
    }
}
