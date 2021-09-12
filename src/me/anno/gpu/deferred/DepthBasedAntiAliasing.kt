package me.anno.gpu.deferred

import me.anno.gpu.GFX.flat01
import me.anno.gpu.ShaderLib.simplestVertexShader
import me.anno.gpu.ShaderLib.uvList
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.ITexture2D
import me.anno.utils.image.ImageWriter
import me.anno.utils.image.ImageWriter.MSAAx8
import me.anno.utils.maths.Maths.clamp
import me.anno.utils.maths.Maths.max
import me.anno.utils.maths.Maths.min
import me.anno.utils.maths.Maths.mix
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.abs


/**
 * idea: use msaa on the depth in a separate pass,
 * and use its blurred information to find the correct amount of blurring,
 * which then can be applied to the colors to at least fix multi-pixel-wide edges
 *
 * in my simple test here, it works perfectly, but for curved planes, it didn't
 * -> use edge detection by walking along the edge, and find the interpolation values
 * */
object DepthBasedAntiAliasing {

    // only works for flat sections -> we need edge detection
    /*val shader0 = lazy {
        Shader(
            "Depth-Based MSAA", null,
            simplestVertexShader, uvList,
            "out vec4 fragColor;\n" +
                    "uniform sampler2D color,depth0,depthMSAA;\n" +
                    "uniform float strength;\n" +
                    "void main(){" +
                    "   ivec2 p = ivec2(gl_FragCoord.xy);\n" +
                    "   float self = texelFetch(depth0,p,0).r;\n" +
                    "   float middle = texelFetch(depthMSAA,p,0).r;\n" +
                    "   float mn=middle,mx=middle,v;\n" +
                    (Array(9) {
                        val dx = (it % 3) - 1
                        val dy = (it / 3) - 1
                        if (dx == 0 && dy == 0) "" else
                            "v=texelFetch(depthMSAA,p+ivec2($dx,$dy),0).r;mn=min(mn,v);mx=max(mx,v);\n"
                    }.joinToString("")) +
                    "   float blur = clamp(strength*abs(self-middle)/(1e-38+(mx-mn)),0,1);\n" +
                    "   if(blur < ${1f / 255f}){\n" +
                    "       fragColor = texelFetch(color,p,0);\n" +
                    "   } else {\n" +
                    "       vec4 center = texelFetch(color,p,0);\n" +
                    "       vec4 avg = ${1f / 9f} * (${
                        Array(9) {
                            val dx = (it % 3) - 1
                            val dy = (it / 3) - 1
                            if (dx == 0 && dy == 0) "center" else
                                "texelFetch(color,p+ivec2($dx,$dy),0)"
                        }.joinToString("+")
                    });\n" +
                    "       fragColor = mix(center,avg,blur);\n" +
                    "   }\n" +
                    "   fragColor = vec4(blur,blur,blur,1);\n" +
                    "}"
        ).apply { setTextureIndices(listOf("color", "depth0", "depthMSAA")) }
    }*/

    // works on the sobel filter
    // -> edges are 2 wide instead of 1 one wide
    // todo make this only 1 wide somehow...
    val shader = lazy {
        Shader(
            "Depth-Based MSAA", null,
            simplestVertexShader, uvList,
            "out vec4 fragColor;\n" +
                    // use color instead of depth?
                    // depth is great for false-positives, but bad for false-negatives (it blurs too few pixels)
                    // color does not guarantee the 2 wide transitions -> cannot really be used
                    // todo combine color with depth???
                    "uniform sampler2D color,depth0,depthMSAA;\n" +
                    "uniform float strength;\n" +
                    "uniform float threshold;\n" +
                    "bool needsBlur(ivec2 p){\n" +
                    "   float d0 = texelFetch(depth0,p,0).r;\n" +
                    "   float d1 = texelFetch(depth0,p-ivec2(1,0),0).r;\n" +
                    "   float d2 = texelFetch(depth0,p-ivec2(0,1),0).r;\n" +
                    "   float d3 = texelFetch(depth0,p+ivec2(1,0),0).r;\n" +
                    "   float d4 = texelFetch(depth0,p+ivec2(0,1),0).r;\n" +
                    "   float sobel = 4.0-(d1+d2+d3+d4)/d0;\n" +
                    "   return abs(sobel) > threshold;\n" +
                    "}\n" +
                    "void main(){\n" +
                    "   ivec2 p = ivec2(gl_FragCoord.xy);\n" +
                    "   float d0 = texelFetch(depth0,p,0).r;\n" +
                    "   float d1 = texelFetch(depth0,p-ivec2(1,0),0).r;\n" +
                    "   float d2 = texelFetch(depth0,p-ivec2(0,1),0).r;\n" +
                    "   float d3 = texelFetch(depth0,p+ivec2(1,0),0).r;\n" +
                    "   float d4 = texelFetch(depth0,p+ivec2(0,1),0).r;\n" +
                    "   float sobel = 4.0-(d1+d2+d3+d4)/d0;\n" +
                    "   if(abs(sobel) > threshold){\n" +
                    "#define maxSteps 15\n" +
                    "       float dx = d3 - d1;\n" +
                    "       float dy = d4 - d2;\n" +
                    "       bool dirX = abs(dx) > abs(dy);\n" +
                    "       ivec2 di = dirX ? ivec2(1,0) : ivec2(0,1);\n" +
                    "       int pos,neg;\n" +
                    "       for(pos=1;pos<=maxSteps;pos++){\n" +
                    "           if(!needsBlur(p+(dirX?ivec2(0,pos):ivec2(pos,0)))) break;\n" +
                    "       }\n" +
                    "       for(neg=1;neg<=maxSteps;neg++){\n" +
                    "           if(!needsBlur(p-(dirX?ivec2(0,neg):ivec2(neg,0)))) break;\n" +
                    "       }\n" +
                    "       pos--;\n" +
                    "       neg--;\n" +
                    "       float fraction = (float(pos)+0.5)/float(1+pos+neg);\n" +
                    "       int other = (dirX?abs(d1-d0)>abs(d3-d0) : abs(d2-d0)>abs(d4-d0)) ? -1:+1;\n" +
                    "       float blur = min(1-fraction,fraction);\n" +
                    "       vec4 mixColor = texelFetch(color,p+(dirX?ivec2(other,0):ivec2(0,other)),0);\n" +
                    // "       fragColor = vec4(vec3(fraction),1);\n" +
                    // "       fragColor = vec4(vec3(blur),1);\n" +
                    "       fragColor = mix(texelFetch(color,p,0), mixColor, blur);\n" +
                    "   } else {\n" +
                    "       fragColor = texelFetch(color,p,0);\n" +
                    "   }\n" +
                    // "   if(true) fragColor = vec4(vec2(abs(sobel) > THRESHOLD ? 1 : 0),texelFetch(depth0,p,0).r,1);\n" +
                    "}"
        ).apply { setTextureIndices(listOf("color", "depth0")) }
    }

