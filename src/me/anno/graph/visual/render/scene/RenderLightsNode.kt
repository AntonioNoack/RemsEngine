package me.anno.graph.visual.render.scene

import me.anno.ecs.components.light.LightType
import me.anno.gpu.Blitting
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.mask
import me.anno.graph.visual.render.Texture.Companion.mask1Index
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.graph.visual.render.compiler.GraphShader
import me.anno.graph.visual.render.scene.utils.DeferredLightsShader
import me.anno.maths.Maths.clamp

/**
 * collects the lights within a scene
 * */
class RenderLightsNode : RenderViewNode(
    "Render Lights",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Int", "Camera Index",
        // make them same order as outputs from RenderSceneNode
        "Vector3f", "Normal",
        "Float", "Reflectivity",
        "Float", "Translucency",
        "Float", "Sheen",
        "Float", "Depth",
    ),
    listOf("Texture", "Light")
) {

    val firstInputIndex = 5
    val depthIndex = firstInputIndex + 4

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 1) // samples
        setInput(4, 0) // camera index
    }

    override fun invalidate() {
        for (it in shaders) it?.first?.destroy()
        shaders.fill(null)
    }

    private val shaders =
        arrayOfNulls<GraphShader>(LightType.entries.size.shl(1)) // current number of shaders

    private fun getLightShader(lightType: LightType, isInstanced: Boolean): Shader {
        return DeferredLightsShader.getLightShader(
            inputs, name, lightType, isInstanced, graph as FlowGraph,
            firstInputIndex, shaders
        )
    }

    override fun executeAction() {

        // default output in case of error
        setOutput(1, null)

        val width = getIntInput(1)
        val height = getIntInput(2)
        val samples = clamp(getIntInput(3), 1, GFX.maxSamples)
        if (width < 1 || height < 1) return

        val depthTexture = getInput(depthIndex) as? Texture
        val depthT = depthTexture.texOrNull ?: return
        val depthM = depthTexture.mask1Index

        val useDepth = true
        val framebuffer = FBStack[
            name, width, height, TargetType.Float16x3, samples,
            if (useDepth) DepthBufferType.INTERNAL else DepthBufferType.NONE
        ]

        timeRendering(name, timer) {
            useFrame(width, height, true, framebuffer, copyRenderer) {
                val stage = pipeline.lightStage
                Blitting.copyColorAndDepth(blackTexture, depthT, depthM, false)
                stage.bind {
                    stage.draw(pipeline, ::getLightShader, depthT, depthTexture.mask)
                }
            }
            setOutput(1, Texture.texture(framebuffer, 0, "rgb", DeferredLayerType.LIGHT_SUM))
        }
    }
}