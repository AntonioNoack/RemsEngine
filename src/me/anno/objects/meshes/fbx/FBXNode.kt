package me.anno.objects.meshes.fbx

import me.anno.utils.Tabs
import java.util.zip.DeflaterInputStream

class FBXNode(val input: FBXReader): FBXNodeBase() {

    val endOffset = input.readUInt()
    init { if(endOffset == 0L) throw EmptyNodeException() }
    val numProperties = input.readInt()
    val propertyListLength = input.readInt()
    val name = input.readLength8String()

    val properties = Array(numProperties){
        readProperty()
    }

    init {
        val zeroBlockLength = 13
        if(input.position < endOffset){
            while(input.position < endOffset - zeroBlockLength){
                children += FBXNode(input)
            }
            for(i in 0 until zeroBlockLength){
                if(input.read() != 0) throw RuntimeException("Failed to read nested block sentinel, expected all bytes to be 0")
            }
        }
        if(input.position != endOffset){
            throw RuntimeException("Scope length not reached, something is wrong")
        }
    }

    // optional??
    // nestedList???
    // nullRecord, 13 bytes

    fun readProperty(): Any {
        val type = input.read()
        return when(type){
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
                when(encoding){
                    0 -> {
                        when(type){
                            'f'.toInt() -> FloatArray(arrayLength){ Float.fromBits(input.readInt()) }
                            'd'.toInt() -> DoubleArray(arrayLength){ Double.fromBits(input.readLong()) }
                            'l'.toInt() -> LongArray(arrayLength){ input.readLong() }
                            'i'.toInt() -> IntArray(arrayLength){ input.readInt() }
                            'b'.toInt() -> BooleanArray(arrayLength){ input.read() > 0 }
                            else -> throw RuntimeException()
                        }
                    }
                    1 -> {
                        // deflate/zip
                        val bytes = input.readNBytes(compressedLength)
                        val decoder = DeflaterInputStream(bytes.inputStream())
                        fun readInt(): Int {
                            return decoder.read() + decoder.read().shl(8) +
                                    decoder.read().shl(16) + decoder.read().shl(24)
                        }
                        fun readLong(): Long {// todo our values are wrong... why???
                            return (readInt().toULong() + readInt().toULong().shl(32)).toLong()
                        }
                        when(type){
                            'f'.toInt() -> FloatArray(arrayLength){ Float.fromBits(readInt()) }
                            'd'.toInt() -> DoubleArray(arrayLength){ Double.fromBits(readLong()) }
                            'l'.toInt() -> LongArray(arrayLength){ readLong() }
                            'i'.toInt() -> IntArray(arrayLength){ readInt() }
                            'b'.toInt() -> BooleanArray(arrayLength){ decoder.read() > 0 }
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
                if(type == 'R'.toInt()){
                    bytes
                } else String(bytes)
            }
            else -> throw RuntimeException("Unknown type $type, ${type.toChar()}, something went wrong!")
        }

    }

    fun toString(depth: Int): String = Tabs.spaces(depth * 2) + "$name: ${properties.joinToString(", ", "[", "]"){ "$it:${it.javaClass.simpleName}" }}, ${children.joinToString(""){ "\n${it.toString(depth+1)}" }}"
    override fun toString() = toString(0)


}