    fun render(color: ITexture2D, simpleDepth: ITexture2D, threshold: Float = 1e-5f) {
        val shader = shader.value
        shader.use()
        shader.v1("threshold", threshold)
        simpleDepth.bindTrulyNearest(1)
        color.bindTrulyNearest(0)
        flat01.draw(shader)
    }

    @JvmStatic
    fun main(args: Array<String>) {

        val size = 64
        val sm1 = size - 1

        val normal = Vector2f(1f, 10f).normalize()
        val position = Vector2f(size / 2f)
        val plane = Vector3f(normal, -normal.dot(position))

        fun render(dst: FloatArray, v0: Float, v1: Float, msaa: FloatArray) {
            val samples = msaa.size / 2
            val fSamples = 1f / samples
            for (y in 0 until size) {
                for (x in 0 until size) {
                    var sum = 0f
                    for (sample in 0 until samples) {
                        val msaaIndex = sample shl 1
                        val xf = x + msaa[msaaIndex]
                        val yf = y + msaa[msaaIndex + 1]
                        sum += if (plane.dot(xf, yf, 1f) >= 0f) v1 else v0 // inside / outside plane
                    }
                    dst[x * size + y] = sum * fSamples // resolve
                }
            }
        }

        val noMSAA = floatArrayOf(0.5f, 0.5f)

        val msaa = MSAAx8

        fun sample(f: FloatArray, x: Int, y: Int): Float {
            return f[clamp(x, 0, sm1) + clamp(y, 0, sm1) * size]
        }

        val msaaDepth = FloatArray(64 * 64) // already with resolve
        val defaultDepth = FloatArray(64 * 64)

        // render a triangle onto msaa and default depth
        render(msaaDepth, 0f, 1f, msaa)
        render(defaultDepth, 0f, 1f, noMSAA)

        val newDepth = FloatArray(64 * 64)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val self = sample(defaultDepth, x, y)
                val middle = sample(msaaDepth, x, y)
                var max = middle
                var min = middle
                var sum = 0f
                for (i in 0 until 9) {
                    val dx = (i % 3) - 1
                    val dy = (i / 3) - 1
                    val v = sample(msaaDepth, x + dx, y + dy)
                    max = max(max, v)
                    min = min(min, v)
                    sum += v
                }
                // 2f is needed, because the theoretical maximum distance is 0.5f to the other,
                // and the maximum interpolation that we need at this point is 1f
                val blur = clamp(2f * abs(self - middle) / max(1e-38f, max - min), 0f, 1f)
                // avg or (max-min)/2 doesn't make a quality difference
                // there is a slight pixel difference
                newDepth[x + y * size] = mix(self, sum / 9f, blur)
            }
        }

        ImageWriter.writeImageFloat(size, size, "d-def.png", true, defaultDepth)
        ImageWriter.writeImageFloat(size, size, "d-msaa.png", true, msaaDepth)
        ImageWriter.writeImageFloat(size, size, "msaa.png", true, newDepth)

    }
}