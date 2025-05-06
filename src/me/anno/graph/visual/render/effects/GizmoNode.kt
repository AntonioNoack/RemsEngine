package me.anno.graph.visual.render.effects

import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.FlatShaders.getColor0SS
import me.anno.gpu.shader.FlatShaders.getColor1MS
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.depthTexture
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.mask1Index
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.graph.visual.render.scene.RenderViewNode
import me.anno.maths.Maths.clamp
import me.anno.utils.structures.lists.LazyList
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt

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
            name, width, height, TargetType.UInt8x4, samples,
            if (readsDepth) DepthBufferType.TEXTURE else DepthBufferType.INTERNAL
        ]

        timeRendering(name, timer) {
            framebuffer.isSRGBMask = 1
            useFrame(framebuffer, gizmoRenderer) {
                copyColorAndDepth(colorT, depthT, depthM)
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

        val copyShader = LazyList(16) {
            val colorMS = it.hasFlag(4)
            val depthMS = it.hasFlag(8)
            val depthMask = "xyzw"[it]
            BaseShader(
                "copyColorDepth", emptyList(),
                coordsUVVertexShader, uvList, listOf(
                    Variable(if (colorMS) GLSLType.S2DMS else GLSLType.S2D, "colorTex"),
                    Variable(if (depthMS) GLSLType.S2DMS else GLSLType.S2D, "depthTex"),
                    Variable(GLSLType.V1I, "colorSamples"),
                    Variable(GLSLType.V1I, "depthSamples"),
                    Variable(GLSLType.V3F, "finalColor", VariableMode.OUT)
                ), "" +
                        (if (colorMS || depthMS) getColor1MS else "") +
                        (if (!colorMS || !depthMS) getColor0SS else "") +
                        "void main(){\n" +
                        "   finalColor = getColor${colorMS.toInt()}(colorTex,colorSamples,uv).rgb;\n" +
                        "   gl_FragDepth = getColor${depthMS.toInt()}(depthTex,depthSamples,uv).$depthMask;\n" +
                        "}\n"
            )
        }

        val gizmoRenderer by lazy {
            val settings = DeferredSettings(
                if (GFX.supportsDepthTextures) listOf(DeferredLayerType.COLOR, DeferredLayerType.ALPHA) else
                    listOf(DeferredLayerType.COLOR, DeferredLayerType.ALPHA, DeferredLayerType.DEPTH)
            )
            Renderer("gizmo", settings)
        }

        fun copyColorAndDepth(color0: ITexture2D?, depth0: ITexture2D?, depthM: Int) {
            val color1 = color0 ?: blackTexture
            val depth1 = depth0 ?: depthTexture
            val shaderId = (color1.samples > 1).toInt(4) + (depth1.samples > 1).toInt(8) + depthM
            val shader = copyShader[shaderId].value
            shader.use()
            shader.v1i("colorSamples", color1.samples)
            shader.v1i("depthSamples", depth1.samples)
            color1.bindTrulyNearest(shader, "colorTex")
            depth1.bindTrulyNearest(shader, "depthTex")
            flat01.draw(shader)
        }
    }
}