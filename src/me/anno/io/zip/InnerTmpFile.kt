package me.anno.io.zip

import me.anno.audio.AudioReadable
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.image.Image
import me.anno.image.ImageReadable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextWriter
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.lang.ref.WeakReference
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicInteger

abstract class InnerTmpFile private constructor(
    name: String,
    val uuid: Int
) : InnerFile(name, name, false, InvalidRef) {

    init {
        if (printTmpFiles) LOGGER.debug("Registered $name")
        synchronized(Companion) {
            val ref = WeakReference(this)
            while (files.size <= uuid) {
                files.add(ref)
            }
            files[uuid] = ref
        }
    }

    constructor(ext: String) : this("", ext)
    constructor(prefix: String, ext: String, uuid: Int = id.getAndIncrement()) : this(
        if (prefix.isBlank2()) "tmp://$uuid.$ext"
        else "tmp://$prefix.$uuid.$ext", uuid
    )

    override fun toLocalPath(workspace: FileReference?) =
        absolutePath

    @Suppress("unused")
    class InnerTmpByteFile(bytes: ByteArray, ext: String = "bin") : InnerTmpFile(ext) {

        var bytes: ByteArray = bytes
            set(value) {
                field = value
                val size = value.size.toLong()
                this.size = size
                this.compressedSize = size
            }

        override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) {
            callback(bytes.inputStream(), null)
        }

        override fun readBytes(callback: (it: ByteArray?, exc: Exception?) -> Unit) {
            callback(bytes, null)
        }

        override fun readBytesSync() = bytes
    }

    @Suppress("unused")
    class InnerTmpTextFile(text: String, ext: String = "txt") : InnerTmpFile(ext) {

        var text: String = text
            set(value) {
                field = value
                val size = value.length.toLong()
                this.size = size
                this.compressedSize = size
            }

        init {
            size = text.length.toLong()
            compressedSize = size
        }

        override fun readTextSync() = text
        override fun readBytesSync() = text.toByteArray()
        override fun inputStreamSync() = text.byteInputStream()

        override fun readText(charset: Charset, callback: (String?, Exception?) -> Unit) {
            callback(readTextSync(), null)
        }

        override fun readBytes(callback: (it: ByteArray?, exc: Exception?) -> Unit) {
            callback(readBytesSync(), null)
        }

        override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) {
            callback(inputStreamSync(), null)
        }
    }

    class InnerTmpPrefabFile(val prefab: Prefab, name: String, ext: String = "json") :
        InnerTmpFile(name, ext), PrefabReadable {

        constructor(prefab: Prefab) : this(prefab, prefab.getProperty("name") as? String ?: "")

        init {
            val size = Int.MAX_VALUE.toLong()
            this.size = size
            this.compressedSize = size
            prefab.source = this
        }

        val text by lazy { TextWriter.toText(prefab, InvalidRef) }
        val bytes by lazy { text.toByteArray() }

        override fun isSerializedFolder(): Boolean = false
        override fun listChildren(): List<FileReference>? = null

        override fun readText(charset: Charset, callback: (String?, Exception?) -> Unit) {
            callback(text, null)
        }

        override fun readBytes(callback: (it: ByteArray?, exc: Exception?) -> Unit) {
            callback(bytes, null)
        }

        override fun readTextSync() = text
        override fun readBytesSync() = bytes

        override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) {
            callback(text.byteInputStream(), null)
        }

        override fun readPrefab(): Prefab {
            return prefab
        }
    }

    class InnerTmpImageFile(val image: Image, ext: String = "png") : InnerTmpFile(ext), ImageReadable {

        init {
            val size = Int.MAX_VALUE.toLong()
            this.size = size
            this.compressedSize = size
        }

        val text = lazy { "" } // we could write a text based image here
        val bytes = lazy {
            val bos = ByteArrayOutputStream(1024)
            image.write(bos, "png")
            bos.toByteArray()
        }

        override fun isSerializedFolder(): Boolean = false
        override fun listChildren(): List<FileReference>? = null

        override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) {
            callback(bytes.value.inputStream(), null)
        }

        override fun inputStreamSync() = bytes.value.inputStream()
        override fun readBytes(callback: (it: ByteArray?, exc: Exception?) -> Unit) {
            callback(bytes.value, null)
        }

        override fun readBytesSync(): ByteArray = bytes.value

        override fun readImage(): Image = image
    }

    abstract class InnerTmpAudioFile : InnerTmpFile("mp3"), AudioReadable {
        override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) {
            throw NotImplementedError()
        }
    }

    companion object { // only works if extension does not contain dots
        @JvmField
        var printTmpFiles = false

        @JvmStatic
        private val LOGGER = LogManager.getLogger(InnerTmpFile::class)

        @JvmStatic
        private val files = ArrayList<WeakReference<InnerTmpFile>>()

        @JvmStatic
        private val id = AtomicInteger()

        @JvmStatic
        fun find(str: String): InnerTmpFile? {
            // prefix.uuid.ext or prefix.ext
            LOGGER.debug("Parsing $str")
            val endOfExt = str.lastIndexOf('.')
            if (endOfExt < 3) return null
            var prevOfExt = str.lastIndexOf('.', endOfExt - 1)
            if (prevOfExt < 0) prevOfExt = str.lastIndexOf('/', endOfExt - 2)
            LOGGER.debug("Reading index from ${str.substring(prevOfExt + 1, endOfExt)}")
            val uuid = str.substring(prevOfExt + 1, endOfExt).toIntOrNull() ?: return null
            LOGGER.debug("Getting file with index $uuid")
            val reference = files.getOrNull(uuid) ?: return null
            val candidate = reference.get()
            if (candidate != null) {
                if (candidate.absolutePath == str) return candidate
                LOGGER.warn("InnerTmpFile mismatch: searched for '$str', but found '$candidate'")
                return null
            } else {
                LOGGER.warn("InnerTmpFile #$uuid was already GCed, '$str'!")
                return null
            }
        }
    }
}