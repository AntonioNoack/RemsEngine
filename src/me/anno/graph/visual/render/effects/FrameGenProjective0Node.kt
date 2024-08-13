package me.anno.graph.visual.render.effects

import me.anno.cache.ICacheData
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFX
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
import me.anno.graph.visual.render.Texture.Companion.mask
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import org.joml.Matrix4f
import org.joml.Quaterniond
import org.joml.Vector3d

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
        val cameraPosition = Vector3d()
        var worldScale = 1.0
        val cameraDirection = Vector3d()
        val cameraRotation = Quaterniond()
        val cameraMatrixInv = Matrix4f()

        override fun destroy() {
            color.destroy()
            depth.destroy()
        }
    }

    override fun createPerViewData(): PerViewProjData {
        return PerViewProjData()
    }

    private fun fill(width: Int, height: Int, data0: Texture2D, motion: Texture2D) {
        data0.resize(width, height, TargetType.UInt8x3)
        useFrame(data0) {
            GFX.copyNoAlpha((getInput(3) as? Texture).texOrNull ?: whiteTexture)
        }
        motion.resize(width, height, TargetType.Float32x3)
        useFrame(motion) {
            val depthSrc0 = getInput(5) as? Texture
            val depthSrc = depthSrc0.texOrNull ?: blackTexture
            val shader = loadDepthShader
            shader.use()
            depthSrc.bindTrulyNearest(shader, "depthTex")
            shader.v4f("depthMask", depthSrc0.mask)
            DepthTransforms.bindDepthUniforms(shader)
            flat01.draw(shader)
        }
    }

    override fun canInterpolate(view: PerViewProjData): Boolean {
        return view.color.isCreated() && view.depth.isCreated()
    }

    override fun renderOriginal(view: PerViewProjData, width: Int, height: Int) {
        // copy input onto data0
        view.worldScale = RenderState.worldScale
        view.cameraPosition.set(RenderState.cameraPosition)
        view.cameraRotation.set(RenderState.cameraRotation)
        view.cameraDirection.set(RenderState.cameraDirection)
        view.cameraMatrixInv.set(RenderState.cameraMatrixInv)
        fill(width, height, view.color, view.depth)
        // todo setting output has stuttering... why???
        // showOutput(view.color)
        renderInterpolated(view, width, height, 0f)
    }

    fun bind(shader: Shader, view: PerViewProjData, width: Int, height: Int) {
        shader.use()
        // invalidate filtering
        view.color.bindTrulyNearest(shader, "colorTex")
        view.depth.bindTrulyNearest(shader, "depthTex")
        shader.m4x4("cameraMatrixI", RenderState.cameraMatrix)
        shader.v3f("cameraPositionI", RenderState.cameraPosition)
        shader.v3f("cameraPosition0", view.cameraPosition)
        shader.v1f("worldScaleI", RenderState.worldScale)
        shader.v1f("worldScale0", view.worldScale)
        DepthTransforms.bindDepthUniforms(
            shader,
            view.cameraDirection,
            view.cameraRotation,
            view.cameraMatrixInv
        )
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
        val loadDepthShader = Shader(
            "loadDepth", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "depthMask"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT),
            ) + DepthTransforms.depthVars, DepthTransforms.rawToDepth +
                    "void main() {\n" +
                    "   float rawDepth = dot(texture(depthTex, uv),depthMask);\n" +
                    "   result = vec4(rawToDepth(rawDepth), 0.0, 0.0, 1.0);\n" +
                    "}\n"
        ).apply { ignoreNameWarnings("d_camRot,d_uvCenter,cameraMatrixInv") }
    }
}