package me.anno.io.json.generic

import me.anno.utils.types.Strings
import org.joml.*
import java.io.OutputStream

/**
 * simple class to write JSON content; if you write Saveables, consider BinaryWriter and TextWriter first!
 * used in Rem's Studio to save UI layout
 * */
open class JsonWriter(val output: OutputStream) {

    private var first = true
    private var isKeyValue = false

    private fun writeString(value: String) {
        output.write('"'.code)
        val sb = StringBuilder()
        Strings.writeEscaped(value, sb)
        output.write(sb.toString().toByteArray())
        output.write('"'.code)
    }

    fun attr(key: String) {
        next()
        writeString(key)
        isKeyValue = true
    }

    fun next() {
        if (!first) {
            output.write(if (isKeyValue) ':'.code else ','.code)
        }
        isKeyValue = false
        first = false
    }

    fun write(b: Boolean) {
        next()
        if (b) {
            output.write("true".toByteArray())
        } else {
            output.write("false".toByteArray())
        }
    }

    fun write(i: Int) {
        next()
        output.write(i.toString().toByteArray())
    }

    fun write(l: Long) {
        next()
        output.write(l.toString().toByteArray())
    }

    fun write(f: Float) {
        next()
        output.write(f.toString().toByteArray())
    }

    fun write(d: Double) {
        next()
        output.write(d.toString().toByteArray())
    }

    fun write(value: String) {
        next()
        writeString(value)
    }

    fun beginArray() {
        next()
        output.write('['.code)
        first = true
    }

    fun beginObject() {
        next()
        output.write('{'.code)
        first = true
    }

    fun endArray() {
        output.write(']'.code)
        first = false
    }

    fun endObject() {
        output.write('}'.code)
        first = false
    }

    fun finish() {
        output.close()
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

    fun write(q: Quaterniond) {
        beginArray()
        write(q.x)
        write(q.y)
        write(q.z)
        write(q.w)
        endArray()
    }
}