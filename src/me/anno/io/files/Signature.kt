package me.anno.io.files

import me.anno.ecs.prefab.PrefabReadable
import me.anno.image.gimp.GimpImage
import me.anno.io.zip.SignatureFile
import me.anno.utils.Color.hex8
import me.anno.utils.structures.lists.Lists.firstOrNull2
import java.nio.ByteBuffer
import kotlin.math.min

class Signature(val name: String, val offset: Int, val bytes: ByteArray) {

    constructor(name: String, offset: Int, signature: String) : this(name, offset, signature.toByteArray())

    constructor(name: String, offset: Int, signature: String, vararg extraBytes: Int) : this(
        name, offset,
        signature.toByteArray() + ByteArray(extraBytes.size) { extraBytes[it].toByte() }
    )

    constructor(name: String, offset: Int, prefix: ByteArray, signature: String, extraBytes: ByteArray) : this(
        name, offset,
        prefix + signature.toByteArray() + extraBytes
    )

    constructor(name: String, offset: Int, prefix: ByteArray, signature: String) : this(
        name, offset,
        prefix + signature.toByteArray()
    )

    constructor(name: String, offset: Int, vararg bytes: Int) : this(
        name, offset,
        ByteArray(bytes.size) { bytes[it].toByte() }
    )

    fun matches(bytes: ByteBuffer): Boolean {
        val position = bytes.position()
        val size = bytes.remaining()
        if (offset >= size) return false
        if (offset < 0) {
            // search the signature instead of requiring it
            search@ for (offset in 0 until size - this.bytes.size) {
                for (i in 0 until min(size - offset, this.bytes.size)) {
                    if (bytes[position + i + offset] != this.bytes[i]) {
                        continue@search
                    }
                }
                return true
            }
            return false
        } else {
            for (i in 0 until min(size - offset, this.bytes.size)) {
                if (bytes[position + i + offset] != this.bytes[i]) return false
            }
            return true
        }
    }

    fun matches(bytes: ByteArray): Boolean {
        if (offset >= bytes.size) return false
        if (offset < 0) {
            // search the signature instead of requiring it
            search@ for (offset in 0 until bytes.size - this.bytes.size) {
                for (i in 0 until min(bytes.size - offset, this.bytes.size)) {
                    if (bytes[i + offset] != this.bytes[i]) {
                        continue@search
                    }
                }
                return true
            }
            return false
        } else {
            for (i in 0 until min(bytes.size - offset, this.bytes.size)) {
                if (bytes[i + offset] != this.bytes[i]) return false
            }
            return true
        }
    }

    override fun toString() = "\"$name\" by [${bytes.joinToString { hex8(it.toInt()) }}] + $offset"

