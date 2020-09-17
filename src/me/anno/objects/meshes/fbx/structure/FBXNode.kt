package me.anno.objects.meshes.fbx.structure

import me.anno.io.binary.LittleEndianDataInputStream
import me.anno.utils.Tabs
import org.joml.Matrix4f
import java.util.zip.InflaterInputStream

// ObjectType defined the default values...

class FBXNode(val nameOrType: String, val properties: Array<Any>) : FBXNodeBase {

    override val children = ArrayList<FBXNode>()

    companion object {
        fun create(input: FBXReader): FBXNode {
            val endOffset = input.readUInt()
            if (endOffset == 0L) throw EmptyNodeException
            val numProperties = input.readInt()
            /*val propertyListLength = */input.readInt()
            val nameOrType = input.readLength8String()
            val properties = Array(numProperties) {
                readProperty(input)
            }

            val node = FBXNode(nameOrType, properties)

            val zeroBlockLength = 13
            if (input.position < endOffset) {
                while (input.position < endOffset - zeroBlockLength) {
                    node.children += create(input)
                }
                for (i in 0 until zeroBlockLength) {
                    if (input.read() != 0) throw RuntimeException("Failed to read nested block sentinel, expected all bytes to be 0")
                }
            }
            if (input.position != endOffset) {
                throw RuntimeException("Scope length not reached, something is wrong")
            }

            return node

        }

        fun readProperty(input: FBXReader): Any {
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
                            val bytes = input.readNBytes(compressedLength)
                            // val allBytes = InflaterInputStream(bytes.inputStream()).readBytes()
                            // println("${bytes.size} zip = ${allBytes.size} raw, for ${type.toChar()} * $arrayLength")
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
                    val bytes = input.readNBytes(length)
                    if (type == 'R'.toInt()) {
                        bytes
                    } else String(bytes)
                }
                else -> throw RuntimeException("Unknown type $type, ${type.toChar()}, something went wrong!")
            }

        }

    }

    fun toString(depth: Int): String = "${Tabs.spaces(depth * 2)}$nameOrType: " +
            "${properties.joinToString(", ", "[", "]") {
                "${when (it) {
                    is IntArray -> "[i:${it.size}, " + it.filterIndexed { index, _ -> index < 50 }.joinToString()
                    is LongArray -> "[l:${it.size}, " + it.filterIndexed { index, _ -> index < 50 }.joinToString()
                    is FloatArray -> "[f:${it.size}, " + it.filterIndexed { index, _ -> index < 50 }.joinToString()
                    is DoubleArray -> "[d:${it.size}, " + it.filterIndexed { index, _ -> index < 50 }.joinToString()
                    else -> it.toString()
                }
                }:${it.javaClass.simpleName}"
            }}\n${children.joinToString("") {
                it.toString(depth + 1)
            }}"

    override fun toString() = toString(0)

    fun getProperty(name: String) = this[name].firstOrNull()?.properties?.first()

    fun getIntArray(name: String) = getProperty(name) as? IntArray
    fun getLongArray(name: String) = getProperty(name) as? LongArray
    fun getFloatArray(name: String) = getProperty(name) as? FloatArray
    fun getDoubleArray(name: String) = getProperty(name) as? DoubleArray
    fun getBoolean(name: String): Boolean? {
        val value = getProperty(name) ?: return null
        return when (value) {
            0, false, "0", "false" -> false
            1, true, "1", "true" -> true
            else -> true
        }
    }

    fun getM4x4(name: String): Matrix4f? {
        val da = getDoubleArray(name) ?: return null
        if(da.size != 16) throw RuntimeException("Got mat4x4 of size ${da.size}, expected 16")
        val m = Matrix4f()
        // correct
        for (i in 0 until 16) {
            m.set(i/4, i and 3, da[i].toFloat())
        }
        return m
    }

    fun getName() = (properties[1] as String).split(0.toChar())[0]
    fun getId() = properties[0] as Long

}