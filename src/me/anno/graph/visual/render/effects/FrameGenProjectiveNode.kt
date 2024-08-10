package me.anno.graph.visual.render.effects

import me.anno.cache.ICacheData
import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredSettings.Companion.singleToVectorR
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
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
import org.joml.Matrix4f
import org.joml.Quaterniond
import org.joml.Vector3d

// todo it would also be nice, if we could render our images over multiple frames to reduce the workload,
//  and display interpolations during that time
//  e.g. SSAO, bloom and gizmos are quite expensive
class FrameGenProjectiveNode : FrameGenOutputNode<FrameGenProjectiveNode.PerViewProjData>(
    "FrameGenProjective", listOf(
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
            GFX.copyNoAlpha((getInput(3) as? Texture)?.texOrNull ?: whiteTexture)
        }
        motion.resize(width, height, TargetType.Float32x3)
        useFrame(motion) {
            val depthSrc0 = getInput(5) as? Texture
            val depthSrc = depthSrc0?.texOrNull ?: blackTexture
            val shader = loadDepthShader
            shader.use()
            depthSrc.bindTrulyNearest(shader, "depthTex")
            shader.v4f("depthMask", depthSrc0?.mask ?: singleToVectorR)
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
        renderInterpolated(view,width,height,0f)
    }

    override fun renderInterpolated(view: PerViewProjData, width: Int, height: Int, fraction: Float) {
        val withGaps = FBStack["frameGen", width, height, TargetType.UInt8x3, 1, DepthBufferType.TEXTURE]
        val withoutGaps = FBStack["frameGen", width, height, TargetType.UInt8x3, 1, DepthBufferType.TEXTURE]
        useFrame(withGaps) {
            GFXState.depthMode.use(DepthMode.CLOSER) {
                withGaps.clearColor(0, depth = true)
                val shader = predictiveShader
                bind(shader, view, width, height)
                gridMesh.proceduralLength = width * height
                gridMesh.drawMode = DrawMode.POINTS
                gridMesh.draw(null, shader, 0)
            }
        }
        useFrame(withoutGaps) {
            val shader = fillInGapsShader
            shader.use()
            withGaps.getTexture0().bindTrulyNearest(shader, "colorTex")
            withGaps.depthTexture!!.bindTrulyNearest(shader, "depthTex")
            view.color.bindTrulyNearest(shader, "backupTex")
            flat01.draw(shader)
        }
        showOutput(withoutGaps.getTexture0())
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
        val gridMesh = Mesh()
        val loadDepthShader = Shader(
            "loadDepth", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "depthMask"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT),
            ) + DepthTransforms.depthVars, DepthTransforms.rawToDepth +
                    "void main() {\n" +
                    "   float rawDepth = dot(texture(depthTex, uv),depthMask);\n" +
                    "   result = vec4(rawToDepth(rawDepth), 0.0, 0.0, 1.0);\n" +
                    "}\n"
        )
        val predictiveShader = Shader(
            "predictive", listOf(
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.M4x4, "cameraMatrixI"),
                Variable(GLSLType.V3F, "cameraPositionI"),
                Variable(GLSLType.V3F, "cameraPosition0"),
                Variable(GLSLType.V1F, "worldScaleI"),
                Variable(GLSLType.V1F, "worldScale0"),
                Variable(GLSLType.V2I, "resolution"),
            ) + DepthTransforms.depthVars, "" +
                    DepthTransforms.rawToDepth +
                    DepthTransforms.depthToPosition +
                    "void main(){\n" +
                    "   int x = gl_InstanceID % resolution.x;\n" +
                    "   int y = gl_InstanceID / resolution.x;\n" +
                    "   vec2 uv = vec2((vec2(x,y))/vec2(resolution));\n" +
                    "   color = texture(colorTex,uv,0).xyz;\n" +
                    "   float depth = clamp(texture(depthTex,uv,0).x, 1e-3, 1e15);\n" + // must be clamped to avoid Inf/NaN
                    "   vec3 pos = depthToPosition(uv,depth);\n" +
                    // could be simplified, is just 1x scale, 1x translation
                    "   pos = (pos/worldScale0)+cameraPosition0;\n" +
                    "   pos = (pos-cameraPositionI)*worldScaleI;\n" +
                    "   gl_Position = matMul(cameraMatrixI,vec4(pos,1.0));\n" +
                    "}",
            listOf(Variable(GLSLType.V3F, "color")),
            listOf(Variable(GLSLType.V4F, "result", VariableMode.OUT)), "" +
                    "void main() {\n" +
                    "   result = vec4(color,1.0);\n" +
                    "}\n"
        )
        val fillInGapsShader = Shader(
            "fillInGaps", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.S2D, "backupTex"),
                Variable(GLSLType.V1B, "reverseDepth"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT),
            ), "" +
                    "void main() {\n" +
                    "   vec3 color = texture(colorTex,uv).xyz;\n" +
                    "   float depth = texture(depthTex, uv).x;\n" +
                    "   for(int r=1;r<5;r++){\n" +
                    "                          depth = textureOffset(depthTex, uv, ivec2(+r,+r)).x;\n" +
                    "       if(depth != 0.0) { color = textureOffset(colorTex, uv, ivec2(+r,+r)).xyz; break; }\n" +
                    "                          depth = textureOffset(depthTex, uv, ivec2(-r,+r)).x;\n" +
                    "       if(depth != 0.0) { color = textureOffset(colorTex, uv, ivec2(-r,+r)).xyz; break; }\n" +
                    "                          depth = textureOffset(depthTex, uv, ivec2(-r,-r)).x;\n" +
                    "       if(depth != 0.0) { color = textureOffset(colorTex, uv, ivec2(-r,-r)).xyz; break; }\n" +
                    "                          depth = textureOffset(depthTex, uv, ivec2(+r,-r)).x;\n" +
                    "       if(depth != 0.0) { color = textureOffset(colorTex, uv, ivec2(+r,-r)).xyz; break; }\n" +
                    "   }\n" +
                    "   if(depth == 0.0) color = texture(backupTex,uv).xyz;\n" +
                    "   result = vec4(color, 1.0);\n" +
                    "   gl_FragDepth = depth;\n" +
                    "}\n"
        )
    }
}