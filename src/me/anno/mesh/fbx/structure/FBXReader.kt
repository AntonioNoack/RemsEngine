package me.anno.mesh.fbx.structure

import me.anno.io.binary.LittleEndianDataInputStream
import me.anno.mesh.fbx.model.*
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import java.io.EOFException
import java.io.File
import java.io.InputStream
import java.util.zip.InflaterInputStream

// todo create a fbx reader to transform this Video Studio into our own game engine? :)
// todo this data is only a decoded representation -> get the data out of it, including animations <3

class FBXReader(input: InputStream) : LittleEndianDataInputStream(input.buffered()) {

    val fbxObjects = ArrayList<FBXObject>()
    private val objectById = HashMap<Long, FBXObject>()

    val root = FBXObject(FBXNode("Root", arrayOf(0L, "Root", "")))

    var major = 0
    var minor = 0

    init {

        val majorSections = ArrayList<FBXNode>()

        try {

            readHeader()

            val version = major * 1000 + minor
            val version7500 = version >= 7500

            // the end is the empty node
            try {
                while (true) {
                    majorSections += readBinaryNode(version7500)
                }
            } catch (e: EmptyNodeException) {
            }

        } catch (e: UnknownHeaderException) {
            try {
                while (true) {
                    majorSections += readASCIINode()
                }
            } catch (e: EmptyNodeException) {
            }
        }

        if (printDebugMessages) {
            saveMajorSections(majorSections)
        }

        objectById[root.ptr] = root

        // transform nodes into objects
        collectObjects(majorSections)
        connectNodes(majorSections)

        if (printDebugMessages) {
            debug {
                root.toString(0, 0){ parent, child ->
                    when(child){
                        is FBXAnimationCurve, is FBXAnimationCurveNode -> false
                        is FBXNodeAttribute -> false // flags like Skeleton, and size, why ever...
                       // is FBXModel -> parent !is FBXDeformer
                        else -> true
                    }
                }
            }
        }

        // apply operations to objects
        defineSkeletons()

    }

    private fun saveMajorSections(majorSections: List<FBXNode>) {
        val out = File(OS.desktop.file, "fbx.yaml").outputStream().buffered()
        debug { "${majorSections.size} major sections" }
        for (section in majorSections) {
            out.write(section.toString().toByteArray())
            out.write('\n'.code)
        }
        out.close()
    }

    private fun collectObjects(majorSections: List<FBXNode>) {
        for (nodes in majorSections) {
            for (node in nodes.children) {
                if (node.properties.isNotEmpty()) {
                    createObject(node)
                }
            }
        }
    }

    private fun getObject(node: FBXNode, nameOrType: String): FBXObject? {
        return when (nameOrType) {
            "Model" -> FBXModel(node)
            "Geometry" -> FBXGeometry(node)
            "Material" -> FBXMaterial(node)
            "Video" -> FBXVideo(node)
            "Texture" -> FBXTexture(node)
            "Pose" -> FBXPose(node)
            "Deformer" -> FBXDeformer(node)
            "AnimationStack" -> FBXAnimationStack(node)
            "AnimationLayer" -> FBXAnimationLayer(node)
            "NodeAttribute" -> FBXNodeAttribute(node)
            "AnimationCurve" -> FBXAnimationCurve(node)
            "AnimationCurveNode" -> FBXAnimationCurveNode(node)
            else -> null
        }
    }

    private fun createObject(node: FBXNode) {
        val fbxObject = getObject(node, node.nameOrType)
        if (fbxObject != null) {
            fbxObject.applyProperties(node)
            fbxObjects.add(fbxObject)
            objectById[fbxObject.ptr] = fbxObject
        }
    }

    private fun connectNodes(majorSections: List<FBXNode>) {
        for (majorSection in majorSections) {
            if (majorSection.nameOrType == "Connections") {
                for (connection in majorSection.children) {
                    if (connection.nameOrType == "C") {
                        applyConnection(connection)
                    }
                }
            }
        }
    }

    private fun applyConnection(node: FBXNode) {
        // a connection (child, parent)
        val p = node.properties
        val type = p[0] as String
        val child = objectById[p[1] as Long]
        val parent = objectById[p[2] as Long]
        if (child == null || parent == null) {
            // Missing pointers actually occur;
            // but it's good to check anyways
            // if(n1 == null) println("Missing ${p[1]}")
            // if(n2 == null) println("Missing ${p[2]}")
        } else connect(child, parent, type, p.getOrNull(3) as? String)
    }

    private fun connect(child: FBXObject, parent: FBXObject, type: String, propertyName: String?) {
        when (type) {
            "OO" -> {
                // add parent-child relation
                parent.children.add(child)
            }
            "OP" -> {
                // add object override
                parent.overrides.add(propertyName!! to child)
            }
            else -> throw RuntimeException("Unknown connection type $type")
        }
    }

