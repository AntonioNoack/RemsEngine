package me.anno.graph.render.effects

import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.Texture2D
import me.anno.graph.render.Texture
import me.anno.graph.render.scene.RenderSceneNode0
import org.lwjgl.opengl.GL30C.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL30C.GL_DEPTH_BUFFER_BIT

class GizmoNode : RenderSceneNode0(
    "Gizmos",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Bool", "Grid",
        "Bool", "AABBs",
        "Bool", "Debug Shapes",
        "Texture", "Illuminated",
        "Texture", "Depth",
    ), listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 8) // samples
        setInput(4, true) // grid
        setInput(5, false) // aabbs
        setInput(6, true) // debug shapes
        setInput(7, null) // depth
    }

    override fun executeAction() {

        val width = getInput(1) as Int
        val height = getInput(2) as Int
        val samples = getInput(3) as Int
        if (width < 1 || height < 1 || samples < 1) return

        val grid = getInput(4) == true
        val aabbs = getInput(5) == true
        val debug = getInput(6) == true

        val colorT = ((getInput(7) as? Texture)?.tex as? Texture2D)
        val depthT = ((getInput(8) as? Texture)?.tex as? Texture2D)


        val framebuffer = FBStack[name, width, height, 4, false, samples, true]

        GFX.check()

        GFXState.useFrame(width, height, true, framebuffer, copyRenderer) {
            copyColorAndDepth(colorT, depthT, framebuffer)
            GFXState.depthMode.use(renderView.pipeline.defaultStage.depthMode) {
                GFXState.blendMode.use(BlendMode.DEFAULT) {
                    renderView.drawGizmos1(grid, debug, aabbs)
                }
            }
        }

        setOutput(1, Texture(framebuffer.getTexture0()))
    }

    companion object {
        fun copyColorAndDepth(colorT: Texture2D?, depthT: Texture2D?, framebuffer: Framebuffer) {
            val color = colorT?.owner
            val depth = depthT?.owner
            if (depth == null && color == null) {
                framebuffer.clearColor(0, true)
            } else if (depth != null && color === depth) {
                color.copyColorTo(
                    framebuffer, color.textures.indexOf(colorT), 0,
                    GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT
                )
            } else {
                if (depth == null) framebuffer.clearDepth()
                else depth.copyTo(framebuffer, GL_DEPTH_BUFFER_BIT)
                if (color == null) framebuffer.clearColor(0, false)
                else color.copyColorTo(
                    framebuffer, color.textures.indexOf(colorT), 0,
                    GL_COLOR_BUFFER_BIT
                )
            }
        }
    }
}