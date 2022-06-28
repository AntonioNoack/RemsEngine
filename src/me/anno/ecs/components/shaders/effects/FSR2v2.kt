package me.anno.ecs.components.shaders.effects

import me.anno.gpu.GFX
import me.anno.gpu.OpenGL.renderPurely
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import org.joml.Matrix4f
import java.util.*
import kotlin.math.ceil
import kotlin.math.sqrt

// todo implement the ideas of FSR2, but just in principle and much easier
class FSR2v2 {

    val dataTargetTypes = arrayOf(TargetType.FP16Target4, TargetType.FP16Target4)
    var data0 = Framebuffer("data", 1, 1, dataTargetTypes)
    var data1 = Framebuffer("data", 1, 1, dataTargetTypes)

    var previousDepth = Framebuffer("depth", 1, 1, TargetType.FloatTarget1)

    companion object {

        val updateShader = Shader(
            "reproject", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V2F, "currJitter"),
                Variable(GLSLType.V2F, "prevJitter"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.S2D, "motionTex"),
                Variable(GLSLType.S2D, "prevColorNWeights"),
                Variable(GLSLType.S2D, "prevDepths"),
                Variable(GLSLType.V2F, "renderSizeF"),
                Variable(GLSLType.V2I, "renderSizeI"),
                Variable(GLSLType.V2I, "displaySizeI"),
                Variable(GLSLType.V2F, "displaySizeF"),
                Variable(GLSLType.V1F, "sharpness"),
                Variable(GLSLType.V1F, "maxWeight"),
                Variable(GLSLType.V4F, "colorNWeightResult", VariableMode.OUT),
                Variable(GLSLType.V4F, "depthResult", VariableMode.OUT)
            ), "" +
                    "void main(){\n" +
                    // todo indices are off in render by 0.5/renderSize
                    // iterate over all 4 old neighbors
                    "   vec4 colorNWeight = vec4(0.0);\n" +
                    "   vec4 weightedDepth = vec4(0.0);\n" +
                    "   vec2 motion2 = (currJitter - prevJitter) / renderSizeF;\n" +
                    // display space
                    "   vec2  srcUV0  = uv * renderSizeF - currJitter, uvi01 = floor(srcUV0), duv0 = srcUV0-uvi01;\n" +
                    "   ivec2 srcUV0f = ivec2(uvi01);\n" +
                    "   for(int y=-1;y<=2;y++){\n" +
                    "       for(int x=-1;x<=2;x++){\n" +
                    "           ivec2 uvi2 = srcUV0f + ivec2(x,y);\n" +
                    "           if(any(lessThan(uvi2, ivec2(0))) || any(greaterThanEqual(uvi2,displaySizeI))) continue;\n" +
                    "           vec3 fullMotion = texelFetch(motionTex,uvi2,0).xyz;\n" +
                    "           float depth1 = log2(texelFetch(depthTex,uvi2,0).x - fullMotion.z);\n" +
                    "           vec2 motion = fullMotion.xy * 0.5 + motion2;\n" +
                    "           vec2 uviX = (uv - motion) * displaySizeF - 0.5,  uviX1 = floor(uviX), duvX = uviX-uviX1;\n" +
                    "           ivec2 uvx = ivec2(uviX1);\n" +
                    // iterate over all 4 hit pixels
                    "           for(int y2=0;y2<=1;y2++){\n" +
                    "               for(int x2=0;x2<=1;x2++){\n" +
                    "                   uvi2 = uvx + ivec2(x2,y2);\n" +
                    // accumulate old pixels
                    "                   if(all(greaterThanEqual(uvi2, ivec2(0))) && all(lessThan(uvi2, displaySizeI))){\n" +
                    "                       vec2 fg2 = mix(1.0-duvX, duvX, vec2(x2,y2));\n" +
                    "                       vec4 prevColorNWeight = texelFetch(prevColorNWeights,uvi2,0);\n" +
                    "                       float prevDepth = texelFetch(prevDepths,uvi2,0).x;\n" +
                    "                       float w = fg2.x * fg2.y * \n" + // weight depending on distance to pixel
                    // todo better depth reconstruction; maybe near+far (<single edge>-optimized)
                    "                               1.0;//(1.0+0.1*pow(prevDepth-depth1,2.0));\n" + // depth difference
                    // todo by color difference (?)
                    "                       colorNWeight += prevColorNWeight * w;\n" +
                    "                       weightedDepth.x += prevDepth * w;\n" +
                    // "                       colorNWeightResult = vec4(fract(log2(vec2(depth1,prevDepth))), 0.0, 1.0);\n" +
                    // "                       return;\n" +
                    "                   }\n" +
                    "               }\n" +
                    "           }\n" +
                    "       }\n" +
                    "   }\n" +
                    "   if(colorNWeight.w > 1.0){\n" +
                    "       float factor = 1.0/colorNWeight.w;\n" +
                    "       colorNWeight *= factor;\n" +
                    "       weightedDepth *= factor;\n" +
                    "   };\n" +
                    "   for(int y=0;y<=1;y++){\n" +
                    "       for(int x=0;x<=1;x++){\n" +
                    // accumulate new pixels
                    "           ivec2 srcUV = srcUV0f + ivec2(x,y);\n" +
                    "           if(all(greaterThanEqual(srcUV, ivec2(0))) && all(lessThan(srcUV, renderSizeI))){\n" +
                    "               vec2 fg = mix(duv0, 1.0-duv0, vec2(x,y));\n" +
                    "               float w = 1.0/((1.0+sharpness*dot(fg,fg)) * maxWeight);\n" + // weight depending on distance to pixel
                    "               vec3 color = texelFetch(colorTex,srcUV,0).rgb;\n" +
                    "               float depthI = clamp(log2(texelFetch(depthTex,srcUV,0).r), -128.0, 128.0);\n" +
                    "               colorNWeight += vec4(color * w, w);\n" +
                    "               weightedDepth.x += depthI * w;\n" +
                    "           }\n" +
                    "       }\n" +
                    "   }\n" +
                    "   colorNWeightResult = colorNWeight;//vec4(motion+0.5, 0.0, 1.0);\n" +
                    "   depthResult = weightedDepth;\n" +
                    "}\n"
        ).apply { setTextureIndices("colorTex", "depthTex", "motionTex", "prevColorNWeights", "prevDepths") }

        val displayShader = Shader(
            "display", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.S2D, "colorNWeights"),
                Variable(GLSLType.S2D, "depths"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    "void main(){\n" +
                    "   vec4 colorNWeight = texture(colorNWeights,uv);\n" +
                    "   vec4 depth = texture(depths,uv);\n" +
                    "   result = vec4(colorNWeight.rgb/colorNWeight.w, 1.0);\n" +
                    // "   result = vec4(vec3(fract(depth/colorNWeight.w)), 1.0);\n" +
                    "}\n"
        ).apply { setTextureIndices("colorNWeights", "depths") }
    }

    var jx = 0f
    var jy = 0f
    var pjx = 0f
    var pjy = 0f
    val random = Random(1234L)
    var idx = 0
    val phaseCount get() = ceil(8f * lastScaleX * lastScaleY).toInt()
    var randomness = 1f

    fun halton(index: Int, base: Int): Float {
        var f = 1f
        var result = 0f
        var currIndex = index
        while (currIndex > 0) {
            f /= base
            result += f * (currIndex % base)
            currIndex /= base
        }
        return result
    }

    fun jitter(m: Matrix4f, w: Int, h: Int) {
        pjx = jx
        pjy = jy
        jx = halton(idx + 1, 2) - 0.5f
        jy = halton(idx + 1, 3) - 0.5f
        // jx = random.nextFloat() - 0.5f
        // jy = random.nextFloat() - 0.5f
        jx *= randomness
        jy *= randomness
        m.m20(m.m20() + jx * 2f * lastScaleX / w)
        m.m21(m.m21() + jy * 2f * lastScaleY / h)
        if (++idx > phaseCount) idx = 0
    }

    var lastScaleX = 1f
    var lastScaleY = 1f

    fun calculate(
        color: Texture2D,
        depth: Texture2D,
        motion: Texture2D, // motion in 3d
        pw: Int, ph: Int,
    ) {
        renderPurely {
            val rw = color.w
            val rh = color.h
            lastScaleX = pw.toFloat() / rw
            lastScaleY = ph.toFloat() / rh
            if (previousDepth.pointer <= 0) {
                previousDepth.w = rw
                previousDepth.h = rh
            }
            previousDepth.ensure()
            data1.ensure()
            useFrame(pw, ph, true, data0) {
                val shader = updateShader
                shader.use()
                shader.v2f("prevJitter", pjx, pjy)
                shader.v2f("currJitter", jx, jy)
                shader.v2i("renderSizeI", rw, rh)
                shader.v2f("renderSizeF", rw.toFloat(), rh.toFloat())
                color.bindTrulyNearest(0)
                depth.bindTrulyNearest(1)
                motion.bindTrulyNearest(2)
                data1.bindTextures(3, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP) // 3,4
                shader.v2i("displaySizeI", data1.w, data1.h)
                shader.v2f("displaySizeF", data1.w.toFloat(), data1.h.toFloat())
                shader.v1f("sharpness", 3f * lastScaleX * lastScaleY) // a guess
                shader.v1f("maxWeight", sqrt(lastScaleX * lastScaleY))
                flat01.draw(shader)
            }
            /*useFrame(depth.w, depth.h, true, previousDepth) {
                GFX.copyNoAlpha(depth)
            }*/
            // render result
            val shader = displayShader
            shader.use()
            data0.bindTextures(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
            flat01.draw(shader)
        }
        val tmp = data0
        data0 = data1
        data1 = tmp
    }

}