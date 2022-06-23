package me.anno.mesh.unreal

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.types.Buffers.skip
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

// the idea is, just like with the UnityReader, to read native Unreal Engine files,
// because there are assets in this format

// https://github.com/FabianFG/CUE4Parse

object UnrealReader {

    @JvmStatic
    fun main(args: Array<String>) {
        val ref = getReference(
            "" +
                    "E:/Assets/Polygon_Office_Unreal_4_22_01.zip/" +
                    "PolygonOffice/Content/PolygonOffice/Meshes/Generic/" +
                    "SM_Generic_Cloud_01.uasset"
        )
        ref.inputStream().use { read(it) }
    }

    fun read(input: InputStream) {
        read(
            ByteBuffer.wrap(input.readBytes())
                .order(ByteOrder.LITTLE_ENDIAN)
        )
    }

    val packageFileTag = 0x9E2A83C1.toInt()

    fun readPackageSummary(buffer: ByteBuffer) {
        val tag = buffer.int
        assert(tag, MAGIC)
        val legacyFileVersion = buffer.int
        var currentLegacyFileVersion = -8
        if (legacyFileVersion < 0) {
            // modern version numbers
            if (legacyFileVersion < currentLegacyFileVersion) {
                throw IllegalStateException("cannot safely load")
            }
            if (legacyFileVersion != -4) {
                val legacyUE3Version = buffer.int
            }
            fileVersionUE4 = buffer.int
            if (legacyFileVersion <= -8) {
                fileVersionUE5 = buffer.int
            }
            // todo read license version
            val customVersionContainer =
                if (legacyFileVersion <= -2) readArray { readCustomVersion() } else emptyArray()
            if (fileVersionUE4 == 0 && fileVersionUE5 == 0 && fileVersionLicensee == 0) {
                unversioned = true
                fileVersionUE = 0 // todo Ar.Ver
                fileVersionLicensee // ...
            } else {
                unversioned = false

            }
        } else throw RuntimeException("Legacy UE3 is not supported")

        /*val totalHeaderSize = buffer.int
        val folderName = readFString()
        val packageFlags = readPackageFlags()

        val nameCount = buffer.int
        val nameOffset = buffer.int

        if(!packageFlags.hasFlag())*/

    }

    var fileVersionUE4 = 0
    var fileVersionUE5 = 0
    var unversioned = false
    var fileVersionUE = 0
    var fileVersionLicensee = 0

    fun <V> readArray(readElement: () -> V): Array<V> {
        TODO()
    }

    fun readCustomVersion() {
        TODO()
    }

    fun read(buffer: ByteBuffer) {

        // https://github.com/FabianFG/CUE4Parse/blob/1fc543a0434c8a2aa020443a377659fd4a908895/CUE4Parse/UE4/Assets/Package.cs
        // https://github.com/FabianFG/CUE4Parse/blob/1fc543a0434c8a2aa020443a377659fd4a908895/CUE4Parse/UE4/Objects/UObject/FPackageFileSummary.cs#L32

        // http://wiki.xentax.com/index.php/Unreal_Engine_4_UASSET
        assert(buffer.int, MAGIC)
        val version = buffer.int xor 255
        buffer.skip(16)
        buffer.int // file directory offset?
        buffer.int // unknown
        val packageName = buffer.int
        buffer.int // null
        val numNames = buffer.int
        val nameDirOffset = buffer.int
        buffer.skip(8)
        val numExports = buffer.int
        val exportDirOffset = buffer.int
        val numImports = buffer.int
        val importDirOffset = buffer.int
        buffer.skip(20)
        val guidHash = buffer.long to buffer.long // 16 bytes
        buffer.skip(12)
        buffer.skip(32)
        buffer.skip(8)
        val paddingOffset = buffer.int
        val fileLengthSometimes = buffer.int
        buffer.skip(12)
        buffer.skip(4)
        val filesDataOffset = buffer.int
        println("name vs offset: ${buffer.position()} vs $nameDirOffset")
        val names = Array(numNames) {
            val length = buffer.int // including null
            println("length $it/$numNames: $length")
            val name = String(ByteArray(length - 1) { buffer.get() })
            println("name: $name")
            assert(buffer.get(), 0.toByte()) // \0
            val flags = buffer.int
            name to flags
        }
        val imports = Array(numImports) {
            val parentDirNameId = buffer.long
            val classId = buffer.long
            val parentImportObjectId = buffer.int xor 255
            val nameId = buffer.int
            buffer.int // unknown
        }
        // unknown
        buffer.skip(numExports * 100)
        /*val exports = Array(numExports){

        }*/
        buffer.skip(4) // padding

    }

    fun <V> assert(a: V, b: V) {
        if (a != b) throw RuntimeException("$a != $b")
    }

    private const val MAGIC = 0x9e2a83c1.toInt()

}