package me.anno.graph.visual.render.effects.framegen

import me.anno.cache.ICacheData
import me.anno.gpu.Blitting
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.mask1Index
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.utils.structures.lists.LazyList

// todo it would also be nice, if we could render our images over multiple frames to reduce the workload,
//  and display interpolations during that time
//  e.g. SSAO, bloom and gizmos are quite expensive
abstract class FrameGenProjective0Node(name: String) : FrameGenOutputNode<FrameGenProjective0Node.PerViewProjData>(
    name, listOf(
        "Int", "Width",
        "Int", "Height",
        "Texture", "Illuminated",
        "Texture", "Unused",
        "Texture", "Depth",
    ), listOf("Texture", "Illuminated")
) {

    class PerViewProjData : ICacheData {
        val color = Texture2D("frameGenC", 1, 1, 1)
        val depth = Texture2D("frameGenM", 1, 1, 1)
        val cameraState = SavedCameraState()
        override fun destroy() {
            color.destroy()
            depth.destroy()
        }
    }

    override fun createPerViewData(): PerViewProjData {
        return PerViewProjData()
    }

    override fun canInterpolate(view: PerViewProjData): Boolean {
        return view.color.isCreated() && view.depth.isCreated()
    }

    override fun renderOriginal(view: PerViewProjData, width: Int, height: Int) {
        // copy input onto data0
        view.cameraState.save()
        fillColor(view.color, width, height, getInput(3) as? Texture)
        fillDepth(view.depth, width, height, getInput(5) as? Texture)
        // todo setting output has stuttering... why???
        // showOutput(view.color)
        renderInterpolated(view, width, height, 0f)
    }

    fun bind(shader: Shader, view: PerViewProjData, width: Int, height: Int) {
        shader.use()
        // invalidate filtering
        view.color.bindTrulyNearest(shader, "colorTex")
        view.depth.bindTrulyNearest(shader, "depthTex")
        view.cameraState.bind(shader)
        shader.v2i("resolution", width, height)
    }

    override fun destroy() {
        super.destroy()
        for (view in views.values) {
            view.color.destroy()
            view.depth.destroy()
        }
    }

    companion object {

        fun fillColor(data0: Texture2D, width: Int, height: Int, colorSrc0: Texture?) {
            data0.resize(width, height, TargetType.UInt8x3)
            useFrame(data0) {
                Blitting.copyNoAlpha(colorSrc0.texOrNull ?: whiteTexture, true)
            }
        }

        fun fillDepth(depth: Texture2D, width: Int, height: Int, depthSrc0: Texture?) {
            depth.resize(width, height, TargetType.Float32x3)
            useFrame(depth) {
                val depthSrc = depthSrc0.texOrNull ?: blackTexture
                val shader = loadDepthShader[depthSrc0.mask1Index]
                shader.use()
                depthSrc.bindTrulyNearest(shader, "depthTex")
                DepthTransforms.bindDepthUniforms(shader)
                flat01.draw(shader)
            }
        }

        val loadDepthShader = LazyList(4) {
            val depthMask = "xyzw"[it]
            Shader(
                "loadDepth", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
                listOf(
                    Variable(GLSLType.S2D, "depthTex"),
                    Variable(GLSLType.V4F, "depthMask"),
                    Variable(GLSLType.V4F, "result", VariableMode.OUT),
                ) + DepthTransforms.depthVars, DepthTransforms.rawToDepth +
                        "void main() {\n" +
                        "   float rawDepth = texture(depthTex, uv).$depthMask;\n" +
                        "   result = vec4(rawToDepth(rawDepth), 0.0, 0.0, 1.0);\n" +
                        "}\n"
            )
        }
    }
}