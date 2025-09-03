package me.anno.io.numpy

import me.anno.io.Streams.readBE16
import me.anno.io.Streams.readBE32
import me.anno.io.Streams.readBE32F
import me.anno.io.Streams.readBE64
import me.anno.io.Streams.readBE64F
import me.anno.io.Streams.readLE16
import me.anno.io.Streams.readLE32
import me.anno.io.Streams.readLE32F
import me.anno.io.Streams.readLE64
import me.anno.io.Streams.readLE64F
import me.anno.io.Streams.readNBytes2
import me.anno.io.files.FileReference
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map
import me.anno.utils.async.Callback.Companion.mapAsync
import me.anno.utils.async.Callback.Companion.mapCallback
import me.anno.utils.types.Strings.indexOf2
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteOrder
import kotlin.math.min

object NumPyReader {

    fun readNPZ(folder: FileReference, callback: Callback<Map<String, NumPyData?>>) {
        folder.listChildren(callback.mapAsync { children, cb2 ->
            children.mapCallback({ _, file, cb1 ->
                file.inputStream { stream, err ->
                    val data = if (stream != null) readNPY(stream) else null
                    if (data is NumPyData?) {
                        cb1.ok(file.nameWithoutExtension to data)
                    } else cb1.err(data as? Exception ?: err)
                }
            }, cb2.map { it.toMap() })
        })
    }

    /**
     * returns NumPyData or exception
     * */
    fun readNPY(input: InputStream): Any {

        val data = DataInputStream(input)
        for (c in NUMPY_MAGIC) {
            if (data.read() != c.code) {
                return IOException("Invalid magic")
            }
        }

        val major = data.read()
        /*val minor =*/ data.read()
        val headerLen = if (major >= 2) data.readLE32() else data.readLE16()
        val header = data.readNBytes2(headerLen, true)!!.decodeToString().trim()
        if (!header.startsWith("{") || !header.endsWith("}")) {
            return IOException("Header broken $header")
        }
        val i0 = header.indexOf("descr") + "descr".length + 1
        val i1 = min(header.indexOf2("'", i0), header.indexOf2("\"", i0)) + 1
        if (i1 >= header.length) return IOException("Header broken $header")
        val i2 = min(header.indexOf2("'", i1), header.indexOf2("\"", i1))
        val descriptor = header.substring(i1, i2)
        val columnMajor = header.contains("true", true)
        val i3 = header.indexOf("shape") + "shape".length + 1
        val i4 = header.indexOf("(", i3) + 1
        val i5 = header.indexOf(")", i4)
        if (i5 < 0) return IOException("Header broken $header")
        val shape = header.substring(i4, i5)
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .toIntArray()
        // what does < or | mean in the descriptor???
        // {'descr': '<i4', 'fortran_order': False, 'shape': (1,), }
        val leFlags = if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) "<=" else "<"
        val littleEndian = descriptor[0] in leFlags
        if (descriptor.length < 3 || descriptor[0] !in "<|" || ',' in descriptor) {
            return IllegalStateException("Unsupported descriptor $descriptor") // unknown, maybe structured type
        }
        val totalSize = shape.reduce { a, b -> a * b }
        val doubleSize = totalSize * 2
        val data1: Any = when (val sub = descriptor.substring(1)) {
            // floats
            "f4" -> if (littleEndian) FloatArray(totalSize) { data.readLE32F() }
            else FloatArray(totalSize) { data.readBE32F() }
            "f8" -> if (littleEndian) DoubleArray(totalSize) { data.readLE64F() }
            else DoubleArray(totalSize) { data.readBE64F() }
            // complex numbers
            "c8" -> if (littleEndian) FloatArray(doubleSize) { data.readLE32F() }
            else FloatArray(doubleSize) { data.readBE32F() }
            "c16" -> if (littleEndian) DoubleArray(doubleSize) { data.readLE64F() }
            else DoubleArray(doubleSize) { data.readBE64F() }
            // integers
            "i1", "u1" -> data.readNBytes2(totalSize, true)!!
            "i2", "u2" -> if (littleEndian) ShortArray(totalSize) { data.readLE16().toShort() }
            else ShortArray(totalSize) { data.readBE16().toShort() }
            "i4", "u4" -> if (littleEndian) IntArray(totalSize) { data.readLE32() }
            else IntArray(totalSize) { data.readBE32() }
            "i8", "u8" -> LongArray(totalSize) { if (littleEndian) data.readLE64() else data.readBE64() }
            // strings
            "S1" -> data.readNBytes2(totalSize, true)!!.decodeToString()
            else -> {
                if (sub.startsWith("S")) {
                    val individualLength = sub.substring(1).toIntOrNull()
                        ?: return IOException("Unsupported string descriptor $descriptor")
                    Array(totalSize) {
                        data.readNBytes2(individualLength, true)!!.decodeToString()
                    }
                } else return IOException("Unknown descriptor type $descriptor")
            }
        }
        return NumPyData(descriptor, shape, columnMajor, data1)
    }

    const val NUMPY_MAGIC = "\u0093NUMPY"
}