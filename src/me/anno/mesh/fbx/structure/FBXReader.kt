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

class FBXReader(input: InputStream) : LittleEndianDataInputStream(input.buffered()),
    FBXNodeBase {

    override val children = ArrayList<FBXNode>()

    val fbxObjects = ArrayList<FBXObject>()
    val fbxObjectMap = HashMap<Long, FBXObject>()
    val root = FBXObject(FBXNode("Root", arrayOf(0L, "Root", "")))

    var major = 0
    var minor = 0

    fun getObject(node: FBXNode, nameOrType: String): FBXObject? {
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

    init {

        try {

            readHeader()

            val version = major * 1000 + minor
            val version7500 = version >= 7500

            // the end is the empty node
            try {
                while (true) {
                    children += readBinary(version7500)
                }
            } catch (e: EmptyNodeException) {
            }

        } catch (e: UnknownHeaderException) {
            try {
                while (true) {
                    children += readASCII()
                }
            } catch (e: EmptyNodeException) {
            }
        }

        if (printDebugMessages) {
            val out = File(OS.desktop.file, "fbx.json").outputStream().buffered()
            debug { "${children.size} children" }
            for (node in children) {
                out.write(node.toString().toByteArray())
                out.write('\n'.toInt())
            }
            out.close()
        }

        fbxObjectMap[root.ptr] = root

        children.forEach { nodes ->
            nodes.children.forEach { node ->
                if (node.properties.isNotEmpty()) {
                    val fbxObject = getObject(node, node.nameOrType)
                    if (fbxObject != null) {
                        fbxObject.applyProperties(node)
                        fbxObjects += fbxObject
                        fbxObjectMap[fbxObject.ptr] = fbxObject
                    }
                }
            }
        }

        children.filter { it.nameOrType == "Connections" }.forEach { connections ->
            connections.children.forEach { node ->
                when (node.nameOrType) {
                    "C" -> {
                        // a connection (child, parent)
                        val p = node.properties
                        val type = p[0] as String
                        val n1 = fbxObjectMap[p[1] as Long]
                        val n2 = fbxObjectMap[p[2] as Long]
                        if (n1 == null || n2 == null) {
                            // Missing pointers actually occur;
                            // but it's good to check anyways
                            // if(n1 == null) println("Missing ${p[1]}")
                            // if(n2 == null) println("Missing ${p[2]}")
                        } else {
                            when (type) {
                                "OO" -> {
                                    // add parent-child relation
                                    assert(p.size == 3)
                                    n2.children.add(n1)
                                }
                                "OP" -> {
                                    // add object override
                                    assert(p.size == 4)
                                    val propertyName = p[3] as String
                                    n1.overrides[propertyName] = n2
                                }
                                else -> throw RuntimeException("Unknown connection type $type")
                            }
                        }
                    }
                }
                Unit
            }
        }

        if (printDebugMessages) {
            debug { root }
        }

        fbxObjects.filterIsInstance<FBXGeometry>().forEach {
            val realBone = root.children.filterIsInstance<FBXModel>().getOrNull(1)
            if (realBone != null) it.findBoneWeights(realBone)
        }

    }

    class UnknownHeaderException : RuntimeException()

    fun readHeader() {
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

    fun readASCIIProperties(name: String, mayReadObject: Boolean, node: FBXNode?): List<Any> {
        val values = ArrayList<Any>()
        // todo read the list of values
        // todo they may contain blocks (new nodes)
        // todo and names, arrays, numbers, strings
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
                    readASCIIBody(child)
                    node.children.add(child)
                    return emptyList()
                }
                '*' -> {
                    // array with size
                    val size = readNumber(readCharSkipSpaces()).toInt()
                    assert(readCharSkipSpaces(), '{')
                    assert(readCharSkipSpaces(), 'a')
                    assert(readCharSkipSpaces(), ':')
                    // todo read all *size numbers
                    // todo how do we know whether it's doubles or longs? idk...
                    when (name) {
                        "Vertices", "Matrix", "Transform", "TransformLink" -> {
                            val values2 = DoubleArray(size)
                            for (i in 0 until size) {
                                if (i > 0) assert(readCharSkipSpaces(), ',')
                                val value = readNumber(readCharSkipSpaces())
                                values2[i] = value.toDouble()
                            }
                            values.add(values2)
                        }
                        "Normals", "UV", "Weights" -> {
                            val values2 = FloatArray(size)
                            for (i in 0 until size) {
                                if (i > 0) assert(readCharSkipSpaces(), ',')
                                val value = readNumber(readCharSkipSpaces())
                                values2[i] = value.toFloat()
                            }
                            values.add(values2)
                        }
                        "Edges", "Indexes", "PolygonVertexIndex", "ColorIndex", "UVIndex" -> {
                            val values2 = IntArray(size)
                            for (i in 0 until size) {
                                if (i > 0) assert(readCharSkipSpaces(), ',')
                                val value = readNumber(readCharSkipSpaces())
                                values2[i] = value.toInt()
                            }
                            values.add(values2)
                        }
                        else -> {
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
                else -> {
                    TODO("property starting with $first, ${readString()}")
                }
            }
        }
    }

    fun readASCII(): FBXNode {
        val nameOrType = try {
            readRawName()
        } catch (e: EOFException) {
            throw EmptyNodeException
        }
        assert(readCharSkipSpaces(), ':')
        val properties = readASCIIProperties(nameOrType, false, null).toTypedArray()
        val node = FBXNode(nameOrType, properties)
        readASCIIBody(node)
        return node
    }

    fun readASCIIBody(node: FBXNode) {
        while (true) {
            when (val next = readCharSkipSpaces()) {
                '}' -> return
                in 'A'..'Z', in 'a'..'z' -> {
                    putBack(next)
                    val propertyName = readRawName()
                    assert(readCharSkipSpaces(), ':')
                    val values = readASCIIProperties(propertyName, true, node)
                    // do sth with the data...
                    if(values.isNotEmpty()){
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

    fun readBinary(version7500: Boolean): FBXNode {

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
                node.children += readBinary(version7500)
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

    fun readBinaryProperty(input: FBXReader): Any {
        return when (val type = input.read()) {
            // primitive types
            'Y'.toInt() -> {
                // signed int, 16
                (input.read() + input.read() * 256).toShort()
            }
            'C'.toInt() -> {
                // 1 bit boolean in 1 byte
                (input.read() > 0)
            }
            'I'.toInt() -> {
                // 32 bit int
                input.readInt()
            }
            'F'.toInt() -> {
                // float
                Float.fromBits(input.readInt())
            }
            'D'.toInt() -> {
                Double.fromBits(input.readLong())
            }
            'L'.toInt() -> {
                input.readLong()
            }
            // array of primitives
            'f'.toInt(), 'd'.toInt(), 'l'.toInt(), 'i'.toInt(), 'b'.toInt() -> {
                val arrayLength = input.readInt()
                val encoding = input.readInt()
                val compressedLength = input.readInt()
                when (encoding) {
                    0 -> {
                        when (type) {
                            'f'.toInt() -> FloatArray(arrayLength) { Float.fromBits(input.readInt()) }
                            'd'.toInt() -> DoubleArray(arrayLength) { Double.fromBits(input.readLong()) }
                            'l'.toInt() -> LongArray(arrayLength) { input.readLong() }
                            'i'.toInt() -> IntArray(arrayLength) { input.readInt() }
                            'b'.toInt() -> BooleanArray(arrayLength) { input.read() > 0 }
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
                            'f'.toInt() -> FloatArray(arrayLength) { Float.fromBits(decoder.readInt()) }
                            'd'.toInt() -> DoubleArray(arrayLength) { Double.fromBits(decoder.readLong()) }
                            'l'.toInt() -> LongArray(arrayLength) { decoder.readLong() }
                            'i'.toInt() -> IntArray(arrayLength) { decoder.readInt() }
                            'b'.toInt() -> BooleanArray(arrayLength) { decoder.read() > 0 }
                            else -> throw RuntimeException()
                        }
                    }
                    else -> throw RuntimeException("Unknown encoding $encoding")
                }
            }
            'R'.toInt(), 'S'.toInt() -> {
                // raw or string
                val length = input.readInt()
                val bytes = input.readNBytes2(length)
                if (type == 'R'.toInt()) {
                    bytes
                } else String(bytes)
            }
            else -> throw RuntimeException("Unknown type $type, ${type.toChar()}, something went wrong!")
        }

    }


    companion object {
        var printDebugMessages = false
        private val LOGGER = LogManager.getLogger(FBXReader::class)
    }

}