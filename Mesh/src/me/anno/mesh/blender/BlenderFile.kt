package me.anno.mesh.blender

import me.anno.io.files.FileReference
import me.anno.mesh.blender.blocks.Block
import me.anno.mesh.blender.blocks.BlockTable
import me.anno.mesh.blender.impl.BAction
import me.anno.mesh.blender.impl.BActionChannel
import me.anno.mesh.blender.impl.BActionGroup
import me.anno.mesh.blender.impl.BAnimData
import me.anno.mesh.blender.impl.BArmature
import me.anno.mesh.blender.impl.BArmatureModifierData
import me.anno.mesh.blender.impl.BBone
import me.anno.mesh.blender.impl.BCustomData
import me.anno.mesh.blender.impl.BCustomDataExternal
import me.anno.mesh.blender.impl.BCustomDataLayer
import me.anno.mesh.blender.impl.BDeformGroup
import me.anno.mesh.blender.impl.BID
import me.anno.mesh.blender.impl.BImage
import me.anno.mesh.blender.impl.BImagePackedFile
import me.anno.mesh.blender.impl.BImageView
import me.anno.mesh.blender.impl.BLamp
import me.anno.mesh.blender.impl.BLink
import me.anno.mesh.blender.impl.BLinkData
import me.anno.mesh.blender.impl.BListBase
import me.anno.mesh.blender.impl.BMaterial
import me.anno.mesh.blender.impl.BMesh
import me.anno.mesh.blender.impl.BObject
import me.anno.mesh.blender.impl.BPackedFile
import me.anno.mesh.blender.impl.BPose
import me.anno.mesh.blender.impl.BPoseChannel
import me.anno.mesh.blender.impl.BRenderData
import me.anno.mesh.blender.impl.BScene
import me.anno.mesh.blender.impl.primitives.BVector2s
import me.anno.mesh.blender.impl.BezTriple
import me.anno.mesh.blender.impl.BlendData
import me.anno.mesh.blender.impl.DrawDataList
import me.anno.mesh.blender.impl.FCurve
import me.anno.mesh.blender.impl.mesh.FPoint
import me.anno.mesh.blender.impl.mesh.MDeformVert
import me.anno.mesh.blender.impl.mesh.MDeformWeight
import me.anno.mesh.blender.impl.mesh.MEdge
import me.anno.mesh.blender.impl.mesh.MLoop
import me.anno.mesh.blender.impl.mesh.MLoopCol
import me.anno.mesh.blender.impl.mesh.MLoopUV
import me.anno.mesh.blender.impl.mesh.MPoly
import me.anno.mesh.blender.impl.mesh.MVert
import me.anno.mesh.blender.impl.nodes.BNode
import me.anno.mesh.blender.impl.nodes.BNodeLink
import me.anno.mesh.blender.impl.nodes.BNodeSocket
import me.anno.mesh.blender.impl.nodes.BNodeTexBase
import me.anno.mesh.blender.impl.nodes.BNodeTexImage
import me.anno.mesh.blender.impl.nodes.BNodeTree
import me.anno.mesh.blender.impl.nodes.BTexMapping
import me.anno.mesh.blender.impl.primitives.BVector2f
import me.anno.mesh.blender.impl.primitives.BVector2i
import me.anno.mesh.blender.impl.primitives.BVector3f
import me.anno.mesh.blender.impl.primitives.BVector1i
import me.anno.mesh.blender.impl.values.BNSVBoolean
import me.anno.mesh.blender.impl.values.BNSVFloat
import me.anno.mesh.blender.impl.values.BNSVInt
import me.anno.mesh.blender.impl.values.BNSVRGBA
import me.anno.mesh.blender.impl.values.BNSVRotation
import me.anno.mesh.blender.impl.values.BNSVVector
import me.anno.utils.Color.rgba
import org.apache.logging.log4j.LogManager
import speiger.primitivecollections.LongToObjectHashMap
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
    private val names = List(file.readInt()) {
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

    val types = Array(typeNames.size) { i ->
        val typeLength = file.readShort().toInt().and(0xffff)
        DNAType(typeNames[i], typeLength)
    }

    val dnaTypeByName = types.associateBy { it.name }

    init {
        file.padding(4)
        file.consumeIdentifier('S', 'T', 'R', 'C')
    }

    private val structsWithIndices = List(file.readInt()) { Struct(file) }

    val structs = Array(structsWithIndices.size) { i ->
        val s = structsWithIndices[i]
        val type = types[s.type.toInt().and(0xffff)]
        val fields = Array(s.fieldsAsTypeName.size shr 1) { j ->
            val j2 = j * 2
            val typeIndex = s.fieldsAsTypeName[j2].toInt().and(0xffff)
            val nameIndex = s.fieldsAsTypeName[j2 + 1].toInt().and(0xffff)
            DNAField(j, names[nameIndex], types[typeIndex])
        }
        DNAStruct(i, type, fields, pointerSize)
    }

    val structByName = structs.associateBy { it.type.name }

    init {
        if (LOGGER.isDebugEnabled()) {
            for (name in structByName.keys.sorted()) {
                val struct = structByName[name]!!
                LOGGER.debug(
                    "Struct {}({}): { {} }", name, struct.type.size,
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

        val offHeapAreas = if (version < 276) listOf("FileGlobal") else listOf("FileGlobal", "TreeStoreElem")
        var indices = IntArray(offHeapAreas.size)
        var length = 0
        for (index in offHeapAreas.indices) {
            val struct = structByName[offHeapAreas[index]]
            if (struct == null) LOGGER.warn("List of off-heap areas contains struct name, which does not exist")
            else indices[length++] = struct.index
        }
        // shorten the array, if needed
        if (length < offHeapAreas.size) indices = IntArray(length) { indices[it] }
        blockTable = BlockTable(this, blocks, indices)
    }

    private val objectCache = HashMap<String, LongToObjectHashMap<BlendData?>>()

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
                            instances.getOrPut(name, ::ArrayList).add(instance)
                        }
                    }
                }
            }
        }
        LOGGER.debug("Instances: {}", instances.mapValues { it.value.size })
    }

    @Suppress("unused")
    fun printIdStructs() {
        for (struct in structs) {
            if (struct.fields.first().type.name == "ID") {
                printStruct(struct)
            }
        }
    }

    @Suppress("unused")
    fun printStructs() {
        for (struct in structs) {
            printStruct(struct)
        }
    }

    private fun printStruct(struct: DNAStruct) {
        LOGGER.info(
            "class ${struct.type.name}:\n${
                struct.byName.entries
                    .filter { !it.key.startsWith("_pad") }
                    .sortedBy { it.key }
                    .joinToString("\n") { "\t${it.key}: ${it.value}" }
            }"
        )
    }

    fun getOrCreate(struct: DNAStruct, clazz: String, block: Block, address: Long): BlendData? {
        return objectCache.getOrPut(clazz, ::LongToObjectHashMap).getOrPut(address) {
            create(struct, clazz, block, address)
        }
    }

    fun create(struct: DNAStruct, clazz: String, block: Block, address: Long): BlendData? {
        if (address == 0L) return null
        val position = (address + block.dataOffset).toInt()
        val data = file.data
        val ptr = ConstructorData(this, struct, data, position)
        return when (clazz) {
            // unused classes have been commented out
            "Mesh" -> BMesh(ptr)
            "Material" -> BMaterial(ptr)
            "MVert" -> MVert(ptr)
            "MPoly" -> MPoly(ptr)
            "MLoop" -> MLoop(ptr)
            "MLoopUV" -> MLoopUV(ptr)
            "MLoopCol" -> MLoopCol(ptr)
            "MEdge" -> MEdge(ptr)
            "MDeformVert" -> MDeformVert(ptr)
            "MDeformWeight" -> MDeformWeight(ptr)
            "ID" -> BID(ptr)
            "Image" -> BImage(ptr)
            "ImageView" -> BImageView(ptr)
            "ImagePackedFile" -> BImagePackedFile(ptr)
            "PackedFile" -> BPackedFile(ptr)
            "Object" -> BObject(ptr)
            "Lamp" -> BLamp(ptr)
            "bNode" -> BNode(ptr)
            "bNodeLink" -> BNodeLink(ptr)
            "bNodeTree" -> BNodeTree(ptr)
            "bNodeSocket" -> BNodeSocket(ptr)
            "Link" -> BLink<Any>(ptr)
            "LinkData" -> BLinkData(ptr)
            "ListBase" -> BListBase<Any>(ptr)
            "Scene" -> BScene(ptr)
            "RenderData" -> BRenderData(ptr)
            // collections and such may be interesting
            "CustomData" -> BCustomData(ptr)
            "CustomDataExternal" -> BCustomDataExternal(ptr)
            "CustomDataLayer" -> BCustomDataLayer(ptr)
            // "Brush", "bScreen", "wmWindowManager" -> null // idc
            "bNodeSocketValueVector" -> BNSVVector(ptr)
            "bNodeSocketValueBoolean" -> BNSVBoolean(ptr)
            "bNodeSocketValueFloat" -> BNSVFloat(ptr)
            "bNodeSocketValueInt" -> BNSVInt(ptr)
            "bNodeSocketValueRGBA" -> BNSVRGBA(ptr)
            "bNodeSocketValueRotation" -> BNSVRotation(ptr)
            "bArmature" -> BArmature(ptr)
            "bDeformGroup" -> BDeformGroup(ptr)
            "ArmatureModifierData" -> BArmatureModifierData(ptr)
            "Bone" -> BBone(ptr)
            "bPose" -> BPose(ptr)
            "bPoseChannel" -> BPoseChannel(ptr)
            "bAction" -> BAction(ptr)
            "bActionChannel" -> BActionChannel(ptr)
            "bActionGroup" -> BActionGroup(ptr)
            "AnimData" -> BAnimData(ptr)
            "FCurve" -> FCurve(ptr)
            "FPoint" -> FPoint(ptr)
            "BezTriple" -> BezTriple(ptr)
            "TexMapping" -> BTexMapping(ptr)
            "NodeTexBase" -> BNodeTexBase(ptr)
            "NodeTexImage" -> BNodeTexImage(ptr)
            "vec2s" -> BVector2s(ptr)
            "vec2f" -> BVector2f(ptr)
            "vec3f" -> BVector3f(ptr)
            "vec2i" -> BVector2i(ptr)
            "MIntProperty" -> BVector1i(ptr)
            "DrawDataList" -> DrawDataList(ptr)
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