package me.anno.mesh.usd

import me.anno.io.binary.ByteArrayIO.readLE16
import me.anno.io.binary.ByteArrayIO.readLE32
import me.anno.io.binary.ByteArrayIO.readLE64
import me.anno.mesh.usd.CompressedPaths.buildDecompressedPaths
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Arrays.indexOf
import me.anno.utils.types.Arrays.startsWith
import me.anno.utils.types.Booleans.hasFlag
import org.apache.logging.log4j.LogManager

class USDCReader(val data: ByteArray, val path: String) {

    companion object {
        private val LOGGER = LogManager.getLogger(USDCReader::class)
    }

    var pos = 0

    var major = 0
    var minor = 0
    var patch = 0

    lateinit var tokens: List<String>
    lateinit var strings: IntArray
    lateinit var paths: Array<USDNode>

    fun read(): USDPrim {
        readHeader()
        readTOC()
        readTokens(sections["TOKENS"])
        readStrings(sections["STRINGS"])
        readFields(sections["FIELDS"])
        readFieldSets(sections["FIELDSETS"])
        readPaths(sections["PATHS"])
        readSpecs(sections["SPECS"])
        TODO()
    }

    fun readHeader() {
        consume("PXR-USDC")
        major = data[pos++].toInt()
        minor = data[pos++].toInt()
        patch = data[pos].toInt()
        pos += 6 // unused
        println("USDC $major.$minor.$patch")

    }

    fun readTOC() {
        val toc = data.readLE64(pos)
        pos += 16 // 8 bytes extra are unused
        println("TOC: $toc (0x${toc.toString(16)})")
        readTOC(toc)
    }

    data class Section(val offset: Int, val size: Int)

    val sections = HashMap<String, Section>()

    fun readTOC(toc: Long) {
        check(toc + 8 < data.size)
        pos = toc.toInt()
        val numSections = readLE64()
        check(toc + 8 + numSections * 32 <= data.size) { "Not enough space for sections" }
        for (i in 0 until numSections) {
            val name = readString16()
            val start = readLE64()
            val size = readLE64()
            println("Section '$name' @$start += $size")
            check(start > 0 && start + size <= data.size)
            sections[name] = Section(start.toInt(), size.toInt())
        }
    }

    fun readTokens(section: Section?) {
        section ?: return
        pos = section.offset
        val numTokens = readLE64() + 1 // ;-) is not counted
        val decompressedSize = readLE64()
        val compressedSize = readLE64()
        // println("start for compressed tokens: $pos (0x${pos.toString(16)})")
        // pos = (pos + alignment).and(alignment.inv())
        check(pos + compressedSize <= data.size)
        check(decompressedSize <= Int.MAX_VALUE.toLong())
        // println("#tokens: $numTokens, decompressed size: $decompressedSize, compressed size: $compressedSize")
        val compressedData = data.copyOfRange(pos, pos + compressedSize.toInt())
            .copyOf(compressedSize.toInt() + 128)
        // println("compressed data: $compressedSize, ${compressedData.toReadable()}, $numTokens tokens")
        val decompressedData = ByteArray(decompressedSize.toInt())
        val resultSize = LZ4.decompressFromBuffer(
            compressedData,
            compressedSize.toInt(),
            decompressedData, decompressedSize.toInt(),
        )
        check(decompressedData.startsWith(";-)\u0000", 0))
        tokens = decompressedData.decodeToString(0, resultSize)
            .split(0.toChar())
        assertEquals(tokens.size, numTokens.toInt())
    }

    fun readStrings(section: Section?) {
        section ?: return
        pos = section.offset

        val numIndices = readLE64()
        check(numIndices <= Int.MAX_VALUE)

        strings = IntArray(numIndices.toInt()) {
            readLE32()
        }

        println("Strings: ${strings.toList()}")
    }

    fun readFields(section: Section?) {
        section ?: return
        pos = section.offset

        val numFields = readLE64()
        println("#fields: $numFields")
        if (numFields == 0L) return
        check(numFields <= Int.MAX_VALUE)

        // field[i].token_index.value
        val indices = readCompressedIntArray(numFields.toInt())
        println("Indices: ${indices.toList()}")
        fieldNames = List(numFields.toInt()) { index ->
            tokens[indices[index]]
        }

        val compressedSize = readLE64()
        check(pos + compressedSize <= data.size)
        val compressed = data.copyOfRange(pos, pos + compressedSize.toInt())
        val decompressedSize = numFields.toInt() * 8
        val decompressed = ByteArray(decompressedSize)
        val numRead = LZ4.decompressFromBuffer(
            compressed, compressed.size,
            decompressed, decompressedSize, null
        )
        assertEquals(decompressedSize, numRead)

        fieldTypes = LongArray(numFields.toInt()) { index ->
            decompressed.readLE64(index * 8)
        }

        for (i in 0 until numFields.toInt()) {
            println("Field[$i] name=${fieldNames[i]}, value=${formatType(fieldTypes[i])}")
        }
    }

    lateinit var fieldNames: List<String>
    lateinit var fieldTypes: LongArray

    fun formatType(type: Long): String {
        var str = type.getValueType().name
        if (type.isArray()) str = "$str[]"
        if (type.isInlined()) str = "inline $str"
        if (type.isCompressed()) str = "comp $str"
        str += ", payload=${type.payload()}"
        return str
    }

    val valueTypeArray = 1L shl 63
    val valueTypeInlined = 1L shl 62
    val valueTypeCompressed = 1L shl 61
    val valueTypePayloadMask = (1L shl 48) - 1
    val valueTypeTypeMask = 255L shl 48

