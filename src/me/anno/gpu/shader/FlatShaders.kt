package me.anno.gpu.shader

import me.anno.gpu.shader.OpenGLShader.Companion.attribute
import me.anno.gpu.shader.ShaderLib.blacklist
import me.anno.gpu.shader.builder.Variable

object FlatShaders {

    val copyShader = ShaderLib.createShader(
        "copy", ShaderLib.simplestVertexShader, listOf(Variable(GLSLType.V2F, "uv")), "" +
                "uniform sampler2D tex;\n" +
                "uniform float am1;\n" +
                "void main(){\n" +
                "   gl_FragColor = (1.0-am1) * texture(tex, uv);\n" +
                "}", listOf("tex")
    )

    // color only for a rectangle
    // (can work on more complex shapes)
    val flatShader = BaseShader(
        "flatShader", "" +
                "$attribute vec2 attr0;\n" +
                "uniform vec2 pos, size;\n" +
                "void main(){\n" +
                "   gl_Position = vec4((pos + attr0 * size)*2.0-1.0, 0.0, 1.0);\n" +
                "}", emptyList(), "" +
                "uniform vec4 color;\n" +
                "void main(){\n" +
                "   gl_FragColor = color;\n" +
                "}"
    )


    val flatShaderStriped = BaseShader(
        "flatShader", "" +
                "$attribute vec2 attr0;\n" +
                "uniform vec2 pos, size;\n" +
                "void main(){\n" +
                "   gl_Position = vec4((pos + attr0 * size)*2.0-1.0, 0.0, 1.0);\n" +
                "}", emptyList(), "" +
                "uniform vec4 color;\n" +
                "uniform int offset, stride;\n" +
                "void main(){\n" +
                "   int x = int(gl_FragCoord.x);\n" +
                "   if(x % stride != offset) discard;\n" +
                "   gl_FragColor = color;\n" +
                "}"
    )

    val flatShaderGradient = ShaderLib.createShader(
        "flatShaderGradient", "" +
                "$attribute vec2 attr0;\n" +
                "uniform vec2 pos, size;\n" +
                "uniform vec4 uvs;\n" +
                ShaderLib.yuv2rgb +
                "uniform vec4 lColor, rColor;\n" +
                "uniform bool inXDirection;\n" +
                "void main(){\n" +
                "   gl_Position = vec4((pos + attr0 * size)*2.0-1.0, 0.0, 1.0);\n" +
                "   color = (inXDirection ? attr0.x : attr0.y) < 0.5 ? lColor : rColor;\n" +
                "   color = color * color;\n" + // srgb -> linear
                "   uv = mix(uvs.xy, uvs.zw, attr0);\n" +
                "}", listOf(Variable(GLSLType.V2F, "uv"), Variable(GLSLType.V4F, "color")), "" +
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
        "flatShaderTexture", "" +
                ShaderLib.simpleVertexShader, ShaderLib.uvList, "" +
                "uniform sampler2D tex;\n" +
                "uniform vec4 color;\n" +
                "uniform bool ignoreTexAlpha;\n" +
                "void main(){\n" +
                "   vec4 col = color;\n" +
                "   if(ignoreTexAlpha) col.rgb *= texture(tex, uv).rgb;\n" +
                "   else col *= texture(tex, uv);\n" +
                "   gl_FragColor = col;\n" +
                "}"
    )

    val flatShaderCubemap = BaseShader(
        "flatShaderCubemap", "" +
                "$attribute vec2 attr0;\n" +
                "uniform vec2 pos, size;\n" +
                "void main(){\n" +
                "   gl_Position = vec4((pos + attr0 * size)*2.0-1.0, 0.0, 1.0);\n" +
                "   uv = (attr0 - 0.5) * vec2(${Math.PI * 2},${Math.PI});\n" +
                "}", listOf(Variable(GLSLType.V2F, "uv")), "" +
                "uniform samplerCube tex;\n" +
                "uniform vec4 color;\n" +
                "uniform bool ignoreTexAlpha;\n" +
                // "uniform mat3 rotation;\n" +
                "void main(){\n" +
                "   vec2 sc = vec2(sin(uv.y),cos(uv.y));\n" +
                "   vec3 uvw = vec3(sin(uv.x),1.0,cos(uv.x)) * sc.yxy;\n" +
                // "   uvw = rotation * uvw;\n" +
                "   vec4 col = color;\n" +
                "   if(ignoreTexAlpha) col.rgb *= texture(tex, uvw).rgb;\n" +
                "   else col *= texture(tex, uvw);\n" +
                "   gl_FragColor = col;\n" +
                "}"
    )

    init {
        flatShaderTexture.ignoreUniformWarnings(blacklist)
        flatShaderCubemap.ignoreUniformWarnings(blacklist)
    }

}