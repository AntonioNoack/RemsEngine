package me.anno.gpu.shader

import me.anno.cache.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.maths.Maths.sq
import me.anno.ui.editor.files.toAllowedFilename
import me.anno.utils.OS
import me.anno.utils.types.Strings.countLines
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import org.joml.*
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL21C.*
import java.nio.FloatBuffer

abstract class OpenGLShader(val name: String) : ICacheData {

    companion object {

        var logShaders = false
        var useShaderFileCache = false

        private val LOGGER = LogManager.getLogger(OpenGLShader::class)

        /** how attributes are called; is attribute in WebGL */
        val attribute get() = "in" // if(OS.isWeb) "attribute" else "in"

        private val matrixBuffer = BufferUtils.createFloatBuffer(16)
        private val identity2 = Matrix2f()
        private val identity3 = Matrix3f()
        private val identity4 = Matrix4f()
        private val identity4x3 = Matrix4x3f()
        const val DefaultGLSLVersion = 150
        var UniformCacheSize = if (OS.isWeb) 0 else 256 // todo remove when everything works
        val UniformCacheSizeX4 get() = UniformCacheSize * 4
        var safeShaderBinding = false
        var lastProgram = -1

        fun invalidateBinding() {
            lastProgram = -1
        }

        fun formatVersion(version: Int): String {
            return when {
                // ERROR: 0:1: '150' : client/version number not supported
                // todo how do we find out which version is supported in WebGL?
                OS.isWeb -> "#version 300 es\n"
                OS.isAndroid -> "#version $version es\n"
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
                compileShader(type, source)
            }
            glAttachShader(program, shader)
            postPossibleError(shaderName, shader, true, source)
            return shader
        }

        fun compileShader(type: Int, source: String): Int {
            val shader = glCreateShader(type)
            glShaderSource(shader, source)
            glCompileShader(shader)
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
                LOGGER.warn(formatShader(shaderName, log, s0, s1))
            }
            if (!isShader) {
                if (glGetProgrami(shader, GL_LINK_STATUS) == 0) {
                    throw IllegalStateException("Linking $shader failed")
                }
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

        fun logShader(shaderName: String, vertex: String, fragment: String) {
            if (logShaders) {
                val folder = OS.desktop.getChild("shaders")
                folder.tryMkdirs()
                fun print(ext: String, data: String) {
                    val name = "$shaderName.$ext".toAllowedFilename() ?: return
                    getReference(folder, name).writeText(data)
                }
                print("vert", vertex)
                print("frag", fragment)
            }
        }

        fun logShader(shaderName: String, comp: String) {
            if (logShaders) {
                val folder = OS.desktop.getChild("shaders")
                folder.tryMkdirs()
                fun print(ext: String, data: String) {
                    val name = "$shaderName.$ext".toAllowedFilename() ?: return
                    getReference(folder, name).writeText(data)
                }
                print("comp", comp)
            }
        }

    }

    var safeShaderBinding = Companion.safeShaderBinding

    var glslVersion = DefaultGLSLVersion

    var program = 0
    var session = 0

    fun use(): Boolean {
        GFX.check()
        // Frame.bindMaybe()
        GFX.check()
        if (program == 0 || session != GFXState.session) {
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

    private val uniformLocations = HashMap<String, Int>()
    private val uniformCache = FloatArray(UniformCacheSizeX4) { Float.NaN }
    var textureNames: List<String> = emptyList()

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

    val pointer get() = program
    private val ignoredNames = HashSet<String>()

    fun updateSession() {
        session = GFXState.session
        uniformLocations.clear()
        uniformCache.fill(Float.NaN)
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

    fun setTextureIndices(vararg textures: String) =
        setTextureIndices(textures.toList())

    fun setTextureIndices(textures: List<String>?): OpenGLShader {
        if (textures == null) return this
        if (pointer != 0) {
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

    fun ignoreNameWarnings(names: Collection<String>) {
        ignoredNames += names
    }

    fun ignoreNameWarnings(vararg names: String): OpenGLShader {
        ignoredNames += names
        return this
    }

    fun ignoreNameWarnings(name: String): OpenGLShader {
        ignoredNames += name
        return this
    }

    fun getUniformLocation(name: String, warnIfMissing: Boolean = true): Int {
        return uniformLocations.getOrPut(name) {
            if (safeShaderBinding) use()
            val loc = glGetUniformLocation(program, name)
            if (loc < 0 && warnIfMissing && name !in ignoredNames && !sourceContainsWord(name)) {
                LOGGER.warn("Uniform location \"$name\" not found in shader ${this.name}")
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
            if (loc >= UniformCacheSize) {
                potentiallyUse()
                glUniform1i(loc, x)
            } else {
                val xf = x.toFloat()
                val index0 = loc * 4
                when {
                    xf.toInt() != x -> {
                        // cannot be represented as a float -> cannot currently be cached
                        uniformCache[index0] = Float.NaN
                        potentiallyUse()
                        glUniform1i(loc, x)
                    }
                    uniformCache[index0] != xf -> {
                        // it has changed
                        uniformCache[index0] = xf
                        potentiallyUse()
                        glUniform1i(loc, x)
                    }
                }
            }
        }
    }

    fun v1f(name: String, x: Double) = v1f(name, x.toFloat())
    fun v1f(name: String, x: Float) = v1f(getUniformLocation(name), x)
    fun v1f(loc: Int, x: Float) {
        if (loc > -1) {
            if (loc >= UniformCacheSize) {
                potentiallyUse()
                glUniform1f(loc, x)
            } else {
                val index0 = loc * 4
                if (uniformCache[index0 + 0] != x) {
                    uniformCache[index0 + 0] = x
                    potentiallyUse()
                    glUniform1f(loc, x)
                }
            }
        }
    }

    /**
     * sets an array of float uniforms
     * */
    fun v1fs(name: String, vs: FloatArray) = v1fs(getUniformLocation(name), vs)
    fun v1fs(loc: Int, vs: FloatArray) {
        if (loc > -1) {
            potentiallyUse()
            glUniform1fv(loc, vs)
        }
    }

    @Suppress("unused")
    fun v1fs(name: String, vs: FloatBuffer) = v1fs(getUniformLocation(name), vs)
    fun v1fs(loc: Int, vs: FloatBuffer) {
        if (loc > -1) {
            potentiallyUse()
            glUniform1fv(loc, vs)
        }
    }

    fun v2f(name: String, x: Float, y: Float) = v2f(getUniformLocation(name), x, y)
    fun v2f(loc: Int, x: Float, y: Float) {
        if (loc > -1) {
            if (loc >= UniformCacheSize) {
                potentiallyUse()
                glUniform2f(loc, x, y)
            } else {
                val index0 = loc * 4
                if (
                    uniformCache[index0 + 0] != x ||
                    uniformCache[index0 + 1] != y
                ) {
                    uniformCache[index0 + 0] = x
                    uniformCache[index0 + 1] = y
                    potentiallyUse()
                    glUniform2f(loc, x, y)
                }
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
            potentiallyUse()
            glUniform2fv(loc, vs)
        }
    }

    @Suppress("unused")
    fun v2fs(name: String, vs: FloatBuffer) = v2fs(getUniformLocation(name), vs)
    fun v2fs(loc: Int, vs: FloatBuffer) {
        if (loc > -1) {
            potentiallyUse()
            glUniform2fv(loc, vs)
        }
    }

    fun v2i(name: String, x: Int, y: Int = x) = v2i(getUniformLocation(name), x, y)
    fun v2i(loc: Int, x: Int, y: Int = x) {
        if (loc > -1) {
            if (loc >= UniformCacheSize) {
                potentiallyUse()
                glUniform2i(loc, x, y)
            } else {
                val xf = x.toFloat()
                val yf = y.toFloat()
                val i0 = loc * 4
                when {
                    uniformCache[i0] != xf || uniformCache[i0 + 1] != yf -> {
                        // cannot be represented as a float -> cannot currently be cached
                        uniformCache[i0] = Float.NaN
                        uniformCache[i0 + 1] = Float.NaN
                        potentiallyUse()
                        glUniform2i(loc, x, y)
                    }
                    uniformCache[i0] != xf -> {
                        // it has changed
                        uniformCache[i0] = xf
                        uniformCache[i0 + 1] = yf
                        potentiallyUse()
                        glUniform2i(loc, x, y)
                    }
                }
            }
        }
    }

    fun v3f(name: String, x: Float, y: Float, z: Float) = v3f(getUniformLocation(name), x, y, z)
    fun v3f(loc: Int, x: Float, y: Float, z: Float) {
        if (loc > -1) {
            if (loc >= UniformCacheSize) {
                potentiallyUse()
                glUniform3f(loc, x, y, z)
            } else {
                val index0 = loc * 4
                if (
                    uniformCache[index0 + 0] != x ||
                    uniformCache[index0 + 1] != y ||
                    uniformCache[index0 + 2] != z
                ) {
                    uniformCache[index0 + 0] = x
                    uniformCache[index0 + 1] = y
                    uniformCache[index0 + 2] = z
                    potentiallyUse()
                    glUniform3f(loc, x, y, z)
                }
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
            potentiallyUse()
            glUniform3fv(loc, vs)
        }
    }

    @Suppress("unused")
    fun v3fs(name: String, vs: FloatBuffer) = v3fs(getUniformLocation(name), vs)
    fun v3fs(loc: Int, vs: FloatBuffer) {
        if (loc > -1) {
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
            if (loc >= UniformCacheSize) {
                potentiallyUse()
                glUniform3i(loc, x, y, z)
            } else {
                val xf = x.toFloat()
                val yf = y.toFloat()
                val zf = z.toFloat()
                val i0 = loc * 4
                when {
                    xf.toInt() != x || yf.toInt() != y -> {
                        // cannot be represented as a float -> cannot currently be cached
                        uniformCache[i0] = Float.NaN
                        uniformCache[i0 + 1] = Float.NaN
                        uniformCache[i0 + 2] = Float.NaN
                        potentiallyUse()
                        glUniform3i(loc, x, y, z)
                    }
                    uniformCache[i0] != xf || uniformCache[i0 + 1] != yf ||
                            uniformCache[i0 + 2] != zf -> {
                        // it has changed
                        uniformCache[i0] = xf
                        uniformCache[i0 + 1] = yf
                        uniformCache[i0 + 2] = zf
                        potentiallyUse()
                        glUniform3i(loc, x, y, z)
                    }
                }
            }
        }
    }

    fun v4f(name: String, x: Float, y: Float, z: Float, w: Float) =
        v4f(getUniformLocation(name), x, y, z, w)

    fun v4f(loc: Int, x: Float, y: Float, z: Float, w: Float) {
        if (loc > -1) {
            if (loc >= UniformCacheSize) {
                potentiallyUse()
                glUniform4f(loc, x, y, z, w)
            } else {
                val index0 = loc * 4
                if (
                    uniformCache[index0 + 0] != x ||
                    uniformCache[index0 + 1] != y ||
                    uniformCache[index0 + 2] != z ||
                    uniformCache[index0 + 3] != w
                ) {
                    uniformCache[index0 + 0] = x
                    uniformCache[index0 + 1] = y
                    uniformCache[index0 + 2] = z
                    uniformCache[index0 + 3] = w
                    potentiallyUse()
                    glUniform4f(loc, x, y, z, w)
                }
            }
        }
    }

    fun v4f(name: String, color: Int) = v4f(getUniformLocation(name), color)
    fun v4f(loc: Int, color: Int) {
        v4f(
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
        if (loc > -1) {
            potentiallyUse()
            glUniform4fv(loc, vs)
        }
    }

    @Suppress("unused")
    fun v4fs(name: String, vs: FloatBuffer) = v4fs(getUniformLocation(name), vs)
    fun v4fs(loc: Int, vs: FloatBuffer) {
        if (loc > -1) {
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
            if (loc >= UniformCacheSize) {
                potentiallyUse()
                glUniform4i(loc, x, y, z, w)
            } else {
                val xf = x.toFloat()
                val yf = y.toFloat()
                val zf = z.toFloat()
                val wf = w.toFloat()
                val i0 = loc * 4
                when {
                    xf.toInt() != x || yf.toInt() != y || zf.toInt() != z || wf.toInt() != w -> {
                        // cannot be represented as a float -> cannot currently be cached
                        uniformCache[i0] = Float.NaN
                        uniformCache[i0 + 1] = Float.NaN
                        uniformCache[i0 + 2] = Float.NaN
                        uniformCache[i0 + 3] = Float.NaN
                        potentiallyUse()
                        glUniform4i(loc, x, y, z, w)
                    }
                    uniformCache[i0] != xf || uniformCache[i0 + 1] != yf ||
                            uniformCache[i0 + 2] != zf || uniformCache[i0 + 3] != wf -> {
                        // it has changed
                        uniformCache[i0] = xf
                        uniformCache[i0 + 1] = yf
                        uniformCache[i0 + 2] = zf
                        uniformCache[i0 + 3] = wf
                        potentiallyUse()
                        glUniform4i(loc, x, y, z, w)
                    }
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
    fun v4f(name: String, v: Vector4f) = v4f(name, v.x, v.y, v.z, v.w)
    fun v4f(name: String, v: Quaternionf) = v4f(name, v.x, v.y, v.z, v.w)
    fun v4f(name: String, v: Quaterniond) = v4f(name, v.x.toFloat(), v.y.toFloat(), v.z.toFloat(), v.w.toFloat())
    fun v4f(name: String, v: Vector4d) =
        v4f(name, v.x.toFloat(), v.y.toFloat(), v.z.toFloat(), v.w.toFloat())

    fun m2x2(name: String, value: Matrix2f?) = m2x2(getUniformLocation(name), value)
    fun m2x2(loc: Int, value: Matrix2f? = null) {
        if (loc > -1) {
            potentiallyUse()
            matrixBuffer.position(0).limit(4)
            (value ?: identity2).putInto(matrixBuffer)
            matrixBuffer.flip()
            glUniformMatrix2fv(loc, false, matrixBuffer)
        }
    }

    fun m3x3(name: String, value: Matrix3f?) = m3x3(getUniformLocation(name), value)
    fun m3x3(loc: Int, value: Matrix3f? = null) {
        if (loc > -1) {
            potentiallyUse()
            matrixBuffer.position(0).limit(9)
            (value ?: identity3).putInto(matrixBuffer)
            matrixBuffer.flip()
            glUniformMatrix3fv(loc, false, matrixBuffer)
        }
    }

    fun m4x3(name: String, value: Matrix4x3f?) = m4x3(getUniformLocation(name), value)
    fun m4x3(loc: Int, value: Matrix4x3f? = null) {
        if (loc > -1) {
            potentiallyUse()
            matrixBuffer.position(0).limit(12)
            (value ?: identity4x3).putInto(matrixBuffer)
            matrixBuffer.flip()
            glUniformMatrix4x3fv(loc, false, matrixBuffer)
        }
    }

    fun m4x3Array(loc: Int, values: FloatBuffer) {
        if (loc > -1) {
            potentiallyUse()
            glUniformMatrix4x3fv(loc, false, values)
        }
    }

    fun m4x4(name: String, value: Matrix4f? = null) = m4x4(getUniformLocation(name), value)
    fun m4x4(loc: Int, value: Matrix4f? = null) {
        if (loc > -1) {
            potentiallyUse()
            matrixBuffer.position(0).limit(16)
            (value ?: identity4).putInto(matrixBuffer)
            matrixBuffer.flip()
            glUniformMatrix4fv(loc, false, matrixBuffer)
        }
    }

    @Suppress("unused")
    fun v1Array(name: String, value: FloatBuffer) = v1Array(getUniformLocation(name), value)
    fun v1Array(loc: Int, value: FloatBuffer) {
        if (loc > -1) {
            potentiallyUse()
            glUniform1fv(loc, value)
        }
    }

    fun v2Array(name: String, value: FloatBuffer) = v2Array(getUniformLocation(name), value)
    fun v2Array(loc: Int, value: FloatBuffer) {
        if (loc > -1) {
            potentiallyUse()
            glUniform2fv(loc, value)
        }
    }

    @Suppress("unused")
    fun v3Array(name: String, value: FloatBuffer) = v3Array(getUniformLocation(name), value)
    fun v3Array(loc: Int, value: FloatBuffer) {
        if (loc > -1) {
            potentiallyUse()
            glUniform3fv(loc, value)
        }
    }

    fun v4Array(name: String, value: FloatBuffer) = v4Array(getUniformLocation(name), value)
    fun v4Array(loc: Int, value: FloatBuffer) {
        if (loc > -1) {
            potentiallyUse()
            glUniform4fv(loc, value)
        }
    }

    fun check() = GFX.check()

    operator fun get(name: String) = getUniformLocation(name)

    override fun destroy() {
        if (program != 0) {
            glDeleteProgram(program)
            program = 0
        }
    }

}