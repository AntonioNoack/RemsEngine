package me.anno.tests.mesh.minecraft

import me.anno.io.Streams.readNBytes2
import java.io.DataInputStream

object NBTReader {
    fun read(data: DataInputStream, type: Int): Any? {
        return when (type) {
            0 -> null
            1 -> data.readByte()
            2 -> data.readShort()
            3 -> data.readInt()
            4 -> data.readLong()
            5 -> data.readFloat()
            6 -> data.readDouble()
            7 -> {
                val len = data.readInt()
                data.readNBytes2(len, true)
            }
            8 -> data.readUTF()
            9 -> {
                val type1 = data.read()
                val len = data.readInt()
                when (type1) {
                    0 -> if (len == 0) emptyList<Any?>() else "EmptyList($len)"
                    1 -> data.readNBytes2(len, true)
                    2 -> ShortArray(len) { data.readShort() }
                    3 -> IntArray(len) { data.readInt() }
                    4 -> LongArray(len) { data.readLong() }
                    5 -> FloatArray(len) { data.readFloat() }
                    6 -> DoubleArray(len) { data.readDouble() }
                    else -> {
                        val list = ArrayList<Any?>(len)
                        for (i in 0 until len) {
                            list.add(read(data, type1))
                        }
                        list
                    }
                }
            }
            10 -> {
                val map = HashMap<String, Any?>()
                while (true) {
                    val type1 = data.read()
                    if (type1 > 0) {
                        val key = data.readUTF()
                        val value = read(data, type1)!!
                        map[key] = value
                    } else break
                }
                map
            }
            11 -> IntArray(data.readInt()) { data.readInt() }
            12 -> LongArray(data.readInt()) { data.readLong() }
            else -> throw NotImplementedError()
        }
    }
}