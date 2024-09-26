package me.anno.graph.visual.render.effects

import me.anno.gpu.Blitting
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.ITexture2D
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

        val colorT = (getInput(7) as? Texture).texOrNull
        val depth = getInput(8) as? Texture
        val depthT = depth.texOrNull
        val depthM = depth.mask1Index

        // else we can't cast to Framebuffer
        val readsDepth = isOutputUsed(outputs[2])
        val framebuffer = FBStack[
            name, width, height, 4, false, samples,
            if (readsDepth) DepthBufferType.TEXTURE else DepthBufferType.INTERNAL
        ]

        timeRendering(name, timer) {
            framebuffer.isSRGBMask = 1
            useFrame(framebuffer, copyRenderer) {
                copyColorAndDepth(colorT, depthT, depthM, framebuffer)
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
        val settings by lazy {
            DeferredSettings(
                if (GFX.supportsDepthTextures) listOf(DeferredLayerType.COLOR, DeferredLayerType.ALPHA) else
                    listOf(DeferredLayerType.COLOR, DeferredLayerType.ALPHA, DeferredLayerType.DEPTH)
            )
        }

        private fun copyColorAndDepth(
            colorT: ITexture2D?, depthT: ITexture2D?, depthM: Int,
            framebuffer: IFramebuffer
        ) {
            useFrame(framebuffer) {
                renderPurely {
                    Blitting.copyColorAndDepth(
                        colorT ?: blackTexture,
                        depthT ?: depthTexture,
                        depthM, true
                    )
                }
            }
        }
    }
}