package me.anno.io.files

import me.anno.graph.hdb.ByteSlice
import me.anno.io.files.ImportType.AUDIO
import me.anno.io.files.ImportType.CODE
import me.anno.io.files.ImportType.COMPILED
import me.anno.io.files.ImportType.CONTAINER
import me.anno.io.files.ImportType.CUBEMAP_EQU
import me.anno.io.files.ImportType.EXECUTABLE
import me.anno.io.files.ImportType.FONT
import me.anno.io.files.ImportType.IMAGE
import me.anno.io.files.ImportType.LINK
import me.anno.io.files.ImportType.MESH
import me.anno.io.files.ImportType.METADATA
import me.anno.io.files.ImportType.TEXT
import me.anno.io.files.ImportType.VIDEO
import me.anno.utils.Color.hex8
import me.anno.utils.types.Ranges.size
import java.nio.ByteBuffer
import kotlin.math.min

/**
 * Most file formats specify a magic value in the first bytes.
 * This allows us to detect what kind of content a file contains.
 *
 * This class stores the data, and is a registry for known file signatures.
 * */
class Signature(
    val name: String,
    val importType: String,
    private val offset: Int,
    private val pattern: ByteArray
) {

    constructor(name: String, importType: String, offset: Int, signature: String) :
            this(name, importType, offset, signature.encodeToByteArray())

    constructor(name: String, importType: String, offset: Int, signature: String, vararg extraBytes: Int) : this(
        name, importType, offset,
        signature.encodeToByteArray() + ByteArray(extraBytes.size) { extraBytes[it].toByte() })

    constructor(
        name: String, importType: String, offset: Int,
        prefix: ByteArray, signature: String, extraBytes: ByteArray
    ) : this(name, importType, offset, prefix + signature.encodeToByteArray() + extraBytes)

    constructor(name: String, importType: String, offset: Int, prefix: ByteArray, signature: String) :
            this(name, importType, offset, prefix + signature.encodeToByteArray())

    constructor(name: String, importType: String, offset: Int, vararg bytes: Int) :
            this(name, importType, offset, ByteArray(bytes.size) { bytes[it].toByte() })

    val order = if (offset < 0) {
        // bad format: no identifier, so test it last
        1024 - pattern.size
    } else {
        // test long ones first, because they are more specific
        -pattern.size
    }

    fun matches(bytes: ByteBuffer): Boolean {
        val position = bytes.position()
        val size = min(bytes.remaining(), maxSampleSize)
        if (offset >= size) return false
        if (offset < 0) {
            // search the signature instead of requiring it
            search@ for (offset in 0 until size - pattern.size) {
                for (i in 0 until min(size - offset, pattern.size)) {
                    if (bytes[position + i + offset] != pattern[i]) {
                        continue@search
                    }
                }
                return true
            }
            return false
        } else {
            for (i in 0 until min(size - offset, pattern.size)) {
                if (bytes[position + i + offset] != pattern[i]) return false
            }
            return true
        }
    }

    fun matches(bytes: ByteArray, range: IntRange): Boolean {
        val bytesSize = range.size
        val size = min(bytesSize, maxSampleSize)
        if (offset >= bytesSize) return false
        if (offset < 0) {
            // search the signature instead of requiring it
            search@ for (offset in 0 until size - pattern.size) {
                val readOffset = offset + range.first
                for (i in 0 until min(size - offset, pattern.size)) {
                    if (bytes[i + readOffset] != pattern[i]) {
                        continue@search
                    }
                }
                return true
            }
            return false
        } else {
            val readOffset = offset + range.first
            for (i in 0 until min(size - offset, pattern.size)) {
                if (bytes[i + readOffset] != pattern[i]) return false
            }
            return true
        }
    }

    override fun toString() = "Signature { \"$name\" by [${pattern.joinToString { hex8(it.toInt()) }}] + $offset }"

    companion object {

        const val sampleSize = 128
        const val maxSampleSize = 4096

        fun findName(bytes: ByteArray) = find(bytes)?.name
        fun find(bytes: ByteArray): Signature? {
            return find(bytes, bytes.indices)
        }

        fun findName(bytes: ByteSlice) = find(bytes)?.name
        fun find(bytes: ByteSlice): Signature? {
            return find(bytes.bytes, bytes.range)
        }

        fun findName(bytes: ByteArray, range: IntRange) = find(bytes, range)?.name
        fun find(bytes: ByteArray, range: IntRange): Signature? {
            val nonHashed = signatures
            for (i in nonHashed.indices) {
                val s = nonHashed[i]
                if (s.matches(bytes, range)) {
                    return s
                }
            }
            return null
        }

        fun register(signature: Signature) {
            // alternatively could find the correct insert index
            // still would be O(n)
            var index = signatures.binarySearch {
                signature.order.compareTo(it.order)
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

        val bmp = Signature("bmp", IMAGE, 0, "BM")
        val json = Signature("json", METADATA, 0, "[")

        // source: https://en.wikipedia.org/wiki/List_of_file_signatures
        // https://www.garykessler.net/library/file_sigs.html
        @Suppress("SpellCheckingInspection")
        private val signatures = arrayListOf(
            Signature("bz2", CONTAINER, 0, "BZh"),
            Signature("rar", CONTAINER, 0, "Rar!", 0x1a, 0x07),
            Signature("zip", CONTAINER, 0, "PK", 3, 4),
            Signature("zip", CONTAINER, 0, "PK", 5, 6), // "empty archive" after wikipedia
            Signature("zip", CONTAINER, 0, "PK", 7, 8), // "spanned archive"
            Signature("tar", CONTAINER, 0, 0x1F, 0x9D), // lempel-ziv-welch
            Signature("tar", CONTAINER, 0, 0x1F, 0xA0),// lzh
            // Signature("tar", 257, "ustar"), // this large offset is unfortunate; we'd have to adjust the signature readout for ALL others
            Signature("gzip", CONTAINER, 0, 0x1F, 0x8B), // gz/tar.gz
            Signature("xz", CONTAINER, 0, byteArrayOf(0xFD.toByte()), "7zXZ", byteArrayOf(0)), // xz compression
            Signature("lz4", CONTAINER, 0, 0x04, 0x22, 0x4D, 0x18), // another compression
            Signature("7z", CONTAINER, 0, "7z", 0xBC, 0xAF, 0x27, 0x1C),
            Signature("xar", CONTAINER, 0, "xar!"), // file compression for apple stuff?
            Signature("oar", CONTAINER, 0, "OAR"), // oar compression (mmh)
            Signature("java", CODE, 0, 0xCA, 0xFE, 0xBA, 0xBE), // java class
            Signature("text", TEXT, 0, 0xEF, 0xBB, 0xBF), // UTF8
            Signature("text", TEXT, 0, 0xFF, 0xFE), // UTF16
            Signature("text", TEXT, 0, 0xFE, 0xFF),
            Signature("text", TEXT, 0, 0xFF, 0xFE, 0, 0), // UTF32
            Signature("text", TEXT, 0, 0xFE, 0xFF, 0, 0),
            Signature("text", TEXT, 0, "+/v8"), // UTF7
            Signature("text", TEXT, 0, "+/v9"), // UTF7
            Signature("text", TEXT, 0, "+/v+"), // UTF7
            Signature("text", TEXT, 0, "+/v/"), // UTF7
            Signature("text", TEXT, 0, 0x0E, 0xFE, 0xFF), // SOSU compressed text
            Signature("pdf", "PDF", 0, "%PDF"),
            Signature("wasm", COMPILED, 0, byteArrayOf(0), "asm"),
            Signature("ttf", FONT, 0, 0, 1, 0, 0, 0),// true type font
            Signature("woff1", FONT, 0, "wOFF"),
            Signature("woff2", FONT, 0, "wOF2"),
            Signature("lua-bytecode", COMPILED, 0, byteArrayOf(0x1B), "Lua"),
            Signature("shell", CODE, 0, "#!"),
            // images
            Signature("png", IMAGE, 0, byteArrayOf(0x89.toByte()), "PNG", byteArrayOf(0xd, 0xa, 0x1a, 0x0a)),
            Signature("jpg", IMAGE, 0, 0xFF, 0xD8, 0xFF, 0xDB),
            Signature("jpg", IMAGE, 0, 0xFF, 0xD8, 0xFF, 0xE0),
            Signature("jpg", IMAGE, 0, 0xFF, 0xD8, 0xFF, 0xEE),
            Signature("jpg", IMAGE, 0, 0xFF, 0xD8, 0xFF, 0xE1),
            bmp,
            Signature("psd", IMAGE, 0, "8BPS"), // photoshop image format
            Signature("hdr", CUBEMAP_EQU, 0, "#?RADIANCE"), // high dynamic range
            Signature("ico", IMAGE, 0, 0, 0, 1, 0, 1),// ico with 1 "image"
            Signature("ico", IMAGE, 0, 0, 0, 2, 0, 1),// cursor with 1 "image"
            Signature("dds", IMAGE, 0, "DDS "), // direct x image file format
            Signature("gif", VIDEO, 0, "GIF8"), // graphics interchange format, often animated
            Signature("gimp", IMAGE, 0, "gimp xcf "), // gimp image file
            Signature("qoi", IMAGE, 0, "qoif"),
            Signature("exr", IMAGE, 0, 0x76, 0x2f, 0x31, 0x01), // HDR image format, can be exported from Blender
            Signature("webp", IMAGE, 8, "WEBP"), // after RIFF header
            Signature("aseprite", VIDEO, 4, 0xE0, 0xA5), // magic comes after size
            // tga has header at the end of the file, and only sometimes...
            // other
            Signature("xml", METADATA, 0, "<?xml"), // plus other variations with UTF16, UTF32, ...
            // are we using 1.0??
            Signature("xml-re", METADATA, 0, "<?xml version=\"1.0\" encoding=\"utf-8\"?><RemsEngine"),
            Signature("xml-re", METADATA, 0, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<RemsEngine"),
            Signature("svg", IMAGE, 0, "<svg"),
            Signature("exe", EXECUTABLE, 0, "MZ"),
            Signature("rem", METADATA, 0, "RemsEngineZZ"), // deflate-compressed binary format for Rem's Engine
            // media (video/audio)
            Signature("media", VIDEO, 0, 0x1A, 0x45, 0xDF, 0xA3), // mkv, mka, mks, mk3d, webm
            Signature("media", AUDIO, 0, "ID3"),// mp3 container
            Signature("media", AUDIO, 0, 0xFF, 0xFB),// mp3
            Signature("media", AUDIO, 0, 0xFF, 0xF3),// mp3
            Signature("media", AUDIO, 0, 0xFF, 0xF2),// mp3
            Signature("media", VIDEO, 0, "OggS"),// ogg, opus
            Signature("media", VIDEO, 0, "RIFF"),// can be a lot of stuff, e.g., wav, avi
            Signature("media", VIDEO, 0, "FLV"),// flv, flash video format
            Signature("media", VIDEO, 0, 0x47),// mpeg stream
            Signature("media", VIDEO, 0, 0x00, 0x00, 0x01, 0xBA), // m2p, vob, mpg, mpeg
            Signature("media", VIDEO, 0, 0x00, 0x00, 0x01, 0xB3),// mpg, mpeg
            Signature("media", VIDEO, 4, "ftypisom"), // mp4
            Signature("media", VIDEO, 4, "ftypmp42"), // mp4
            Signature("media", VIDEO, 4, "ftypdash"), // m4a
            Signature("media", VIDEO, 4, "ftyp"), // probably media... (I am unsure)
            // wmv, wma, asf (Windows Media file)
            Signature("media", VIDEO, 0, 0x30, 0x26, 0xb2, 0x75, 0x8e, 0x66, 0xcf, 0x11),
            // meshes
            Signature("vox", MESH, 0, "VOX "),
            Signature("fbx", MESH, 0, "Kaydara FBX Binary"),
            Signature("fbx", MESH, 0, "; FBX "), // text fbx, is followed by a version
            Signature("obj", MESH, -1, "\nmtllib "),
            Signature("obj", MESH, -1, "OBJ File"),
            Signature("obj", MESH, 0, "o "), // ^^, stripped, very compact obj
            Signature("mtl", MESH, -1, "newmtl "),
            Signature("mtl", MESH, 0, "# Blender MTL"), // ^^
            Signature("blend", MESH, 0, "BLENDER"),
            Signature("blend-zstd", MESH, 0, 0x28 /* ( */, 0xb5, 0x2f /* / */, 0xfd), // Zstd compressed-file
            Signature("gltf", MESH, 0, "glTF"),
            Signature("gltf", MESH, -1, "\"scenes\""),
            Signature("mesh-draco", MESH, 0, "DRACO"),
            Signature("md2", MESH, 0, "IDP2"),
            Signature("md5mesh", MESH, 0, "MD5Version"),
            Signature("dae", MESH, -1, "<COLLADA"),
            Signature("maya", MESH, 0, "//Maya ASCII "),
            Signature("ply", MESH, 0, "ply"),
            // scenes and meshes from mitsuba renderer
            Signature("mitsuba-meshes", MESH, 0, byteArrayOf(0x1c, 0x04)),
            Signature("mitsuba-scene", MESH, 0, "<scene version="),
            Signature("mitsuba-scene", MESH, -1, "<scene version="),
            // unity support
            Signature("yaml", METADATA, 0, "%YAML"),
            Signature("yaml-re", METADATA, 0, "RemsEngine:\n - class:"),
            Signature("yaml-re", METADATA, 0, "RemsEngine:\n  - class:"),
            // json, kind of
            json,
            Signature("json", METADATA, 0, "[{"),
            Signature("json", METADATA, 0, "[{\"class\":"),
            Signature("json", METADATA, 0, "{"),
            Signature("sims", METADATA, 0, "DBPF"),
            // windows link file
            Signature("lnk", LINK, 0, byteArrayOf(0x4c, 0, 0, 0)),
            // window url file
            Signature("url", LINK, 0, "[InternetShortcut]"),
        ).apply {
            // first long ones, then short ones; to be more specific first
            sortBy { it.order }
        }
    }
}