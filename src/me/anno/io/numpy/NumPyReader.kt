package me.anno.io.numpy

import me.anno.io.Streams.readBE16
import me.anno.io.Streams.readBE32
import me.anno.io.Streams.readBE64
import me.anno.io.Streams.readDoubleBE
import me.anno.io.Streams.readDoubleLE
import me.anno.io.Streams.readFloatBE
import me.anno.io.Streams.readFloatLE
import me.anno.io.Streams.readLE16
import me.anno.io.Streams.readLE32
import me.anno.io.Streams.readLE64
import me.anno.io.files.FileReference
import me.anno.utils.strings.StringHelper.indexOf2
import me.anno.utils.structures.tuples.Quad
import me.anno.utils.types.InputStreams.readNBytes2
import java.io.DataInputStream
import java.io.IOException
import java.nio.ByteOrder
import kotlin.math.min

object NumPyReader {

    fun readNPZ(file: FileReference): Map<String, Any?> {
        return file.listChildren()?.associate { readNPY(it) } ?: emptyMap()
    }

    fun readNPY(file: FileReference): Pair<String, Any?> {
        val name = file.nameWithoutExtension
        return name to try {
            readNPY1(file)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun readNPY1(file: FileReference): Any? {

        val data = DataInputStream(file.inputStreamSync())
        for (c in magic) {
            if (data.read() != c.code) return null
        }
        val major = data.read()
        /*val minor =*/ data.read()
        val headerLen = if (major >= 2) data.readLE32() else data.readLE16()
        val header = String(data.readNBytes2(headerLen, true)).trim()
        if (!header.startsWith("{") || !header.endsWith("}")) return header
        val i0 = header.indexOf("descr") + "descr".length + 1
        val i1 = min(header.indexOf2("'", i0), header.indexOf2("\"", i0)) + 1
        if (i1 >= header.length) return header
        val i2 = min(header.indexOf2("'", i1), header.indexOf2("\"", i1))
        val descriptor = header.substring(i1, i2)
        val columnMajor = header.contains("true", true)
        val i3 = header.indexOf("shape") + "shape".length + 1
        val i4 = header.indexOf("(", i3) + 1
        val i5 = header.indexOf(")", i4)
        if (i5 < 0) return header
        val shape = header.substring(i4, i5)
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .toIntArray()
        // what does < or | mean in the descriptor???
        // {'descr': '<i4', 'fortran_order': False, 'shape': (1,), }
        val leFlags = if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) "<=" else "<"
        val littleEndian = descriptor[0] in leFlags
        if (descriptor.length < 3) return descriptor // unknown
        if (descriptor[0] !in "<|") return descriptor // unknown
        if (',' in descriptor) return descriptor // unknown, maybe structured type
        val totalSize = shape.reduce { a, b -> a * b }
        val doubleSize = totalSize * 2
        val data1 = when (val sub = descriptor.substring(1)) {
            // floats
            "f4" -> if (littleEndian) FloatArray(totalSize) { data.readFloatLE() }
            else FloatArray(totalSize) { data.readFloatBE() }
            "f8" -> if (littleEndian) DoubleArray(totalSize) { data.readDoubleLE() }
            else DoubleArray(totalSize) { data.readDoubleBE() }
            // complex numbers
            "c8" -> if (littleEndian) FloatArray(doubleSize) { data.readFloatLE() }
            else FloatArray(doubleSize) { data.readFloatBE() }
            "c16" -> if (littleEndian) DoubleArray(doubleSize) { data.readDoubleLE() }
            else DoubleArray(doubleSize) { data.readDoubleBE() }
            // integers
            "i1", "u1" -> data.readNBytes2(totalSize, true)
            "i2", "u2" -> if (littleEndian) ShortArray(totalSize) { data.readLE16().toShort() }
            else ShortArray(totalSize) { data.readBE16().toShort() }
            "i4", "u4" -> if (littleEndian) IntArray(totalSize) { data.readLE32() }
            else IntArray(totalSize) { data.readBE32() }
            "i8", "u8" -> LongArray(totalSize) { if (littleEndian) data.readLE64() else data.readBE64() }
            // strings
            "S1" -> String(data.readNBytes2(totalSize, true))
            else -> {
                if (sub.startsWith("S")) {
                    val individualLength = sub.substring(1).toIntOrNull() ?: return descriptor
                    Array(totalSize) {
                        String(data.readNBytes2(individualLength, true))
                    }
                } else null // unknown
            }
        }
        return Quad(descriptor, shape, columnMajor, data1)
    }

    const val magic = "\u0093NUMPY"

}