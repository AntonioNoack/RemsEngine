package me.anno.io.json.generic

import me.anno.io.generic.GenericWriter
import me.anno.io.generic.GenericWriterImpl
import me.anno.utils.assertions.assertNull
import me.anno.utils.types.Strings
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.Writer

/**
 * simple class to write JSON content; if you write Saveables, consider BinaryWriter and TextWriter first!
 * used in Rem's Studio to save UI layout
 * */
open class JsonWriter(val output: Writer) : GenericWriterImpl() {

    companion object {

        fun GenericWriter.write(v: Vector2f) {
            beginArray()
            write(v.x)
            write(v.y)
            endArray()
        }

        fun GenericWriter.write(v: Vector3f) {
            beginArray()
            write(v.x)
            write(v.y)
            write(v.z)
            endArray()
        }

        fun GenericWriter.write(v: Vector3d) {
            beginArray()
            write(v.x)
            write(v.y)
            write(v.z)
            endArray()
        }

        fun GenericWriter.write(v: Vector4f) {
            beginArray()
            write(v.x)
            write(v.y)
            write(v.z)
            write(v.w)
            endArray()
        }

        fun GenericWriter.write(q: Quaternionf) {
            beginArray()
            write(q.x)
            write(q.y)
            write(q.z)
            write(q.w)
            endArray()
        }

        fun GenericWriter.write(q: Quaterniond) {
            beginArray()
            write(q.x)
            write(q.y)
            write(q.z)
            write(q.w)
            endArray()
        }
    }

    private fun writeEscapedString(value: CharSequence) {
        output.write('"'.code)
        Strings.writeEscaped(value, output, '"')
        output.write('"'.code)
    }

    override fun beginObject(tag: CharSequence?): Boolean {
        super.beginObject(tag)
        assertNull(tag)
        output.write('{'.code)
        return true
    }

    override fun endObject() {
        super.endObject()
        output.write('}'.code)
    }

    override fun beginArray(): Boolean {
        super.beginArray()
        output.write('['.code)
        return true
    }

    override fun endArray() {
        super.endArray()
        output.write(']'.code)
    }

    override fun next() {
        super.next()
        output.write(','.code)
    }

    override fun write(value: CharSequence, isString: Boolean) {
        super.write(value, isString)
        if (isString || value == "NaN" || value == "Infinity" || value == "-Infinity") {
            writeEscapedString(value)
        } else {
            output.write(value.toString())
        }
    }

    override fun attr(tag: CharSequence): Boolean {
        super.attr(tag)
        writeEscapedString(tag)
        output.write(':'.code)
        return true
    }

    override fun finish() {
        super.finish()
        output.close()
    }
}