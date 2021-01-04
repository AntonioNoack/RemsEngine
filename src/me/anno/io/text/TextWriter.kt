package me.anno.io.text

import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f

class TextWriter(beautify: Boolean) : BaseWriter(true) {

    private val separator = if (beautify) ", " else ","

    var data = StringBuilder(512)
    private var hasObject = false
    private var usedPointers: HashSet<Int>? = null

    operator fun StringBuilder.plusAssign(char: Char) {
        append(char)
    }

    operator fun StringBuilder.plusAssign(text: String) {
        append(text)
    }

    fun open(array: Boolean) {
        data += if (array) '[' else '{'
        hasObject = false
    }

    fun close(array: Boolean) {
        data += if (array) ']' else '}'
        hasObject = true
    }

    fun next() {
        if (hasObject) {
            data += ','
        }
        hasObject = true
    }

    private fun writeEscaped(value: String) {
        var i = 0
        var lastI = 0
        fun put() {
            if (i > lastI) {
                data += value.substring(lastI, i)
            }
            lastI = i + 1
        }
        while (i < value.length) {
            when (value[i]) {
                '\\' -> {
                    put()
                    data += "\\\\"
                }
                '\t' -> {
                    put()
                    data += "\\t"
                }
                '\r' -> {
                    put()
                    data += "\\r"
                }
                '\n' -> {
                    put()
                    data += "\\n"
                }
                '"' -> {
                    put()
                    data += "\\\""
                }
                '\b' -> {
                    put()
                    data += "\\b"
                }
                12.toChar() -> {
                    put()
                    data += "\\f"
                }
                else -> {
                } // nothing
            }
            i++
        }
        put()
    }

    private fun writeString(value: String) {
        data += '"'
        writeEscaped(value)
        data += '"'
    }

    private fun writeTypeNameString(type: String, name: String?) {
        if (name != null) {
            data += '"'
            writeEscaped(type)
            data += ':'
            writeEscaped(name)
            data += '"'
        }
    }

    private fun writeAttributeStart(type: String, name: String?) {
        if (name != null) {
            next()
            writeTypeNameString(type, name)
            data += ':'
        }
    }

    override fun writeBoolean(name: String, value: Boolean, force: Boolean) {
        if (force || value) {
            writeAttributeStart("b", name)
            data += if (value) "true" else "false"
        }
    }

