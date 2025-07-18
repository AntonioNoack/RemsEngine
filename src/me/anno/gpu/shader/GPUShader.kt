package me.anno.gpu.shader

import me.anno.Build
import me.anno.cache.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.OpenGLBuffer
import me.anno.gpu.shader.builder.Variable
import me.anno.io.files.FileReference
import me.anno.maths.Maths.sq
import me.anno.ui.editor.files.FileNames.toAllowedFilename
import me.anno.utils.GFXFeatures
import me.anno.utils.OS
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.structures.maps.BiMap
import me.anno.utils.types.Strings.countLines
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.isNotBlank2
import org.apache.logging.log4j.LogManager
import org.joml.Matrix2f
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Matrix4x3
import org.joml.Matrix4x3f
import org.joml.Planed
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4d
import org.joml.Vector4f
import org.joml.Vector4i
import org.lwjgl.opengl.GL46C
import org.lwjgl.opengl.GL46C.GL_LINK_STATUS
import org.lwjgl.opengl.GL46C.GL_PROGRAM
import org.lwjgl.opengl.GL46C.GL_SHADER
import org.lwjgl.opengl.GL46C.glAttachShader
import org.lwjgl.opengl.GL46C.glCompileShader
import org.lwjgl.opengl.GL46C.glCreateShader
import org.lwjgl.opengl.GL46C.glDeleteProgram
import org.lwjgl.opengl.GL46C.glGetProgramInfoLog
import org.lwjgl.opengl.GL46C.glGetProgrami
import org.lwjgl.opengl.GL46C.glGetShaderInfoLog
import org.lwjgl.opengl.GL46C.glGetUniformLocation
import org.lwjgl.opengl.GL46C.glObjectLabel
import org.lwjgl.opengl.GL46C.glShaderSource
import org.lwjgl.opengl.GL46C.glUniform1f
import org.lwjgl.opengl.GL46C.glUniform1fv
import org.lwjgl.opengl.GL46C.glUniform1i
import org.lwjgl.opengl.GL46C.glUniform2f
import org.lwjgl.opengl.GL46C.glUniform2fv
import org.lwjgl.opengl.GL46C.glUniform2i
import org.lwjgl.opengl.GL46C.glUniform3f
import org.lwjgl.opengl.GL46C.glUniform3fv
import org.lwjgl.opengl.GL46C.glUniform3i
import org.lwjgl.opengl.GL46C.glUniform4f
import org.lwjgl.opengl.GL46C.glUniform4fv
import org.lwjgl.opengl.GL46C.glUniform4i
import org.lwjgl.opengl.GL46C.glUniformMatrix2fv
import org.lwjgl.opengl.GL46C.glUniformMatrix3fv
import org.lwjgl.opengl.GL46C.glUniformMatrix4fv
import org.lwjgl.opengl.GL46C.glUniformMatrix4x3fv
import org.lwjgl.opengl.GL46C.glUseProgram
import java.nio.FloatBuffer

/**
 * base class for shaders, e.g. for ComputeShader or Shader
 * */
abstract class GPUShader(val name: String, uniformCacheSize: Int) : ICacheData {

