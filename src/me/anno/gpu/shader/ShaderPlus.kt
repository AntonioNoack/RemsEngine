package me.anno.gpu.shader

object ShaderPlus {

    fun create(name: String, vertex: String, varying: String, fragment: String): Shader {
        return Shader(name, vertex, varying, makeFragmentShaderUniversal(varying, fragment))
    }

    fun makeFragmentShaderUniversal(varyingSource: String, fragmentSource: String): String {
        val hasFinalAlpha = "finalAlpha" in fragmentSource
        val hasZDistance = "zDistance" in varyingSource
        val hasTint = "tint" in fragmentSource
        val raw = fragmentSource.trim()
        if (!raw.endsWith("}")) throw RuntimeException()
        return "" +
                "uniform int drawMode;\n" +
                "" + raw.substring(0, raw.length - 1) + "" +
                (if (hasTint) "" else "vec4 tint = vec4(1);\n") +
                (if (hasZDistance) "" else "float zDistance = 0;\n") +
                (if (hasFinalAlpha) "" +
                        "switch(drawMode){\n" +
                        "       case ${DrawMode.COLOR_SQUARED.id}:\n" +
                        "           gl_FragColor = vec4(finalColor * finalColor, clamp(finalAlpha, 0, 1));\n" +
                        "           break;\n" +
                        "       case ${DrawMode.COLOR.id}:\n" +
                        "           gl_FragColor = vec4(finalColor, clamp(finalAlpha, 0, 1));\n" +
                        "           break;\n" +
                        "       case ${DrawMode.ID.id}:\n" +
                        "           if(finalAlpha < 0.01) discard;\n" +
                        "           gl_FragColor.rgb = tint.rgb;\n" +
                        "           break;\n" +
                        "       case ${DrawMode.DEPTH.id}:\n" +
                        "           if(finalAlpha < 0.01) discard;\n" +
                        "           float depth = 0.5 + 0.04 * log2(zDistance);\n" +
                        "           gl_FragColor = vec4(vec3(depth), finalAlpha);\n" +
                        "           break;\n" +
                        "       case ${DrawMode.COPY.id}:\n" +
                        "           gl_FragColor = vec4(finalColor, finalAlpha);\n" +
                        "           break;\n" +
                        "   }\n" +
                        "}"
                else "" +
                        "switch(drawMode){\n" +
                        "       case ${DrawMode.COLOR_SQUARED.id}:\n" +
                        "           gl_FragColor.rgb *= gl_FragColor.rgb;\n" +
                        "           gl_FragColor.a = clamp(gl_FragColor.a, 0, 1);\n" +
                        "           break;\n" +
                        "       case ${DrawMode.COLOR.id}:\n" +
                        "           gl_FragColor.a = clamp(gl_FragColor.a, 0, 1);\n" +
                        "           break;\n" +
                        "       case ${DrawMode.ID.id}:\n" +
                        "           if(gl_FragColor.a < 0.01) discard;\n" +
                        "           gl_FragColor.rgb = tint.rgb;\n" +
                        "           break;\n" +
                        "       case ${DrawMode.DEPTH.id}:\n" +
                        "           if(gl_FragColor.a < 0.01) discard;\n" +
                        "           float depth = 0.5 + 0.04 * log2(zDistance);\n" +
                        "           gl_FragColor.rgb = vec3(depth);\n" +
                        "           break;\n" +
                        "       case ${DrawMode.COPY.id}:\n" +
                        "           break;\n" +
                        "   }\n" +
                        "}"
                        )

    }

    enum class DrawMode(val id: Int) {
        COLOR_SQUARED(0),
        COLOR(1),
        ID(2),
        DEPTH(3),
        COPY(4)
    }

}