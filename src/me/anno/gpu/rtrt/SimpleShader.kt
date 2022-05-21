package me.anno.gpu.rtrt

import me.anno.gpu.GFX
import me.anno.gpu.shader.OpenGLShader
import org.lwjgl.opengl.GL20

open class SimpleShader(name: String, val source: String, val shaderType: Int) : OpenGLShader(name) {
    override fun compile() {

        val program = GL20.glCreateProgram()
        GFX.check()
        updateSession()
        GFX.check()

        val vertexShader = compile(name, program, shaderType, source)

        GFX.check()

        GL20.glLinkProgram(program)
        // these could be reused...
        GL20.glDeleteShader(vertexShader)

        postPossibleError(name, program, false, source, "")

        GFX.check()

        this.program = program

    }

    override fun sourceContainsWord(word: String): Boolean = false
}