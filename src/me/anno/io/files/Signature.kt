package me.anno.io.files

import me.anno.ecs.prefab.PrefabReadable
import me.anno.image.gimp.GimpImage
import me.anno.io.zip.SignatureFile
import java.nio.ByteBuffer
import kotlin.math.min

class Signature(val name: String, val offset: Int, val signature: ByteArray) {

    constructor(name: String, offset: Int, signature: String) : this(name, offset, signature.toByteArray())

    constructor(name: String, offset: Int, signature: String, extraBytes: ByteArray) : this(
        name, offset,
        signature.toByteArray() + extraBytes
    )

    constructor(name: String, offset: Int, signature: String, extraBytes: List<Int>) : this(
        name, offset,
        signature.toByteArray() + extraBytes.map { it.toByte() }.toByteArray()
    )

    constructor(name: String, offset: Int, prefix: ByteArray, signature: String, extraBytes: ByteArray) : this(
        name, offset,
        prefix + signature.toByteArray() + extraBytes
    )

    constructor(name: String, offset: Int, prefix: ByteArray, signature: String) : this(
        name, offset,
        prefix + signature.toByteArray()
    )

    constructor(name: String, offset: Int, bytes: List<Int>) : this(
        name, offset,
        bytes.map { it.toByte() }.toByteArray()
    )

    fun matches(bytes: ByteBuffer): Boolean {
        val position = bytes.position()
        val size = bytes.remaining()
        if (offset >= size) return false
        if (offset < 0) {
            // search the signature instead of requiring it
            search@ for (offset in 0 until size - signature.size) {
                for (i in 0 until min(size - offset, signature.size)) {
                    if (bytes[position + i + offset] != signature[i]) {
                        continue@search
                    }
                }
                return true
            }
            return false
        } else {
            for (i in 0 until min(size - offset, signature.size)) {
                if (bytes[position + i + offset] != signature[i]) return false
            }
            return true
        }
    }

    fun matches(bytes: ByteArray): Boolean {
        if (offset >= bytes.size) return false
        if (offset < 0) {
            // search the signature instead of requiring it
            search@ for (offset in 0 until bytes.size - signature.size) {
                for (i in 0 until min(bytes.size - offset, signature.size)) {
                    if (bytes[i + offset] != signature[i]) {
                        continue@search
                    }
                }
                return true
            }
            return false
        } else {
            for (i in 0 until min(bytes.size - offset, signature.size)) {
                if (bytes[i + offset] != signature[i]) return false
            }
            return true
        }
    }

    override fun toString(): String = name

