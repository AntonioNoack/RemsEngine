package me.anno.gpu.shader

import me.anno.gpu.GFX
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.Varying
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

        val program = glCreateProgram()
        GFX.check()
        updateSession()
        GFX.check()

        val versionString = formatVersion(glslVersion) + "\n// $name\n"
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
            geo = geo.replace("#varying", varyings.joinToString("\n") { "\t${it.type.glslName} ${it.name};" })
            geo = geo.replace("#inOutVarying", varyings.joinToString("") {
                "" +
                        "${it.modifiers} in  ${it.type.glslName} ${it.vShaderName}[];\n" +
                        "${it.modifiers} out ${it.type.glslName} ${it.fShaderName};\n"
            })
            compile(name, program, GL_GEOMETRY_SHADER, geo)
        } else -1

        // the shaders are like a C compilation process, .o-files: after linking, they can be removed
        vertexSource = (versionString +
                // todo only specify mediump float, if we really don't need highp, and there is no common uniforms (issue in OpenGL ES)
                "precision mediump float;\n" +
                varyings.joinToString("\n") { "${it.modifiers} out ${it.type.glslName} ${it.vShaderName};" } +
                "\n" +
                vertex.replaceVaryingNames(true, varyings)
                ).replaceShortCuts(disableShorts)
        val vertexShader = compile(name, program, GL_VERTEX_SHADER, vertexSource)

        fragmentSource = (versionString +
                "precision mediump float;\n" +
                varyings.joinToString("\n") { "${it.modifiers} in  ${it.type.glslName} ${it.fShaderName};" } +
                "\n" +
                (if (!fragment.contains("out ") && glslVersion == DefaultGLSLVersion && fragment.contains("gl_FragColor")) {
                    "" +
                            "out vec4 glFragColor;" +
                            fragment.replace("gl_FragColor", "glFragColor")
                } else fragment).replaceVaryingNames(false, varyings)
                ).replaceShortCuts(disableShorts)
        val fragmentShader = compile(name, program, GL_FRAGMENT_SHADER, fragmentSource)

        GFX.check()

        glLinkProgram(program)
        // these could be reused...
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
        if (geometryShader >= 0) glDeleteShader(geometryShader)
        logShader(name, vertexSource, fragmentSource)

        postPossibleError(name, program, false, vertexSource, fragmentSource)

        GFX.check()

        this.program = program // only assign the program, when no error happened

        if (textureNames.isNotEmpty()) {
            lastProgram = program
            glUseProgram(program)
            setTextureIndicesIfExisting()
            GFX.check()
        }

    }

}