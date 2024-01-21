package me.anno.io.json.generic

import me.anno.io.Streams.writeString
import me.anno.utils.types.Strings
import org.joml.Quaterniond
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.OutputStream

/**
 * simple class to write JSON content; if you write Saveables, consider BinaryWriter and TextWriter first!
 * used in Rem's Studio to save UI layout
 * */
open class JsonWriter(val output: OutputStream) {

    private var first = true
    private var isKeyValue = false

    private fun writeEscapedString(value: String) {
        output.write('"'.code)
        val sb = StringBuilder()
        Strings.writeEscaped(value, sb)
        output.writeString(sb.toString())
        output.write('"'.code)
    }

    fun attr(key: String) {
        next()
        writeEscapedString(key)
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
            output.writeString("true")
        } else {
            output.writeString("false")
        }
    }

    fun write(i: Int) {
        next()
        output.writeString(i.toString())
    }

    fun write(l: Long) {
        next()
        output.writeString(l.toString())
    }

    fun write(f: Float) {
        next()
        output.writeString(f.toString())
    }

    fun write(d: Double) {
        next()
        output.writeString(d.toString())
    }

    fun write(value: String) {
        next()
        writeEscapedString(value)
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