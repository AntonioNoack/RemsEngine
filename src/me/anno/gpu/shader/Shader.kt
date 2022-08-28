package me.anno.gpu.shader

import me.anno.gpu.GFX
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.builder.Varying
import me.anno.utils.structures.lists.Lists.any2
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL20.*

// todo locations for the varyings: for debugging with RenderDoc

// todo replace attributes & uniforms to lists everywhere

open class Shader(
    shaderName: String,
    private val vertexVariables: List<Variable>,
    private val vertexShader: String,
    private val varyings: List<Variable>,
    private val fragmentVariables: List<Variable>,
    private val fragmentShader: String
) : OpenGLShader(shaderName) {

    companion object {
        private val LOGGER = LogManager.getLogger(Shader::class)
    }

    constructor(
        shaderName: String,
        vertex: String,
        varying: List<Variable>,
        fragment: String
    ) : this(shaderName, emptyList(), vertex, varying, emptyList(), fragment)

    val attributes = vertexVariables.filter { it.isAttribute }

    init {
        if (attributes.isEmpty() && ("in " in vertexShader || "attribute " in vertexShader)) {
            LOGGER.warn("Shader '$shaderName' should use Variables")
        }
    }

    var vertexSource = ""
    var fragmentSource = ""

    override fun sourceContainsWord(word: String): Boolean {
        return word in vertexShader || word in fragmentShader
    }

    // shader compile time doesn't really matter... -> move it to the start to preserve ram use?
    // isn't that much either...
    override fun compile() {

        // LOGGER.debug("$shaderName\nGEOMETRY:\n$geometry\nVERTEX:\n$vertex\nVARYING:\n$varying\nFRAGMENT:\n$fragment")

        val varyings = varyings.map { Varying(if (it.isFlat || it.type.isFlat) "flat" else "", it.type, it.name) }

        val program = glCreateProgram()
        GFX.check()
        updateSession()
        GFX.check()

        if (glslVersion < 330 && fragmentVariables.any2 { it.isOutput })
            glslVersion = 330 // needed for layout(location=x) qualifier

        val versionString = formatVersion(glslVersion) + "\n// $name\n"

        // the shaders are like a C compilation process, .o-files: after linking, they can be removed
        val builder = StringBuilder()
        builder.append(versionString)

        for (line in vertexShader.split('\n')
            .filter { it.trim().startsWith("#extension ") }) {
            builder.append(line).append('\n')
        }

        // todo only set them, if not already specified
        builder.append("precision mediump float;\n")
        builder.append("precision mediump int;\n")

        for (v in vertexVariables) {
            val prefix = when (v.inOutMode) {
                VariableMode.ATTR -> attribute
                VariableMode.IN, VariableMode.INOUT -> "uniform"
                VariableMode.OUT -> "out"
                VariableMode.INOUT2 -> continue // not supported
            }
            v.declare(builder, prefix)
        }
        for (v in varyings) {
            builder.append(v.modifiers)
            builder.append(" out ")
            builder.append(v.type.glslName)
            builder.append(' ')
            builder.append(v.vShaderName)
            builder.append(";\n")
        }
        builder.append(
            vertexShader
                .replaceVaryingNames(true, varyings)
                .replace("#extension ", "// #extension ")
        )
        vertexSource = builder.toString()
        builder.clear()

        /*val vertexShader = */
        compile(name, program, GL_VERTEX_SHADER, vertexSource)

        builder.append(versionString)
        for (extension in fragmentShader.split('\n')
            .filter { it.trim().startsWith("#extension ") }) {
            builder.append(extension).append('\n')
        }
        builder.append("precision mediump float;\n")
        builder.append("precision mediump int;\n")
        for (v in varyings) {
            builder.append(v.modifiers)
            builder.append(" in ")
            builder.append(v.type.glslName)
            builder.append(' ')
            builder.append(v.fShaderName)
            builder.append(";\n")
        }

        var outCtr = 0
        for (v in fragmentVariables) {
            val prefix = when (v.inOutMode) {
                VariableMode.IN, VariableMode.INOUT -> { "uniform" }
                VariableMode.INOUT2 -> continue // not really supported
                VariableMode.ATTR -> throw IllegalArgumentException("Fragment variable must not have type ATTR")
                VariableMode.OUT -> {
                    builder.append("layout(location=")
                        .append(outCtr++).append(") ")
                    "out"
                }
            }
            v.declare(builder, prefix)
        }

        val base = if ((outCtr == 0 && "out " !in fragmentShader) && glslVersion == DefaultGLSLVersion &&
            "gl_FragColor" in fragmentShader && fragmentVariables.none { it.isOutput }
        ) "out vec4 glFragColor;\n" + fragmentShader.replace("gl_FragColor", "glFragColor")
        else fragmentShader

        builder.append(
            base
                .replaceVaryingNames(false, varyings)
                .replace("#extension ", "// #extension ")
        )

        fragmentSource = builder.toString()
        builder.clear()

        /*val fragmentShader = */
        compile(name, program, GL_FRAGMENT_SHADER, fragmentSource)

        GFX.check()

        glLinkProgram(program)
        // these could be reused...
        // glDeleteShader(vertexShader)
        // glDeleteShader(fragmentShader)
        // if (geometryShader >= 0) glDeleteShader(geometryShader)
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