package me.anno.gpu.shader

import me.anno.gpu.GFX
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.builder.Varying
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER

// todo locations for the varyings: for debugging with RenderDoc

// todo replace attributes & uniforms to lists everywhere

open class Shader(
    shaderName: String,
    private val geometryShader: String?,
    private val vertexVariables: List<Variable>,
    private val vertexShader: String,
    private val varyings: List<Variable>,
    private val fragmentVariables: List<Variable>,
    private val fragmentShader: String
) : OpenGLShader(shaderName) {

    constructor(
        shaderName: String, vsUniforms: List<Variable>, vertex: String,
        varying: List<Variable>, fsUniforms: List<Variable>, fragment: String
    ) : this(shaderName, null, vsUniforms, vertex, varying, fsUniforms, fragment)

    constructor(shaderName: String, geometry: String?, vertex: String, varying: List<Variable>, fragment: String) :
            this(shaderName, geometry, emptyList(), vertex, varying, emptyList(), fragment)

    constructor(shaderName: String, vertex: String, varying: List<Variable>, fragment: String) :
            this(shaderName, null, vertex, varying, fragment)

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

        val versionString = formatVersion(glslVersion) + "\n// $name\n"
        val geometryShader = if (geometryShader != null) {
            for (v in varyings) v.makeDifferent()
            var geo = versionString + geometryShader
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
                        "${it.modifiers} in ${it.type.glslName} ${it.vShaderName}[];\n" +
                        "${it.modifiers} out ${it.type.glslName} ${it.fShaderName};\n"
            })
            compile(name, program, GL_GEOMETRY_SHADER, geo)
        } else -1

        // the shaders are like a C compilation process, .o-files: after linking, they can be removed
        val builder = StringBuilder()
        builder.append(versionString)
        // todo only set them, if not already specified
        builder.append("precision mediump float;\n")
        builder.append("precision mediump int;\n")
        for (v in vertexVariables) {
            when (v.inOutMode) {
                VariableMode.ATTR -> {
                    builder.append(attribute)
                    builder.append(' ')
                }
                // todo inout...
                VariableMode.IN, VariableMode.INOUT -> {
                    builder.append("uniform ")
                }
                VariableMode.OUT -> {
                    builder.append("out ")
                }
            }
            builder.append(v.type.glslName)
            builder.append(' ')
            builder.append(v.name)
            builder.append(";\n")
        }
        for (v in varyings) {
            builder.append(v.modifiers)
            builder.append(" out ")
            builder.append(v.type.glslName)
            builder.append(' ')
            builder.append(v.vShaderName)
            builder.append(";\n")
        }
        builder.append(vertexShader.replaceVaryingNames(true, varyings))
        vertexSource = builder.toString()
        builder.clear()

        val vertexShader = compile(name, program, GL_VERTEX_SHADER, vertexSource)

        builder.append(versionString)
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
        for (v in fragmentVariables) {
            when (v.inOutMode) {
                VariableMode.IN, VariableMode.INOUT -> {
                    builder.append("uniform ")
                }
                VariableMode.ATTR -> throw IllegalArgumentException("Fragment variable must not have type ATTR")
                VariableMode.OUT -> {
                    builder.append("out ")
                }
            }
            builder.append(v.type.glslName)
            builder.append(' ')
            builder.append(v.name)
            builder.append(";\n")
        }
        val base =
            (if (!fragmentShader.contains("out ") &&
                glslVersion == DefaultGLSLVersion &&
                fragmentShader.contains("gl_FragColor") &&
                fragmentVariables.none { it.isOutput }
            ) {
                "" +
                        "out vec4 glFragColor;\n" +
                        fragmentShader.replace("gl_FragColor", "glFragColor")
            } else fragmentShader)
        builder.append(base.replaceVaryingNames(false, varyings))

        fragmentSource = builder.toString()
        builder.clear()

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