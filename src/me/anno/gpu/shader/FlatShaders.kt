package me.anno.gpu.shader

import me.anno.engine.ui.render.Renderers.tonemapGLSL
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.utils.structures.lists.LazyList
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.iff
import kotlin.math.PI

object FlatShaders {

    val colorSpaceConversion = "" +
            "   if (isColor && convertSRGBToLinear && !convertLinearToSRGB) color.rgb *= color.rgb;\n" +
            "   if (isColor && convertLinearToSRGB && !convertSRGBToLinear) color.rgb = sqrt(max(color.rgb, vec3(0.0)));\n"

    val getColor0SS = "vec4 getColor0(sampler2D colorTex, int srcSamples, vec2 uv, bool isColor) {\n" +
            "   vec4 color = texture(colorTex,uv);\n" +
            colorSpaceConversion +
            "   return color;\n" +
            "}\n"

    val getColor1MS = "vec4 getColor1(sampler2DMS colorTex, int srcSamples, vec2 uv, bool isColor) {\n" +
            "   ivec2 uvi = ivec2(vec2(textureSize(colorTex)) * uv);\n" +
            "   if (srcSamples > targetSamples) {\n" +
            "       vec4 sum = vec4(0.0);\n" +
            "       int numSamples = 0;\n" +
            "       for (int i=gl_SampleID;i<srcSamples;i+=targetSamples) {\n" +
            "           vec4 color = texelFetch(colorTex, uvi, i);\n" +
            "           if (convertSRGBToLinear && isColor) {\n" +
            "               color.rgb *= color.rgb;\n" +
            "           }\n" +
            "           sum += color;\n" +
            "           numSamples++;\n" +
            "       }\n" +
            "       sum *= 1.0 / float(numSamples);\n" +
            "       if (convertLinearToSRGB && isColor) {" +
            "           sum.rgb = sqrt(max(sum.rgb,vec3(0.0)));" +
            "       }\n" +
            "       return sum;\n" +
            "   } else {" +
            "       vec4 color = srcSamples == targetSamples\n" +
            "           ? texelFetch(colorTex, uvi, gl_SampleID)\n" +
            "           : texelFetch(colorTex, uvi, gl_SampleID % srcSamples);\n" +
            colorSpaceConversion +
            "       return color;\n" +
            "   }" +
            "}\n"

    const val MULTISAMPLED_DEPTH_FLAG = 1
    const val MULTISAMPLED_COLOR_FLAG = 2
    const val DEPTH_MASK_MULTIPLIER = 4
    const val DONT_READ_DEPTH = 4

