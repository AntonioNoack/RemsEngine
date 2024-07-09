package me.anno.tests.mesh

import me.anno.io.files.Signature
import me.anno.maths.Maths.min
import me.anno.utils.types.Booleans.hasFlag
import me.anno.tests.LOGGER
import me.anno.utils.OS
import me.anno.utils.OS.desktop
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Buffers.skip
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * it would be interesting to read files from the Sims
 * a) we could support existing custom content
 * b) we could support meshes from The Sims -> nah, too many GB to read, and it's pretty complex anyway
 * */
fun main() {

    // extensions:
    // .package = general collection
    // .world = world
    // .dbc = collection of archives in single file

    val folder = OS.documents.getChild("Electronic Arts/Die Sims 3")
    // val ref = getReference(folder,"Thumbnails/ObjectThumbnails.package")
    // val ref = getReference(folder, "Saves/Fiora.sims3/France_0x0859db50.nhd")
    val ref = folder.getChild("Saves/Fiora.sims3/France_0x0859db50ExportDB.package")

    val dst = desktop.getChild("Sims")
    dst.tryMkdirs()
    // dst.listChildren()?.map { it.delete() }
    // DBPP would be encrypted data

    val input = ref.readByteBufferSync(false)
        .order(ByteOrder.LITTLE_ENDIAN)

    val magic = input.int
    if (magic != leMagic('D', 'B', 'P', 'F')) throw IOException("Incorrect magic: $magic")

    val major = input.int
    val minor = input.int
    LOGGER.info("Version: $major.$minor")

    input.skip(24)
    val numberOfEntries = input.int
    input.skip(4)
    val sizeOfIndexInBytes = input.int
    input.skip(12)
    val indexVersion = input.int
    if (indexVersion != 3) LOGGER.warn("Unknown index version: $indexVersion")
    val indexPosition = input.int

    LOGGER.info("#entries: $numberOfEntries, size: $sizeOfIndexInBytes, position: $indexPosition")

    if (indexPosition > 0) {
        input.position(indexPosition)
        val indexType = input.int
        // read index header
        // header defines what data is the same for all entries
        // read n entries
        val header = IndexHeader(input, indexType)
        val invIndexType = indexType.inv()
        val entries = createArrayList(numberOfEntries) {
            IndexEntry(input, invIndexType, header)
        }
        val buffer = ByteBuffer.allocate(entries.maxOf { it.memSize })
        for (entry in entries) {
            input.limit(entry.chunkOffset + entry.fileSize)
            input.position(entry.chunkOffset)
            val outputBuffer = if (entry.compressed > 0) {
                if (entry.compressed != 0xffff) LOGGER.warn("Unexpected value for compressed, ${entry.compressed} !in listOf(0, 0xffff)")
                buffer.position(0)
                buffer.limit(entry.memSize)
                uncompress(input, buffer)
                buffer
            } else {
                // read content normally, does not happen in our case
                input
            }
            val outputAsBytes = ByteArray(min(outputBuffer.remaining(), Signature.maxSampleSize))
            val pos = outputBuffer.position()
            outputBuffer.get(outputAsBytes)
            outputBuffer.position(pos)
            var extension = Signature.findName(outputAsBytes) ?: "bin"
            if ((0 until outputBuffer.remaining()).all { outputBuffer[it] == 0.toByte() })
                extension = "null"
            if (entry.compressed > 0)
                extension = "c.$extension"
            val ref2 = dst.getChild(entry.chunkOffset.toString(16) + ".$extension")
            ref2.writeBytes(outputBuffer)

            // http://simswiki.info/wiki.php?title=Sims_3:PackedFileTypes
            val type = when (entry.resourceType) {
                0x00AE6C67 -> "bone"
                0x00B2D882 -> "dds"
                0x00B552EA -> "speed tree resource"
                0x015A1849 -> "body geometry"
                0x0166038C -> "name map"
                0x01661233 -> "object geometry"
                0x01A527DB -> "audio snr"
                0x01D0E75D -> "material"
                0x01D0E76B -> "skin"
                0x01D10F34 -> "object lods"
                0x01EEF63A -> "audio sns (fx/music)"
                0x02019972 -> "scene graph"
                0x021D7E8C -> "speed tree data"
                0x025C90A6 -> "css"
                0x025C95B6 -> "xml for ui"
                0x025ED6F4 -> "outfit as xml"
                0x029E333B -> "voice mix"
                0x02C9EFF2 -> "audio submix"
                0x02D5DF13 -> "animation sequences"
                0x02DC343F -> "object information (type, scripts)"
                0x033260E3 -> "animation track mask"
                0x0333406C -> "xml resource, incl. tuning"
                0x033A1435 -> "texture compositor"
                0x0341ACC9 -> "fabric compositor"
                0x034AEECB -> "cas part data"
                0x0354796A -> "skin tone"
                0x03555BA8 -> "hair tone"
                0x0355E0A6 -> "bone delta / slot adjusts"
                0x0358B08A -> "face blends"
                0x03B33DDF -> "interaction tuning as xml"
                0x03B4C61D -> "params for lighting"
                0x03D843C2 -> "compositor cache entry"
                0x03D86EA4 -> "world lot information"
                0x0418FE2A -> "catalog fence"
                // and many more...
                else -> "not yet registered"
            }
            println("$type $entry")
        }
    }
}