    companion object {

        var logShaders = false
        var useShaderFileCache = false

        private val LOGGER = LogManager.getLogger(GPUShader::class)

        /** how attributes are called; is attribute in WebGL */
        val attribute get() = "in" // if(OS.isWeb) "attribute" else "in"

        private val matrixBuffer = ByteBufferPool.allocateDirect(16 * 4).asFloatBuffer()
        private val identity2f = Matrix2f()
        private val identity3f = Matrix3f()
        private val identity4f = Matrix4f()
        private val identity4x3f = Matrix4x3f()
        private val identity4x3m = Matrix4x3()

        const val DEFAULT_GLSL_VERSION = 150

        var UniformCacheSize = 256
        var safeShaderBinding = false
        var lastProgram = -1
        var showUniformWarnings = false

        fun invalidateBinding() {
            lastProgram = -1
        }

        fun formatVersion(version: Int): String {
            return when {
                // ERROR: 0:1: '150' : client/version number not supported
                // todo how do we find out which version is supported in WebGL?
                OS.isWeb -> "#version 300 es\n" // WebGL is limited to exactly 300
                GFXFeatures.isOpenGLES -> "#version $version es\n"
                else -> "#version $version\n"
            }
        }

        // needs to be cleared when the opengl session changes
        private val shaderCache = HashMap<Pair<Int, String>, Int>(256)
        private var shaderCacheSession = -1

        fun compile(shaderName: String, program: Int, type: Int, source: String): Int {
            if (shaderCacheSession != GFXState.session) {
                shaderCacheSession = GFXState.session
                shaderCache.clear()
            }
            val shader = shaderCache.getOrPut(type to source) {
                compileShader(type, source, shaderName)
            }
            glAttachShader(program, shader)
            postPossibleError(shaderName, shader, true, source)
            return shader
        }

        fun compileShader(type: Int, source: String, name: String): Int {
            val shader = glCreateShader(type)
            glShaderSource(shader, source)
            glCompileShader(shader)
            if (Build.isDebug) glObjectLabel(GL_SHADER, shader, name)
            return shader
        }

        // shader loading from SPIR-V is an option for the future for faster load times :)
        /*fun compile2(shaderName: String, program: Int, type: Int, source: ByteBuffer, entryPoint: String): Int {
            val shader = glCreateShader(type)
            glShaderBinary(intArrayOf(0), shader, source)
            glSpecializeShader(shader, entryPoint, null as IntBuffer?, null)
            glAttachShader(program, shader)
            postPossibleError(shaderName, shader, true, "")
            return shader
        }*/

        private fun addText(warning: StringBuilder, s0: String): Int {
            var idx = 1
            var i = 0
            while (i < s0.length) {
                val i0 = i
                i = s0.indexOf('\n', i)
                if (i < 0) i = s0.length
                if (idx < 100) warning.append(' ')
                if (idx < 10) warning.append(' ')
                warning.append(idx).append(": ")
                warning.append(s0, i0, i).append('\n')
                idx++
                i++
            }
            return idx
        }

        fun postPossibleError(shaderName: String, shader: Int, isShader: Boolean, s0: String, s1: String = "") {
            val log: String? = if (isShader) {
                glGetShaderInfoLog(shader)
            } else {
                glGetProgramInfoLog(shader)
            }
            if (log != null && !log.isBlank2()) {
                if (hasErrorMessages(log)) {
                    LOGGER.warn(formatShader(shaderName, log, s0, s1))
                } else {
                    LOGGER.warn("$shaderName: $log")
                }
            }
            if (!isShader) {
                if (glGetProgrami(shader, GL_LINK_STATUS) == 0) {
                    throw IllegalStateException("Linking $shader failed")
                }
            }
        }

        private fun hasErrorMessages(log: String): Boolean {
            return log.split('\n').any2 { line ->
                line.isNotBlank2() && ": warning " !in line
            }
        }

        fun formatShader(shaderName: String, log: String, s0: String, s1: String): StringBuilder {
            val dst = StringBuilder( // estimate size to prevent unnecessary allocations
                shaderName.length + log.length + 6 + // intro line
                        s0.length + s1.length +
                        (countLines(s0) + countLines(s1)) * 6 // 4 for number, 2 for :+space
            )
            if (log.isEmpty()) {
                dst.append(shaderName).append(":\n")
            } else {
                dst.append(log).append(" by ").append(shaderName).append("\n\n")
            }
            addText(dst, s0)
            addText(dst, s1)
            return dst
        }

        fun getLogFolder(): FileReference {
            val folder = OS.desktop.getChild("shaders")
            folder.tryMkdirs()
            return folder
        }

        fun print(shaderName: String, folder: FileReference, ext: String, data: String) {
            val name = "$shaderName.$ext".toAllowedFilename() ?: return
            folder.getChild(name).writeText(data)
        }

        fun logShader(shaderName: String, vertex: String, fragment: String) {
            if (logShaders) {
                val folder = getLogFolder()
                print(shaderName, folder, "vert", vertex)
                print(shaderName, folder, "frag", fragment)
            }
        }

        fun logShader(shaderName: String, comp: String) {
            if (logShaders) {
                val folder = getLogFolder()
                print(shaderName, folder, "comp", comp)
            }
        }
    }

