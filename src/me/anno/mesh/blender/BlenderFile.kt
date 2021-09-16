package me.anno.mesh.blender

import me.anno.mesh.blender.blocks.Block
import me.anno.mesh.blender.blocks.BlockHeader
import me.anno.mesh.blender.blocks.BlockTable
import me.anno.mesh.blender.impl.*
import me.anno.utils.Color.rgba
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.nio.ByteOrder

// blender file reader inspired by Blend.js (https://github.com/AntonioNoack/BlendJS),
// which was inspired by the Java library http://homac.cakelab.org/projects/JavaBlend/spec.html
// I rewrote it, because they generate the classes automatically, and it was soo bloated.
// also it was not easy to read/use, and did not support reading input streams / byte arrays, only files
class BlenderFile(val file: BinaryFile) {

    // read file header
    var version = 0
    var pointerSize = 0

    init {
        file.consumeIdentifier("BLENDER")
        file.is64Bit = when (file.char()) {
            '_' -> false
            '-' -> true
            else -> throw IOException("Expected _ or - after magic")
        }
        pointerSize = if (file.is64Bit) 8 else 4
        file.data.order(
            when (file.char()) {
                'v' -> ByteOrder.LITTLE_ENDIAN
                'V' -> ByteOrder.BIG_ENDIAN
                else -> throw IOException("Unknown endianness")
            }
        )
        // next 3 chars are the version: read them quickly
        version = (file.read() * 100 + file.read() * 10 + file.read()) - (48 + 480 + 4800)
    }

    private val firstBlockOffset = file.offset()
    init {
        val code = DNA1
        // file.offset(firstBlockOffset) // is given automatically
        val blockHeader = BlockHeader(file)
        while (blockHeader.code != ENDBlock) {
            if (blockHeader.code == code) break
            file.skip(blockHeader.size)
            blockHeader.read(file)
        }
    }

    init {
        file.consumeIdentifier("SDNA")
        file.consumeIdentifier("NAME")
    }

    // read struct dna
    private val names = Array(file.readInt()) {
        file.read0String(true)
    }

    init {
        file.padding(4)
        file.consumeIdentifier("TYPE")
    }

    private val typeNames = Array(file.readInt()) {
        file.read0String(true)
    }

    init {
        file.padding(4)
        file.consumeIdentifier("TLEN")
    }

    val types: Array<DNAType> = Array(typeNames.size) { i ->
        val typeLength = file.readShort().toUShort().toInt()
        DNAType(typeNames[i], typeLength, pointerSize)
    }

    val dnaTypeByName = types.associateBy { it.name }

    init {
        file.padding(4)
        file.consumeIdentifier("STRC")
    }

    private val structsWithIndices = Array(file.readInt()) { Struct(file) }

    // read blocks
    val blocks: ArrayList<Block>

    init {
        val blocks = ArrayList<Block>()
        this.blocks = blocks
        file.offset(firstBlockOffset)
        var blockHeader = BlockHeader(file)
        while (blockHeader.code != ENDBlock) {
            blocks.add(Block(blockHeader, file.clone(), file.index))
            file.skip(blockHeader.size)
            blockHeader = BlockHeader(file)
        }
    }

    val structs: Array<DNAStruct> = Array(structsWithIndices.size) { i ->
        val s = structsWithIndices[i]
        val type = types[s.type.toUShort().toInt()]
        val fields = Array(s.fieldsAsTypeName.size shr 1) { j ->
            val j2 = j * 2
            val typeIndex = s.fieldsAsTypeName[j2].toUShort().toInt()
            val nameIndex = s.fieldsAsTypeName[j2 + 1].toUShort().toInt()
            DNAField(j, names[nameIndex], types[typeIndex])
        }
        DNAStruct(i, type, fields)
    }

    val structByName = structs.associateBy { it.type.name }

    val blockTable: BlockTable

    init {

        val offHeapAreas = if (version < 276) arrayOf("FileGlobal") else arrayOf("FileGlobal", "TreeStoreElem")
        var indices = IntArray(offHeapAreas.size)
        var length = 0
        for (structName in offHeapAreas.indices) {
            val struct = structs.getOrNull(structName)
            if (struct == null) LOGGER.warn("List of off-heap areas contains struct name, which does not exist")
            else indices[length++] = struct.index
        }
        // shorten the array, if needed
        if (length < offHeapAreas.size) indices = IntArray(length) { indices[it] }
        blockTable = BlockTable(blocks.toTypedArray(), indices)

    }

    private val objectCache = HashMap<Long, BlendData?>()

    // read all main instances
    val instances = HashMap<String, ArrayList<BlendData>>(64)

    init {
        for (block in blockTable.sorted) {
            val header = block.header
            val code = header.code
            if (!(code == DNA1 || code == ENDBlock || code == TEST)) {
                val struct = structs[header.sdnaIndex]
                val blendFields = struct.fields
                if (blendFields.isNotEmpty() && blendFields.first().type.name == "ID") {
                    // maybe we should create multiple instances, if there are multiples
                    val name = struct.type.name
                    val instance = create(struct, struct.type.name, block, block.header.address)
                    if (instance != null) {
                        instances.getOrPut(name) { ArrayList() }
                            .add(instance)
                    }
                }
            }
        }
    }

    fun printIdTypes() {
        for (struct in structs) {
            if (struct.fields.first().type.name == "ID") {
                LOGGER.info(
                    "class ${struct.type.name}:\n${
                        struct.byName.entries.filter { !it.key.startsWith("_pad") }
                            .joinToString("\n") { "\t${it.key}: ${it.value}" }
                    }"
                )
            }
        }
    }

    fun printTypes() {
        for (struct in structs) {
            LOGGER.info(
                "class ${struct.type.name}:\n${
                    struct.byName.entries.filter { !it.key.startsWith("_pad") }
                        .joinToString("\n") { "\t${it.key}: ${it.value}" }
                }"
            )
        }
    }

    fun create(struct: DNAStruct, clazz: String, block: Block, address: Long): BlendData? {
        return objectCache.getOrPut(address) {
            val position = (address + block.dataOffset).toInt()
            val data = file.data
            when (clazz) {
                "Mesh" -> BMesh(this, struct, data, position)
                "Material" -> BMaterial(this, struct, data, position)
                "MVert" -> MVert(this, struct, data, position)
                "MPoly" -> MPoly(this, struct, data, position)
                "MLoop" -> MLoop(this, struct, data, position)
                "MLoopUV" -> MLoopUV(this, struct, data, position)
                "MEdge" -> MEdge(this, struct, data, position)
                "ID" -> BID(this, struct, data, position)
                "Image" -> BImage(this, struct, data, position)
                // node trees, collections and such may be interesting
                "CustomData" -> BCustomData(this, struct, data, position)
                "CustomDataExternal" -> BCustomDataExternal(this, struct, data, position)
                "CustomDataLayer" -> BCustomDataLayer(this, struct, data, position)
                "Brush", "bScreen", "wmWindowManager" -> null // idc
                else -> {
                    null
                }
            }
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(BlenderFile::class)
        val DNA1 = getCode("DNA1")
        val ENDBlock = getCode("ENDB")
        val TEST = getCode("TEST")
        private fun getCode(code: String) = rgba(
            // must be the same function as inside BinaryFile
            // the order itself doesn't matter, as long as it's consistent
            code[0].code.toByte(),
            code[1].code.toByte(),
            code[2].code.toByte(),
            code[3].code.toByte()
        )
    }
}