    /**
     * blit-like shader without any stupid OpenGL constraints like size or format;
     * copies color and depth
     * */
    val copyShaderAnyToAny = LazyList(20) { flags ->
        val colorMS = flags.hasFlag(MULTISAMPLED_COLOR_FLAG)
        val depthMS = flags.hasFlag(MULTISAMPLED_DEPTH_FLAG)
        val depthMask = "xyzw "[flags / DEPTH_MASK_MULTIPLIER]
        val writeDepth = depthMask != ' '
        Shader(
            "copyMSAnyToAny/${flags.toString(2)}", emptyList(),
            coordsUVVertexShader, uvList, listOf(
                Variable(if (colorMS) GLSLType.S2DMS else GLSLType.S2D, "colorTex"),
                Variable(if (depthMS) GLSLType.S2DMS else GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V1B, "monochrome"),
                Variable(GLSLType.V1B, "overrideAlpha"),
                Variable(GLSLType.V1F, "newAlpha"),
                Variable(GLSLType.V1B, "convertLinearToSRGB"),
                Variable(GLSLType.V1B, "convertSRGBToLinear"),
                Variable(GLSLType.V1I, "colorSamples"),
                Variable(GLSLType.V1I, "depthSamples"),
                Variable(GLSLType.V1I, "targetSamples"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    getColor1MS.iff(colorMS || (writeDepth && depthMS)) +
                    getColor0SS.iff(!colorMS || (writeDepth && !depthMS)) +
                    "void main() {\n" +
                    "   result = getColor${colorMS.toInt()}(colorTex, colorSamples, uv, true);\n" +
                    "   if (monochrome) result.rgb = result.rrr;\n" +
                    "   if (overrideAlpha) result.a = newAlpha;\n" +
                    // is this [-1,1] or [0,1]? -> looks like it works just fine for now
                    (if (writeDepth) {
                        "gl_FragDepth = getColor${depthMS.toInt()}(depthTex, depthSamples, uv, false).$depthMask;\n"
                    } else "") +
                    "}"
        ).apply {
            setTextureIndices("colorTex", "depthTex")
        }
    }

    val coordsPosSize = coordsList + listOf(
        Variable(GLSLType.V4F, "posSize"),
        Variable(GLSLType.M4x4, "transform")
    )

    val coordsPosSizeVShader = "" +
            "void main(){\n" +
            "   gl_Position = matMul(transform, vec4((posSize.xy + positions * posSize.zw)*2.0-1.0, 0.0, 1.0));\n" +
            "}"

    // color only for a rectangle
    // (can work on more complex shapes)
    val flatShader = BaseShader(
        "flatShader", coordsPosSize, coordsPosSizeVShader,
        emptyList(), listOf(
            Variable(GLSLType.V4F, "color")
        ), "" +
                "void main(){\n" +
                "   gl_FragColor = color;\n" +
                "}"
    )

    private val alphaModeProcessing = "" +
            "   if(alphaMode == 0) { col *= data; }\n" +
            "   else if(alphaMode == 1) { col.rgb *= data.rgb; }\n" +
            "   else if(alphaMode == 2) { col.rgb *= data.x; }\n" +
            "   else { col.rgb *= data.a; }\n"

    val flatShaderTexture = BaseShader(
        "flatShaderTexture",
        ShaderLib.uiVertexShaderList,
        ShaderLib.uiVertexShader, uvList,
        listOf(
            Variable(GLSLType.V1I, "alphaMode"), // 0 = rgba, 1 = rgb, 2 = rrr, 3 = a
            Variable(GLSLType.V4F, "color"),
            Variable(GLSLType.V1B, "applyToneMapping"),
            Variable(GLSLType.S2D, "tex"),
        ), "" +
                tonemapGLSL +
                "void main(){\n" +
                "   vec4 col = color;\n" +
                "   vec4 data = texture(tex, uv);\n" +
                alphaModeProcessing +
                "   if(!(col.x >= -1e38 && col.x <= 1e38)) { col = vec4(1.0,0.0,1.0,1.0); }\n" +
                "   else if(applyToneMapping) { col = tonemap(col); }\n" +
                "   gl_FragColor = col;\n" +
                "}"
    )

    val flatShaderTextureArray = BaseShader(
        "flatShaderTexture",
        ShaderLib.uiVertexShaderList,
        ShaderLib.uiVertexShader, uvList,
        listOf(
            Variable(GLSLType.V1I, "alphaMode"), // 0 = rgba, 1 = rgb, 2 = a
            Variable(GLSLType.V4F, "color"),
            Variable(GLSLType.V1F, "layer"),
            Variable(GLSLType.V1B, "applyToneMapping"),
            Variable(GLSLType.S2DA, "tex"),
        ), "" +
                tonemapGLSL +
                "void main(){\n" +
                "   vec4 col = color;\n" +
                "   vec4 data = texture(tex, vec3(uv,layer));\n" +
                alphaModeProcessing +
                "   if(!(col.x >= -1e38 && col.x <= 1e38)) col = vec4(1.0,0.0,1.0,1.0);\n" +
                "   if(applyToneMapping) col = tonemap(col);\n" +
                "   gl_FragColor = col;\n" +
                "}"
    )

    val flatShaderTexture3D = BaseShader(
        "flatShaderTexture3D",
        ShaderLib.uiVertexShaderList,
        ShaderLib.uiVertexShader, uvList,
        listOf(
            Variable(GLSLType.V1I, "alphaMode"), // 0 = rgba, 1 = rgb, 2 = a
            Variable(GLSLType.V4F, "color"),
            Variable(GLSLType.V1B, "applyToneMapping"),
            Variable(GLSLType.V1F, "layer"),
            Variable(GLSLType.S3D, "tex"),
        ), "" +
                tonemapGLSL +
                "void main(){\n" +
                "   vec4 col = color;\n" +
                "   vec3 uvw = vec3(uv, layer);\n" +
                "   if(alphaMode == 0) col *= texture(tex, uvw);\n" +
                "   else if(alphaMode == 1) col.rgb *= texture(tex, uvw).rgb;\n" +
                "   else col.rgb *= texture(tex, uvw).a;\n" +
                "   if(applyToneMapping) col = tonemap(col);\n" +
                "   gl_FragColor = col;\n" +
                "}"
    )

    val depthShader = BaseShader(
        "depth", ShaderLib.uiVertexShaderList, ShaderLib.uiVertexShader, uvList, listOf(
            Variable(GLSLType.V1B, "reverseDepth"),
            Variable(GLSLType.S2D, "tex")
        ), "" +
                "void main(){\n" +
                "   float depth0 = texture(tex, uv).x;\n" +
                "   float depth1 = reverseDepth ? depth0 : 1.0-depth0;\n" +
                "   float depth2 = fract(log2(abs(depth1)));\n" +
                "   float depth3 = 0.1 + 0.9 * depth2;\n" +
                "   bool withinBounds = depth0 > 0.0 && depth0 < 1.0;\n" +
                "   gl_FragColor = vec4(withinBounds ? vec3(depth3) : vec3(depth3, 0.0, 0.0), 1.0);\n" +
                "}"
    )

    val depthArrayShader = BaseShader(
        "depth", ShaderLib.uiVertexShaderList, ShaderLib.uiVertexShader, uvList, listOf(
            Variable(GLSLType.S2DA, "tex"),
            Variable(GLSLType.V1F, "layer")
        ), "" +
                "void main(){\n" +
                "   float depth0 = texture(tex, vec3(uv,layer)).x;\n" +
                "   float depth1 = 0.1 + 0.9 * fract(log2(abs(depth0)));\n" +
                "   gl_FragColor = vec4(depth0 > 0.0 ? vec3(depth1) : vec3(depth1, 0.0, 0.0), 1.0);\n" +
                "}"
    )

    val flatShaderCubemap = BaseShader(
        "flatShaderCubemap", listOf(
            Variable(GLSLType.V2F, "positions", VariableMode.ATTR),
            Variable(GLSLType.V4F, "posSize"),
            Variable(GLSLType.M4x4, "transform"),
        ), "" +
                "void main(){\n" +
                "   gl_Position = matMul(transform, vec4((posSize.xy + positions * posSize.zw)*2.0-1.0, 0.0, 1.0));\n" +
                "   uv = (positions - 0.5) * vec2(${PI * 2},${PI});\n" +
                "}", listOf(Variable(GLSLType.V2F, "uv")), listOf(
            Variable(GLSLType.SCube, "tex"),
            Variable(GLSLType.V4F, "color"),
            Variable(GLSLType.V1B, "ignoreTexAlpha"),
            Variable(GLSLType.V1B, "showDepth"),
            // Variable(GLSLType.M3x3, "rotation"),
        ), "" +
                "void main(){\n" +
                "   vec2 sc = vec2(sin(uv.y),cos(uv.y));\n" +
                "   vec3 uvw = vec3(sin(uv.x),1.0,cos(uv.x)) * sc.yxy;\n" +
                // "   uvw = rotation * uvw;\n" +
                "   vec4 col = color;\n" +
                "   if(ignoreTexAlpha) col.rgb *= texture(tex, uvw).rgb; else col *= texture(tex, uvw);\n" +
                "   if(showDepth) col = vec4(vec3(fract(log2(col.r))), 1.0);\n" +
                "   gl_FragColor = col;\n" +
                "}"
    )

    val flatShader3dSlice = BaseShader(
        "flatShader3dSlice", listOf(
            Variable(GLSLType.V2F, "positions", VariableMode.ATTR),
            Variable(GLSLType.V4F, "posSize"),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.V1F, "z")
        ), "" +
                "void main(){\n" +
                "   gl_Position = matMul(transform, vec4((posSize.xy + positions * posSize.zw)*2.0-1.0, 0.0, 1.0));\n" +
                "   uvw = vec3(positions, z);\n" +
                "}", listOf(Variable(GLSLType.V3F, "uvw")), listOf(
            Variable(GLSLType.S3D, "tex"),
            Variable(GLSLType.V4F, "color"),
            Variable(GLSLType.V1B, "ignoreTexAlpha"),
            Variable(GLSLType.V1B, "showDepth")
        ), "" +
                "void main(){\n" +
                // "   uvw = rotation * uvw;\n" +
                "   vec4 col = color;\n" +
                "   if(ignoreTexAlpha) col.rgb *= texture(tex, uvw).rgb; else col *= texture(tex, uvw);\n" +
                "   if(showDepth) col = vec4(vec3(fract(log2(col.r))), 1.0);\n" +
                "   gl_FragColor = col;\n" +
                "}"
    )

    val flatShader2DArraySlice = BaseShader(
        "flatShader3dSlice", listOf(
            Variable(GLSLType.V2F, "positions", VariableMode.ATTR),
            Variable(GLSLType.V4F, "posSize"),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.V1F, "layer"),
        ), "" +
                "void main(){\n" +
                "   gl_Position = matMul(transform, vec4((posSize.xy + positions * posSize.zw)*2.0-1.0, 0.0, 1.0));\n" +
                "   uvw = vec3(positions, layer);\n" +
                "}", listOf(Variable(GLSLType.V3F, "uvw")), listOf(
            Variable(GLSLType.S2DA, "tex"),
            Variable(GLSLType.V4F, "color"),
            Variable(GLSLType.V1B, "ignoreTexAlpha"),
            Variable(GLSLType.V1B, "showDepth")
        ), "" +
                "void main(){\n" +
                // "   uvw = rotation * uvw;\n" +
                "   vec4 col = color;\n" +
                "   if(ignoreTexAlpha) col.rgb *= texture(tex, uvw).rgb; else col *= texture(tex, uvw);\n" +
                "   if(showDepth) col = vec4(vec3(fract(log2(col.r))), 1.0);\n" +
                "   gl_FragColor = col;\n" +
                "}"
    )
}