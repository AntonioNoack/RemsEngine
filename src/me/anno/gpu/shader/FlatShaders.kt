package me.anno.gpu.shader

import me.anno.engine.ui.render.Renderers.tonemapGLSL
import me.anno.gpu.shader.OpenGLShader.Companion.attribute
import me.anno.gpu.shader.ShaderLib.blacklist
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import kotlin.math.PI

object FlatShaders {

    val copyShader = Shader(
        "copy", coordsList, coordsVShader, uvList, listOf(
            Variable(GLSLType.S2D, "tex"),
            Variable(GLSLType.V1F, "alpha"),
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ), "void main(){ result = alpha * texture(tex, uv); }"
    )

    val copyShaderMS = Shader(
        "copy", coordsList, coordsVShader, uvList, listOf(
            Variable(GLSLType.S2DMS, "tex"),
            Variable(GLSLType.V1F, "alpha"),
            Variable(GLSLType.V1I, "samples"),
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ), "void main() {\n" +
                "   vec4 sum = vec4(0.0);\n" +
                "   ivec2 uvi = ivec2(textureSize(tex) * uv);\n" +
                "   for(int i=0;i<samples;i++) sum += texelFetch(tex, uvi, i);\n" +
                "   result = (alpha / float(samples)) * sum;\n" +
                "}"
    )

    val coordsPosSize = coordsList + listOf(
        Variable(GLSLType.V2F, "pos"),
        Variable(GLSLType.V2F, "size"),
        Variable(GLSLType.M4x4, "transform")
    )

    val coordsPosSizeVShader = "" +
            "void main(){\n" +
            "   gl_Position = transform * vec4((pos + coords * size)*2.0-1.0, 0.0, 1.0);\n" +
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

    val flatShaderStriped = BaseShader(
        "flatShaderStriped", coordsPosSize, coordsPosSizeVShader,
        emptyList(), listOf(
            Variable(GLSLType.V4F, "color"),
            Variable(GLSLType.V1I, "offset"),
            Variable(GLSLType.V1I, "stride")
        ), "" +
                "void main(){\n" +
                "   int x = int(gl_FragCoord.x);\n" +
                "   if(x % stride != offset) discard;\n" +
                "   gl_FragColor = color;\n" +
                "}"
    )

    val flatShaderGradient = ShaderLib.createShader(
        "flatShaderGradient", listOf(Variable(GLSLType.V2F, "coords", VariableMode.ATTR)), "" +
                "uniform vec2 pos, size;\n" +
                "uniform mat4 transform;\n" +
                "uniform vec4 uvs;\n" +
                ShaderLib.yuv2rgb +
                "uniform vec4 lColor, rColor;\n" +
                "uniform bool inXDirection;\n" +
                "void main(){\n" +
                "   gl_Position = transform * vec4((pos + coords * size)*2.0-1.0, 0.0, 1.0);\n" +
                "   color = (inXDirection ? coords.x : coords.y) < 0.5 ? lColor : rColor;\n" +
                "   color = color * color;\n" + // srgb -> linear
                "   uv = mix(uvs.xy, uvs.zw, coords);\n" +
                "}", listOf(Variable(GLSLType.V2F, "uv"), Variable(GLSLType.V4F, "color")), listOf(), "" +
                "uniform int code;\n" +
                "uniform sampler2D tex0,tex1;\n" +
                ShaderLib.yuv2rgb +
                "void main(){\n" +
                "   vec4 texColor;\n" +
                "   if(uv.x >= 0.0 && uv.x <= 1.0){\n" +
                "       switch(code){\n" +
                "           case 0: texColor = texture(tex0, uv).gbar;break;\n" + // ARGB
                "           case 1: texColor = texture(tex0, uv).bgra;break;\n" + // BGRA
                "           case 2: \n" +
                "               vec3 yuv = vec3(texture(tex0, uv).r, texture(tex1, uv).xy);\n" +
                "               texColor = vec4(yuv2rgb(yuv), 1.0);\n" +
                "               break;\n" + // YUV
                "           default: texColor = texture(tex0, uv);\n" + // RGBA
                "       }\n" +
                "   } else texColor = vec4(1.0);\n" +
                "   gl_FragColor = sqrt(color) * texColor;\n" +
                "}", listOf("tex0", "tex1")
    )