    companion object {

        const val sampleSize = 128

        val bmp = Signature("bmp", 0, "BM")

        fun findName(bytes: ByteBuffer) = find(bytes)?.name
        fun find(bytes: ByteBuffer): Signature? {
            for (signature in signatures) {
                if (signature.matches(bytes)) {
                    return signature
                }
            }
            return null
        }

        fun findName(bytes: ByteArray) = find(bytes)?.name
        fun find(bytes: ByteArray): Signature? {
            for (signature in signatures) {
                if (signature.matches(bytes)) {
                    return signature
                }
            }
            return null
        }

        fun register(signature: Signature) {
            // alternatively could find the correct insert index
            // still would be O(n)
            signatures.add(signature)
            signatures.sortByDescending { it.signature.size }
        }

        @Suppress("unused")
        fun unregister(signature: Signature) {
            // could use binary search to find signature
            // still would be O(n)
            signatures.remove(signature)
        }

        fun findName(fileReference: FileReference) = find(fileReference)?.name
        fun find(fileReference: FileReference): Signature? {
            if (fileReference is SignatureFile) return fileReference.signature
            if (!fileReference.exists) return null
            return when (fileReference) {
                is PrefabReadable -> signatures.first { it.name == "json" }
                else -> {
                    // reads the bytes, or 255 if at end of file
                    // how much do we read? 🤔
                    // some formats are easy, others require more effort
                    // maybe we could read them piece by piece...
                    fileReference.inputStream().use { input ->
                        find(ByteArray(sampleSize) { input.read().toByte() })
                    }
                }
            }
        }

        // source: https://en.wikipedia.org/wiki/List_of_file_signatures
        // https://www.garykessler.net/library/file_sigs.html
        @Suppress("SpellCheckingInspection")
        val signatures = arrayListOf(
            Signature("bz2", 0, "BZh"),
            Signature("rar", 0, "Rar!", byteArrayOf(0x1a, 0x07)),
            Signature("zip", 0, "PK", byteArrayOf(3, 4)),
            Signature("zip", 0, "PK", byteArrayOf(5, 6)), // "empty archive" after wikipedia
            Signature("zip", 0, "PK", byteArrayOf(7, 8)), // "spanned archive"
            Signature("tar", 0, listOf(0x1F, 0x9D)), // lempel-ziv-welch
            Signature("tar", 0, listOf(0x1F, 0xA0)),// lzh
            // Signature("tar", 257, "ustar"), // this large offset is unfortunate; we'd have to adjust the signature readout for ALL others
            Signature("gzip", 0, listOf(0x1F, 0x8B)), // gz/tar.gz
            Signature("xz", 0, byteArrayOf(0xFD.toByte()), "7zXZ", byteArrayOf(0)), // xz compression
            Signature("lz4", 0, byteArrayOf(0x04, 0x22, 0x4D, 0x18)), // another compression
            Signature("7z", 0, "7z", listOf(0xBC, 0xAF, 0x27, 0x1C)),
            Signature("xar", 0, "xar!"), // file compression for apple stuff?
            Signature("oar", 0, "OAR"), // oar compression (mmh)
            Signature("java", 0, listOf(0xCA, 0xFE, 0xBA, 0xBE)), // java class
            Signature("text", 0, listOf(0xEF, 0xBB, 0xBF)), // UTF8
            Signature("text", 0, listOf(0xFF, 0xFE)), // UTF16
            Signature("text", 0, listOf(0xFE, 0xFF)),
            Signature("text", 0, listOf(0xFF, 0xFE, 0, 0)), // UTF32
            Signature("text", 0, listOf(0xFE, 0xFF, 0, 0)),
            Signature("text", 0, "+/v8"), // UTF7
            Signature("text", 0, "+/v9"), // UTF7
            Signature("text", 0, "+/v+"), // UTF7
            Signature("text", 0, "+/v/"), // UTF7
            Signature("text", 0, listOf(0x0E, 0xFE, 0xFF)), // SOSU compressed text
            Signature("pdf", 0, "%PDF"),
            Signature("wasm", 0, byteArrayOf(0), "asm"),
            Signature("ttf", 0, listOf(0, 1, 0, 0, 0)),// true type font
            Signature("woff1", 0, "wOFF"),
            Signature("woff2", 0, "wOF2"),
            Signature("lua-bytecode", 0, byteArrayOf(0x1B), "Lua"),
            Signature("shell", 0, "#!"),
            // images
            Signature("png", 0, byteArrayOf(0x89.toByte()), "PNG", byteArrayOf(0xd, 0xa, 0x1a, 0x0a)),
            Signature("jpg", 0, listOf(0xFF, 0xD8, 0xFF, 0xDB)),
            Signature("jpg", 0, listOf(0xFF, 0xD8, 0xFF, 0xE0)),
            Signature("jpg", 0, listOf(0xFF, 0xD8, 0xFF, 0xEE)),
            Signature("jpg", 0, listOf(0xFF, 0xD8, 0xFF, 0xE1)),
            bmp,
            Signature("psd", 0, "8BPS"), // photoshop image format
            Signature("hdr", 0, "#?RADIANCE"), // high dynamic range
            Signature("ico", 0, listOf(0x00, 0x00, 0x01, 0x00, 0x01)),// ico with 1 "image"
            Signature("ico", 0, listOf(0x00, 0x00, 0x02, 0x00, 0x01)),// cursor with 1 "image"
            Signature("dds", 0, "DDS "), // direct x image file format
            Signature("gif", 0, "GIF8"), // graphics interchange format, often animated
            Signature("gimp", 0, GimpImage.MAGIC), // gimp file
            // todo qoi support?, https://github.com/phoboslab/qoi, https://github.com/saharNooby/qoi-java
            Signature("qoi", 0, "qoif"),
            // todo exr reader, https://openexr.readthedocs.io/en/latest/OpenEXRFileLayout.html
            Signature("exr", 0, listOf(0x76, 0x2f, 0x31, 0x01)), // HDR image format, can be exported from Blender
            // other
            Signature("xml", 0, "<?xml"), // plus other variations with UTF16, UTF32, ...
            Signature("exe", 0, "MZ"),
            // media (video/audio)
            Signature("media", 0, listOf(0x1A, 0x45, 0xDF, 0xA3)), // mkv, mka, mks, mk3d, webm
            Signature("media", 0, "ID3"),// mp3 container
            Signature("media", 0, listOf(0xFF, 0xFB)),// mp3
            Signature("media", 0, listOf(0xFF, 0xF3)),// mp3
            Signature("media", 0, listOf(0xFF, 0xF2)),// mp3
            Signature("media", 0, "OggS"),// ogg
            Signature("media", 0, "RIFF"),// can be a lot of stuff, e.g. wav, avi
            Signature("media", 0, "FLV"),// flv
            Signature("media", 0, listOf(0x47)),// mpeg stream
            Signature("media", 0, listOf(0x00, 0x00, 0x01, 0xBA)), // m2p, vob, mpg, mpeg
            Signature("media", 0, listOf(0x00, 0x00, 0x01, 0xB3)),// mpg, mpeg
            Signature("media", 4, "ftypisom"), // mp4
            Signature("media", 4, "ftypmp42"), // mp4
            Signature(
                "media",
                0,
                listOf(0x30, 0x26, 0xb2, 0x75, 0x8e, 0x66, 0xcf, 0x11)
            ), // wmv, wma, asf (Windows Media file)
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
            // unity support
            Signature("yaml", 0, "%YAML"),
            // json, kind of
            Signature("json", 0, "["),
            Signature("json", 0, "{"),
            Signature("sims", 0, "DBPF")
        ).apply {
            // first long ones, then short ones; to be more specific first
            sortByDescending { it.signature.size }
        }

    }

}