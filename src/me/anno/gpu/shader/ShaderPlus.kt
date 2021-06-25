package me.anno.gpu.shader

object ShaderPlus {

    fun create(name: String, vertex: String, varying: String, fragment: String): Shader {
        return Shader(name, vertex, varying, makeFragmentShaderUniversal(varying, fragment))
    }

    fun makeFragmentShaderUniversal(varyingSource: String, fragmentSource: String): String {
        val hasFinalColor = "finalColor" in fragmentSource
        val hasZDistance = "zDistance" in varyingSource
        val hasTint = "vec4 tint;" in fragmentSource
        val raw = fragmentSource.trim()
        if (!raw.endsWith("}")) throw RuntimeException()
        return "" +
                "uniform int drawMode;\n" +
                (if (hasTint) "" else "uniform vec4 tint;\n") +
                "" + raw.substring(0, raw.length - 1) + "" +
                (if (hasZDistance) "" else "float zDistance = 0;\n") +
                (if (hasFinalColor) "" else "vec3 finalColor = gl_FragColor.rgb;float finalAlpha = gl_FragColor.a;\n") +
                "switch(drawMode){\n" +
                "       case ${DrawMode.COLOR_SQUARED.id}:\n" +
                "           vec3 tmpCol = ${if (hasTint) "finalColor" else "finalColor * tint.rgb"};\n" +
                "           gl_FragColor = vec4(tmpCol * tmpCol, clamp(finalAlpha, 0, 1) * tint.a);\n" +
                "           break;\n" +
                "       case ${DrawMode.COLOR.id}:\n" +
                "           gl_FragColor = vec4(${if (hasTint) "finalColor" else "finalColor * tint.rgb"}, clamp(finalAlpha, 0, 1) * tint.a);\n" +
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
                "       case ${DrawMode.TINT.id}:\n" +
                "           gl_FragColor = tint;\n" +
                "           break;\n" +
                "   }\n" +
                "}"

    }

    enum class DrawMode(val id: Int) {
        COLOR_SQUARED(0),
        COLOR(1),
        ID(2),
        DEPTH(3),
        COPY(4),
        TINT(5)
    }

}