/**
 * confirmed to work mostly correct:
 * I have dds files, which were correctly read
 * */
private fun uncompress(input: ByteBuffer, output: ByteBuffer) {
    // uncompress content
    val type = input.get() // 0x10 / 0x20 / 0x80
    if (input.get() != 0xFB.toByte()) throw IOException("Expected 0xFB")
    input.skip(if (type.toInt().hasFlag(0x80)) 4 else 3) // file size
    while (input.hasRemaining()) {
        // read control character
        // read 0-3 bytes
        // find how many / from where
        // read 0-n chars from source, and append to output
        // copy 0-n chars from somewhere in output to end of output
        val control = input.get().toInt().and(255)
        val numPlainText: Int
        var numToCopy: Int
        var copyOffset: Int
        when {
            control < 0x80 -> {
                val byte1 = input.get().toInt().and(255)
                numPlainText = control.and(3)
                numToCopy = (control.and(0x1c) shr 2) + 3
                copyOffset = (control.and(0x60) shl 3) + byte1 + 1
            }
            control < 0xC0 -> {
                val byte1 = input.get().toInt().and(255)
                val byte2 = input.get().toInt().and(255)
                numPlainText = byte1.and(0xc0).shr(6).and(3)
                numToCopy = control.and(0x3f) + 4
                copyOffset = (byte1.and(0x3f) shl 8) + byte2 + 1
            }
            control < 0xE0 -> {
                val b1 = input.get().toInt().and(255)
                val b2 = input.get().toInt().and(255)
                val b3 = input.get().toInt().and(255)
                numPlainText = control.and(3)
                numToCopy = (control.and(0xc).shl(6)) + b3 + 5
                copyOffset = control.and(0x10).shl(12) + b1.shl(8) + b2 + 1
            }
            control < 0xFC -> {
                numPlainText = control.and(0x1f).shl(2) + 4
                numToCopy = 0
                copyOffset = 0
            }
            else -> {
                numPlainText = control.and(3)
                numToCopy = 0
                copyOffset = 0
            }
        }
        for (i in 0 until numPlainText) {
            output.put(input.get())
        }
        copyOffset = -copyOffset
        val p0 = output.position()
        if (numToCopy + output.position() > output.limit()) {
            numToCopy = output.remaining()
            for (i in 0 until numToCopy) {
                output.put(output.get(p0 + copyOffset + i))
            }
            LOGGER.warn("too many bytes, and ${input.remaining()} input bytes remaining")
            break
        } else {
            for (i in 0 until numToCopy) {
                output.put(output.get(p0 + copyOffset + i))
            }
        }
    }
    if (output.hasRemaining()) {
        LOGGER.warn("Missing ${output.remaining()} bytes")
    }
    output.flip()
}

class IndexHeader(val input: ByteBuffer, val mask: Int) {
    val resourceType: Int = if (mask.hasFlag(1)) input.int else 0
    val resourceGroup: Int = if (mask.hasFlag(2)) input.int else 0
    val instanceHi: Int = if (mask.hasFlag(4)) input.int else 0
    val instanceLo: Int = if (mask.hasFlag(8)) input.int else 0
    val chunkOffset: Int = if (mask.hasFlag(16)) input.int else 0
    val fileSize: Int = if (mask.hasFlag(32)) input.int.and(0x7fffffff) else 0 // highest bit has unknown meaning
    val memSize: Int = if (mask.hasFlag(64)) input.int else 0
    val compressed: Int = if (mask.hasFlag(128)) input.int.and(0xffff) else 0 // high part is unknown
}

class IndexEntry(val input: ByteBuffer, val mask: Int, base: IndexHeader) {
    val resourceType: Int = if (mask.hasFlag(1)) input.int else base.resourceType
    val resourceGroup: Int = if (mask.hasFlag(2)) input.int else base.resourceGroup
    val instanceHi: Int = if (mask.hasFlag(4)) input.int else base.instanceHi
    val instanceLo: Int = if (mask.hasFlag(8)) input.int else base.instanceLo
    val chunkOffset: Int = if (mask.hasFlag(16)) input.int else base.chunkOffset
    val fileSize: Int = if (mask.hasFlag(32)) input.int.and(0x7fffffff) else base.fileSize
    val memSize: Int = if (mask.hasFlag(64)) input.int else base.memSize
    val compressed: Int = if (mask.hasFlag(128)) input.int.and(0xffff) else base.compressed
    override fun toString(): String {
        return "type $resourceType, group: $resourceGroup, " +
                "i-hi: ${instanceHi.toUInt().toString(16)}, " +
                "i-lo: ${instanceLo.toUInt().toString(16)}, " +
                "offset: ${chunkOffset.toString(16)}, " +
                "file-size: $fileSize, mem-size: $memSize, " +
                "compressed: $compressed"
    }
}

fun toStr(i: Int): String {
    return String(
        charArrayOf(
            i.shr(24).and(255).toChar(),
            i.shr(16).and(255).toChar(),
            i.shr(8).and(255).toChar(),
            i.and(255).toChar()
        )
    )
}

fun leMagic(b: Char, g: Char, r: Char, a: Char): Int {
    return (a.code shl 24) or (r.code shl 16) or (g.code shl 8) or b.code
}

fun beMagic(a: Char, r: Char, g: Char, b: Char): Int {
    return (a.code shl 24) or (r.code shl 16) or (g.code shl 8) or b.code
}
