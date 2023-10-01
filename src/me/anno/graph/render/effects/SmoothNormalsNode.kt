package me.anno.graph.render.effects

import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.actions.ActionNode

class SmoothNormalsNode : ActionNode(
    "Smooth Normals",
    listOf(
        "Float", "Radius",
        "Texture", "Normal",
        "Texture", "Depth",
    ), listOf("Texture", "Normal") // depth could be smoothed, too...
) {

    init {
        setInput(1, 1f) // radius
    }

    override fun executeAction() {
        val radius = getInput(1) as Float
        val normalTex = getInput(2) as? Texture ?: return
        val normal = normalTex.tex
        val depth = ((getInput(3) as? Texture)?.tex as? Texture2D) ?: return
        val target = TargetType.FP16Target2 // depends a bit on quality..., could be RG8 for Android
        val result = FBStack[name, normal.width, normal.height, target, 1, DepthBufferType.NONE]
        if (smoothNormals(normal, normalTex.mapping == "zw", depth, result, radius)) {
            setOutput(1, Texture.texture(result, 0, "xy", DeferredLayerType.NORMAL))
        } else {
            setOutput(2, normalTex)
        }
    }

    companion object {

        private val unpackShader = Shader(
            "smoothNormals-unpack", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList, listOf(
                Variable(GLSLType.S2D, "normalTex"),
                Variable(GLSLType.V1B, "normalZW"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT),
            ), "" +
                    ShaderLib.octNormalPacking +
                    "void main() {\n" +
                    "   ivec2 uvi = ivec2(gl_FragCoord.xy);\n" +
                    "   vec4 normal = texelFetch(normalTex,uvi,0);\n" +
                    "   result = vec4(UnpackNormal(normalZW ? normal.zw : normal.xy),1.0);\n" +
                    "}\n"
        ).apply {
            ignoreNameWarnings("depthTex", "d_camRot")
        }

        private val blurShader = Shader(
            "smoothNormals-blur", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList, listOf(
                Variable(GLSLType.S2D, "normalTex"),
                Variable(GLSLType.S2D, "baseTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V1F, "radius"),
                Variable(GLSLType.V1B, "normalZW"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT),
            ) + DepthTransforms.depthVars, "" +
                    ShaderLib.quatRot + DepthTransforms.rawToDepth +
                    ShaderLib.octNormalPacking +
                    "void main() {\n" +
                    "   ivec2 uvi = ivec2(gl_FragCoord.xy);\n" +
                    "   vec4 base = texelFetch(baseTex,uvi,0);\n" +
                    "   float depth0 = rawToDepth(texelFetch(depthTex,uvi,0).x);\n" +
                    // we skip processing in the sky
                    "   if(depth0 < 1e34) {\n" +
                    "       ivec2 size = textureSize(normalTex,0);\n" +
                    "       int r = int(radius), w0 = int(radius*radius);\n" +
                    "       float invW0 = 1.0 / float(w0);\n" +
                    "       vec4 value = vec4(0.0);\n" +
                    "       int dx0 = -min(r,uvi.x);\n" +
                    "       int dy0 = -min(r,uvi.y);\n" +
                    "       int dx1 = min(r,size.x-1-uvi.x);\n" +
                    "       int dy1 = min(r,size.y-1-uvi.y);\n" +
                    "       for(int dy=dy0;dy<=dy1;dy++){\n" +
                    "           int w1 = w0-dy*dy;\n" +
                    "           for(int dx=dx0;dx<=dx1;dx++){\n" +
                    "               int w2 = w0-dx*dx;\n" +
                    "               if(w2 > 0) {\n" +
                    // multiply weight by delta-depth
                    "                   ivec2 uvi2 = uvi+ivec2(dx,dy);\n" +
                    "                   float depth1 = rawToDepth(texelFetch(depthTex,uvi2,0).x);\n" +
                    "                   float depthWeight = min(depth0,depth1)/max(depth0,depth1);\n" +
                    "                   float weight = dx == 0 && dy == 0 ? 1.0 : float(w2) * invW0 * depthWeight;\n" +
                    "                   vec3 normalI = texelFetch(normalTex,uvi2,0).xyz;\n" +
                    "                   value += vec4(normalI*weight,weight);\n" +
                    "               }\n" +
                    "           }\n" +
                    "       }\n" +
                    "       vec2 normal = PackNormal(value.xyz);\n" +
                    "       if(normalZW) { base.zw = normal; } else { base.xy = normal; }\n" +
                    "   }\n" +
                    "   result = base;\n" +
                    "}\n"
        ).apply {
            ignoreNameWarnings("d_camRot")
        }

        fun smoothNormals(
            normal: ITexture2D,
            normalZW: Boolean,
            depth: ITexture2D,
            dst: IFramebuffer,
            radius: Float
        ): Boolean {
            if (radius <= 0.5f) return false
            // input = output, so copy normal to avoid data races
            val tmp = FBStack["tmpNormal", dst.width, dst.height, TargetType.FP16Target3, 1, DepthBufferType.NONE]
            GFXState.depthMode.use(DepthMode.ALWAYS) {
                GFXState.useFrame(tmp) {
                    val shader = unpackShader
                    shader.use()
                    shader.v1b("normalZW", normalZW)
                    normal.bindTrulyNearest(0)
                    SimpleBuffer.flat01.draw(shader)
                }
                GFXState.useFrame(dst) {
                    val shader = blurShader
                    shader.use()
                    shader.v1f("radius", 0.5f + radius)
                    shader.v1b("normalZW", normalZW)
                    DepthTransforms.bindDepthToPosition(shader)
                    tmp.getTexture0().bindTrulyNearest(shader, "normalTex")
                    normal.bindTrulyNearest(shader, "baseTex")
                    depth.bindTrulyNearest(shader, "depthTex")
                    SimpleBuffer.flat01.draw(shader)
                }
            }
            return true
        }
    }
}