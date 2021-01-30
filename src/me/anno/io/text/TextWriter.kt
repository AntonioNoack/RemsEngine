package me.anno.io.text

import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.utils.types.Strings
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f

class TextWriter(beautify: Boolean) : BaseWriter(true) {

    private val separator = if (beautify) ", " else ","

    val data = StringBuilder(512)
    private var hasObject = false
    private var usedPointers: HashSet<Int>? = null

    operator fun StringBuilder.plusAssign(char: Char) {
        append(char)
    }

    operator fun StringBuilder.plusAssign(text: String) {
        append(text)
    }

    fun open(array: Boolean) {
        data.append(if (array) '[' else '{')
        hasObject = false
    }

    fun close(array: Boolean) {
        data.append(if (array) ']' else '}')
        hasObject = true
    }

    fun next() {
        if (hasObject) {
            data.append(',')
        }
        hasObject = true
    }

    private fun writeEscaped(value: String) {
        Strings.writeEscaped(value, data)
    }

    private fun writeString(value: String) {
        data.append('"')
        writeEscaped(value)
        data.append('"')
    }

    private fun writeTypeNameString(type: String, name: String?) {
        if (name != null) {
            data.append('"')
            writeEscaped(type)
            data.append(':')
            writeEscaped(name)
            data.append('"')
        }
    }

    private fun writeAttributeStart(type: String, name: String?) {
        if (name != null && name.isNotBlank()) {
            next()
            writeTypeNameString(type, name)
            data.append(':')
        }
    }

    private fun append(f: Float) {
        val str = f.toString()
        if (str.endsWith(".0")) {
            data.append(str.substring(0, str.length - 2))
        } else {
            data.append(str)
        }
    }

    private fun append(f: Double) {
        val str = f.toString()
        if (str.endsWith(".0")) {
            data.append(str.substring(0, str.length - 2))
        } else {
            data.append(str)
        }
    }

    override fun writeBoolean(name: String, value: Boolean, force: Boolean) {
        if (force || value) {
            writeAttributeStart("b", name)
            data.append(if (value) "true" else "false")
        }
    }