    private val uniformCache = FloatArray(uniformCacheSize * 4)

    val uniformLocations = BiMap<String, Int>()
    val uniformTypes = HashMap<String, Variable>()
    var textureNames: List<String> = emptyList()

    var safeShaderBinding = Companion.safeShaderBinding

    var glslVersion = DEFAULT_GLSL_VERSION

    var program = 0
    var session = 0

    // todo this should be set automatically
    var failedCompilation = false

    /**
     * binds the shader, and returns whether this shader was actually bound
     * */
    fun use(): Boolean {
        GFX.check()
        // Frame.bindMaybe()
        GFX.check()
        if (program == 0 || session != GFXState.session) {
            clearState()
            compile()
        }
        if (program == 0) throw IllegalStateException("Program is 0 after compilation")
        return if (program != lastProgram) {
            glUseProgram(program)
            GFX.check()
            lastProgram = program
            true
        } else false
    }

    fun clearState() {
        uniformCache.fill(0f)
        uniformLocations.clear()
    }

    fun checkIsUsed() {
        if (program == 0 || session != GFXState.session) {
            LOGGER.error("Program $name isn't even created")
            use()
            return
        }
        if (program != lastProgram) {
            LOGGER.error("Program $name isn't used")
            use()
            return
        }
    }

    fun checkUniformType(loc: Int, type: GLSLType, isArray: Boolean) {
        if (Build.isDebug && loc >= 0) {
            val present = uniformTypes[uniformLocations.reverse[loc]]
            val ok = present == null ||
                    (typeToOpenGLFuncType(present.type) == type && present.isArray == isArray)
            if (!ok) {
                throw IllegalArgumentException("Cannot set uniform to $type[$isArray] to $present")
            }
        }
    }

    private fun typeToOpenGLFuncType(presentType: GLSLType): GLSLType {
        return when (presentType) {
            GLSLType.V1B -> GLSLType.V1I
            GLSLType.V2B -> GLSLType.V2I
            GLSLType.V3B -> GLSLType.V3I
            GLSLType.V4B -> GLSLType.V4I
            else -> presentType
        }
    }

    abstract fun compile()
    abstract fun sourceContainsWord(word: String): Boolean

    fun setTextureIndicesIfExisting() {
        for ((index, name) in textureNames.withIndex()) {
            val location = getUniformLocation(name)
            if (location >= 0) glUniform1i(location, index)
        }
    }

    fun getTextureIndex(name: String): Int {
        return textureNames.indexOf(name)
    }

    fun compileSetDebugLabel() {
        if (Build.isDebug) {
            glObjectLabel(GL_PROGRAM, program, name)
        }
    }

    fun compileBindTextureNames() {
        if (textureNames.isNotEmpty()) {
            use()
            setTextureIndicesIfExisting()
            GFX.check()
        }
    }

    fun updateSession() {
        session = GFXState.session
        uniformLocations.clear()
        uniformCache.fill(0f)
    }

    @Suppress("unused")
    open fun printLocationsAndValues() {
        for ((key, value) in uniformLocations.entries.sortedBy { it.value }) {
            LOGGER.info("Uniform $key[$value] = (${uniformCache[value * 4]},${uniformCache[value * 4 + 1]},${uniformCache[value * 4 + 2]},${uniformCache[value * 4 + 3]})")
        }
    }