    val flatShaderTexture = BaseShader(
        "flatShaderTexture",
        ShaderLib.simpleVertexShaderV2List,
        ShaderLib.simpleVertexShaderV2, uvList,
        listOf(
            Variable(GLSLType.V1I, "alphaMode"), // 0 = rgba, 1 = rgb, 2 = a
            Variable(GLSLType.V4F, "color"),
            Variable(GLSLType.V1B, "applyToneMapping"),
            Variable(GLSLType.S2D, "tex"),
        ), "" +
                tonemapGLSL +
                "void main(){\n" +
                "   vec4 col = color;\n" +
                "   if(alphaMode == 0) col *= texture(tex, uv);\n" +
                "   else if(alphaMode == 1) col.rgb *= texture(tex, uv).rgb;\n" +
                "   else col.rgb *= texture(tex, uv).a;\n" +
                "   if(applyToneMapping) col = tonemap(col);\n" +
                "   gl_FragColor = col;\n" +
                "}"
    )

    val flatShaderTexture3D = BaseShader(
        "flatShaderTexture3D",
        ShaderLib.simpleVertexShaderV2List,
        ShaderLib.simpleVertexShaderV2, uvList,
        listOf(
            Variable(GLSLType.V1I, "alphaMode"), // 0 = rgba, 1 = rgb, 2 = a
            Variable(GLSLType.V4F, "color"),
            Variable(GLSLType.V1B, "applyToneMapping"),
            Variable(GLSLType.V1F, "uvZ"),
            Variable(GLSLType.S3D, "tex"),
        ), "" +
                tonemapGLSL +
                "void main(){\n" +
                "   vec4 col = color;\n" +
                "   vec3 uvw = vec3(uv, uvZ);\n" +
                "   if(alphaMode == 0) col *= texture(tex, uvw);\n" +
                "   else if(alphaMode == 1) col.rgb *= texture(tex, uvw).rgb;\n" +
                "   else col.rgb *= texture(tex, uvw).a;\n" +
                "   if(applyToneMapping) col = tonemap(col);\n" +
                "   gl_FragColor = col;\n" +
                "}"
    )

    val depthShader = BaseShader(
        "depth", ShaderLib.simpleVertexShaderV2List, ShaderLib.simpleVertexShaderV2, uvList, listOf(
            Variable(GLSLType.S2D, "tex")
        ), "" +
                "void main(){\n" +
                "   float depth0 = texture(tex, uv).x;\n" +
                "   float depth1 = 0.1 + 0.9 * fract(log2(abs(depth0)));\n" +
                "   gl_FragColor = vec4(depth0 > 0.0 ? vec3(depth1) : vec3(depth1, 0.0, 0.0), 1.0);\n" +
                "}"
    )

    val flatShaderCubemap = BaseShader(
        "flatShaderCubemap", listOf(Variable(GLSLType.V2F,"coords",VariableMode.ATTR)), "" +
                "uniform vec2 pos, size;\n" +
                "uniform mat4 transform;\n" +
                "void main(){\n" +
                "   gl_Position = transform * vec4((pos + coords * size)*2.0-1.0, 0.0, 1.0);\n" +
                "   uv = (coords - 0.5) * vec2(${PI * 2},${PI});\n" +
                "}", listOf(Variable(GLSLType.V2F, "uv")), listOf(), "" +
                "uniform samplerCube tex;\n" +
                "uniform vec4 color;\n" +
                "uniform bool ignoreTexAlpha;\n" +
                "uniform bool showDepth;\n" +
                // "uniform mat3 rotation;\n" +
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
        "flatShader3dSlice", listOf(Variable(GLSLType.V2F,"coords",VariableMode.ATTR)), "" +
                "uniform vec2 pos, size;\n" +
                "uniform mat4 transform;\n" +
                "uniform float z;\n" +
                "void main(){\n" +
                "   gl_Position = transform * vec4((pos + coords * size)*2.0-1.0, 0.0, 1.0);\n" +
                "   uvw = vec3(coords, z);\n" +
                "}", listOf(Variable(GLSLType.V3F, "uvw")), listOf(), "" +
                "uniform sampler3D tex;\n" +
                "uniform vec4 color;\n" +
                "uniform bool ignoreTexAlpha;\n" +
                "uniform bool showDepth;\n" +
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