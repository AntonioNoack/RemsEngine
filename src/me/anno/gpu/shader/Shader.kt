package me.anno.gpu.shader

import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.shader.ShaderLib.matMul
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.builder.Varying
import me.anno.utils.structures.Compare.ifSame
import me.anno.utils.structures.lists.Lists.any2
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL46C

open class Shader(
    shaderName: String,
    val vertexVariables: List<Variable>,
    val vertexShader: String,
    val varyings: List<Variable>,
    val fragmentVariables: List<Variable>,
    val fragmentShader: String
) : GPUShader(shaderName) {

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

    var attributes = vertexVariables.filter { it.isAttribute }

    init {
        if (attributes.isEmpty() && ("in " in vertexShader || "attribute " in vertexShader)) {
            LOGGER.warn("Shader '$shaderName' should use Variables")
        }
        if (attributes.size > GFX.maxAttributes) {
            // when this happens, try to find a way to compact your data more,
            // or we'll have to find a way how we can bind buffers with per-index access,
            // or use textures for that...
            // my RTX 3070 has a limit of 16, my phone (Honor 10) has a limit of 8,
            // so 8 is probably a reasonable lower limit
            throw IllegalArgumentException(
                "Cannot use more than ${GFX.maxAttributes} attributes" +
                        " in $name, given: ${attributes.map { it.name }}"
            )
        }
    }

    var vertexSource = vertexShader
    var fragmentSource = fragmentShader

    init {
        val candidates = (vertexVariables.filter { it.type.isSampler } +
                fragmentVariables.filter { it.type.isSampler }).map { it.name }
        setTextureIndices(ArrayList(LinkedHashSet(candidates))) // removing duplicates, while keeping the order
    }

    override fun sourceContainsWord(word: String): Boolean {
        return word in vertexShader || word in fragmentShader
    }

    private fun appendPrecisions(variables: List<Variable>) {
        // these need default values, why ever...
        builder.append("precision highp float;\n")
        builder.append("precision highp int;\n")
        val types = HashSet<GLSLType>()
        for (variable in variables) {
            var type = variable.type
            if (!GFX.supportsDepthTextures) type = type.withoutShadow()
            if (types.add(type) && type.isSampler) {
                builder.append("precision highp ").append(type.glslName).append(";\n")
            }
        }
    }

    override fun compile() {

        val varyings = varyings.map {
            // matrix interpolation is not supported properly on my RTX3070. Although the value should be constant, the matrix is not.
            Varying(
                if (it.isFlat || it.type.isFlat || it.type.glslName.startsWith("mat")) "flat" else "",
                it.type, it.name
            )
        }

        if (glslVersion < 330 && fragmentVariables.any2 { it.isOutput }) {
            glslVersion = 330 // needed for layout(location=x) qualifier
        }

        if (glslVersion < 400 && ("gl_SampleID" in fragmentShader || "gl_SamplePosition" in fragmentShader)) {
            glslVersion = 400
        }

        val versionString = formatVersion(glslVersion) + "\n// $name\n"

        // the shaders are like a C compilation process, .o-files: after linking, they can be removed
        builder.clear()
        builder.append(versionString)

        for (line in vertexShader.split('\n')
            .filter { it.trim().startsWith("#extension ") }) {
            builder.append(line).append('\n')
        }

        appendPrecisions(vertexVariables)
        builder.append(matMul)

        for (v in vertexVariables) {
            val prefix = when (v.inOutMode) {
                VariableMode.ATTR -> attribute
                VariableMode.IN, VariableMode.INOUT, VariableMode.INMOD -> "uniform"
                VariableMode.OUT -> "out"
            }
            v.declare(builder, prefix, false)
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

        builder.append(versionString)
        for (extension in fragmentShader.split('\n')
            .filter { it.trim().startsWith("#extension ") }) {
            builder.append(extension).append('\n')
        }

        appendPrecisions(fragmentVariables)
        builder.append(matMul)

        for (v in varyings) {
            if (v.modifiers.isNotEmpty()) {
                builder.append(v.modifiers).append(" ")
            }
            builder.append("in ").append(v.type.glslName)
                .append(' ').append(v.fShaderName).append(";\n")
        }

        var outCtr = 0
        for (v in fragmentVariables.sortedWith { a, b ->
            a.type.compareTo(b.type).ifSame(a.name.compareTo(b.name))
        }) {
            val prefix = when (v.inOutMode) {
                VariableMode.IN, VariableMode.INOUT, VariableMode.INMOD -> "uniform"
                VariableMode.ATTR -> throw IllegalArgumentException("Fragment variable must not have type ATTR")
                VariableMode.OUT -> {
                    val slot = if (v.slot < 0) outCtr else v.slot
                    builder.append("layout(location=").append(slot).append(") ")
                    outCtr++
                    "out"
                }
            }
            v.declare(builder, prefix, false)
        }

        val base = if ((outCtr == 0 && "out " !in fragmentShader) && glslVersion == DefaultGLSLVersion &&
            "gl_FragColor" in fragmentShader && fragmentVariables.none { it.isOutput }// && !OS.isWeb // in WebGL, it is ok; only if version is low enough...
        ) "out vec4 glFragColor;\n" + fragmentShader.replace("gl_FragColor", "glFragColor")
        else fragmentShader

        if ("#extension" in base) builder.append(base.replace("#extension ", "// #extension "))
        else builder.append(base)

        fragmentSource = builder.toString()
        builder.clear()

        val program = GL46C.glCreateProgram()
        GFX.check()
        updateSession()
        GFX.check()
        /*val vertexShader = */
        compile(name, program, GL46C.GL_VERTEX_SHADER, vertexSource)

        GFX.check()

        /*val fragmentShader = */
        compile(name, program, GL46C.GL_FRAGMENT_SHADER, fragmentSource)

        GFX.check()

        val attributes = attributes
        for (i in attributes.indices) {
            GL46C.glBindAttribLocation(program, i, attributes[i].name)
        }

        GFX.check()

        GL46C.glLinkProgram(program)

        GFX.check()

        // glDeleteShader(vertexShader)
        // glDeleteShader(fragmentShader)
        // if (geometryShader >= 0) glDeleteShader(geometryShader)

        logShader(name, vertexSource, fragmentSource)

        GFX.check()

        postPossibleError(name, program, false, vertexSource, fragmentSource)
        GFX.check()

        this.program = program // only assign the program, when no error happened
        this.session = GFXState.session

        compileBindTextureNames()
        compileSetDebugLabel()

        // ^^
        use()
        if (hasUniform("tint")) {
            v4f("tint", 1f)
        }
    }

    fun getAttributeLocation(name: String): Int {
        return attributes.indexOfFirst { it.name == name }
    }

    override fun printLocationsAndValues() {
        super.printLocationsAndValues()
        for ((key, value) in attributes.withIndex()) {
            LOGGER.info("Attribute $key = $value")
        }
    }

    @Suppress("unused")
    fun printCode() {
        LOGGER.warn(formatShader(name, "", vertexSource, fragmentSource))
    }
}