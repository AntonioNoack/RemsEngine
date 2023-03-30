package me.anno.gpu

import me.anno.cache.CacheData
import me.anno.cache.FileCache
import me.anno.gpu.shader.OpenGLShader.Companion.compileShader
import me.anno.gpu.shader.OpenGLShader.Companion.postPossibleError
import me.anno.io.Base64.encodeBase64
import me.anno.io.Streams.readLE32
import me.anno.io.Streams.writeLE32
import me.anno.io.files.FileReference
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.types.InputStreams.readNBytes2
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL43C.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.*
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream
import kotlin.math.max
import kotlin.math.min

object ShaderCache : FileCache<Pair<String, String?>, ShaderCache.BinaryData?>(
    "ShaderCache.json", "shaders", "ShaderCache"
) {

    private val LOGGER = LogManager.getLogger(ShaderCache::class.java)

    class BinaryData(val format: Int, val data: ByteBuffer)

    var enableDebugging = false

    private val length = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer()
    private val format = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer()
    private var data = ByteBuffer.allocateDirect(65536)
    private val dataI = ByteArray(4096)

    fun ensureCapacity(size: Int) {
        if (data.capacity() < size) {
            data = ByteBuffer.allocateDirect(max(size, data.capacity() * 2))
            LOGGER.debug("Resizing to ${size.toLong().formatFileSize()}")
        }
    }

    private val magicStr = "SHADERv0".toByteArray()

    private var wkSession = -1
    private val weakCache = WeakHashMap<Pair<String, String?>, Int>()

    fun createShader(vs: String, fs: String?): Int {
        val key = Pair(vs, fs)
        val binary = (getEntry(key, 10000, false, ::generateFile) as CacheData<*>).value as BinaryData
        return createShader(key, binary)
    }

    private fun createShader(key: Pair<String, String?>, data: BinaryData): Int {

        // check if we know the value already
        if (wkSession == GFXState.session) {
            val program = weakCache.remove(key)
            if (program != null) {
                // LOGGER.warn("Reused existing program [${getUniqueFilename(key)}]")
                glUseProgram(program)
                GFX.check()
                return program
            }
        } else weakCache.clear()

        val program = glCreateProgram()
        glProgramBinary(program, data.format, data.data)

        if (glGetProgrami(program, GL_LINK_STATUS) != 1) {
            val uuid = getUniqueFilename(key)
            LOGGER.warn("Failed to create shader from binary $uuid")
            if (enableDebugging) {
                val (vs, fs) = key
                if (fs != null) {
                    cacheFolder.getChild("$uuid.vs.glsl").writeText(vs)
                    cacheFolder.getChild("$uuid.fs.glsl").writeText(fs)
                } else {
                    cacheFolder.getChild("$uuid.cs.glsl").writeText(vs)
                }
            }
            glDeleteProgram(program)
            return compile(key)
        }

        postPossibleError(name, program, false, "<bin>")
        glUseProgram(program)
        GFX.check()

        //LOGGER.warn("$program [${key.first.length}+${key.second?.length}] created from " +
        //        "${data.format} x ${data.data.capacity()} [${getUniqueFilename(key)}]")

        return program

    }

    override fun load(key: Pair<String, String?>, src: FileReference?): BinaryData? {

        // create a new program
        if (src != null && src.exists) {
            try {
                src.inputStreamSync().use {

                    for (i in magicStr.indices) {
                        if (it.read() != magicStr[i].toInt())
                            throw IOException("Incorrect magic")
                    }
                    val length = it.readLE32()
                    val format = it.readLE32()
                    val tmp = InflaterInputStream(it)
                    val data = ByteBuffer.allocateDirect(length)
                    tmp.readNBytes2(length, data, true)
                    tmp.close()

                    return BinaryData(format, data)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        } else return null
    }

    fun compile(key: Pair<String, String?>): Int {
        val (vs, fs) = key
        val program = glCreateProgram()
        if (fs == null) {

            val shader = compileShader(GL_COMPUTE_SHADER, vs)
            postPossibleError("", shader, true, vs)
            glAttachShader(program, shader)

        } else {

            val vertexShader = compileShader(GL_VERTEX_SHADER, vs)
            postPossibleError("", vertexShader, true, vs)

            val fragmentShader = compileShader(GL_FRAGMENT_SHADER, fs)
            postPossibleError("", fragmentShader, true, fs)

            glAttachShader(program, vertexShader)
            glAttachShader(program, fragmentShader)

        }

        glLinkProgram(program)

        postPossibleError(name, program, false, vs, fs ?: "")

        return program
    }

    override fun fillFileContents(
        key: Pair<String, String?>,
        dst: FileReference,
        onSuccess: () -> Unit,
        onError: (Exception?) -> Unit
    ) {

        GFX.check()

        val program = compile(key)

        if (wkSession != GFXState.session) {
            weakCache.clear()
            wkSession = GFXState.session
        }
        weakCache[key] = program

        val length0 = glGetProgrami(program, GL_PROGRAM_BINARY_LENGTH)
        ensureCapacity(length0)
        data.position(0).limit(length0)
        glGetProgramBinary(program, length, format, data)

        val length = length[0]
        val format = format[0]

        if (length > data.capacity() || length <= 0) {
            LOGGER.warn("Shader data too long, $length > ${data.capacity()}")
            onError(null)
            return
        }

        dst.outputStream().use {
            it.write(magicStr)
            it.writeLE32(length)
            it.writeLE32(format)
            val tmp = DeflaterOutputStream(it)
            var i = 0
            while (i < length) {
                val len = min(dataI.size, length - i)
                data.position(i)
                data.get(dataI, 0, len)
                tmp.write(dataI, 0, len)
                i += len
            }
            tmp.close()
        }

        onSuccess()

    }

    override fun getUniqueFilename(key: Pair<String, String?>): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(key.toString().toByteArray())
        return encodeBase64(messageDigest.digest())
            .replace('/', '-') + ".bin"
    }

}