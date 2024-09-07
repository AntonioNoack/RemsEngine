package me.anno.graph.visual.render.effects.framegen

import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

// todo it would also be nice, if we could render our images over multiple frames to reduce the workload,
//  and display interpolations during that time
//  e.g. SSAO, bloom and gizmos are quite expensive
class FrameGenProjectiveXNode : FrameGenProjective0Node("FrameGenProjectiveX") {

    override fun renderInterpolated(view: PerViewProjData, width: Int, height: Int, fraction: Float) {
        val withGaps = FBStack["frameGen", width, height, TargetType.UInt8x3, 1, DepthBufferType.TEXTURE]
        val withoutGaps = FBStack["frameGen", width, height, TargetType.UInt8x3, 1, DepthBufferType.NONE]
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

    companion object {
        val gridMesh = Mesh()
        val predictiveShader = Shader(
            "projectiveX", listOf(
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.M4x4, "cameraMatrixI"),
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
                    "   gl_Position = matMul(cameraMatrixI,vec4(pos,1.0));\n" +
                    "}",
            listOf(Variable(GLSLType.V3F, "color")),
            listOf(Variable(GLSLType.V4F, "result", VariableMode.OUT)), "" +
                    "void main() {\n" +
                    "   result = vec4(color,1.0);\n" +
                    "}\n"
        )
        val fillInGapsShader = Shader(
            "fillInGaps", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
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