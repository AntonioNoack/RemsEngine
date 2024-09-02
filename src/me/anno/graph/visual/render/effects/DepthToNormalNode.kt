package me.anno.graph.visual.render.effects

import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.DepthTransforms.bindDepthUniforms
import me.anno.gpu.shader.DepthTransforms.depthToPosition
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.octNormalPacking
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.mask
import me.anno.graph.visual.render.Texture.Companion.texOrNull

/**
 * reconstructs normal from depth value;
 * disadvantage: doesn't support normal maps
 * */
class DepthToNormalNode : TimedRenderingNode(
    "Depth To Normal",
    listOf("Texture", "Depth", "Int", "Precision"),
    listOf("Texture", "Normal")
) {

    override fun executeAction() {
        val depth = getInput(1) as? Texture ?: return
        val target = when (getInput(2) as Int) {
            1 -> TargetType.UInt16x2
            2 -> TargetType.UInt32x2
            else -> TargetType.UInt8x2
        }
        val depthTex = depth.texOrNull
        if (depthTex != null) {
            timeRendering(name, timer) {
                val result = FBStack[name, depthTex.width, depthTex.height, target, 1, DepthBufferType.NONE]
                GFXState.useFrame(result) {
                    val shader = shader
                    shader.use()
                    shader.v4f("depthMask", depth.mask)
                    depthTex.bindTrulyNearest(0)
                    bindDepthUniforms(shader)
                    flat01.draw(shader)
                }
                setOutput(1, Texture.texture(result, 0, "xy", DeferredLayerType.NORMAL))
            }
        } else {
            setOutput(1, null)
        }
    }

    companion object {
        val shader = Shader(
            "depthToNormal", emptyList(), coordsUVVertexShader, uvList,
            listOf(
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "depthMask"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ) + depthVars, "" +
                    quatRot +
                    rawToDepth +
                    depthToPosition +
                    octNormalPacking +
                    "float getDepth(ivec2 uv){\n" +
                    "   ivec2 uvi = clamp(uv, ivec2(0,0), textureSize(depthTex,0)-1);\n" +
                    "   float rawDepth = dot(depthMask,texelFetch(depthTex,uvi,0));\n" +
                    "   return rawToDepth(rawDepth);\n" +
                    "}\n" +
                    "vec3 getPosition(vec2 uv, float depth){\n" +
                    "   return depthToPosition(uv, max(depth, 1e-10));\n" +
                    "}\n" +
                    "void main(){\n" +
                    "   vec2 duv = vec2(dFdx(uv.x),dFdy(uv.y));\n" +
                    "   ivec2 uvi = ivec2(gl_FragCoord.xy);\n" +
                    "   float d00 = getDepth(uvi);\n" +
                    "   float dpx = getDepth(uvi+ivec2(1,0));\n" +
                    "   float dmx = getDepth(uvi-ivec2(1,0));\n" +
                    "   float dpy = getDepth(uvi+ivec2(0,1));\n" +
                    "   float dmy = getDepth(uvi-ivec2(0,1));\n" +
                    "   vec3 normal;\n" +
                    "   if(d00 > 1e10){\n" +
                    // sky doesn't have depth information / is at infinity -> different algorithm for it
                    "       normal = -rawCameraDirection(uv);\n" +
                    "   } else {\n" +
                    // prefer px/mx with closer depth value
                    "       vec3 pos0 = depthToPosition(uv, max(d00, 1e-10));\n" +
                    "       vec3 dx = abs(dpx-d00) < abs(dmx-d00) ?\n" +
                    "                   getPosition(uv+vec2(duv.x,0.0),dpx) - pos0 :\n" +
                    "            pos0 - getPosition(uv-vec2(duv.x,0.0),dmx);\n" +
                    "       vec3 dy = abs(dpy-d00) < abs(dmy-d00) ?\n" +
                    "                   getPosition(uv+vec2(0.0,duv.y),dpy) - pos0 :\n" +
                    "            pos0 - getPosition(uv-vec2(0.0,duv.y),dmy);\n" +
                    "       normal = cross(dx,dy);\n" +
                    "   }\n" +
                    "   result = vec4(PackNormal(normal),0.0,1.0);\n" +
                    "}\n"
        )
    }
}