    @Suppress("unused")
    fun invalidateCacheForTests() {
        uniformLocations.clear()
        uniformCache.fill(Float.NaN)
    }

    fun setTextureIndices(vararg textures: String): GPUShader =
        setTextureIndices(textures.toList())

    fun setTextureIndices(textures: List<String>?): GPUShader {
        if (textures == null) return this
        if (program != 0) {
            use()
            textureNames = textures
            for ((index, name) in textures.withIndex()) {
                if (',' in name) throw IllegalArgumentException("Name must not contain comma!")
                val texName = getUniformLocation(name)
                if (texName >= 0) {
                    glUniform1i(texName, index)
                }
            }
        } else {
            textureNames = textures
        }
        return this
    }

    fun getUniformLocation(name: String, warnIfMissing: Boolean = true): Int {
        return uniformLocations.getOrPut(name) {
            if (safeShaderBinding) use()
            val loc = glGetUniformLocation(program, name)
            if (showUniformWarnings && loc < 0 && warnIfMissing && !sourceContainsWord(name)) {
                LOGGER.warn("Uniform location \"$name\" not found in shader \"${this.name}\"")
            }
            loc
        }
    }

    fun potentiallyUse() {
        if (safeShaderBinding) {
            if (use()) {
                throw IllegalStateException("Shader $name wasn't bound!")
            }
        }
    }

