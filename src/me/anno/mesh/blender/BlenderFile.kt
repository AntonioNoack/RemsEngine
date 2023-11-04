package me.anno.mesh.blender

import me.anno.io.files.FileReference
import me.anno.mesh.blender.blocks.Block
import me.anno.mesh.blender.blocks.BlockTable
import me.anno.mesh.blender.impl.*
import me.anno.mesh.blender.impl.nodes.*
import me.anno.mesh.blender.impl.values.*
import me.anno.utils.Color.rgba
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.nio.ByteOrder

/**
 * Blender file reader inspired by Blend.js (https://github.com/AntonioNoack/BlendJS),
 * which was inspired by the Java library http://homac.cakelab.org/projects/JavaBlend/spec.html
 * I rewrote it, because they generate the classes automatically, and it was soo bloated.
 * also it was not easy to read/use, and did not support reading input streams / byte arrays, only files
 * */
class BlenderFile(val file: BinaryFile, val folder: FileReference) {

    // read file header
    var version = 0
    var pointerSize = 0

    init {
        val magic = "BLENDER"
        for (i in magic.indices) {
            val char = file.char()
            if (char != magic[i]) throw IOException("Identifier is not matching $magic, got $char at $i")
        }
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

    // read blocks
    val blocks = ArrayList<Block>()

    init {
        file.offset(firstBlockOffset)
        while (true) {
            val block = Block(file)
            if (block.code == ENDB) break
            file.skip(block.size)
            blocks.add(block)
        }

        val offset = blocks.first { it.code == DNA1 }.positionInFile
        file.offset(offset)

        file.consumeIdentifier('S', 'D', 'N', 'A')
        file.consumeIdentifier('N', 'A', 'M', 'E')
    }

    // read struct dna
    private val names = Array(file.readInt()) {
        file.read0String()
    }

    init {
        file.padding(4)
        file.consumeIdentifier('T', 'Y', 'P', 'E')
    }

    private val typeNames = Array(file.readInt()) {
        file.read0String()
    }

    init {
        file.padding(4)
        file.consumeIdentifier('T', 'L', 'E', 'N')
    }

    val types: Array<DNAType> = Array(typeNames.size) { i ->
        val typeLength = file.readShort().toUShort().toInt()
        DNAType(typeNames[i], typeLength)
    }

    val dnaTypeByName = types.associateBy { it.name }

    init {
        file.padding(4)
        file.consumeIdentifier('S', 'T', 'R', 'C')
    }

    private val structsWithIndices = Array(file.readInt()) { Struct(file) }

    val structs = Array(structsWithIndices.size) { i ->
        val s = structsWithIndices[i]
        val type = types[s.type.toUShort().toInt()]
        val fields = Array(s.fieldsAsTypeName.size shr 1) { j ->
            val j2 = j * 2
            val typeIndex = s.fieldsAsTypeName[j2].toUShort().toInt()
            val nameIndex = s.fieldsAsTypeName[j2 + 1].toUShort().toInt()
            DNAField(j, names[nameIndex], types[typeIndex])
        }
        DNAStruct(i, type, fields, pointerSize)
    }

    val structByName = structs.associateBy { it.type.name }

    init {
        if (LOGGER.isDebugEnabled) {
            for (name in structByName.keys.sorted()) {
                val struct = structByName[name]!!
                LOGGER.debug("Struct {}({}): { {} }", name, struct.type.size,
                    struct.byName.entries
                        .filter {
                            !it.key.startsWith("_pad") &&
                                    !it.key.startsWith("*_pad")
                        }
                        .joinToString { "${it.key}: ${it.value.type.name}" })
            }
        }
    }

    val blockTable: BlockTable

    init {

        val offHeapAreas = if (version < 276) arrayOf("FileGlobal") else arrayOf("FileGlobal", "TreeStoreElem")
        var indices = IntArray(offHeapAreas.size)
        var length = 0
        for (index in offHeapAreas.indices) {
            val struct = structByName[offHeapAreas[index]]
            if (struct == null) LOGGER.warn("List of off-heap areas contains struct name, which does not exist")
            else indices[length++] = struct.index
        }
        // shorten the array, if needed
        if (length < offHeapAreas.size) indices = IntArray(length) { indices[it] }
        blockTable = BlockTable(this, blocks.toTypedArray(), indices)
    }

    private val objectCache = HashMap<String, HashMap<Long, BlendData?>>()

    // read all main instances
    val instances = HashMap<String, ArrayList<BlendData>>(64)

    init {
        // println("read blocks, now instantiating:")
        for (block in blockTable.blockList) {
            val code = block.code
            if (code != DNA1 && code != ENDB && code != TEST) {
                val struct = structs[block.sdnaIndex]
                val blendFields = struct.fields
                // println("block ${header.address} contains ${header.count}x ${struct.type.name}")
                if (blendFields.isNotEmpty() && blendFields.first().type.name == "ID") {
                    // maybe we should create multiple instances, if there are multiples
                    val name = struct.type.name
                    for (i in 0 until block.count) {
                        val address = block.address + struct.type.size * i
                        val instance = getOrCreate(struct, struct.type.name, block, address)
                        if (instance != null) {
                            instances.getOrPut(name) { ArrayList() }
                                .add(instance)
                        }
                    }
                }
            }
        }
        LOGGER.debug("Instances: {}", instances.mapValues { it.value.size })
    }

    @Suppress("unused")
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

    @Suppress("unused")
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

    fun getOrCreate(struct: DNAStruct, clazz: String, block: Block, address: Long): BlendData? {
        return objectCache.getOrPut(clazz) { HashMap() }.getOrPut(address) {
            create(struct, clazz, block, address)
        }
    }

    fun create(struct: DNAStruct, clazz: String, block: Block, address: Long): BlendData? {
        if (address == 0L) return null
        val position = (address + block.dataOffset).toInt()
        val data = file.data
        return when (clazz) {
            // unused classes have been commented out
            "Mesh" -> BMesh(this, struct, data, position)
            "Material" -> BMaterial(this, struct, data, position)
            "MVert" -> MVert(this, struct, data, position)
            "MPoly" -> MPoly(this, struct, data, position)
            "MLoop" -> MLoop(this, struct, data, position)
            "MLoopUV" -> MLoopUV(this, struct, data, position)
            "MLoopCol" -> MLoopCol(this, struct, data, position)
            "MEdge" -> MEdge(this, struct, data, position)
            "MDeformVert" -> MDeformVert(this, struct, data, position)
            "MDeformWeight" -> MDeformWeight(this, struct, data, position)
            "ID" -> BID(this, struct, data, position)
            "Image" -> BImage(this, struct, data, position)
            "ImageView" -> BImageView(this, struct, data, position)
            "ImagePackedFile" -> BImagePackedFile(this, struct, data, position)
            "PackedFile" -> BPackedFile(this, struct, data, position)
            "Object" -> BObject(this, struct, data, position)
            "Lamp" -> BLamp(this, struct, data, position)
            "bNode" -> BNode(this, struct, data, position)
            "bNodeLink" -> BNodeLink(this, struct, data, position)
            "bNodeTree" -> BNodeTree(this, struct, data, position)
            "bNodeSocket" -> BNodeSocket(this, struct, data, position)
            "Link" -> BLink<Any>(this, struct, data, position)
            "LinkData" -> BLinkData(this, struct, data, position)
            "ListBase" -> BListBase<Any>(this, struct, data, position)
            // "Scene" -> BScene(this, struct, data, position)
            // collections and such may be interesting
            "CustomData" -> BCustomData(this, struct, data, position)
            "CustomDataExternal" -> BCustomDataExternal(this, struct, data, position)
            "CustomDataLayer" -> BCustomDataLayer(this, struct, data, position)
            // "Brush", "bScreen", "wmWindowManager" -> null // idc
            "bNodeSocketValueVector" -> BNSVVector(this, struct, data, position)
            "bNodeSocketValueBoolean" -> BNSVBoolean(this, struct, data, position)
            "bNodeSocketValueFloat" -> BNSVFloat(this, struct, data, position)
            "bNodeSocketValueInt" -> BNSVInt(this, struct, data, position)
            "bNodeSocketValueRGBA" -> BNSVRGBA(this, struct, data, position)
            "bNodeSocketValueRotation" -> BNSVRotation(this, struct, data, position)
            "bArmature" -> BArmature(this, struct, data, position)
            "bDeformGroup" -> BDeformGroup(this, struct, data, position)
            "ArmatureModifierData" -> BArmatureModifierData(this, struct, data, position)
            "Bone" -> BBone(this, struct, data, position)
            "bPose" -> BPose(this, struct, data, position)
            "bPoseChannel" -> BPoseChannel(this, struct, data, position)
            "bAction" -> BAction(this, struct, data, position)
            "bActionChannel" -> BActionChannel(this, struct, data, position)
            "bActionGroup" -> BActionGroup(this, struct, data, position)
            "AnimData" -> BAnimData(this, struct, data, position)
            "FCurve" -> FCurve(this, struct, data, position)
            "FPoint" -> FPoint(this, struct, data, position)
            "BezTriple" -> BezTriple(this, struct, data, position)
            "TexMapping" -> BTexMapping(this, struct, data, position)
            "NodeTexBase" -> BNodeTexBase(this, struct, data, position)
            "NodeTexImage" -> BNodeTexImage(this, struct, data, position)
            "vec2s" -> BVector2f(this, struct, data, position)
            else -> {
                LOGGER.warn("Skipping instance of class $clazz")
                null
            }
        }
    }

    @Suppress("unused")
    fun searchReferencesByStructsAtPositions(positions: List<Int>, names: List<String>) {
        val positionsOfInterest = HashSet<Int>()
        val nextPositions = ArrayList<Pair<Int, String>>()
        for (i in positions.indices) {
            positionsOfInterest.add(positions[i])
            nextPositions.add(positions[i] to names[i])
        }
        val nio = file.data
        val limit = nio.capacity() - 7
        while (nextPositions.isNotEmpty()) {
            val posPath = nextPositions.removeAt(0)
            val position = posPath.first
            val address = blockTable.getAddressAt(position)
            // 4 byte alignment is typically given
            for (searchPosition in 0 until limit step 4) {
                if (nio.getLong(searchPosition) == address) {
                    val block = blockTable.getBlockAt(searchPosition)
                    val typeName = block.getTypeName(this)
                    val struct = block.getType(this)
                    val localAddress = searchPosition - block.positionInFile
                    val localIndex = localAddress / struct.type.size
                    val localOffset = localAddress % struct.type.size
                    val field = struct.fields.firstOrNull { it.offset >= localOffset }
                    if (field != null && field.isPointer) {
                        val structPosition = searchPosition - localOffset
                        if (positionsOfInterest.add(structPosition)) {
                            val newPath = "$typeName[$localIndex].${field.decoratedName}/${posPath.second}"
                            nextPositions.add(structPosition to newPath)
                            LOGGER.info("found ref at $newPath")
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(BlenderFile::class)

        @Suppress("SpellCheckingInspection")
        private val ENDB = getCode('E', 'N', 'D', 'B')
        private val DNA1 = getCode('D', 'N', 'A', '1')
        private val TEST = getCode('T', 'E', 'S', 'T')
        private fun getCode(c0: Char, c1: Char, c2: Char, c3: Char) = rgba(
            // must be the same function as inside BinaryFile
            // the order itself doesn't matter, as long as it's consistent
            c0.code.toByte(),
            c1.code.toByte(),
            c2.code.toByte(),
            c3.code.toByte()
        )
    }
}