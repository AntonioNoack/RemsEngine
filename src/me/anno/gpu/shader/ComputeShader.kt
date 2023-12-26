package me.anno.gpu.shader

import me.anno.Build
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.buffer.OpenGLBuffer
import me.anno.gpu.shader.ShaderLib.matMul
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture3D
import me.anno.maths.Maths.ceilDiv
import org.apache.logging.log4j.LogManager
import org.joml.Vector2i
import org.joml.Vector3i
import org.lwjgl.opengl.GL43.*
import org.lwjgl.opengl.GL43C

@Suppress("unused", "MemberVisibilityCanBePrivate")
class ComputeShader(
    shaderName: String,
    val version: Int,
    val groupSize: Vector3i,
    val source: String
) : OpenGLShader(shaderName) {

    constructor(shaderName: String, localSize: Vector2i, source: String) :
            this(shaderName, Vector3i(localSize, 1), source)

    constructor(shaderName: String, localSize: Vector3i, source: String) :
            this(shaderName, 430, localSize, source)

    constructor(shaderName: String, localSize: Vector2i, variables: List<Variable>, source: String) :
            this(shaderName, 430, Vector3i(localSize, 1), variables, source)

    constructor(shaderName: String, version: Int, localSize: Vector2i, variables: List<Variable>, source: String) :
            this(shaderName, version, Vector3i(localSize, 1), variables, source)

    constructor(shaderName: String, localSize: Vector3i, variables: List<Variable>, source: String) :
            this(shaderName, 430, localSize, variables, source)

    constructor(shaderName: String, version: Int, localSize: Vector3i, variables: List<Variable>, source: String) :
            this(shaderName, version, localSize,
                variables.joinToString("") {
                    when (it.inOutMode) {
                        VariableMode.IN -> {
                            val arr = if (it.arraySize >= 0) "[${it.arraySize}]" else ""
                            "uniform ${it.type.glslName}$arr ${it.name};\n"
                        }
                        else -> throw NotImplementedError()
                    }
                } + source)

    override fun compile() {

        checkGroupSizeBounds()
        val source = "" +
                "#version $version\n" +
                "// $name\n" +
                "layout(local_size_x = ${groupSize.x}, local_size_y = ${groupSize.y}, local_size_z = ${groupSize.z}) in;\n" +
                matMul +
                source

        updateSession()

        if (useShaderFileCache) {
            this.program = ShaderCache.createShader(source, null)
        } else {
            val program = glCreateProgram()
            /*val shader = */compile(name, program, GL_COMPUTE_SHADER, source)
            glLinkProgram(program)
            postPossibleError(name, program, false, source)
            // glDeleteShader(shader)
            logShader(name, source)
            GFX.check()
            this.program = program // only assign this value, when no error has occurred
            this.session = GFXState.session
        }

        if (Build.isDebug) {
            glObjectLabel(GL_PROGRAM, pointer, name)
        }
    }

    override fun sourceContainsWord(word: String): Boolean {
        return word in source
    }

    fun checkGroupSizeBounds() {
        if (groupSize.x < 1) groupSize.x = 1
        if (groupSize.y < 1) groupSize.y = 1
        if (groupSize.z < 1) groupSize.z = 1
        val groupSize = groupSize.x * groupSize.y * groupSize.z
        val maxGroupSize = stats[3]
        if (groupSize > maxGroupSize) throw RuntimeException(
            "Group size too large: ${this.groupSize.x} x ${this.groupSize.y} x ${this.groupSize.z} > $maxGroupSize"
        )
    }

    fun bindTexture(slot: Int, texture: Texture2D, mode: ComputeTextureMode) {
        bindTexture1(slot, texture, mode)
    }

    fun bindBuffer(slot: Int, buffer: OpenGLBuffer) {
        bindBuffer1(slot, buffer)
    }

    /**
     * for array textures to bind a single layer
     * */
    fun bindTexture(slot: Int, texture: Texture2D, mode: ComputeTextureMode, layer: Int) {
        bindTexture1(slot, texture, mode, layer)
    }

    fun bindTexture(slot: Int, texture: Texture3D, mode: ComputeTextureMode) {
        bindTexture1(slot, texture, mode)
    }

    companion object {

        @JvmStatic
        private val LOGGER = LogManager.getLogger(ComputeShader::class)

        @JvmStatic
        val stats by lazy {
            val tmp = IntArray(1)
            glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0, tmp)
            val sx = tmp[0]
            glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 1, tmp)
            val sy = tmp[0]
            glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 2, tmp)
            val sz = tmp[0]
            glGetIntegerv(GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS, tmp)
            val maxUnitsPerGroup = tmp[0]
            LOGGER.info("Max compute group count: $sx x $sy x $sz") // 65k³
            LOGGER.info("Max units per group: $maxUnitsPerGroup") // 1024
            intArrayOf(sx, sy, sz, maxUnitsPerGroup)
        }

        // texture type could be derived from the shader and texture -> verify it?
        // todo can we dynamically create shaders for the cases that we need? probably best :)
        @JvmStatic
        fun bindTexture1(slot: Int, texture: Texture2D, mode: ComputeTextureMode) {
            glBindImageTexture(slot, texture.pointer, 0, true, 0, mode.code, findFormat(texture.internalFormat))
        }

        @JvmStatic
        fun findFormat(format: Int) = when (format) {
            GL_RGBA32F, GL_RGBA16F, GL_RG32F, GL_RG16F,
            GL_R11F_G11F_B10F, GL_R32F, GL_R16F,
            GL_RGBA32UI, GL_RGBA16UI,
            GL_RGB10_A2UI, GL_RGBA8UI, GL_RG32UI,
            GL_RG16UI, GL_RG8UI, GL_R32UI, GL_R16UI, GL_R8UI,
            GL_RGBA32I, GL_RGBA16I, GL_RGBA8I,
            GL_RG32I, GL_RG16I, GL_RG8I, GL_R32I,
            GL_R16I, GL_R8I, GL_RGBA16, GL_RGB10_A2, GL_RGBA8,
            GL_RG16, GL_RG8, GL_R16, GL_R8,
            GL_RGBA16_SNORM, GL_RGBA8_SNORM, GL_RG16_SNORM, GL_RG8_SNORM,
            GL_R16_SNORM, GL_R8_SNORM -> format
            0 -> throw IllegalArgumentException("Texture hasn't been created yet")
            else -> throw IllegalArgumentException("Format ${GFX.getName(format)} is not supported in Compute shaders (glBindImageTexture), use a sampler!")
        }

        @JvmStatic
        fun bindBuffer1(slot: Int, buffer: OpenGLBuffer) {
            buffer.ensureBuffer()
            glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, slot, buffer.pointer)
        }

        /**
         * for array textures to bind a single layer
         * */
        @JvmStatic
        fun bindTexture1(slot: Int, texture: Texture2D, mode: ComputeTextureMode, layer: Int) {
            glBindImageTexture(slot, texture.pointer, 0, false, layer, mode.code, findFormat(texture.internalFormat))
        }

        @JvmStatic
        fun bindTexture1(slot: Int, texture: Texture3D, mode: ComputeTextureMode) {
            glBindImageTexture(slot, texture.pointer, 0, true, 0, mode.code, findFormat(texture.internalFormat))
        }
    }

    fun runBySize(width: Int, height: Int = 1, depth: Int = 1) {
        runByGroups(ceilDiv(width, groupSize.x), ceilDiv(height, groupSize.y), ceilDiv(depth, groupSize.z))
    }

    fun runByGroups(widthGroups: Int, heightGroups: Int = 1, depthGroups: Int = 1) {
        if (lastProgram != program) {
            glUseProgram(program)
            lastProgram = program
        }
        val maxGroupSize = stats
        if (widthGroups > maxGroupSize[0] || heightGroups > maxGroupSize[1] || depthGroups > maxGroupSize[2]) {
            throw IllegalArgumentException(
                "Group count out of bounds: ($widthGroups x $heightGroups x $depthGroups x GroupSize) > " +
                        "(${maxGroupSize.joinToString(" x ")})"
            )
        }
        glDispatchCompute(widthGroups, heightGroups, depthGroups)
        // currently true, but that might change, if we just write to data buffers or similar
        Texture2D.wasModifiedInComputePipeline = true
    }
}