package me.anno.gpu.shader

import java.lang.RuntimeException

object ShaderPlus {

    fun create(name: String, vertex: String, varying: String, fragment: String): Shader {
        return Shader(name, vertex, varying, makeUniversal(fragment))
    }

    fun makeUniversal(shader: String): String {

        val raw = shader.trim()
        if(!raw.endsWith("}")) throw RuntimeException()
        return "" +
                "uniform int drawMode;\n" +
                "" + raw.substring(0, raw.length-1) + "" +
                "   switch(drawMode){\n" +
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
                "   }\n" +
                "}"

    }

    enum class DrawMode(val id: Int){
        COLOR_SQUARED(0),
        COLOR(1),
        ID(2),
        DEPTH(3)
    }

}