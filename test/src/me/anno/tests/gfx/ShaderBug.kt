package me.anno.tests.gfx

import me.anno.gpu.hidden.HiddenOpenGLContext
import org.lwjgl.opengl.GL20C

fun main() {
    HiddenOpenGLContext.createOpenGL()

    val vertex = "#version 150\n" +
            "precision highp float;\n" +
            "precision highp int;\n" +
            "in vec4 coords;\n" +
            "void main(){\n" +
            "   gl_Position = coords;\n" +
            "}"

    val fragment = "#version 150\n" +
            "precision highp float;\n" +
            "precision highp int;\n" +
            "uniform sampler2D tex0;\n" +
            "out vec4 result;\n" +
            "void main(){\n" +
            "   #define color texture(tex0,vec2(0.0))\n" + // fatal error
            // "   #define color vec4(1,2,3,4)\n" + // fine
            "   #define brackets vec4(color.rgb,1.0)\n" +
            "   result = vec4(brackets.xyz,1.0);\n" +
            "}\n"

    println(vertex)
    println(fragment)

    fun compile(program: Int, type: Int, source: String) {
        val shader = GL20C.glCreateShader(type)
        GL20C.glShaderSource(shader, source)
        GL20C.glCompileShader(shader)
        GL20C.glAttachShader(program, shader)
        val log = GL20C.glGetShaderInfoLog(shader)
        if (log.isNotBlank()) throw RuntimeException(log)
    }

    val program = GL20C.glCreateProgram()
    compile(program, GL20C.GL_VERTEX_SHADER, vertex)
    compile(program, GL20C.GL_FRAGMENT_SHADER, fragment)
    GL20C.glLinkProgram(program)
    val log = GL20C.glGetProgramInfoLog(program)
    if (log.isNotBlank()) throw RuntimeException(log) // crashes
}