    companion object {

        const val sampleSize = 128

        fun findName(bytes: ByteBuffer) = find(bytes)?.name
        fun find(bytes: ByteBuffer): Signature? {
            val nonHashed = signatures
            for (i in nonHashed.indices) {
                val s = nonHashed[i]
                if (s.matches(bytes)) return s
            }
            return null
        }

        fun findName(bytes: ByteArray) = find(bytes)?.name
        fun find(bytes: ByteArray): Signature? {
            val nonHashed = signatures
            for (i in nonHashed.indices) {
                val s = nonHashed[i]
                if (s.matches(bytes)) {
                    return s
                }
            }
            return null
        }

        fun register(signature: Signature) {
            // alternatively could find the correct insert index
            // still would be O(n)
            // to
            var index = signatures.binarySearch {
                signature.bytes.size.compareTo(it.bytes.size)
            }
            if (index < 0) index = -1 - index
            signatures.add(index, signature)
        }

        @Suppress("unused")
        fun unregister(signature: Signature) {
            // could use binary search to find signature
            // still would be O(n)
            signatures.remove(signature)
        }

        fun findName(file: FileReference, callback: (String?) -> Unit) {
            find(file) { callback(it?.name) }
        }

        fun find(file: FileReference, callback: (Signature?) -> Unit) {
            if (file is SignatureFile) return callback(file.signature)
            if (!file.exists) return callback(null)
            return when (file) {
                is PrefabReadable -> callback(signatures.firstOrNull2 { it.name == "json" })
                else -> {
                    // reads the bytes, or 255 if at end of file
                    // how much do we read? 🤔
                    // some formats are easy, others require more effort
                    // maybe we could read them piece by piece...
                    file.inputStream(sampleSize.toLong()) { it, _ ->
                        if (it != null) {
                            val bytes = it.use {
                                ByteArray(sampleSize) { _ -> it.read().toByte() }
                            }
                            callback(find(bytes))
                        } else callback(null)
                    }
                }
            }
        }

        fun findNameSync(file: FileReference) = findSync(file)?.name
        fun findSync(file: FileReference): Signature? {
            if (file is SignatureFile) return file.signature
            if (!file.exists) return null
            return when (file) {
                is PrefabReadable -> signatures.first { it.name == "json" }
                else -> {
                    // reads the bytes, or 255 if at end of file
                    // how much do we read? 🤔
                    // some formats are easy, others require more effort
                    // maybe we could read them piece by piece...
                    file.inputStreamSync().use { input ->
                        find(ByteArray(sampleSize) { input.read().toByte() })
                    }
                }
            }
        }

        val bmp = Signature("bmp", 0, "BM")

        // source: https://en.wikipedia.org/wiki/List_of_file_signatures
        // https://www.garykessler.net/library/file_sigs.html
        @Suppress("SpellCheckingInspection")
        private val signatures = arrayListOf(
            Signature("bz2", 0, "BZh"),
            Signature("rar", 0, "Rar!", 0x1a, 0x07),
            Signature("zip", 0, "PK", 3, 4),
            Signature("zip", 0, "PK", 5, 6), // "empty archive" after wikipedia
            Signature("zip", 0, "PK", 7, 8), // "spanned archive"
            Signature("tar", 0, 0x1F, 0x9D), // lempel-ziv-welch
            Signature("tar", 0, 0x1F, 0xA0),// lzh
            // Signature("tar", 257, "ustar"), // this large offset is unfortunate; we'd have to adjust the signature readout for ALL others
            Signature("gzip", 0, 0x1F, 0x8B), // gz/tar.gz
            Signature("xz", 0, byteArrayOf(0xFD.toByte()), "7zXZ", byteArrayOf(0)), // xz compression
            Signature("lz4", 0, 0x04, 0x22, 0x4D, 0x18), // another compression
            Signature("7z", 0, "7z", 0xBC, 0xAF, 0x27, 0x1C),
            Signature("xar", 0, "xar!"), // file compression for apple stuff?
            Signature("oar", 0, "OAR"), // oar compression (mmh)
            Signature("java", 0, 0xCA, 0xFE, 0xBA, 0xBE), // java class
            Signature("text", 0, 0xEF, 0xBB, 0xBF), // UTF8
            Signature("text", 0, 0xFF, 0xFE), // UTF16
            Signature("text", 0, 0xFE, 0xFF),
            Signature("text", 0, 0xFF, 0xFE, 0, 0), // UTF32
            Signature("text", 0, 0xFE, 0xFF, 0, 0),
            Signature("text", 0, "+/v8"), // UTF7
            Signature("text", 0, "+/v9"), // UTF7
            Signature("text", 0, "+/v+"), // UTF7
            Signature("text", 0, "+/v/"), // UTF7
            Signature("text", 0, 0x0E, 0xFE, 0xFF), // SOSU compressed text
            Signature("pdf", 0, "%PDF"),
            Signature("wasm", 0, byteArrayOf(0), "asm"),
            Signature("ttf", 0, 0, 1, 0, 0, 0),// true type font
            Signature("woff1", 0, "wOFF"),
            Signature("woff2", 0, "wOF2"),
            Signature("lua-bytecode", 0, byteArrayOf(0x1B), "Lua"),
            Signature("shell", 0, "#!"),
            // images
            Signature("png", 0, byteArrayOf(0x89.toByte()), "PNG", byteArrayOf(0xd, 0xa, 0x1a, 0x0a)),
            Signature("jpg", 0, 0xFF, 0xD8, 0xFF, 0xDB),
            Signature("jpg", 0, 0xFF, 0xD8, 0xFF, 0xE0),
            Signature("jpg", 0, 0xFF, 0xD8, 0xFF, 0xEE),
            Signature("jpg", 0, 0xFF, 0xD8, 0xFF, 0xE1),
            bmp,
            Signature("psd", 0, "8BPS"), // photoshop image format
            Signature("hdr", 0, "#?RADIANCE"), // high dynamic range
            Signature("ico", 0, 0, 0, 1, 0, 1),// ico with 1 "image"
            Signature("ico", 0, 0, 0, 2, 0, 1),// cursor with 1 "image"
            Signature("dds", 0, "DDS "), // direct x image file format
            Signature("gif", 0, "GIF8"), // graphics interchange format, often animated
            Signature("gimp", 0, GimpImage.MAGIC), // gimp file
            Signature("qoi", 0, "qoif"),
            Signature("exr", 0, 0x76, 0x2f, 0x31, 0x01), // HDR image format, can be exported from Blender
            Signature("webp", 8, "WEBP"), // after RIFF header
            // other
            Signature("xml", 0, "<?xml"), // plus other variations with UTF16, UTF32, ...
            Signature("svg", 0, "<svg"),
            Signature("exe", 0, "MZ"),
            // media (video/audio)
            Signature("media", 0, 0x1A, 0x45, 0xDF, 0xA3), // mkv, mka, mks, mk3d, webm
            Signature("media", 0, "ID3"),// mp3 container
            Signature("media", 0, 0xFF, 0xFB),// mp3
            Signature("media", 0, 0xFF, 0xF3),// mp3
            Signature("media", 0, 0xFF, 0xF2),// mp3
            Signature("media", 0, "OggS"),// ogg, opus
            Signature("media", 0, "RIFF"),// can be a lot of stuff, e.g., wav, avi
            Signature("media", 0, "FLV"),// flv
            Signature("media", 0, 0x47),// mpeg stream
            Signature("media", 0, 0x00, 0x00, 0x01, 0xBA), // m2p, vob, mpg, mpeg
            Signature("media", 0, 0x00, 0x00, 0x01, 0xB3),// mpg, mpeg
            Signature("media", 4, "ftypisom"), // mp4
            Signature("media", 4, "ftypmp42"), // mp4
            Signature("media", 4, "ftypdash"), // m4a
            Signature("media", 4, "ftyp"), // probably media... (I am unsure)
            Signature("media", 0, 0x30, 0x26, 0xb2, 0x75, 0x8e, 0x66, 0xcf, 0x11), // wmv, wma, asf (Windows Media file)
            // meshes
            Signature("vox", 0, "VOX "),
            Signature("fbx", 0, "Kaydara FBX Binary"),
            Signature("fbx", 0, "; FBX "), // text fbx, is followed by a version
            Signature("obj", -1, "\nmtllib "),
            Signature("obj", -1, "OBJ File"),
            Signature("blend", 0, "BLENDER"),
            Signature("gltf", 0, "glTF"),
            Signature("mesh-draco", 0, "DRACO"),
            Signature("md2", 0, "IDP2"),
            Signature("md5mesh", 0, "MD5Version"),
            Signature("dae", -1, "<COLLADA"),
            // scenes and meshes from mitsuba renderer
            Signature("mitsuba-meshes", 0, byteArrayOf(0x1c, 0x04)),
            Signature("mitsuba-scene", 0, "<scene version="),
            Signature("mitsuba-scene", -1, "<scene version="),
            // unity support
            Signature("yaml", 0, "%YAML"),
            // json, kind of
            Signature("json", 0, "["),
            Signature("json", 0, "{"),
            Signature("sims", 0, "DBPF"),
            // windows link file
            Signature("lnk", 0, 0x4c),
            // window url file
            Signature("url", 0, "[InternetShortcut]")
        ).apply {
            // first long ones, then short ones; to be more specific first
            sortByDescending { it.bytes.size }
        }

    }

}