    private fun defineSkeletons() {
        for (geometry in fbxObjects) {
            if (geometry is FBXGeometry) {
                try {
                    geometry.findBoneWeights()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    class UnknownHeaderException : RuntimeException()

    private fun readHeader() {
        if (!input.markSupported()) throw RuntimeException("Input must support marking for ascii fbx files")
        val magic = "Kaydara FBX Binary  "
        input.mark(magic.length + 1)
        for (i in magic.indices) {
            val char = read().toChar()
            if (char != magic[i]) {
                input.reset()
                throw UnknownHeaderException()
            }
        }
        assert(read() == 0x00)
        assert(read() == 0x1a)
        assert(read() == 0x00)
        val version = readInt()
        major = version / 1000
        minor = version % 1000
        if (printDebugMessages) {
            debug { "Version: $major.$minor" }
        }
    }

    fun debug(msg: () -> Any?) {
        if (printDebugMessages) LOGGER.debug(msg().toString())
    }

    private fun readASCIIProperties(name: String, mayReadObject: Boolean, node: FBXNode?): List<Any> {
        val values = ArrayList<Any>()
        // read the list of values
        // they may contain blocks (new nodes)
        // and names, arrays, numbers, strings
        var hasComma = true
        while (true) {
            when (val first = readCharSkipSpacesNoNL()) {
                '\n', '}' -> return values
                in '0'..'9', '-', '+' -> {
                    // may be double or long
                    if (!hasComma) throw IllegalStateException("Expected ,")
                    val valueStr = readNumber(first)
                    values.add(valueStr.toLongOrNull() ?: valueStr.toDouble())
                    hasComma = false
                }
                '"' -> {
                    if (!hasComma) throw IllegalStateException("Expected ,")
                    values.add(readString())
                    hasComma = false
                }
                in 'A'..'Z' -> {
                    if (!hasComma) throw IllegalStateException("Expected ,")
                    values.add(first)
                    hasComma = false
                }
                ',' -> {
                    if (hasComma) throw IllegalStateException("Double comma")
                    hasComma = true
                }
                '{' -> {
                    if (!mayReadObject) return values
                    node!!
                    // if (hasComma) throw IllegalStateException("{}-Block is missing name")
                    val child = FBXNode(name, values.toTypedArray())
                    readASCIINodeBlock(child)
                    node.children.add(child)
                    return emptyList()
                }
                '*' -> return readASCIIArray(name, values)
                else -> {
                    throw RuntimeException("Unexpected property starting with '$first'")
                }
            }
        }
    }

    private fun readASCIIDoubleArray(size: Int): DoubleArray {
        val values = DoubleArray(size)
        for (i in 0 until size) {
            if (i > 0) assert(readCharSkipSpaces(), ',')
            val value = readNumber(readCharSkipSpaces())
            values[i] = value.toDouble()
        }
        return values
    }

    private fun readASCIIFloatArray(size: Int): FloatArray {
        val values = FloatArray(size)
        for (i in 0 until size) {
            if (i > 0) assert(readCharSkipSpaces(), ',')
            val value = readNumber(readCharSkipSpaces())
            values[i] = value.toFloat()
        }
        return values
    }

    private fun readASCIIIntArray(size: Int): IntArray {
        val values = IntArray(size)
        for (i in 0 until size) {
            if (i > 0) assert(readCharSkipSpaces(), ',')
            val value = readNumber(readCharSkipSpaces())
            values[i] = value.toInt()
        }
        return values
    }

    private fun readASCIIArray(name: String, values: ArrayList<Any>): List<Any> {
        // array with size
        val size = readNumber(readCharSkipSpaces()).toInt()
        assert(readCharSkipSpaces(), '{')
        assert(readCharSkipSpaces(), 'a')
        assert(readCharSkipSpaces(), ':')
        // read all *size numbers
        // how do we know whether it's doubles or longs
        // we could try all elements or decide based on the type
        when (name) {
            "Vertices", "Matrix", "Transform", "TransformLink" -> {
                values.add(readASCIIDoubleArray(size))
            }
            // smoothing isn't used yet... what does it mean?
            "Normals", "UV", "Weights", "Smoothing", "Colors" -> {
                values.add(readASCIIFloatArray(size))
            }
            // NormalsW is the winding order/errors, see https://stackoverflow.com/questions/38815549/rewinding-indexes-of-a-polygon
            "Edges", "Indexes", "PolygonVertexIndex", "ColorIndex", "UVIndex", "Materials", "NormalsW" -> {
                values.add(readASCIIIntArray(size))
            }
            else -> {
                if (size > 10) LOGGER.warn("TODO: '$name' in FBX should be assigned pre-defined type")
                values.ensureCapacity(size)
                for (i in 0 until size) {
                    if (i > 0) assert(readCharSkipSpaces(), ',')
                    val value = readNumber(readCharSkipSpaces())
                    values.add(value.toLongOrNull() ?: value.toDouble())
                }
            }
        }
        var last = readCharSkipSpaces()
        // support dangling comma
        if (last == ',') last = readCharSkipSpaces()
        assert(last, '}')
        return values
    }

    private fun readASCIINode(): FBXNode {
        val nameOrType = try {
            readRawName()
        } catch (e: EOFException) {
            throw EmptyNodeException
        }
        assert(readCharSkipSpaces(), ':')
        val properties = readASCIIProperties(nameOrType, false, null).toTypedArray()
        val node = FBXNode(nameOrType, properties)
        readASCIINodeBlock(node)
        return node
    }

    private fun readASCIINodeBlock(node: FBXNode) {
        while (true) {
            when (val next = readCharSkipSpaces()) {
                '}' -> return
                in 'A'..'Z', in 'a'..'z' -> {
                    putBack(next)
                    val propertyName = readRawName()
                    assert(readCharSkipSpaces(), ':')
                    val values = readASCIIProperties(propertyName, true, node)
                    // do sth with the data...
                    if (values.isNotEmpty()) {
                        val child = if (values.size == 1 && values[0] is FBXNode) {
                            values[0] as FBXNode
                        } else FBXNode(propertyName, values.toTypedArray())
                        node.children.add(child)
                    }
                }
                else -> {
                    throw IllegalStateException("$next")
                }
            }
        }
    }

    private fun readBinaryNode(version7500: Boolean): FBXNode {

        val endOffset = if (version7500) readLong() else readUInt()
        if (endOffset == 0L) throw EmptyNodeException
        val numProperties = if (version7500) readLong().toInt() else readInt()
        /*val propertyListLength = */if (version7500) readLong().toInt() else readInt()
        val nameOrType = readLength8String()
        val properties = Array(numProperties) {
            readBinaryProperty(this)
        }

        val node = FBXNode(nameOrType, properties)

        val zeroBlockLength = if (version7500) 25 else 13
        if (position < endOffset) {

            while (position < endOffset - zeroBlockLength) {
                node.children += readBinaryNode(version7500)
            }

            for (i in 0 until zeroBlockLength) {
                if (read() != 0) throw RuntimeException("Failed to read nested block sentinel, expected all bytes to be 0")
            }

        }

        if (position != endOffset) {
            throw RuntimeException("Scope length not reached, something is wrong")
        }

        return node

    }

    private fun readBinaryProperty(input: FBXReader): Any {
        return when (val type = input.read()) {
            // primitive types
            'Y'.code -> {
                // signed int, 16
                (input.read() + input.read() * 256).toShort()
            }
            'C'.code -> {
                // 1 bit boolean in 1 byte
                (input.read() > 0)
            }
            'I'.code -> {
                // 32 bit int
                input.readInt()
            }
            'F'.code -> {
                // float
                Float.fromBits(input.readInt())
            }
            'D'.code -> {
                Double.fromBits(input.readLong())
            }
            'L'.code -> {
                input.readLong()
            }
            // array of primitives
            'f'.code, 'd'.code, 'l'.code, 'i'.code, 'b'.code -> {
                val arrayLength = input.readInt()
                val encoding = input.readInt()
                val compressedLength = input.readInt()
                when (encoding) {
                    0 -> {
                        when (type) {
                            'f'.code -> FloatArray(arrayLength) { Float.fromBits(input.readInt()) }
                            'd'.code -> DoubleArray(arrayLength) { Double.fromBits(input.readLong()) }
                            'l'.code -> LongArray(arrayLength) { input.readLong() }
                            'i'.code -> IntArray(arrayLength) { input.readInt() }
                            'b'.code -> BooleanArray(arrayLength) { input.read() > 0 }
                            else -> throw RuntimeException()
                        }
                    }
                    1 -> {
                        // deflate/zip
                        val bytes = input.readNBytes2(compressedLength)
                        // val allBytes = InflaterInputStream(bytes.inputStream()).readBytes()
                        // ("${bytes.size} zip = ${allBytes.size} raw, for ${type.toChar()} * $arrayLength")
                        val decoder = LittleEndianDataInputStream(InflaterInputStream(bytes.inputStream()))
                        when (type) {
                            'f'.code -> FloatArray(arrayLength) { Float.fromBits(decoder.readInt()) }
                            'd'.code -> DoubleArray(arrayLength) { Double.fromBits(decoder.readLong()) }
                            'l'.code -> LongArray(arrayLength) { decoder.readLong() }
                            'i'.code -> IntArray(arrayLength) { decoder.readInt() }
                            'b'.code -> BooleanArray(arrayLength) { decoder.read() > 0 }
                            else -> throw RuntimeException()
                        }
                    }
                    else -> throw RuntimeException("Unknown encoding $encoding")
                }
            }
            'R'.code, 'S'.code -> {
                // raw or string
                val length = input.readInt()
                val bytes = input.readNBytes2(length)
                if (type == 'R'.code) {
                    bytes
                } else String(bytes)
            }
            else -> throw RuntimeException("Unknown type $type, ${type.toChar()}, something went wrong!")
        }

    }

    companion object {
        var printDebugMessages = true
        private val LOGGER = LogManager.getLogger(FBXReader::class)
    }

}