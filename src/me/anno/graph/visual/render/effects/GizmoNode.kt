package me.anno.graph.visual.render.effects

import me.anno.gpu.Blitting.copyColorAndDepth
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.depthTexture
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.mask1Index
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.graph.visual.render.scene.RenderViewNode
import me.anno.maths.Maths.clamp

class GizmoNode : RenderViewNode(
    "Gizmos",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Int", "Grid Mask",
        "Bool", "AABBs",
        "Bool", "Debug Shapes",
        "Texture", "Illuminated",
        "Texture", "Depth",
    ), listOf(
        "Texture", "Illuminated",
        "Texture", "Depth"
    )
) {

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 8) // samples
        setInput(4, -1) // grid, -1 = auto
        setInput(5, false) // aabbs
        setInput(6, true) // debug shapes
        setInput(7, null) // depth
    }

    private fun getGridMask(): Int {
        var grid = getIntInput(4)
        if (grid == -1) {
            grid = renderView.drawGridWhenEditing
        }
        return grid
    }

    override fun executeAction() {

        val width = getIntInput(1)
        val height = getIntInput(2)
        val samples = clamp(getIntInput(3), 1, GFX.maxSamples)
        if (width < 1 || height < 1) return

        val grid = getGridMask()
        val aabbs = getBoolInput(5)
        val debug = getBoolInput(6)

        val colorT = getTextureInput(7, blackTexture)
        val depth = getInput(8) as? Texture
        val depthT = depth.texOrNull ?: depthTexture
        val depthM = depth.mask1Index

        // else we can't cast to Framebuffer
        val readsDepth = isOutputUsed(outputs[2])
        val framebuffer = FBStack[
            name, width, height, TargetType.UInt8x4, samples,
            if (readsDepth) DepthBufferType.TEXTURE else DepthBufferType.INTERNAL
        ]

        timeRendering(name, timer) {
            framebuffer.isSRGBMask = 1
            useFrame(framebuffer, gizmoRenderer) {
                copyColorAndDepth(colorT, depthT, depthM, isSRGB = true)
                GFXState.depthMode.use(renderView.pipeline.defaultStage.depthMode) {
                    GFXState.blendMode.use(BlendMode.DEFAULT) {
                        renderView.drawGizmos1(grid, debug, aabbs)
                    }
                }
            }
            setOutput(1, Texture.texture(framebuffer, 0))
            setOutput(2, if (readsDepth) Texture.depth(framebuffer) else depth)
        }
    }

    companion object {
        val gizmoRenderer by lazy {
            val settings = DeferredSettings(
                if (GFX.supportsDepthTextures) listOf(DeferredLayerType.COLOR, DeferredLayerType.ALPHA) else
                    listOf(DeferredLayerType.COLOR, DeferredLayerType.ALPHA, DeferredLayerType.DEPTH)
            )
            Renderer("gizmo", settings)
        }
    }
}