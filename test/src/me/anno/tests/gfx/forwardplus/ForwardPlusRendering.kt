package me.anno.tests.gfx.forwardplus

import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderMode.Companion.opaqueNodeSettings
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeLayout.Companion.bind
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.TextureLib
import me.anno.graph.visual.render.QuickPipeline
import me.anno.graph.visual.render.effects.ToneMappingNode
import me.anno.graph.visual.render.scene.CombineLightsNode
import me.anno.graph.visual.render.scene.RenderDeferredNode
import me.anno.graph.visual.render.scene.RenderLightsNode
import me.anno.graph.visual.render.scene.RenderViewNode
import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.clamp
import me.anno.tests.engine.light.createLightTypesScene
import me.anno.utils.Color

val bucketSize = 32
val maxLightsPerTile = 32

val lightBucketLayout = bind(Attribute("index", AttributeType.UINT32, 1))

class ForwardPlusFillLightBufferNode : RenderViewNode(
    "Forward+ FillLights", listOf(
        "Int", "Width",
        "Int", "Height",
    ), listOf(
        "Buffer", "LightBuckets"
    )
) {

    var lightBuckets: ComputeBuffer? = null

    override fun executeAction() {
        val width = getIntInput(1)
        val height = getIntInput(2)

        val width2 = ceilDiv(width, bucketSize)
        val height2 = ceilDiv(height, bucketSize)
        if (width2 <= 0 || height2 <= 0) return

        val numBuckets = width2 * height2
        val numValues = numBuckets * maxLightsPerTile
        var lightBuckets = lightBuckets
        if (lightBuckets == null || lightBuckets.elementCount != numValues) {
            lightBuckets?.destroy()
            lightBuckets = ComputeBuffer("LightBuckets", lightBucketLayout, numValues)
            this.lightBuckets = lightBuckets
        }

        var framebuffer = framebuffer
        if (framebuffer !is Framebuffer || framebuffer.width != width2 || framebuffer.height != height2) {
            framebuffer?.destroy()
            if (framebuffer !is Framebuffer) {
                framebuffer = Framebuffer(
                    "UnusedForLightBuckets", width2, height2,
                    1, TargetType.UInt8x1, DepthBufferType.NONE
                )
            } else {
                framebuffer.width = width2
                framebuffer.height = height2
            }
        }

        useFrame(framebuffer) {
            // todo render all lights in 32x smaller for testing in a pseudo-framebuffer,
            //  register their IDs into a separate buffer
            pipeline.lightStage.draw(
                pipeline, { lightType, isInstanced ->
                    // todo create shader
                    // todo bind shader
                    // todo bind buckets-buffer
                    TODO()
                },
                // todo can we get a down-scaled depth-texture? :) maybe the one from BoxCulling
                TextureLib.depthTexture, Color.black4
            )
        }
    }

    override fun destroy() {
        super.destroy()
        lightBuckets?.destroy()
        lightBuckets = null
    }
}

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

/**
 * Forward+ rendering (very WIP)
 * */
fun main() {

    val renderMode = RenderMode(
        "Forward+",
        QuickPipeline()
            // .then(BoxCullingNode())
            .then1(RenderDeferredNode(), opaqueNodeSettings)
            // .then(RenderDecalsNode())
            .then(RenderLightsNode())
            // .then(SSAONode())
            .then(CombineLightsNode())
            // .then(SSRNode())
            // .then(RenderGlassNode())
            .then(ToneMappingNode())
            // .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
            // .then(OutlineEffectSelectNode())
            // .then1(OutlineEffectNode(), mapOf("Fill Colors" to listOf(Vector4f()), "Radius" to 1))
            // .then(GizmoNode())
            // .then(UnditherNode())
            // .then(FXAANode())
            .finish()
    )

    testSceneWithUI("Forward+", createLightTypesScene()) {
        (it.editControls as DraggingControls).settings.renderMode = renderMode
    }
}