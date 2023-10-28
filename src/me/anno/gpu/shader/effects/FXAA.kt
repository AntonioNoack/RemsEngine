package me.anno.gpu.shader.effects

import me.anno.gpu.GFX.flat01
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
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
object FXAA {

    // source: https://github.com/McNopper/OpenGL/tree/master
    // I tried to base mine on depth, but theirs was way better on thin lines, so let's use theirs :)

    // not ideal, but still ok most times
    // todo curves are still an issue
    // if we are lucky, FSR 2.0 tells us how to implement this without bugs :)
    // only issue: axis aligned boxes...

    val shader = lazy {
        Shader(
            "FXAA", coordsList, coordsUVVertexShader, uvList, listOf(
                Variable(GLSLType.V4F, "fragColor", VariableMode.OUT),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.V1B, "showEdges"),
                Variable(GLSLType.V1B, "disableEffect"),
                Variable(GLSLType.V1F, "threshold"),
            ),
            "" +
                    "void main(){\n" +

                    "#define u_lumaThreshold threshold\n" +
                    "#define u_texelStep 1.0/vec2(textureSize(colorTex,0))\n" +
                    "#define u_maxSpan 8.0\n" +
                    "#define u_mulReduce 0.5\n" +
                    "#define u_minReduce 0.01\n" +

                    "   vec3 rgbM = texture(colorTex, uv).rgb;\n" +
                    "   if (disableEffect) {\n" +
                    "       fragColor = vec4(rgbM,1.0);\n" +
                    "       return;\n" +
                    "   }\n" +

                    // Sampling neighbour texels. Offsets are adapted to OpenGL texture coordinates
                    "    vec3 rgbNW = textureOffset(colorTex, uv, ivec2(-1, +1)).rgb;\n" +
                    "    vec3 rgbNE = textureOffset(colorTex, uv, ivec2(+1, +1)).rgb;\n" +
                    "    vec3 rgbSW = textureOffset(colorTex, uv, ivec2(-1, -1)).rgb;\n" +
                    "    vec3 rgbSE = textureOffset(colorTex, uv, ivec2(+1, -1)).rgb;\n" +
                    // see http://en.wikipedia.org/wiki/Grayscale
                    "    const vec3 toLuma = vec3(0.299, 0.587, 0.114);\n" +
                    // Convert from RGB to luma
                    "    float lumaNW = dot(rgbNW, toLuma);\n" +
                    "    float lumaNE = dot(rgbNE, toLuma);\n" +
                    "    float lumaSW = dot(rgbSW, toLuma);\n" +
                    "    float lumaSE = dot(rgbSE, toLuma);\n" +
                    "    float lumaM = dot(rgbM, toLuma);\n" +
                    // Gather minimum and maximum luma
                    "    float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));\n" +
                    "    float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));\n" +

                    // If contrast is lower than a maximum threshold ...
                    "    if (lumaMax - lumaMin <= lumaMax * u_lumaThreshold) {\n" +
                    "        fragColor = vec4(rgbM, 1.0);\n" +
                    "        return;\n" +
                    "    }\n" +
                    // Sampling is done along the gradient.
                    "    vec2 samplingDirection;\n" +
                    "    samplingDirection.x = -((lumaNW + lumaNE) - (lumaSW + lumaSE));\n" +
                    "    samplingDirection.y =  ((lumaNW + lumaSW) - (lumaNE + lumaSE));\n" +

                    // Sampling step distance depends on the luma: The brighter the sampled texels, the smaller the final sampling step direction.
                    // The result is that brighter areas are less blurred/sharper than dark areas.
                    "    float samplingDirectionReduce = max((lumaNW + lumaNE + lumaSW + lumaSE) * 0.25 * u_mulReduce, u_minReduce);\n" +
                    // Factor for norming the sampling direction plus adding the brightness influence.
                    "    float minSamplingDirectionFactor = 1.0 / (min(abs(samplingDirection.x), abs(samplingDirection.y)) + samplingDirectionReduce);\n" +
                    // Calculate final sampling direction vector by reducing, clamping to a range and finally adapting to the texture size.
                    "    samplingDirection = clamp(samplingDirection * minSamplingDirectionFactor, vec2(-u_maxSpan), vec2(u_maxSpan)) * u_texelStep;\n" +
                    // Inner samples on the tab.
                    "    vec3 rgbSampleNeg = texture(colorTex, uv + samplingDirection * (1.0/3.0 - 0.5)).rgb;\n" +
                    "    vec3 rgbSamplePos = texture(colorTex, uv + samplingDirection * (2.0/3.0 - 0.5)).rgb;\n" +

                    "    vec3 rgbTwoTab = (rgbSamplePos + rgbSampleNeg) * 0.5;  \n" +

                    // Outer samples on the tab.
                    "    vec3 rgbSampleNegOuter = texture(colorTex, uv + samplingDirection * (0.0/3.0 - 0.5)).rgb;\n" +
                    "    vec3 rgbSamplePosOuter = texture(colorTex, uv + samplingDirection * (3.0/3.0 - 0.5)).rgb;\n" +

                    "    vec3 rgbFourTab = (rgbSamplePosOuter + rgbSampleNegOuter) * 0.25 + rgbTwoTab * 0.5;   \n" +

                    // Calculate luma for checking against the minimum and maximum value.
                    "    float lumaFourTab = dot(rgbFourTab, toLuma);\n" +

                    // Are outer samples of the tab beyond the edge ...
                    "    if (lumaFourTab < lumaMin || lumaFourTab > lumaMax) {\n" +
                    // ... yes, so use only two samples.
                    "        fragColor = vec4(rgbTwoTab, 1.0); \n" +
                    "    } else {\n" +
                    // ... no, so use four samples.
                    "        fragColor = vec4(rgbFourTab, 1.0);\n" +
                    "    }\n" +
                    // Show edges for debug purposes.
                    "    if (showEdges) {\n" +
                    "        fragColor.r = 1.0;\n" +
                    "    }\n" +
                    "}"
        ).apply {
            ignoreNameWarnings("posSize")
        }
    }

    fun render(color: ITexture2D, threshold: Float = 1e-5f) {
        val shader = shader.value
        shader.use()
        shader.v1f("threshold", threshold)
        shader.v1b("disableEffect", enableDebugControls && isControlDown)
        shader.v1b("showEdges", enableDebugControls && isShiftDown)
        color.bindTrulyNearest(0)
        flat01.draw(shader)
    }

    var enableDebugControls = false

}