    fun Long.getValueType() =
        ValueType.entries.getOrNull((this ushr 48).and(255).toInt())
            ?: ValueType.INVALID

    fun Long.isInlined() = this.hasFlag(valueTypeInlined)
    fun Long.isArray() = this.hasFlag(valueTypeArray)
    fun Long.isCompressed() = this.hasFlag(valueTypeCompressed)
    fun Long.payload() = this and valueTypePayloadMask

    fun readFieldSets(section: Section?) {
        section ?: return
        pos = section.offset

        val numFieldSets = readLE64()
        check(numFieldSets <= Int.MAX_VALUE)

        check(numFieldSets > 0) { "At least one field index must exist" }
        fieldSetIndices = readCompressedIntArray(numFieldSets.toInt())

        println("fieldSetIndices: ${fieldSetIndices.toList()}")
    }

    lateinit var fieldSetIndices: IntArray

    fun readPaths(section: Section?) {
        section ?: return
        pos = section.offset

        val numPaths = readLE64()
        check(numPaths <= Int.MAX_VALUE)

        // val paths = readCompressedPaths(numPaths.toInt())
        val numEncodedPaths = readLE64()
        println("#paths: $numPaths, #encodedPaths: $numEncodedPaths")

        check(numPaths >= numEncodedPaths)
        val pathIndices = readCompressedIntArray(numEncodedPaths.toInt())
        val elementTokenIndices = readCompressedIntArray(numEncodedPaths.toInt())
        val jumps = readCompressedIntArray(numEncodedPaths.toInt())

        println("pathIndices: ${pathIndices.toList()}")
        println("elementTokenIndices: ${elementTokenIndices.toList()}")
        println("jumps: ${jumps.toList()}")

        paths = buildDecompressedPaths(pathIndices, elementTokenIndices, jumps, tokens)
        for (path in paths) {
            println(path)
        }
    }

    fun readSpecs(section: Section?) {
        section ?: return
        pos = section.offset

        val numSpecs = readLE64()
        check(numSpecs <= Int.MAX_VALUE)

        val pathIndices = readCompressedIntArray(numSpecs.toInt())
        val fieldSetIndices = readCompressedIntArray(numSpecs.toInt())
        val forms = readCompressedIntArray(numSpecs.toInt())

        println("pathIndices: ${pathIndices.toList()}")
        println("fsi: ${fieldSetIndices.toList()}")
        println("forms: ${forms.toList()}")

        val fsiOriginal = this.fieldSetIndices
        var start = 0
        while (start < fsiOriginal.size) {
            var end = fsiOriginal.indexOf(-1, start)
            if (end == -1) end = fsiOriginal.size

            for (di in start until end) {
                val fieldIndex = fsiOriginal[di]
                if (fieldIndex < 0 || fieldIndex >= fieldNames.size) {
                    LOGGER.warn("Invalid live field set data")
                    continue
                }

                // todo what is 'pairs' used for in the source code?
                // todo where do we access pathIndices, fieldSetIndices, forms?
                unpackValueRep(fieldIndex)
            }

            start = end + 1
        }

    }

    fun unpackValueRep(fieldIndex: Int) {
        val fieldName = fieldNames[fieldIndex]
        val fieldType = fieldTypes[fieldIndex]
        if (fieldType.isInlined()) return unpackValueRepInlined(fieldIndex)
        TODO("read field $fieldName, $fieldType")
    }

    fun unpackValueRepInlined(fieldIndex: Int) {
        val fieldName = fieldNames[fieldIndex]
        val fieldType = fieldTypes[fieldIndex]
        TODO("read field inline $fieldName, $fieldType")
    }

    fun readCompressedIntArray(numInts: Int): IntArray {
        val encodedIntsSize = if (numInts != 0) {
            /* commonValue */ 4 +
                    /* numCodesBytes */ ((numInts * 2 + 7) / 8) +
                    /* maxIntBytes */ (numInts * 4)
        } else 0
        val maxCompressedSize = LZ4.getMaxSize(encodedIntsSize)
        val compressedSize = readLE64()
        check(pos + compressedSize <= data.size)
        check(compressedSize >= 4)

        val compressed = data.copyOfRange(pos, pos + compressedSize.toInt())
        pos += compressedSize.toInt()

        val decompressed = ByteArray(maxCompressedSize)
        LZ4.decompressFromBuffer(
            compressed, compressed.size,
            decompressed, decompressed.size, null
        )

        return IntCodingReader(decompressed).decode(numInts)
    }

    fun ByteArray.toReadable() = toList().map {
        if (it in 32..128) "'${it.toInt().toChar()}'"
        else "#" + it.toInt().and(0xff).toString(16)
    }.toString()

    fun readLE64(): Long {
        val value = data.readLE64(pos)
        pos += 8
        return value
    }

    fun readLE32(): Int {
        val value = data.readLE32(pos)
        pos += 4
        return value
    }

    fun readLE16S(): Int {
        return readLE16U().shl(16).shr(16)
    }

    fun readLE16U(): Int {
        val value = data.readLE16(pos)
        pos += 2
        return value
    }

    fun readLE8S(): Int = data[pos++].toInt()

    fun readString16(): String {
        var i = pos
        val max = pos + 16
        while (i < max && data[i] != 0.toByte()) i++
        val value = String(data, pos, i - pos)
        pos += 16
        return value
    }

    fun consume(str: String) {
        val pos0 = pos
        for (c in str) {
            check(data[pos++].toInt() == c.code) {
                "Expected to find $str at $pos0"
            }
        }
    }

}