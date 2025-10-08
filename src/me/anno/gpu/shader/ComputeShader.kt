package me.anno.gpu.shader

import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GLNames
import me.anno.gpu.shader.ComputeShaderStats.Companion.stats
import me.anno.gpu.shader.Shader.Companion.builder
import me.anno.gpu.shader.ShaderLib.matMul
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture3D
import me.anno.maths.Maths.ceilDiv
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Strings.splitLines
import org.apache.logging.log4j.LogManager
import org.joml.Vector3i
import org.lwjgl.opengl.GL46C

@Suppress("unused", "MemberVisibilityCanBePrivate")
class ComputeShader(
    shaderName: String,
    val version: Int,
    val groupSize: Vector3i,
    val source: String
) : GPUShader(shaderName, UniformCacheSize) {

    constructor(shaderName: String, localSize: Vector3i, variables: List<Variable>, source: String) :
            this(shaderName, 430, localSize, variables, source)

    constructor(shaderName: String, version: Int, localSize: Vector3i, variables: List<Variable>, source: String) :
            this(
                shaderName, version, localSize,
                variables.joinToString("") {
                    when (it.inOutMode) {
                        VariableMode.IN -> {
                            val tmp = StringBuilder()
                            it.declare(tmp, "uniform", false)
                            tmp.toString()
                        }
                        else -> throw NotImplementedError()
                    }
                } + source)

    override fun compile() {

        checkGroupSizeBounds()

        builder.clear()
        builder.append("#version ").append(version).append('\n')
        for (line in source.splitLines()
            .filter { it.trim().startsWith("#extension ") }) {
            builder.append(line).append('\n')
        }

        builder.append("// ").append(name).append('\n')
        builder.append("layout(local_size_x = ${groupSize.x}, local_size_y = ${groupSize.y}, local_size_z = ${groupSize.z}) in;\n")
        builder.append(matMul)
        builder.append(source.replace("#extension", "// #extension"))

        val source = builder.toString()
        builder.clear()

        updateSession()

        val program = GL46C.glCreateProgram()
        /*val shader = */compile(name, program, GL46C.GL_COMPUTE_SHADER, source)
        GL46C.glLinkProgram(program)
        postPossibleError(name, program, false, source)
        // glDeleteShader(shader)
        logShader(name, source)
        GFX.check()
        this.program = program // only assign this value, when no error has occurred
        this.session = GFXState.session

        compileBindTextureNames()
        compileSetDebugLabel()
    }

    override fun sourceContainsWord(word: String): Boolean {
        return word in source
    }

    fun checkGroupSizeBounds() {
        if (groupSize.x < 1) groupSize.x = 1
        if (groupSize.y < 1) groupSize.y = 1
        if (groupSize.z < 1) groupSize.z = 1
        val groupSizeI = groupSize.x * groupSize.y * groupSize.z
        val maxGroupSize = stats.maxUnitsPerGroup
        assertTrue(groupSizeI <= maxGroupSize) {
            "Group size too large: ${groupSize.x} x ${groupSize.y} x ${groupSize.z} > $maxGroupSize"
        }
    }

    fun bindTexture(slot: Int, texture: Texture2D, mode: ComputeTextureMode) {
        // texture type could be derived from the shader and texture -> verify it?
        // todo can we dynamically create shaders for the cases that we need? probably best :)
        GL46C.glBindImageTexture(slot, texture.pointer, 0, true, 0, mode.code, findFormat(texture.internalFormat))
    }

    /**
     * for array textures to bind a single layer
     * */
    fun bindTexture(slot: Int, texture: Texture2D, mode: ComputeTextureMode, layer: Int) {
        GL46C.glBindImageTexture(
            slot, texture.pointer, 0, false,
            layer, mode.code, findFormat(texture.internalFormat)
        )
    }

    fun bindTexture(slot: Int, texture: Texture3D, mode: ComputeTextureMode) {
        GL46C.glBindImageTexture(
            slot, texture.pointer, 0, true,
            0, mode.code, findFormat(texture.internalFormat)
        )
    }

    companion object {

        @JvmStatic
        private val LOGGER = LogManager.getLogger(ComputeShader::class)

        @JvmStatic
        fun findFormat(format: Int) = when (format) {
            GL46C.GL_RGBA32F, GL46C.GL_RGBA16F, GL46C.GL_RG32F, GL46C.GL_RG16F,
            GL46C.GL_R11F_G11F_B10F, GL46C.GL_R32F, GL46C.GL_R16F,
            GL46C.GL_RGBA32UI, GL46C.GL_RGBA16UI,
            GL46C.GL_RGB10_A2UI, GL46C.GL_RGBA8UI, GL46C.GL_RG32UI,
            GL46C.GL_RG16UI, GL46C.GL_RG8UI, GL46C.GL_R32UI, GL46C.GL_R16UI, GL46C.GL_R8UI,
            GL46C.GL_RGBA32I, GL46C.GL_RGBA16I, GL46C.GL_RGBA8I,
            GL46C.GL_RG32I, GL46C.GL_RG16I, GL46C.GL_RG8I, GL46C.GL_R32I,
            GL46C.GL_R16I, GL46C.GL_R8I, GL46C.GL_RGBA16, GL46C.GL_RGB10_A2, GL46C.GL_RGBA8,
            GL46C.GL_RG16, GL46C.GL_RG8, GL46C.GL_R16, GL46C.GL_R8,
            GL46C.GL_RGBA16_SNORM, GL46C.GL_RGBA8_SNORM, GL46C.GL_RG16_SNORM, GL46C.GL_RG8_SNORM,
            GL46C.GL_R16_SNORM, GL46C.GL_R8_SNORM -> format
            // depth formats are not supported! bind a color texture instead, and transfer the data from and to it...
            0 -> throw IllegalArgumentException("Texture hasn't been created yet")
            else -> throw IllegalArgumentException("Format ${GLNames.getName(format)} is not supported in Compute shaders (glBindImageTexture), use a sampler!")
        }
    }

    fun runBySize(width: Int, height: Int = 1, depth: Int = 1) {
        runByGroups(ceilDiv(width, groupSize.x), ceilDiv(height, groupSize.y), ceilDiv(depth, groupSize.z))
    }

    fun runByGroups(widthGroups: Int, heightGroups: Int = 1, depthGroups: Int = 1) {
        use()
        val stats = stats
        if (widthGroups > stats.sx || heightGroups > stats.sy || depthGroups > stats.sz) {
            throw IllegalArgumentException(
                "Group count out of bounds: ($widthGroups x $heightGroups x $depthGroups x GroupSize) > " +
                        "(${stats.sx} x ${stats.sy} x ${stats.sz})"
            )
        }
        GL46C.glDispatchCompute(widthGroups, heightGroups, depthGroups)
        // currently true, but that might change, if we just write to data buffers or similar
        Texture2D.wasModifiedInComputePipeline = true
    }

    fun printCode() {
        LOGGER.warn(formatShader(name, "", source, ""))
    }
}