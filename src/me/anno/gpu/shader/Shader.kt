package me.anno.gpu.shader

import me.anno.Build
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
    private var vertexVariables: List<Variable>,
    private var vertexShader: String,
    private var varyings: List<Variable>,
    private var fragmentVariables: List<Variable>,
    private var fragmentShader: String
) : OpenGLShader(shaderName) {

    companion object {
        private val LOGGER = LogManager.getLogger(Shader::class)
        val builder = StringBuilder(4096) // we only need one, because we compile serially anyway
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

    var vertexSource = vertexShader
    var fragmentSource = fragmentShader

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
        builder.clear()
        builder.append(versionString)

        for (line in vertexShader.split('\n')
            .filter { it.trim().startsWith("#extension ") }) {
            builder.append(line).append('\n')
        }

        // todo only set them, if not already specified
        builder.append("precision mediump float;\n")
        builder.append("precision mediump int;\n")
        if (vertexVariables.any2 { it.type == GLSLType.S3D })
            builder.append("precision highp sampler3D;\n")

        for (v in vertexVariables) {
            val prefix = when (v.inOutMode) {
                VariableMode.ATTR -> attribute
                VariableMode.IN, VariableMode.INOUT -> "uniform"
                VariableMode.OUT -> "out"
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

        if ("#extension" in vertexShader) builder.append(vertexShader.replace("#extension ", "// #extension "))
        else builder.append(vertexShader)

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
        if (varyings.any2 { it.type == GLSLType.S3D } || fragmentVariables.any2 { it.type == GLSLType.S3D })
            builder.append("precision highp sampler3D;\n")

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
                VariableMode.IN, VariableMode.INOUT -> {
                    "uniform"
                }
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
            "gl_FragColor" in fragmentShader && fragmentVariables.none { it.isOutput }// && !OS.isWeb // in WebGL, it is ok; only if version is low enough...
        ) "out vec4 glFragColor;\n" + fragmentShader.replace("gl_FragColor", "glFragColor")
        else fragmentShader

        if ("#extension" in base) builder.append(base.replace("#extension ", "// #extension "))
        else builder.append(base)

        fragmentSource = builder.toString()
        builder.clear()

        /*val fragmentShader = */
        compile(name, program, GL_FRAGMENT_SHADER, fragmentSource)

        GFX.check()

        val attributes = attributes
        for (i in attributes.indices) {
            glBindAttribLocation(program, i, attributes[i].name)
        }

        GFX.check()

        glLinkProgram(program)

        GFX.check()

        // these could be reused...
        // glDeleteShader(vertexShader)
        // glDeleteShader(fragmentShader)
        // if (geometryShader >= 0) glDeleteShader(geometryShader)

        logShader(name, vertexSource, fragmentSource)

        GFX.check()

        postPossibleError(name, program, false, vertexSource, fragmentSource)

        GFX.check()

        this.program = program // only assign the program, when no error happened

        if (textureNames.isNotEmpty()) {
            lastProgram = program
            glUseProgram(program)
            setTextureIndicesIfExisting()
            GFX.check()
        }

        // deleting sources to free up RAM
        if (!GFX.canLooseContext && Build.isShipped) {
            vertexShader = ""
            fragmentSource = ""
            vertexVariables = emptyList()
            fragmentVariables = emptyList()
            this.varyings = emptyList()
            vertexSource = ""
            fragmentSource = ""
        }

    }

    fun getAttributeLocation(name: String): Int {
        val attr = attributes
        for (idx in attr.indices) {
            if (attr[idx].name == name) {
                return idx
            }
        }
        return -1
    }

    override fun printLocationsAndValues() {
        super.printLocationsAndValues()
        for ((key, value) in attributes.withIndex()) {
            LOGGER.info("Attribute $key = $value")
        }
    }

}