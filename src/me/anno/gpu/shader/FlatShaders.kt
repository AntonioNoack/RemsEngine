package me.anno.gpu.shader

import me.anno.engine.ui.render.Renderers.tonemapGLSL
import me.anno.gpu.shader.ShaderLib.blacklist
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.utils.structures.lists.LazyList
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt
import kotlin.math.PI

object FlatShaders {

    val copyShader = Shader(
        "copy", emptyList(), coordsUVVertexShader, uvList, listOf(
            Variable(GLSLType.S2D, "tex"),
            Variable(GLSLType.V1F, "alpha"),
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ), "void main(){ result = alpha * texture(tex, uv); }"
    ).apply { ignoreNameWarnings("samples,posSize") }

    val copyShaderMS = Shader(
        "copyMS", emptyList(), coordsUVVertexShader, uvList, listOf(
            Variable(GLSLType.S2DMS, "tex"),
            Variable(GLSLType.V1F, "alpha"),
            Variable(GLSLType.V1I, "samples"),
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ), "void main() {\n" +
                "   vec4 sum = vec4(0.0);\n" +
                "   ivec2 uvi = ivec2(vec2(textureSize(tex)) * uv);\n" +
                "   for(int i=0;i<samples;i++) sum += texelFetch(tex, uvi, i);\n" +
                "   result = (alpha / float(samples)) * sum;\n" +
                "}"
    ).apply { ignoreNameWarnings("posSize") }

    /**
     * blit-like shader without any stupid OpenGL constraints like size or format
     * */
    val copyShaderAnyToAny = LazyList(16) {
        val colorMS = it.hasFlag(8)
        val depthMS = it.hasFlag(4)
        val depthMask = "xyzw"[it.and(3)]
        Shader(
            "copyMSAnyToAny/${it.toString(2)}", emptyList(), coordsUVVertexShader, uvList, listOf(
                Variable(if (colorMS) GLSLType.S2DMS else GLSLType.S2D, "colorTex"),
                Variable(if (depthMS) GLSLType.S2DMS else GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V1B, "monochrome"),
                Variable(GLSLType.V1I, "colorSamples"),
                Variable(GLSLType.V1I, "depthSamples"),
                Variable(GLSLType.V1I, "targetSamples"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    (if (colorMS || depthMS) "" +
                            "vec4 getColor1(sampler2DMS colorTex, int srcSamples, vec2 uv){\n" +
                            "   ivec2 uvi = ivec2(vec2(textureSize(colorTex)) * uv);\n" +
                            "   if(srcSamples > targetSamples){\n" +
                            "       vec4 sum = vec4(0.0);\n" +
                            "       int ctr = 0;\n" +
                            "       for(int i=gl_SampleID;i<srcSamples;i+=targetSamples) {\n" +
                            "           sum += texelFetch(colorTex, uvi, i);\n" +
                            "           ctr++;\n" +
                            "       }\n" +
                            "       return sum / float(ctr);\n" +
                            "   } else if(srcSamples == targetSamples){\n" +
                            "       return texelFetch(colorTex, uvi, gl_SampleID);\n" +
                            "   } else {\n" +
                            "       return texelFetch(colorTex, uvi, gl_SampleID % srcSamples);\n" +
                            "   }\n" +
                            "}\n" else "") +
                    (if (!colorMS || !depthMS) "" +
                            "vec4 getColor0(sampler2D colorTex, int srcSamples, vec2 uv){\n" +
                            "   return texture(colorTex,uv);\n" +
                            "}\n" else "") +
                    "void main() {\n" +
                    "   result = getColor${colorMS.toInt()}(colorTex, colorSamples, uv);\n" +
                    "   if(monochrome) result.rgb = result.rrr;\n" +
                    // is this [-1,1] or [0,1]? -> looks like it works just fine for now
                    "   gl_FragDepth = getColor${depthMS.toInt()}(depthTex, depthSamples, uv).$depthMask;\n" +
                    "}"
        ).apply {
            setTextureIndices("colorTex", "depthTex")
            ignoreNameWarnings("targetSamples,d_camRot,d_fovFactor,d_uvCenter,reverseDepth")
        }
    }

    val coordsPosSize = coordsList + listOf(
        Variable(GLSLType.V4F, "posSize"),
        Variable(GLSLType.M4x4, "transform")
    )

    val coordsPosSizeVShader = "" +
            "void main(){\n" +
            "   gl_Position = matMul(transform, vec4((posSize.xy + coords * posSize.zw)*2.0-1.0, 0.0, 1.0));\n" +
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
                "   if(alphaMode == 0) { col *= data; }\n" +
                "   else if(alphaMode == 1) { col.rgb *= data.rgb; }\n" +
                "   else if(alphaMode == 2) { col.rgb *= data.x; }\n" +
                "   else { col.rgb *= data.a; }\n" +
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
                "   if(alphaMode == 0) col *= data;\n" +
                "   else if(alphaMode == 1) col.rgb *= data.rgb;\n" +
                "   else col.rgb *= data.a;\n" +
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
            Variable(GLSLType.S2D, "tex")
        ), "" +
                "void main(){\n" +
                "   float depth0 = texture(tex, uv).x;\n" +
                "   float depth1 = 0.1 + 0.9 * fract(log2(abs(depth0)));\n" +
                "   gl_FragColor = vec4(depth0 > 0.0 ? vec3(depth1) : vec3(depth1, 0.0, 0.0), 1.0);\n" +
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
            Variable(GLSLType.V2F, "coords", VariableMode.ATTR),
            Variable(GLSLType.V4F, "posSize"),
            Variable(GLSLType.M4x4, "transform"),
        ), "" +
                "void main(){\n" +
                "   gl_Position = matMul(transform, vec4((posSize.xy + coords * posSize.zw)*2.0-1.0, 0.0, 1.0));\n" +
                "   uv = (coords - 0.5) * vec2(${PI * 2},${PI});\n" +
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
            Variable(GLSLType.V2F, "coords", VariableMode.ATTR),
            Variable(GLSLType.V4F, "posSize"),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.V1F, "z")
        ), "" +
                "void main(){\n" +
                "   gl_Position = matMul(transform, vec4((posSize.xy + coords * posSize.zw)*2.0-1.0, 0.0, 1.0));\n" +
                "   uvw = vec3(coords, z);\n" +
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
            Variable(GLSLType.V2F, "coords", VariableMode.ATTR),
            Variable(GLSLType.V4F, "posSize"),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.V1F, "layer"),
        ), "" +
                "void main(){\n" +
                "   gl_Position = matMul(transform, vec4((posSize.xy + coords * posSize.zw)*2.0-1.0, 0.0, 1.0));\n" +
                "   uvw = vec3(coords, layer);\n" +
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

    init {
        flatShaderTexture.ignoreNameWarnings(blacklist)
        flatShaderCubemap.ignoreNameWarnings(blacklist)
    }
}