    override fun writeBooleanArray(name: String, value: BooleanArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("b[]", name)
            open(true)
            data += value.size.toString()
            val lastIndex = value.indexOfLast { it }
            for (i in 0 until lastIndex) {
                data += ','
                data += if(value[i]) "true" else "false"
            }
            close(true)
        }
    }

    override fun writeByte(name: String, value: Byte, force: Boolean) {
        if (force || value != 0.toByte()) {
            writeAttributeStart("B", name)
            data += value.toString()
        }
    }

    override fun writeByteArray(name: String, value: ByteArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("B[]", name)
            open(true)
            data += value.size.toString()
            val lastIndex = value.indexOfLast { it != 0.toByte() }
            for (i in 0 until lastIndex) {
                data += ','
                data += value[i].toString()
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
            data += value.size.toString()
            val lastIndex = value.indexOfLast { it != 0.toShort() }
            for (i in 0 until lastIndex) {
                data += ','
                data += value[i].toString()
            }
            close(true)
        }
    }

    override fun writeInt(name: String, value: Int, force: Boolean) {
        if (force || value != 0) {
            writeAttributeStart("i", name)
            data += value.toString()
        }
    }

    override fun writeIntArray(name: String, value: IntArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("i[]", name)
            open(true)
            data += value.size.toString()
            val lastIndex = value.indexOfLast { it != 0 }
            for (i in 0 until lastIndex) {
                data += ','
                data += value[i].toString()
            }
            close(true)
        }
    }

    override fun writeFloat(name: String, value: Float, force: Boolean) {
        if (force || value != 0f) {
            writeAttributeStart("f", name)
            data += value.toString()
        }
    }

    override fun writeFloatArray(name: String, value: FloatArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("f[]", name)
            open(true)
            data += value.size.toString()
            val lastIndex = value.indexOfLast { it != 0f }
            for (i in 0 until lastIndex) {
                data += ','
                data += value[i].toString()
            }
            close(true)
        }
    }

    override fun writeFloatArray2D(name: String, value: Array<FloatArray>, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("f[][]", name)
            open(true)
            data += value.size.toString()
            for (vs in value) {
                data += ','
                data += '['
                data += vs.size.toString()
                for(v in value){
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
            data += value.toString()
        }
    }

    override fun writeDoubleArray(name: String, value: DoubleArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("d[]", name)
            open(true)
            data += value.size.toString()
            val lastIndex = value.indexOfLast { it != 0.0 }
            for (i in 0 until lastIndex) {
                data += ','
                data += value[i].toString()
            }
            close(true)
        }
    }

    override fun writeDoubleArray2D(name: String, value: Array<DoubleArray>, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("d[][]", name)
            open(true)
            data += value.size.toString()
            for (vs in value) {
                data += ','
                data += '['
                data += vs.size.toString()
                for(v in value){
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
            if (value == null) data += "null"
            else writeString(value)
        }
    }

    override fun writeStringArray(name: String, value: Array<String>, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("S[]", name)
            open(true)
            data += value.size.toString()
            for (v in value) {
                data += ','
                data += v
            }
            close(true)
        }
    }

    override fun writeLong(name: String, value: Long, force: Boolean) {
        if (force || value != 0L) {
            writeAttributeStart("l", name)
            data += value.toString()
        }
    }

    override fun writeLongArray(name: String, value: LongArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart("l[]", name)
            open(true)
            data += value.size.toString()
            val lastIndex = value.indexOfLast { it != 0L }
            for (i in 0 until lastIndex) {
                data += ','
                data += value[i].toString()
            }
            close(true)
        }
    }

    override fun writeVector2f(name: String, value: Vector2f, force: Boolean) {
        if (force || value.x != 0f || value.y != 0f) {
            writeAttributeStart("v2", name)
            data += '['
            data += value.x.toString()
            data += separator
            data += value.y.toString()
            data += ']'
        }
    }

    override fun writeVector3f(name: String, value: Vector3f, force: Boolean) {
        if (force || value.x != 0f || value.y != 0f || value.z != 0f) {
            writeAttributeStart("v3", name)
            data += '['
            data += value.x.toString()
            data += separator
            data += value.y.toString()
            data += separator
            data += value.z.toString()
            data += ']'
        }
    }

    override fun writeVector4f(name: String, value: Vector4f, force: Boolean) {
        if (force || value.x != 0f || value.y != 0f || value.z != 0f || value.w != 0f) {
            writeAttributeStart("v4", name)
            data += '['
            data += value.x.toString()
            data += separator
            data += value.y.toString()
            data += separator
            data += value.z.toString()
            data += separator
            data += value.w.toString()
            data += ']'
        }
    }

    override fun writeVector4d(name: String, value: Vector4d, force: Boolean) {
        if (force || value.x != 0.0 || value.y != 0.0 || value.z != 0.0 || value.w != 0.0) {
            writeAttributeStart("v4", name)
            data += '['
            data += value.x.toString()
            data += separator
            data += value.y.toString()
            data += separator
            data += value.z.toString()
            data += separator
            data += value.w.toString()
            data += ']'
        }
    }

    override fun writeNull(name: String?) {
        writeAttributeStart("?", name)
        data += "null"
    }

    override fun writeListStart() {
        open(true)
    }

    override fun writeListSeparator() {
        data += ','
        hasObject = true
    }

    override fun writeListEnd() {
        close(true)
    }

    override fun writeVector2fArray(name: String, elements: Array<Vector2f>, force: Boolean) {
        if (force || elements.isNotEmpty()) {
            writeAttributeStart("v2[]", name)
            open(true)
            elements.forEach {
                data += '['
                data += it.x.toString()
                data += separator
                data += it.y.toString()
                data += ']'
            }
            close(true)
        }
    }

    override fun writeVector3fArray(name: String, elements: Array<Vector3f>, force: Boolean) {
        if (force || elements.isNotEmpty()) {
            writeAttributeStart("v3[]", name)
            open(true)
            elements.forEach {
                data += '['
                data += it.x.toString()
                data += separator
                data += it.y.toString()
                data += separator
                data += it.z.toString()
                data += ']'
            }
            close(true)
        }
    }

    override fun writeVector4fArray(name: String, elements: Array<Vector4f>, force: Boolean) {
        if (force || elements.isNotEmpty()) {
            writeAttributeStart("v4[]", name)
            open(true)
            elements.forEach {
                data += '['
                data += it.x.toString()
                data += separator
                data += it.y.toString()
                data += separator
                data += it.z.toString()
                data += separator
                data += it.w.toString()
                data += ']'
            }
            close(true)
        }
    }

    override fun writeObjectImpl(name: String?, value: ISaveable) {
        if (name != null && name.isNotEmpty()) {
            writeAttributeStart(value.getClassName(), name)
            open(false)
        } else {
            open(false)
            writeString("class")
            data += ':'
            writeString(value.getClassName())
            hasObject = true
        }
        val pointer = getPointer(value)!!
        if(usedPointers?.contains(pointer) != false){// null oder true
            writeInt("*ptr", pointer)
        }
        value.save(this)
        close(false)
    }

    override fun <V : ISaveable> writeObjectArray(self: ISaveable?, name: String, elements: Array<V>, force: Boolean) {
        // todo implement correctly xD
        elements.forEach {
            writeObject(self, name, it, force)
        }
    }

    class HomogenousArray<V: ISaveable>(val elements: Array<V>): Saveable() {
        override fun isDefaultValue(): Boolean = false
        override fun getClassName(): String = "HomogenousArray"
        override fun getApproxSize() = elements.map { it.getApproxSize() }.max()?.plus(1) ?: 1
        override fun save(writer: BaseWriter) {
            super.save(writer)

        }
    }

    override fun <V : ISaveable> writeHomogenousObjectArray(
        self: ISaveable?,
        name: String,
        elements: Array<V>,
        force: Boolean
    ) {
        if(force || elements.isNotEmpty()){
            // todo implement correctly xD
            elements.forEach {
                writeObject(self, name, it, force)
            }
        }
    }

    override fun writePointer(name: String?, className: String, ptr: Int) {
        writeAttributeStart(className, name)
        data += ptr.toString()
    }

    override fun writeAllInList() {
        val writer0 = FindReferencesWriter()
        for(todoItem in todo) writer0.add(todoItem)
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

    }


}