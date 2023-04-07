package me.anno.engine.ui.render

import me.anno.gpu.GFXState.renderPurely2
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ReverseDepth.bindDepthToPosition
import me.anno.gpu.shader.ReverseDepth.depthToPositionList
import me.anno.gpu.shader.ReverseDepth.rawToDepth
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.octNormalPacking
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.maths.Maths.hasFlag
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.types.Booleans.toInt

object SmoothedNormals {

    private val unpackShader = Array(2) {
        val ext = if (it > 0) "zw" else "xy"
        Shader(
            "smoothNormals-unpack", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList, listOf(
                Variable(GLSLType.S2D, "normalTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT),
            ), "" +
                    octNormalPacking +
                    "void main() {\n" +
                    "   ivec2 uvi = ivec2(gl_FragCoord.xy);\n" +
                    "   result = vec4(UnpackNormal(texelFetch(normalTex,uvi,0).$ext),1.0);\n" +
                    "}\n"
        )
    }
    val blurShader = LazyMap<Int, Shader> {
        val ext = if (it.hasFlag(1)) "zw" else "xy"
        Shader(
            "smoothNormals-blur", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList, listOf(
                Variable(GLSLType.S2D, "normalTex"),
                Variable(GLSLType.S2D, "baseTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V1F, "radius"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT).apply { ignored = true },
            ) + depthToPositionList, "" +
                    "layout(location=${it shr 1}) out vec4 result;\n" +
                    quatRot + rawToDepth +
                    octNormalPacking +
                    "void main() {\n" +
                    "   ivec2 uvi = ivec2(gl_FragCoord.xy);\n" +
                    "   vec4 base = texelFetch(baseTex,uvi,0);\n" +
                    "   float depth0 = rawToDepth(texelFetch(depthTex,uvi,0).x);\n" +
                    // we skip processing in the sky
                    "   if(depth0 < 1e38) {\n" +
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
                    "       base.$ext = PackNormal(value.xyz);\n" +
                    "   }\n" +
                    "   result = base;\n" +
                    "}\n"
        )
    }

    fun smoothNormals(frame: IFramebuffer, settings: DeferredSettingsV2, radius: Float = 1f): Boolean {

        if (radius <= 0.5f) return false
        val layer = settings.findLayer(DeferredLayerType.NORMAL) ?: return false
        val normal = settings.findTexture(frame, layer)
        val depth = frame.depthTexture ?: return false

        // input = output, so copy normal to avoid data races
        val tmp = FBStack["tmpNormal", frame.w, frame.h, TargetType.FP16Target3, 1, false]
        val flag = settings.zw(DeferredLayerType.NORMAL).toInt()
        renderPurely2 {
            useFrame(tmp) {
                val shader = unpackShader[flag]
                shader.use()
                normal.bindTrulyNearest(0)
                flat01.draw(shader)
            }
            useFrame(frame) {
                val shaderId = settings.findLayer(DeferredLayerType.NORMAL)!!.texIndex.shl(1) + flag
                val shader = blurShader[shaderId]
                shader.use()
                shader.v1f("radius", 0.5f + radius)
                bindDepthToPosition(shader)
                tmp.getTexture0().bindTrulyNearest(shader, "normalTex")
                normal.bindTrulyNearest(shader, "baseTex")
                depth.bindTrulyNearest(shader, "depthTex")
                flat01.draw(shader)
            }
        }

        return true

    }

}