    override fun writeBooleanArray(name: String, value: BooleanArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("b[]", name)
            open(true)
            data.append(value.size)
            val lastIndex = value.indexOfLast { it }
            for (i in 0 until lastIndex) {
                data.append(',')
                data.append(if (value[i]) '1' else '0')
            }
            close(true)
        }
    }

    override fun writeByte(name: String, value: Byte, force: Boolean) {
        if (force || value != 0.toByte()) {
            writeAttributeStart("B", name)
            data.append(value.toString())
        }
    }

    override fun writeByteArray(name: String, value: ByteArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("B[]", name)
            open(true)
            data.append(value.size)
            val lastIndex = value.indexOfLast { it != 0.toByte() }
            for (i in 0 until lastIndex) {
                data.append(',')
                data.append(value[i].toString())
            }
            close(true)
        }
    }

    override fun writeShort(name: String, value: Short, force: Boolean) {
        if (force || value != 0.toShort()) {
            writeAttributeStart("s", name)
            data += value.toString()
        }
    }

    override fun writeShortArray(name: String, value: ShortArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("s[]", name)
            open(true)
            data.append(value.size)
            val lastIndex = value.indexOfLast { it != 0.toShort() }
            for (i in 0 until lastIndex) {
                data.append(',')
                data.append(value[i].toString())
            }
            close(true)
        }
    }

    override fun writeInt(name: String, value: Int, force: Boolean) {
        if (force || value != 0) {
            writeAttributeStart("i", name)
            data.append(value.toString())
        }
    }

    override fun writeIntArray(name: String, value: IntArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("i[]", name)
            open(true)
            data.append(value.size)
            val lastIndex = value.indexOfLast { it != 0 }
            for (i in 0 until lastIndex) {
                data.append(',')
                data.append(value[i].toString())
            }
            close(true)
        }
    }

    override fun writeFloat(name: String, value: Float, force: Boolean) {
        if (force || value != 0f) {
            writeAttributeStart("f", name)
            append(value)
        }
    }

    override fun writeFloatArray(name: String, value: FloatArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("f[]", name)
            open(true)
            data.append(value.size)
            val lastIndex = value.indexOfLast { it != 0f }
            for (i in 0 until lastIndex) {
                data.append(',')
                append(value[i])
            }
            close(true)
        }
    }

    override fun writeFloatArray2D(name: String, value: Array<FloatArray>, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("f[][]", name)
            open(true)
            data.append(value.size)
            for (vs in value) {
                data += ','
                data += '['
                data.append(vs.size)
                for (v in value) {
                    data += ','
                    data += v.toString()
                }
                data += ']'
            }
            close(true)
        }
    }

    override fun writeDouble(name: String, value: Double, force: Boolean) {
        if (force || value != 0.0) {
            writeAttributeStart("d", name)
            append(value)
        }
    }

    override fun writeDoubleArray(name: String, value: DoubleArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("d[]", name)
            open(true)
            data.append(value.size)
            val lastIndex = value.indexOfLast { it != 0.0 }
            for (i in 0 until lastIndex) {
                data += ','
                append(value[i])
            }
            close(true)
        }
    }

    override fun writeDoubleArray2D(name: String, value: Array<DoubleArray>, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("d[][]", name)
            open(true)
            data.append(value.size)
            for (vs in value) {
                data += ','
                data += '['
                data.append(vs.size)
                for (v in value) {
                    data += ','
                    data += v.toString()
                }
                data += ']'
            }
            close(true)
        }
    }

    override fun writeString(name: String, value: String?, force: Boolean) {
        if (force || (value != null && value != "")) {
            writeAttributeStart("S", name)
            if (value == null) data.append("null")
            else writeString(value)
        }
    }

    override fun writeStringArray(name: String, value: Array<String>, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("S[]", name)
            open(true)
            data.append(value.size)
            for (v in value) {
                data.append(',')
                writeString(v)
            }
            close(true)
        }
    }

    override fun writeLong(name: String, value: Long, force: Boolean) {
        if (force || value != 0L) {
            writeAttributeStart("l", name)
            data.append(value)
        }
    }

    override fun writeLongArray(name: String, value: LongArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("l[]", name)
            open(true)
            data.append(value.size)
            val lastIndex = value.indexOfLast { it != 0L }
            for (i in 0 until lastIndex) {
                data.append(',')
                data.append(value[i])
            }
            close(true)
        }
    }

    private fun writeVector2f(value: Vector2f) {
        data.append('[')
        append(value.x)
        if (value.x != value.y) {
            data.append(separator)
            append(value.y)
        }
        data.append(']')
    }

    private fun writeVector3f(value: Vector3f) {
        data.append('[')
        append(value.x)
        if (!(value.x == value.y && value.x == value.z)) {
            data.append(separator)
            append(value.y)
            data.append(separator)
            append(value.z)
        }
        data.append(']')
    }

    private fun writeVector4f(value: Vector4f) {
        data.append('[')
        // compressed writing for gray scale values, which are typical
        val xyz = !(value.x == value.y && value.x == value.z)
        val xw = value.x != value.w
        append(value.x)
        if (xyz) {
            data.append(separator)
            append(value.y)
            data.append(separator)
            append(value.z)
        }
        if (xyz || xw) {
            data.append(separator)
            append(value.w)
        }
        data.append(']')
    }

    private fun writeVector4d(value: Vector4d) {
        data.append('[')
        // compressed writing for gray scale values, which are typical
        val xyz = !(value.x == value.y && value.x == value.z)
        val xw = value.x != value.w
        append(value.x)
        if (xyz) {
            data.append(separator)
            append(value.y)
            data.append(separator)
            append(value.z)
        }
        if (xyz || xw) {
            data.append(separator)
            append(value.w)
        }
        data.append(']')
    }

    override fun writeVector2f(name: String, value: Vector2f, force: Boolean) {
        if (force || value.x != 0f || value.y != 0f) {
            writeAttributeStart("v2", name)
            writeVector2f(value)
        }
    }

    override fun writeVector3f(name: String, value: Vector3f, force: Boolean) {
        if (force || value.x != 0f || value.y != 0f || value.z != 0f) {
            writeAttributeStart("v3", name)
            writeVector3f(value)
        }
    }

    override fun writeVector4f(name: String, value: Vector4f, force: Boolean) {
        if (force || value.x != 0f || value.y != 0f || value.z != 0f || value.w != 0f) {
            writeAttributeStart("v4", name)
            writeVector4f(value)
        }
    }

    override fun writeVector4d(name: String, value: Vector4d, force: Boolean) {
        if (force || value.x != 0.0 || value.y != 0.0 || value.z != 0.0 || value.w != 0.0) {
            writeAttributeStart("v4", name)
            writeVector4d(value)
        }
    }

    override fun writeNull(name: String?) {
        writeAttributeStart("?", name)
        data.append("null")
    }

    override fun writeListStart() {
        open(true)
    }

    override fun writeListSeparator() {
        data.append(',')
        hasObject = true
    }

    override fun writeListEnd() {
        close(true)
    }

    override fun writeVector2fArray(name: String, elements: Array<Vector2f>, force: Boolean) {
        if (force || elements.isNotEmpty()) {
            writeAttributeStart("v2[]", name)
            open(true)
            elements.forEach { writeVector2f(it) }
            close(true)
        }
    }

    override fun writeVector3fArray(name: String, elements: Array<Vector3f>, force: Boolean) {
        if (force || elements.isNotEmpty()) {
            writeAttributeStart("v3[]", name)
            open(true)
            elements.forEach { writeVector3f(it) }
            close(true)
        }
    }

    override fun writeVector4fArray(name: String, elements: Array<Vector4f>, force: Boolean) {
        if (force || elements.isNotEmpty()) {
            writeAttributeStart("v4[]", name)
            open(true)
            elements.forEach { writeVector4f(it) }
            close(true)
        }
    }

    override fun writeObjectImpl(name: String?, value: ISaveable) {
        writeAttributeStart(value.getClassName(), name)
        open(false)
        if(name == null){
            writeString("class")
            data.append(':')
            writeString(value.getClassName())
            hasObject = true
        }
        val pointer = getPointer(value)!!
        if (usedPointers?.contains(pointer) != false) {// null oder true
            writeInt("*ptr", pointer)
        }
        value.save(this)
        close(false)
    }

    override fun <V : ISaveable> writeObjectArray(self: ISaveable?, name: String, elements: Array<V>, force: Boolean) {
        if (force || elements.isNotEmpty()) {
            if(elements.isEmpty()){
                writeAttributeStart("*[]", name)
                data.append("[0]")
            } else {
                val firstType = elements.first().getClassName()
                val allHaveSameType = elements.all { it.getClassName() == firstType }
                if(allHaveSameType){
                    writeAttributeStart("$firstType[]", name)
                    open(true)
                    data.append(elements.size)
                    elements.forEach {
                        data.append(',')
                        // self is null, because later init is not allowed
                        writeObject(null, "", it, true)
                    }
                    close(true)
                } else {
                    writeAttributeStart("*[]", name)
                    open(true)
                    data.append(elements.size)
                    elements.forEach {
                        data.append(',')
                        // self is null, because later init is not allowed
                        writeObject(null, null, it, true)
                    }
                    close(true)
                }
            }
        }
    }

    override fun <V : ISaveable> writeHomogenousObjectArray(
        self: ISaveable?,
        name: String,
        elements: Array<V>,
        force: Boolean
    ) = writeObjectArray(self, name, elements, force)

    override fun writePointer(name: String?, className: String, ptr: Int) {
        writeAttributeStart(className, name)
        data.append(ptr)
    }

    override fun writeAllInList() {
        val writer0 = FindReferencesWriter()
        for (todoItem in todo) writer0.add(todoItem)
        writer0.writeAllInList()
        usedPointers = writer0.usedPointers
        // directly written needs this, because of sortedContent
        usedPointers!!.addAll(todo.map { writer0.getPointer(it)!! })
        super.writeAllInList()
    }

    override fun toString(): String = data.toString()

    companion object {

        fun toText(data: List<Saveable>, beautify: Boolean): String {
            val writer = TextWriter(beautify)
            for (entry in data) writer.add(entry)
            writer.writeAllInList()
            return writer.toString()
        }

        fun toText(data: Saveable, beautify: Boolean): String {
            val writer = TextWriter(beautify)
            writer.add(data)
            writer.writeAllInList()
            return writer.toString()
        }

        fun toBuilder(data: Saveable, beautify: Boolean): StringBuilder {
            val writer = TextWriter(beautify)
            writer.add(data)
            writer.writeAllInList()
            return writer.data
        }

    }


}