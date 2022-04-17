package me.anno.mesh.unreal

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.mesh.sims.Sims3Reader.skip
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

// the idea is, just like with the UnityReader, to read native Unreal Engine files,
// because there are assets in this format

// https://github.com/FabianFG/CUE4Parse

object UnrealReader {

    fun read(input: InputStream) {
        read(
            ByteBuffer.wrap(input.readBytes())
                .order(ByteOrder.LITTLE_ENDIAN)
        )
    }

    fun read(buffer: ByteBuffer) {
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

    fun <V> assert(a: V, b: V) {
        if (a != b) throw RuntimeException("$a != $b")
    }

    private const val MAGIC = 0x9e2a83c1.toInt()

}