package me.anno.gpu.shader

import me.anno.gpu.GFX
import me.anno.gpu.shader.builder.Variable
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER

// todo locations for the varyings: for debugging with RenderDoc

open class Shader(
    shaderName: String,
    val geometry: String? = null,
    val vertex: String,
    val varying: List<Variable>,
    val fragment: String,
    private val disableShorts: Boolean = false
) : OpenGLShader(shaderName) {

    var vertexSource = ""
    var fragmentSource = ""

    override fun sourceContainsWord(word: String): Boolean {
        return word in vertex || word in fragment
    }

    // shader compile time doesn't really matter... -> move it to the start to preserve ram use?
    // isn't that much either...
    override fun init() {

        // LOGGER.debug("$shaderName\nGEOMETRY:\n$geometry\nVERTEX:\n$vertex\nVARYING:\n$varying\nFRAGMENT:\n$fragment")

        val varyings = varying.map { Varying(if (it.isFlat) "flat" else "", it.type, it.name) }

        program = glCreateProgram()
        val versionString = formatVersion(glslVersion)
        val geometryShader = if (geometry != null) {
            for (v in varyings) v.makeDifferent()
            var geo = versionString + geometry
            while (true) {
                // copy over all varyings for the shaders
                val copyIndex = geo.indexOf("#copy")
                if (copyIndex < 0) break
                val indexVar = geo[copyIndex + "#copy[".length]
                // frag.name = vertices[indexVar].name
                geo = geo.substring(0, copyIndex) + varyings.joinToString("\n") {
                    "\t${it.fShaderName} = ${it.vShaderName}[$indexVar];"
                } + geo.substring(copyIndex + "#copy[i]".length)
            }
            geo = geo.replace("#varying", varyings.joinToString("\n") { "\t${it.type} ${it.name};" })
            geo = geo.replace("#inOutVarying", varyings.joinToString("") {
                "" +
                        "${it.modifiers} in  ${it.type} ${it.vShaderName}[];\n" +
                        "${it.modifiers} out ${it.type} ${it.fShaderName};\n"
            })
            compile(shaderName, program, GL_GEOMETRY_SHADER, geo)
        } else -1

        // the shaders are like a C compilation process, .o-files: after linking, they can be removed
        vertexSource = (versionString +
                // todo only specify mediump float, if we really don't need highp, and there is no common uniforms (issue in OpenGL ES)
                "precision mediump float;\n" +
                varyings.joinToString("\n") { "${it.modifiers} out ${it.type} ${it.vShaderName};" } +
                "\n" +
                vertex.replaceVaryingNames(true, varyings)
                ).replaceShortCuts(disableShorts)
        val vertexShader = compile(shaderName, program, GL_VERTEX_SHADER, vertexSource)

        fragmentSource = (versionString +
                "precision mediump float;\n" +
                varyings.joinToString("\n") { "${it.modifiers} in  ${it.type} ${it.fShaderName};" } +
                "\n" +
                (if (!fragment.contains("out ") && glslVersion == DefaultGLSLVersion && fragment.contains("gl_FragColor")) {
                    "" +
                            "out vec4 glFragColor;" +
                            fragment.replace("gl_FragColor", "glFragColor")
                } else fragment).replaceVaryingNames(false, varyings)
                ).replaceShortCuts(disableShorts)
        val fragmentShader = compile(shaderName, program, GL_FRAGMENT_SHADER, fragmentSource)

        glLinkProgram(program)
        // these could be reused...
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
        if (geometryShader >= 0) glDeleteShader(geometryShader)
        logShader(shaderName, vertexSource, fragmentSource)

        postPossibleError(shaderName, program, false, vertexSource, fragmentSource)

        GFX.check()

    }

}