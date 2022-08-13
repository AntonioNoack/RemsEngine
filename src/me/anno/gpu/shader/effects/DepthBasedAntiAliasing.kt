package me.anno.gpu.shader.effects

import me.anno.gpu.GFX.flat01
import me.anno.gpu.monitor.SubpixelLayout
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.simplestVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.texture.ITexture2D
import me.anno.input.Input.isControlDown
import me.anno.input.Input.isShiftDown

/**
 * idea: use msaa on the depth in a separate pass,
 * and use its blurred information to find the correct amount of blurring,
 * which then can be applied to the colors to at least fix multi-pixel-wide edges
 *
 * in my simple test here, it works perfectly, but for curved planes, it didn't
 * -> use edge detection by walking along the edge, and find the interpolation values
 * */
object DepthBasedAntiAliasing {

    // not ideal, but still ok most times
    // todo curves are still an issue
    // if we are lucky, FSR 2.0 tells us how to implement this without bugs :)
    // only issue: axis aligned boxes...

    val shader = lazy {
        Shader(
            "Depth-Based FXAA",
            simplestVertexShader, uvList,
            "out vec4 fragColor;\n" +
                    // use color instead of depth?
                    // depth is great for false-positives, but bad for false-negatives (it blurs too few pixels)
                    // color does not guarantee the 2 wide transitions -> cannot really be used
                    "uniform sampler2D color,depth0;\n" +
                    "uniform bool showEdges,disableEffect;\n" +
                    "uniform float threshold;\n" +
                    "uniform vec2 rbOffset;\n" +
                    "bool needsBlur(ivec2 p){\n" +
                    "   float d0 = max(texelFetch(depth0,p,0).r,1e-38);\n" +
                    "   float d1 = texelFetch(depth0,p-ivec2(1,0),0).r;\n" +
                    "   float d2 = texelFetch(depth0,p-ivec2(0,1),0).r;\n" +
                    "   float d3 = texelFetch(depth0,p+ivec2(1,0),0).r;\n" +
                    "   float d4 = texelFetch(depth0,p+ivec2(0,1),0).r;\n" +
                    "   float sobel = 4.0-(d1+d2+d3+d4)/d0;\n" +
                    "   return abs(sobel) > threshold;\n" +
                    "}\n" +
                    "bool needsBlurX(ivec2 p){\n" +
                    "   float d0 = max(texelFetch(depth0,p,0).r,1e-38);\n" +
                    "   float d1 = texelFetch(depth0,p-ivec2(1,0),0).r;\n" +
                    "   float d2 = texelFetch(depth0,p+ivec2(1,0),0).r;\n" +
                    "   float sobel = 2.0-(d1+d2)/d0;\n" +
                    "   return abs(sobel) > threshold;\n" +
                    "}\n" +
                    "bool needsBlurY(ivec2 p){\n" +
                    "   float d0 = max(texelFetch(depth0,p,0).r,1e-38);\n" +
                    "   float d1 = texelFetch(depth0,p-ivec2(0,1),0).r;\n" +
                    "   float d2 = texelFetch(depth0,p+ivec2(0,1),0).r;\n" +
                    "   float sobel = 2.0-(d1+d2)/d0;\n" +
                    "   return abs(sobel) > threshold;\n" +
                    "}\n" +
                    "float smoothstep1(float x){\n" +
                    "   return pow(x,2.0)*(3.0-2.0*x);\n" +
                    "}\n" +
                    "float max2(vec2 m){ return max(m.x,m.y); }\n" +
                    "float min2(vec2 m){ return min(m.x,m.y); }\n" +
                    "void main(){\n" +
                    "   ivec2 p = ivec2(gl_FragCoord.xy);\n" +
                    "   ivec2 ts = textureSize(color, 0);\n" +
                    "   if(disableEffect || min2(p) <= 0 || max2(p-ts) >= -1){\n" +
                    "       fragColor = texelFetch(color, p, 0);\n" +
                    "       return;\n" +
                    "   }\n" +
                    "   if(showEdges){\n" +
                    "       fragColor = vec4(\n" +
                    "           needsBlur(p)  ? 1.0 : 0.0,\n" +
                    "           needsBlurX(p) ? 1.0 : 0.0,\n" +
                    "           needsBlurY(p) ? 1.0 : 0.0, 1.0);\n" +
                    "       return;\n" +
                    "   }\n" +
                    "   if(needsBlur(p)){\n" +
                    "#define maxSteps 15\n" +
                    "       float d0 = texelFetch(depth0,p,0).r;\n" +
                    "       float d1 = texelFetch(depth0,p-ivec2(1,0),0).r;\n" +
                    "       float d2 = texelFetch(depth0,p-ivec2(0,1),0).r;\n" +
                    "       float d3 = texelFetch(depth0,p+ivec2(1,0),0).r;\n" +
                    "       float d4 = texelFetch(depth0,p+ivec2(0,1),0).r;\n" +
                    "       float sobel = 4.0 - (d1+d2+d3+d4)/d0;\n" +
                    "       float dx = abs(d3-d1);\n" +
                    "       float dy = abs(d4-d2);\n" +
                    "       bool dirX = dx > dy;\n" + // dx > dy
                    "       if (min(dx, dy) * 1.05 > max(dx, dy)) {\n" +
                    // small corner: go to one of the edges, x/y doesn't matter
                    // not perfect, but pretty good
                    "           int ix = abs(d1-d0) > abs(d3-d0) ? +1 : -1;\n" +
                    "           float d02,d12,d22,d32,d42,dx2,dy2;\n" +
                    "           d02 = texelFetch(depth0,p+ivec2(ix,   0),0).r;\n" +
                    "           d12 = texelFetch(depth0,p+ivec2(ix-1, 0),0).r;\n" +
                    "           d22 = texelFetch(depth0,p+ivec2(ix,  -1),0).r;\n" +
                    "           d32 = texelFetch(depth0,p+ivec2(ix+1, 0),0).r;\n" +
                    "           d42 = texelFetch(depth0,p+ivec2(ix,  +1),0).r;\n" +
                    "           dx2 = abs(d32-d12);\n" +
                    "           dy2 = abs(d42-d22);\n" +
                    "           dirX = dx2 >= dy2;\n" +
                    "       }\n" +
                    "       int pos,neg;\n" +
                    "       ivec2 p2=p,dp=dirX?ivec2(0,1):ivec2(1,0);\n" +
                    "       for(pos=1;pos<=maxSteps;pos++){\n" +
                    "           p2+=dp;\n" +
                    "           if(!(dirX?needsBlurX(p2):needsBlurY(p2))) break;\n" +
                    "       }\n" +
                    "       p2=p;\n" +
                    "       for(neg=1;neg<=maxSteps;neg++){\n" +
                    "           p2-=dp;\n" +
                    "           if(!(dirX?needsBlurX(p2):needsBlurY(p2))) break;\n" +
                    "       }\n" +
                    "       float fraction = (float(pos)-0.5)/float(pos+neg-1);\n" +
                    "       float blur = neg==maxSteps && pos==maxSteps ? 0.0 : min(1.0-fraction,fraction);\n" +
                    "       blur = smoothstep1(blur);\n" + // sharpen the edge from 2 wide to 1 wide
                    "       int other = (dirX?" +
                    "           abs(d1-d0) > abs(d3-d0) : " +
                    "           abs(d2-d0) > abs(d4-d0)) ? -1 : +1;\n" +
                    "       vec4 mixColor = texelFetch(color,p+(dirX?ivec2(other,0):ivec2(0,other)),0);\n" +
                    "       vec4 baseColor = texelFetch(color,p,0);\n" +
                    "       vec2 offset = rbOffset * float(other);\n" +
                    "       if(dirX){\n" +
                    "           vec2 ga = mix(baseColor.ga, mixColor.ga, blur);\n" +
                    "           float r = mix(baseColor.r,  mixColor.r,  max(blur-offset.x, 0.0));\n" +
                    "           float b = mix(baseColor.b,  mixColor.b,  max(blur+offset.x, 0.0));\n" +
                    "           fragColor = vec4(r,ga.x,b,ga.y);\n" +
                    "       } else {\n" +
                    "           vec2 ga = mix(baseColor.ga, mixColor.ga, blur);\n" +
                    "           float r = mix(baseColor.r,  mixColor.r,  max(blur-offset.y, 0.0));\n" +
                    "           float b = mix(baseColor.b,  mixColor.b,  max(blur+offset.y, 0.0));\n" +
                    "           fragColor = vec4(r,ga.x,b,ga.y);\n" +
                    "       }\n" +
                    "   } else {\n" +
                    "       fragColor = texelFetch(color, p, 0);\n" +
                    "   }\n" +
                    "}"
        ).apply { setTextureIndices(listOf("color", "depth0")) }
    }

    fun render(color: ITexture2D, depth: ITexture2D, threshold: Float = 1e-5f) {
        val enableDebugControls = false
        val shader = shader.value
        shader.use()
        shader.v1f("threshold", threshold)
        shader.v1b("disableEffect", enableDebugControls && isControlDown)
        // use the subpixel layout to define the correct subpixel rendering
        // only works for RGB or BGR, otherwise just will cancel to 0
        val sr = SubpixelLayout.r
        val sb = SubpixelLayout.b
        // 0.5 for mean, 0.5 to make the effect less obvious
        shader.v2f("rbOffset", (sr.x - sb.x) * 0.25f, (sr.y - sb.y) * 0.25f)
        shader.v1b("showEdges", enableDebugControls && isShiftDown)
        depth.bindTrulyNearest(1)
        color.bindTrulyNearest(0)
        flat01.draw(shader)
    }

}