    fun v1b(name: String, x: Boolean) = v1i(name, if (x) 1 else 0)
    fun v1b(loc: Int, x: Boolean) = v1i(loc, if (x) 1 else 0)
    fun v1i(name: String, x: Int) = v1i(getUniformLocation(name), x)
    fun v1i(loc: Int, x: Int) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.V1I, false)
            val index0 = loc * 4
            val uniformCache = uniformCache
            if (index0 >= uniformCache.size) {
                potentiallyUse()
                glUniform1i(loc, x)
            } else {
                val xf = Float.fromBits(x)
                if (uniformCache[index0] != xf) {
                    // it has changed
                    uniformCache[index0] = xf
                    potentiallyUse()
                    glUniform1i(loc, x)
                }
            }
        }
    }

    fun v1f(name: String, x: Double) = v1f(name, x.toFloat())
    fun v1f(name: String, x: Float) = v1f(getUniformLocation(name), x)
    fun v1f(loc: Int, x: Float) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.V1F, false)
            val index0 = loc * 4
            val uniformCache = uniformCache
            if (index0 >= uniformCache.size) {
                potentiallyUse()
                glUniform1f(loc, x)
            } else if (uniformCache[index0] != x) {
                uniformCache[index0] = x
                potentiallyUse()
                glUniform1f(loc, x)
            }
        }
    }

    /**
     * sets an array of float uniforms
     * */
    fun v1fs(name: String, vs: FloatArray) = v1fs(getUniformLocation(name), vs)
    fun v1fs(loc: Int, vs: FloatArray) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.V1F, true)
            potentiallyUse()
            glUniform1fv(loc, vs)
        }
    }

    @Suppress("unused")
    fun v1fs(name: String, value: FloatBuffer) = v1fs(getUniformLocation(name), value)
    fun v1fs(loc: Int, value: FloatBuffer) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.V1F, true)
            potentiallyUse()
            glUniform1fv(loc, value)
        }
    }

    fun v2f(name: String, x: Float, y: Float) = v2f(getUniformLocation(name), x, y)
    fun v2f(loc: Int, x: Float, y: Float) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.V2F, false)
            val index0 = loc * 4
            val uniformCache = uniformCache
            if (index0 >= uniformCache.size) {
                potentiallyUse()
                glUniform2f(loc, x, y)
            } else if (uniformCache[index0 + 0] != x || uniformCache[index0 + 1] != y) {
                uniformCache[index0 + 0] = x
                uniformCache[index0 + 1] = y
                potentiallyUse()
                glUniform2f(loc, x, y)
            }
        }
    }

    /**
     * sets an array of vec2 uniforms
     * */
    @Suppress("unused")
    fun v2fs(name: String, vs: FloatArray) = v2fs(getUniformLocation(name), vs)
    fun v2fs(loc: Int, vs: FloatArray) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.V2F, true)
            potentiallyUse()
            glUniform2fv(loc, vs)
        }
    }

    @Suppress("unused")
    fun v2fs(name: String, vs: FloatBuffer) = this.v2fs(getUniformLocation(name), vs)
    fun v2fs(loc: Int, vs: FloatBuffer) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.V2F, true)
            potentiallyUse()
            glUniform2fv(loc, vs)
        }
    }

    fun v2i(name: String, x: Int, y: Int = x) = v2i(getUniformLocation(name), x, y)
    fun v2i(loc: Int, x: Int, y: Int = x) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.V2I, false)
            val index0 = loc * 4
            val uniformCache = uniformCache
            if (index0 >= uniformCache.size) {
                potentiallyUse()
                glUniform2i(loc, x, y)
            } else {
                val xf = Float.fromBits(x)
                val yf = Float.fromBits(y)
                if (uniformCache[index0] != xf || uniformCache[index0 + 1] != yf) {
                    // it has changed
                    uniformCache[index0] = xf
                    uniformCache[index0 + 1] = yf
                    potentiallyUse()
                    glUniform2i(loc, x, y)
                }
            }
        }
    }

    fun v3f(name: String, x: Float, y: Float, z: Float) = v3f(getUniformLocation(name), x, y, z)
    fun v3f(loc: Int, x: Float, y: Float, z: Float) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.V3F, false)
            val index0 = loc * 4
            val uniformCache = uniformCache
            if (index0 >= uniformCache.size) {
                potentiallyUse()
                glUniform3f(loc, x, y, z)
            } else if (uniformCache[index0] != x || uniformCache[index0 + 1] != y || uniformCache[index0 + 2] != z) {
                uniformCache[index0] = x
                uniformCache[index0 + 1] = y
                uniformCache[index0 + 2] = z
                potentiallyUse()
                glUniform3f(loc, x, y, z)
            }
        }
    }

    /**
     * sets an array of vec3 uniforms
     * */
    @Suppress("unused")
    fun v3fs(name: String, vs: FloatArray) = v3fs(getUniformLocation(name), vs)
    fun v3fs(loc: Int, vs: FloatArray) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.V3F, true)
            potentiallyUse()
            glUniform3fv(loc, vs)
        }
    }

    @Suppress("unused")
    fun v3fs(name: String, vs: FloatBuffer) = this.v3fs(getUniformLocation(name), vs)
    fun v3fs(loc: Int, vs: FloatBuffer) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.V3F, true)
            potentiallyUse()
            glUniform3fv(loc, vs)
        }
    }

    @Suppress("unused")
    fun v3X(loc: Int, x: Float, y: Float, z: Float, w: Float) = v3f(loc, x / w, y / w, z / w)

    @Suppress("unused")
    fun v3X(name: String, x: Float, y: Float, z: Float, w: Float) = v3f(name, x / w, y / w, z / w)

    @Suppress("unused")
    fun v3X(loc: Int, v: Vector4f) = v3f(loc, v.x / v.w, v.y / v.w, v.z / v.w)

    @Suppress("unused")
    fun v3X(name: String, v: Vector4f) = v3f(name, v.x / v.w, v.y / v.w, v.z / v.w)

    fun v3f(name: String, color: Int) = v3f(getUniformLocation(name), color)
    fun v3f(loc: Int, color: Int) {
        v3f(
            loc,
            (color.shr(16) and 255) / 255f,
            (color.shr(8) and 255) / 255f,
            color.and(255) / 255f
        )
    }

    fun v3i(name: String, x: Int, y: Int, z: Int) = v3i(getUniformLocation(name), x, y, z)
    fun v3i(loc: Int, x: Int, y: Int, z: Int) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.V3I, false)
            val index0 = loc * 4
            val uniformCache = uniformCache
            if (index0 >= uniformCache.size) {
                potentiallyUse()
                glUniform3i(loc, x, y, z)
            } else {
                val xf = Float.fromBits(x)
                val yf = Float.fromBits(y)
                val zf = Float.fromBits(z)
                if (uniformCache[index0] != xf || uniformCache[index0 + 1] != yf || uniformCache[index0 + 2] != zf) {
                    // it has changed
                    uniformCache[index0] = xf
                    uniformCache[index0 + 1] = yf
                    uniformCache[index0 + 2] = zf
                    potentiallyUse()
                    glUniform3i(loc, x, y, z)
                }
            }
        }
    }

    fun v4f(name: String, x: Float, y: Float, z: Float, w: Float) =
        v4f(getUniformLocation(name), x, y, z, w)

    fun v4f(loc: Int, x: Float, y: Float, z: Float, w: Float) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.V4F, false)
            val index0 = loc * 4
            val uniformCache = uniformCache
            if (index0 >= uniformCache.size) {
                potentiallyUse()
                glUniform4f(loc, x, y, z, w)
            } else if (
                uniformCache[index0] != x || uniformCache[index0 + 1] != y ||
                uniformCache[index0 + 2] != z || uniformCache[index0 + 3] != w
            ) {
                uniformCache[index0] = x
                uniformCache[index0 + 1] = y
                uniformCache[index0 + 2] = z
                uniformCache[index0 + 3] = w
                potentiallyUse()
                glUniform4f(loc, x, y, z, w)
            }
        }
    }

    fun v4f(name: String, color: Int) = v4f(getUniformLocation(name), color)
    fun v4f(loc: Int, color: Int) {
        if (loc >= 0) v4f(
            loc,
            (color.shr(16) and 255) / 255f,
            (color.shr(8) and 255) / 255f,
            color.and(255) / 255f,
            (color.shr(24) and 255) / 255f
        )
    }

    /**
     * sets an array of vec4 uniforms
     * */
    fun v4fs(name: String, vs: FloatArray) = v4fs(getUniformLocation(name), vs)
    fun v4fs(loc: Int, vs: FloatArray) {
        if (loc >= 0) {
            checkUniformType(loc, GLSLType.V4F, true)
            potentiallyUse()
            glUniform4fv(loc, vs)
        }
    }

    @Suppress("unused")
    fun v4fs(name: String, vs: FloatBuffer) = this.v4fs(getUniformLocation(name), vs)
    fun v4fs(loc: Int, vs: FloatBuffer) {
        if (loc >= 0) {
            checkUniformType(loc, GLSLType.V4F, true)
            potentiallyUse()
            glUniform4fv(loc, vs)
        }
    }

    fun v4fSq(name: String, color: Int) = v4fSq(getUniformLocation(name), color)
    fun v4fSq(loc: Int, color: Int) {
        v4f(
            loc,
            sq((color.shr(16) and 255) / 255f),
            sq((color.shr(8) and 255) / 255f),
            sq(color.and(255) / 255f),
            sq((color.shr(24) and 255) / 255f)
        )
    }

    fun v4f(name: String, color: Vector3f, alpha: Float) = v4f(getUniformLocation(name), color, alpha)
    fun v4f(loc: Int, color: Vector3f, alpha: Float) =
        v4f(loc, color.x, color.y, color.z, alpha)

    fun v4f(name: String, color: Int, alpha: Float) = v4f(getUniformLocation(name), color, alpha)
    fun v4f(loc: Int, color: Int, alpha: Float) {
        v4f(
            loc,
            color.shr(16).and(255) / 255f,
            color.shr(8).and(255) / 255f,
            color.and(255) / 255f, alpha
        )
    }

    fun v4i(name: String, x: Int, y: Int, z: Int, w: Int) = v4i(getUniformLocation(name), x, y, z, w)
    fun v4i(loc: Int, x: Int, y: Int, z: Int, w: Int) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.V4I, false)
            val index0 = loc * 4
            val uniformCache = uniformCache
            if (index0 >= uniformCache.size) {
                potentiallyUse()
                glUniform4i(loc, x, y, z, w)
            } else {
                val xf = Float.fromBits(x)
                val yf = Float.fromBits(y)
                val zf = Float.fromBits(z)
                val wf = Float.fromBits(w)
                if (uniformCache[index0] != xf || uniformCache[index0 + 1] != yf ||
                    uniformCache[index0 + 2] != zf || uniformCache[index0 + 3] != wf
                ) {
                    // it has changed
                    uniformCache[index0] = xf
                    uniformCache[index0 + 1] = yf
                    uniformCache[index0 + 2] = zf
                    uniformCache[index0 + 3] = wf
                    potentiallyUse()
                    glUniform4i(loc, x, y, z, w)
                }
            }
        }
    }

    fun v2f(loc: Int, all: Float) = v2f(loc, all, all)
    fun v3f(loc: Int, all: Float) = v3f(loc, all, all, all)
    fun v4f(loc: Int, all: Float) = v4f(loc, all, all, all, all)

    fun v2i(loc: Int, all: Int) = v2i(loc, all, all)
    fun v3i(loc: Int, all: Int) = v3i(loc, all, all, all)
    fun v4i(loc: Int, all: Int) = v4i(loc, all, all, all, all)

    fun v2f(loc: Int, v: Vector2f) = v2f(loc, v.x, v.y)
    fun v3f(loc: Int, v: Vector3f) = v3f(loc, v.x, v.y, v.z)
    fun v4f(loc: Int, v: Vector4f) = v4f(loc, v.x, v.y, v.z, v.w)
    fun v4f(loc: Int, v: Quaternionf) = v4f(loc, v.x, v.y, v.z, v.w)
    fun v4f(loc: Int, v: Quaterniond) = v4f(loc, v.x.toFloat(), v.y.toFloat(), v.z.toFloat(), v.w.toFloat())

    fun v2i(loc: Int, v: Vector2i) = v2i(loc, v.x, v.y)
    fun v3i(loc: Int, v: Vector3i) = v3i(loc, v.x, v.y, v.z)
    fun v4i(loc: Int, v: Vector4i) = v4i(loc, v.x, v.y, v.z, v.w)

    fun v2i(loc: String, v: Vector2i) = v2i(loc, v.x, v.y)
    fun v3i(loc: String, v: Vector3i) = v3i(loc, v.x, v.y, v.z)
    fun v4i(loc: String, v: Vector4i) = v4i(loc, v.x, v.y, v.z, v.w)

    fun v2f(name: String, all: Float) = v2f(name, all, all)
    fun v3f(name: String, all: Float) = v3f(name, all, all, all)
    fun v4f(name: String, all: Float) = v4f(name, all, all, all, all)

    fun v2i(name: String, all: Int) = v2i(name, all, all)
    fun v3i(name: String, all: Int) = v3i(name, all, all, all)
    fun v4i(name: String, all: Int) = v4i(name, all, all, all, all)

    fun v2f(name: String, v: Vector2f) = v2f(name, v.x, v.y)
    fun v3f(name: String, v: Vector3f) = v3f(name, v.x, v.y, v.z)
    fun v3f(name: String, v: Vector3d) = v3f(name, v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
    fun v4f(name: String, v: Vector4f) = v4f(name, v.x, v.y, v.z, v.w)
    fun v4f(name: String, v: Planed) =
        v4f(name, v.dirX.toFloat(), v.dirY.toFloat(), v.dirZ.toFloat(), v.distance.toFloat())

    fun v4f(name: String, v: Quaternionf) = v4f(name, v.x, v.y, v.z, v.w)
    fun v4f(name: String, v: Quaterniond) = v4f(name, v.x.toFloat(), v.y.toFloat(), v.z.toFloat(), v.w.toFloat())
    fun v4f(name: String, v: Vector4d) =
        v4f(name, v.x.toFloat(), v.y.toFloat(), v.z.toFloat(), v.w.toFloat())

    fun m2x2(name: String, value: Matrix2f?) = m2x2(getUniformLocation(name), value)
    fun m2x2(loc: Int, value: Matrix2f? = null) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.M2x2, false)
            potentiallyUse()
            val matrixBuffer = matrixBuffer
            matrixBuffer.position(0).limit(4)
            (value ?: identity2f).putInto(matrixBuffer)
            matrixBuffer.flip()
            glUniformMatrix2fv(loc, false, matrixBuffer)
        }
    }

    fun m3x3(name: String, value: Matrix3f?) = m3x3(getUniformLocation(name), value)
    fun m3x3(loc: Int, value: Matrix3f? = null) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.M3x3, false)
            potentiallyUse()
            val matrixBuffer = matrixBuffer
            matrixBuffer.position(0).limit(9)
            (value ?: identity3f).putInto(matrixBuffer)
            matrixBuffer.flip()
            glUniformMatrix3fv(loc, false, matrixBuffer)
        }
    }

    fun m4x3(name: String, value: Matrix4x3f?) = m4x3(getUniformLocation(name), value)
    fun m4x3(loc: Int, value: Matrix4x3f? = null) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.M4x3, false)
            potentiallyUse()
            val matrixBuffer = matrixBuffer
            matrixBuffer.position(0).limit(12)
            (value ?: identity4x3f).putInto(matrixBuffer)
            matrixBuffer.flip()
            glUniformMatrix4x3fv(loc, false, matrixBuffer)
        }
    }

    fun m4x3(name: String, value: Matrix4x3?) = m4x3(getUniformLocation(name), value)
    fun m4x3(loc: Int, value: Matrix4x3? = null) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.M4x3, false)
            potentiallyUse()
            val matrixBuffer = matrixBuffer
            matrixBuffer.position(0).limit(12)
            (value ?: identity4x3m).putInto(matrixBuffer)
            matrixBuffer.flip()
            glUniformMatrix4x3fv(loc, false, matrixBuffer)
        }
    }

    fun m4x3(loc: Int, values: FloatBuffer) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.M4x3, false)
            potentiallyUse()
            glUniformMatrix4x3fv(loc, false, values)
        }
    }

    fun m4x3Array(loc: Int, values: FloatBuffer) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.M4x3, true)
            potentiallyUse()
            glUniformMatrix4x3fv(loc, false, values)
        }
    }

    fun m4x4(name: String, value: Matrix4f? = null) = m4x4(getUniformLocation(name), value)
    fun m4x4(loc: Int, value: Matrix4f? = null) {
        if (loc > -1) {
            checkUniformType(loc, GLSLType.M4x4, false)
            potentiallyUse()
            val matrixBuffer = matrixBuffer
            matrixBuffer.position(0).limit(16)
            (value ?: identity4f).putInto(matrixBuffer)
            matrixBuffer.flip()
            glUniformMatrix4fv(loc, false, matrixBuffer)
        }
    }

    operator fun get(name: String) = getUniformLocation(name)
    fun hasUniform(name: String) = getUniformLocation(name, false) >= 0

    fun bindBuffer(slot: Int, buffer: OpenGLBuffer) {
        buffer.ensureBuffer()
        GL46C.glBindBufferBase(GL46C.GL_SHADER_STORAGE_BUFFER, slot, buffer.pointer)
    }

    override fun destroy() {
        if (program != 0) {
            glDeleteProgram(program)
            program = 0
        }
    }
}