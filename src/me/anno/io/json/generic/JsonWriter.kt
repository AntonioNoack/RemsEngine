package me.anno.io.json.generic

import me.anno.io.Streams.writeString
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.arrays.BooleanArrayList
import me.anno.utils.types.Strings
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.IOException
import java.io.OutputStream
import java.io.Writer

/**
 * simple class to write JSON content; if you write Saveables, consider BinaryWriter and TextWriter first!
 * used in Rem's Studio to save UI layout
 * */
open class JsonWriter(val output: Writer) {

    private var first = true
    private var hasAttr = false
    private val isObjectStack = BooleanArrayList(0)

    private fun writeEscapedString(value: String) {
        output.write('"'.code)
        Strings.writeEscaped(value, output, '"')
        output.write('"'.code)
    }

    fun attr(key: String) {
        assertTrue(isObjectStack.peek())
        if (!first) output.write(','.code)
        writeEscapedString(key)
        output.write(':'.code)
        hasAttr = true
        first = false
    }

    fun next() {
        if (isObjectStack.size > 0 && hasAttr != isObjectStack.peek()) {
            throw IOException(
                if (hasAttr) "attr cannot be used in array"
                else "property needs attr in object"
            )
        }
        if (!first && !hasAttr) {
            output.write(','.code)
        }
        hasAttr = false
        first = false
    }

    fun write(b: Boolean) {
        next()
        output.write(if (b) "true" else "false")
    }

    fun write(d: Number) {
        next()
        output.write(d.toString())
    }

    fun write(value: String) {
        next()
        writeEscapedString(value)
    }

    fun beginArray() {
        next()
        output.write('['.code)
        isObjectStack.push(false)
        first = true
    }

    fun beginObject() {
        next()
        output.write('{'.code)
        isObjectStack.push(true)
        first = true
    }

    fun endArray() {
        output.write(']'.code)
        assertFalse(isObjectStack.pop())
        first = false
    }

    fun endObject() {
        output.write('}'.code)
        assertTrue(isObjectStack.pop())
        first = false
    }

    inline fun writeArray(writeElements: () -> Unit) {
        beginArray()
        writeElements()
        endArray()
    }

    fun <V> writeArray(elements: List<V>, writeElement: (V) -> Unit) {
        writeArray {
            for (i in elements.indices) {
                writeElement(elements[i])
            }
        }
    }

    fun <V> writeArrayIndexed(elements: List<V>, writeElement: (Int, V) -> Unit) {
        writeArray {
            for (i in elements.indices) {
                writeElement(i, elements[i])
            }
        }
    }

    fun writeArrayByIndices(i0: Int, i1: Int, writeElement: (Int) -> Unit) {
        writeArray {
            for (i in i0 until i1) {
                writeElement(i)
            }
        }
    }

    inline fun writeObject(writeAttributes: () -> Unit) {
        beginObject()
        writeAttributes()
        endObject()
    }

    fun finish() {
        output.close()
        if (isObjectStack.size != 0) {
            throw IOException(
                "beginX() doesn't match endX() everywhere, or finishing inside object/array, " +
                        "${isObjectStack.size}"
            )
        }
    }

    fun write(v: Vector2f) {
        beginArray()
        write(v.x)
        write(v.y)
        endArray()
    }

    fun write(v: Vector3f) {
        beginArray()
        write(v.x)
        write(v.y)
        write(v.z)
        endArray()
    }

    fun write(v: Vector3d) {
        beginArray()
        write(v.x)
        write(v.y)
        write(v.z)
        endArray()
    }

    fun write(v: Vector4f) {
        beginArray()
        write(v.x)
        write(v.y)
        write(v.z)
        write(v.w)
        endArray()
    }

    fun write(q: Quaternionf) {
        beginArray()
        write(q.x)
        write(q.y)
        write(q.z)
        write(q.w)
        endArray()
    }

    fun write(q: Quaterniond) {
        beginArray()
        write(q.x)
        write(q.y)
        write(q.z)
        write(q.w)
        endArray()
    }
}