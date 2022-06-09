package me.anno.gpu.shader

import me.anno.cache.data.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL
import me.anno.gpu.shader.builder.Varying
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.maths.Maths.sq
import me.anno.ui.editor.files.toAllowedFilename
import me.anno.utils.OS
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import org.joml.*
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL21.glUniformMatrix4x3fv
import java.nio.FloatBuffer

abstract class OpenGLShader(val name: String) : ICacheData {

    companion object {

        var logShaders = false

        private val LOGGER = LogManager.getLogger(OpenGLShader::class)

        /** how attributes are called; might be "attribute" in WebGL */
        var attribute = "in"

        private val matrixBuffer = BufferUtils.createFloatBuffer(16)
        private val identity3: Matrix3fc = Matrix3f()
        private val identity4: Matrix4fc = Matrix4f()
        private val identity4x3: Matrix4x3fc = Matrix4x3f()
        const val DefaultGLSLVersion = 150
        const val UniformCacheSize = 256
        const val UniformCacheSizeX4 = UniformCacheSize * 4
        var safeShaderBinding = false
        var lastProgram = -1

        fun invalidateBinding() {
            lastProgram = -1
        }

        fun formatVersion(version: Int): String {
            return if (OS.isAndroid) "#version $version es\n" else "#version $version\n"
        }

        // needs to be cleared when the opengl session changes
        private val shaderCache = HashMap<Pair<Int, String>, Int>(256)
        private var shaderCacheSession = -1

        fun compile(shaderName: String, program: Int, type: Int, source: String): Int {
            if (shaderCacheSession != OpenGL.session) {
                shaderCacheSession = OpenGL.session
                shaderCache.clear()
            }
            val shader = shaderCache.getOrPut(type to source) {
                val shader = glCreateShader(type)
                glShaderSource(shader, source)
                glCompileShader(shader)
                shader
            }
            glAttachShader(program, shader)
            postPossibleError(shaderName, shader, true, source)
            return shader
        }

        fun postPossibleError(shaderName: String, shader: Int, isShader: Boolean, s0: String, s1: String = "") {
            val log = if (isShader) {
                glGetShaderInfoLog(shader)
            } else {
                glGetProgramInfoLog(shader)
            }
            if (!log.isBlank2()) {
                LOGGER.warn(
                    "$log by $shaderName\n\n${
                        (s0 + s1)
                            .split('\n')
                            .mapIndexed { index, line ->
                                "${"%1\$3s".format(index + 1)}: $line"
                            }.joinToString("\n")
                    }"
                )
                /*if(!log.contains("deprecated", true)){
                    throw RuntimeException()
                }*/
            }
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

    var program = -1
    var session = 0

    fun use(): Boolean {
        GFX.check()
        // Frame.bindMaybe()
        GFX.check()
        if (program <= 0 || session != OpenGL.session) {
            compile()
        }
        if (program <= 0) throw IllegalStateException()
        return if (program != lastProgram) {
            glUseProgram(program)
            GFX.check()
            lastProgram = program
            true
        } else false
    }

    private val uniformLocations = HashMap<String, Int>()
    private val attributeLocations = HashMap<String, Int>()
    private val uniformCache = FloatArray(UniformCacheSizeX4) { Float.NaN }
    var textureNames: List<String> = emptyList()

    abstract fun compile()
    abstract fun sourceContainsWord(word: String): Boolean

    fun setTextureIndicesIfExisting() {
        for ((index, key) in textureNames.withIndex()) {
            val location = getUniformLocation(key)
            if (location >= 0) {
                glUniform1i(location, index)
            }
        }
    }

    fun getTextureIndex(name: String): Int {
        return textureNames.indexOf(name)
    }

    val pointer get() = program
    private val ignoredNames = HashSet<String>()

    fun updateSession() {
        session = OpenGL.session
        attributeLocations.clear()
        uniformLocations.clear()
        uniformCache.fill(Float.NaN)
    }

    fun printLocationsAndValues() {
        for ((key, value) in attributeLocations.entries.sortedBy { it.value }) {
            LOGGER.info("Attribute $key = $value")
        }
        for ((key, value) in uniformLocations.entries.sortedBy { it.value }) {
            LOGGER.info("Uniform $key[$value] = (${uniformCache[value * 4]},${uniformCache[value * 4 + 1]},${uniformCache[value * 4 + 2]},${uniformCache[value * 4 + 3]})")
        }
    }

    fun invalidateCacheForTests() {
        attributeLocations.clear()
        uniformLocations.clear()
        uniformCache.fill(Float.NaN)
    }

    fun String.replaceVaryingNames(isVertex: Boolean, varyings: List<Varying>): String {
        var str = this
        if (varyings.isNotEmpty() && varyings.first().run { vShaderName != fShaderName })
            for (v in varyings) {
                // regex to really only replace these words
                val target = if (isVertex) v.vShaderName else v.fShaderName
                val anything = "[ .,\\n;(){}\\[\\]]"
                val regex = Regex(anything + v.name + anything)
                val result = StringBuilder()
                var i0 = 0
                while (true) {
                    val match = regex.find(str, i0) ?: break
                    val startIndex = match.range.first + 1
                    result.append(str.substring(i0, startIndex))
                    result.append(target)
                    i0 = startIndex + v.name.length
                }
                result.append(str.substring(i0))
                // str = str.replace(v.name, target)
                str = result.toString()
            }
        return str
    }

    fun setTextureIndices(vararg textures: String) {
        setTextureIndices(textures.toList())
    }

    fun setTextureIndices(textures: List<String>?) {
        use()
        if (textures == null) return
        textureNames = textures
        for ((index, name) in textures.withIndex()) {
            if (',' in name) throw IllegalArgumentException("Name must not contain comma!")
            val texName = getUniformLocation(name)
            if (texName >= 0) glUniform1i(texName, index)
        }
    }

    fun ignoreNameWarnings(names: Collection<String>) {
        ignoredNames += names
    }

    fun ignoreNameWarnings(vararg names: String) {
        ignoredNames += names
    }

    fun ignoreNameWarnings(name: String) {
        ignoredNames += name
    }

    fun getUniformLocation(name: String): Int {
        return uniformLocations.getOrPut(name) {
            if (safeShaderBinding) use()
            val loc = glGetUniformLocation(program, name)
            if (loc < 0 && name !in ignoredNames && !sourceContainsWord(name)) {
                LOGGER.warn("Uniform location \"$name\" not found in shader ${this.name}")
            }
            loc
        }
    }

    fun getAttributeLocation(name: String): Int {
        return attributeLocations.getOrPut(name) {
            if (safeShaderBinding) use()
            val loc = glGetAttribLocation(program, name)
            if (loc < 0 && name !in ignoredNames) {
                LOGGER.warn("Attribute location \"$name\" not found in shader ${this.name}")
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

    fun v3X(loc: Int, x: Float, y: Float, z: Float, w: Float) = v3f(loc, x / w, y / w, z / w)
    fun v3X(name: String, x: Float, y: Float, z: Float, w: Float) = v3f(name, x / w, y / w, z / w)
    fun v3X(loc: Int, v: Vector4f) = v3f(loc, v.x / v.w, v.y / v.w, v.z / v.w)
    fun v3X(name: String, v: Vector4f) = v3f(name, v.x / v.w, v.y / v.w, v.z / v.w)

    fun v3X(loc: Int, v: Vector4fc) = v3X(loc, v.x(), v.y(), v.z(), v.w())
    fun v3X(name: String, v: Vector4fc) = v3X(name, v.x(), v.y(), v.z(), v.w())

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

    fun v4fs(name: String, color: Int) = v4fs(getUniformLocation(name), color)
    fun v4fs(loc: Int, color: Int) {
        v4f(
            loc,
            sq((color.shr(16) and 255) / 255f),
            sq((color.shr(8) and 255) / 255f),
            sq(color.and(255) / 255f),
            sq((color.shr(24) and 255) / 255f)
        )
    }

    fun v4f(name: String, color: Vector3fc, alpha: Float) = v4f(getUniformLocation(name), color, alpha)
    fun v4f(loc: Int, color: Vector3fc, alpha: Float) =
        v4f(loc, color.x(), color.y(), color.z(), alpha)

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

    fun v2f(loc: Int, v: Vector2fc) = v2f(loc, v.x(), v.y())
    fun v3f(loc: Int, v: Vector3fc) = v3f(loc, v.x(), v.y(), v.z())
    fun v4f(loc: Int, v: Vector4fc) = v4f(loc, v.x(), v.y(), v.z(), v.w())
    fun v4f(loc: Int, v: Quaternionfc) = v4f(loc, v.x(), v.y(), v.z(), v.w())

    fun v2i(loc: Int, v: Vector2ic) = v2i(loc, v.x(), v.y())
    fun v3i(loc: Int, v: Vector3ic) = v3i(loc, v.x(), v.y(), v.z())
    fun v4i(loc: Int, v: Vector4ic) = v4i(loc, v.x(), v.y(), v.z(), v.w())

    fun v2i(loc: String, v: Vector2ic) = v2i(loc, v.x(), v.y())
    fun v3i(loc: String, v: Vector3ic) = v3i(loc, v.x(), v.y(), v.z())
    fun v4i(loc: String, v: Vector4ic) = v4i(loc, v.x(), v.y(), v.z(), v.w())

    fun v2f(name: String, all: Float) = v2f(name, all, all)
    fun v3f(name: String, all: Float) = v3f(name, all, all, all)
    fun v4f(name: String, all: Float) = v4f(name, all, all, all, all)

    fun v2i(name: String, all: Int) = v2i(name, all, all)
    fun v3i(name: String, all: Int) = v3i(name, all, all, all)
    fun v4i(name: String, all: Int) = v4i(name, all, all, all, all)

    fun v2f(name: String, v: Vector2fc) = v2f(name, v.x(), v.y())
    fun v3f(name: String, v: Vector3fc) = v3f(name, v.x(), v.y(), v.z())
    fun v4f(name: String, v: Vector4fc) = v4f(name, v.x(), v.y(), v.z(), v.w())
    fun v4f(name: String, v: Quaternionfc) = v4f(name, v.x(), v.y(), v.z(), v.w())
    fun v4f(name: String, v: Vector4dc) =
        v4f(name, v.x().toFloat(), v.y().toFloat(), v.z().toFloat(), v.w().toFloat())

    fun m3x3(name: String, value: Matrix3fc?) = m3x3(getUniformLocation(name), value)
    fun m3x3(loc: Int, value: Matrix3fc? = identity3) {
        if (loc > -1) {
            potentiallyUse()
            (value ?: identity3).get(matrixBuffer)
            glUniformMatrix3fv(loc, false, matrixBuffer)
        }
    }

    fun m4x3(name: String, value: Matrix4x3fc?) = m4x3(getUniformLocation(name), value)
    fun m4x3(loc: Int, value: Matrix4x3fc? = identity4x3) {
        if (loc > -1) {
            potentiallyUse()
            (value ?: identity4x3).get(matrixBuffer)
            glUniformMatrix4x3fv(loc, false, matrixBuffer)
        }
    }

    fun m4x4(name: String, value: Matrix4fc? = identity4) = m4x4(getUniformLocation(name), value)
    fun m4x4(loc: Int, value: Matrix4fc? = identity4) {
        if (loc > -1) {
            potentiallyUse()
            (value ?: identity4).get(matrixBuffer)
            glUniformMatrix4fv(loc, false, matrixBuffer)
        }
    }

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
        if (program > -1) glDeleteProgram(program)
    }

}