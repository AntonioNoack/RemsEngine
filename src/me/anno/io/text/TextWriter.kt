package me.anno.io.text

import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.utils.types.Strings
import me.anno.utils.types.Strings.isBlank2
import org.joml.*

class TextWriter(beautify: Boolean) : BaseWriter(true) {

    private val separator = if (beautify) ", " else ","

    val data = StringBuilder(32)
    private var hasObject = false
    private var usedPointers: HashSet<Int>? = null

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
        if (name != null && !name.isBlank2()) {
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

    override fun writeBooleanArray(name: String, values: BooleanArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart("b[]", name)
            open(true)
            data.append(values.size)
            val lastIndex = values.indexOfLast { it }
            for (i in 0 until lastIndex) {
                data.append(',')
                data.append(if (values[i]) '1' else '0')
            }
            close(true)
        }
    }

    override fun writeBooleanArray2D(name: String, values: Array<BooleanArray>, force: Boolean) {
        writeArray(name, values, force, "b[][]") {
            data.append(",[")
            data.append(values.size)
            val lastIndex = it.indexOfLast { b -> b }
            for (i in 0 until lastIndex) {
                data.append(',')
                data.append(if (it[i]) '1' else '0')
            }
            data.append(']')
        }
    }

    override fun writeChar(name: String, value: Char, force: Boolean) {
        if (force || value != 0.toChar()) {
            writeAttributeStart("c", name)
            data.append(value.code.toString())
        }
    }

    override fun writeCharArray(name: String, values: CharArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart("c[]", name)
            open(true)
            data.append(values.size)
            val lastIndex = values.indexOfLast { it != 0.toChar() }
            for (i in 0 until lastIndex) {
                data.append(',')
                data.append(values[i].code.toString())
            }
            close(true)
        }
    }

    override fun writeCharArray2D(name: String, values: Array<CharArray>, force: Boolean) {
        writeArray(name, values, force, "c[][]") {
            data.append(",[")
            data.append(values.size)
            val lastIndex = it.indexOfLast { i -> i != 0.toChar() }
            for (i in 0 until lastIndex) {
                data.append(',')
                data.append(it[i].code.toString())
            }
            data.append(']')
        }
    }

    override fun writeByte(name: String, value: Byte, force: Boolean) {
        if (force || value != 0.toByte()) {
            writeAttributeStart("B", name)
            data.append(value.toString())
        }
    }

    override fun writeByteArray(name: String, values: ByteArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart("B[]", name)
            open(true)
            data.append(values.size)
            val lastIndex = values.indexOfLast { it != 0.toByte() }
            for (i in 0 until lastIndex) {
                data.append(',')
                data.append(values[i].toString())
            }
            close(true)
        }
    }

    override fun writeByteArray2D(name: String, values: Array<ByteArray>, force: Boolean) {
        writeArray(name, values, force, "B[][]") {
            data.append(",[")
            data.append(values.size)
            val lastIndex = it.indexOfLast { i -> i != 0.toByte() }
            for (i in 0 until lastIndex) {
                data.append(',')
                data.append(it[i].toString())
            }
            data.append(']')
        }
    }

    override fun writeShort(name: String, value: Short, force: Boolean) {
        if (force || value != 0.toShort()) {
            writeAttributeStart("s", name)
            data.append(value)
        }
    }

    override fun writeShortArray(name: String, values: ShortArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart("s[]", name)
            open(true)
            data.append(values.size)
            val lastIndex = values.indexOfLast { it != 0.toShort() }
            for (i in 0 until lastIndex) {
                data.append(',')
                data.append(values[i].toString())
            }
            close(true)
        }
    }

    override fun writeShortArray2D(name: String, values: Array<ShortArray>, force: Boolean) {
        writeArray(name, values, force, "s[][]") {
            data.append(",[")
            data.append(values.size)
            val lastIndex = it.indexOfLast { i -> i != 0.toShort() }
            for (i in 0 until lastIndex) {
                data.append(',')
                data.append(it[i].toString())
            }
            data.append(']')
        }
    }

    override fun writeInt(name: String, value: Int, force: Boolean) {
        if (force || value != 0) {
            writeAttributeStart("i", name)
            data.append(value.toString())
        }
    }

    override fun writeIntArray(name: String, values: IntArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart("i[]", name)
            open(true)
            data.append(values.size)
            val lastIndex = values.indexOfLast { it != 0 }
            for (i in 0 until lastIndex) {
                data.append(',')
                data.append(values[i])
            }
            close(true)
        }
    }

    override fun writeIntArray2D(name: String, values: Array<IntArray>, force: Boolean) {
        writeArray(name, values, force, "i[][]") {
            data.append(",[")
            data.append(values.size)
            val lastIndex = it.indexOfLast { i -> i != 0 }
            for (i in 0 until lastIndex) {
                data.append(',')
                data.append(it[i])
            }
            data.append(']')
        }
    }

    override fun writeFloat(name: String, value: Float, force: Boolean) {
        if (force || value != 0f) {
            writeAttributeStart("f", name)
            append(value)
        }
    }

    override fun writeFloatArray(name: String, values: FloatArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart("f[]", name)
            open(true)
            data.append(values.size)
            val lastIndex = values.indexOfLast { it != 0f }
            for (i in 0 until lastIndex) {
                data.append(',')
                append(values[i])
            }
            close(true)
        }
    }

    override fun writeFloatArray2D(name: String, values: Array<FloatArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart("f[][]", name)
            open(true)
            data.append(values.size)
            for (vs in values) {
                data.append(",[")
                data.append(vs.size)
                for (v in values) {
                    data.append(',')
                    data.append(v.toString())
                }
                data.append(']')
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

    override fun writeDoubleArray(name: String, values: DoubleArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart("d[]", name)
            open(true)
            data.append(values.size)
            val lastIndex = values.indexOfLast { it != 0.0 }
            for (i in 0 until lastIndex) {
                data.append(',')
                append(values[i])
            }
            close(true)
        }
    }

    override fun writeDoubleArray2D(name: String, values: Array<DoubleArray>, force: Boolean) {
        writeArray(name, values, force, "d[][]") {
            data.append(",[")
            data.append(it.size)
            for (v in it) {
                data.append(',')
                data.append(v)
            }
            data.append(']')
        }
    }

    override fun writeString(name: String, value: String?, force: Boolean) {
        if (force || (value != null && value != "")) {
            writeAttributeStart("S", name)
            if (value == null) data.append("null")
            else writeString(value)
        }
    }

    override fun writeStringArray(name: String, values: Array<String>, force: Boolean) {
        writeArray(name, values, force, "S[]") {
            writeString(it)
        }
    }

    override fun writeStringArray2D(name: String, values: Array<Array<String>>, force: Boolean) {
        writeArray(name, values, force, "S[][]") {
            data.append(",[")
            data.append(it.size)
            for (v in it) {
                data.append(',')
                writeString(v)
            }
            data.append(']')
        }
    }

    override fun writeLong(name: String, value: Long, force: Boolean) {
        if (force || value != 0L) {
            writeAttributeStart("l", name)
            data.append(value)
        }
    }

    override fun writeLongArray(name: String, values: LongArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart("l[]", name)
            open(true)
            data.append(values.size)
            val lastIndex = values.indexOfLast { it != 0L }
            for (i in 0 until lastIndex) {
                data.append(',')
                data.append(values[i])
            }
            close(true)
        }
    }

    override fun writeLongArray2D(name: String, values: Array<LongArray>, force: Boolean) {
        writeArray(name, values, force, "l[][]") {
            data.append(",[")
            data.append(it.size)
            for (v in it) {
                data.append(',')
                data.append(v)
            }
            data.append(']')
        }
    }

    private fun writeVector2f(value: Vector2fc) {
        data.append('[')
        val x = value.x()
        val y = value.y()
        append(x)
        if (x != y) {
            data.append(separator)
            append(y)
        }
        data.append(']')
    }

    private fun writeVector3f(value: Vector3fc) {
        data.append('[')
        val x = value.x()
        val y = value.y()
        val z = value.z()
        append(x)
        if (!(x == y && x == z)) {
            data.append(separator)
            append(y)
            data.append(separator)
            append(z)
        }
        data.append(']')
    }

    private fun writeVector4f(value: Vector4fc) {
        data.append('[')
        val x = value.x()
        val y = value.y()
        val z = value.z()
        val w = value.w()
        // compressed writing for gray scale values, which are typical
        val xyz = !(x == y && x == z)
        val xw = x != w
        append(x)
        if (xyz) {
            data.append(separator)
            append(y)
            data.append(separator)
            append(z)
        }
        if (xyz || xw) {
            data.append(separator)
            append(w)
        }
        data.append(']')
    }

    override fun writeQuaternionf(name: String, value: Quaternionf, force: Boolean) {
        if (force || value.x != 0f || value.y != 0f || value.z != 0f || value.w != 1f) {
            writeAttributeStart("q4", name)
            data.append('[')
            val x = value.x()
            val y = value.y()
            val z = value.z()
            val w = value.w()
            append(x)
            data.append(separator)
            append(y)
            data.append(separator)
            append(z)
            data.append(separator)
            append(w)
            data.append(']')
        }
    }

    private fun writeVector2d(value: Vector2dc) {
        data.append('[')
        val x = value.x()
        val y = value.y()
        append(x)
        if (x != y) {
            data.append(separator)
            append(y)
        }
        data.append(']')
    }

    private fun writeVector3d(value: Vector3dc) {
        data.append('[')
        val x = value.x()
        val y = value.y()
        val z = value.z()
        append(x)
        if (!(x == y && x == z)) {
            data.append(separator)
            append(y)
            data.append(separator)
            append(z)
        }
        data.append(']')
    }

    private fun writeVector4d(value: Vector4dc) {
        data.append('[')
        val x = value.x()
        val y = value.y()
        val z = value.z()
        val w = value.w()
        // compressed writing for gray scale values, which are typical
        val xyz = !(x == y && x == z)
        val xw = x != w
        append(x)
        if (xyz) {
            data.append(separator)
            append(y)
            data.append(separator)
            append(z)
        }
        if (xyz || xw) {
            data.append(separator)
            append(w)
        }
        data.append(']')
    }

    override fun writeQuaterniond(name: String, value: Quaterniond, force: Boolean) {
        if (force || value.x != 0.0 || value.y != 0.0 || value.z != 0.0 || value.w != 1.0) {
            writeAttributeStart("q4d", name)
            data.append('[')
            val x = value.x()
            val y = value.y()
            val z = value.z()
            val w = value.w()
            append(x)
            data.append(separator)
            append(y)
            data.append(separator)
            append(z)
            data.append(separator)
            append(w)
            data.append(']')
        }
    }

    private fun writeVector2i(value: Vector2ic) {
        data.append('[')
        data.append(value.x())
        data.append(separator)
        data.append(value.y())
        data.append(']')
    }

    private fun writeVector3i(value: Vector3ic) {
        data.append('[')
        data.append(value.x())
        data.append(separator)
        data.append(value.y())
        data.append(separator)
        data.append(value.z())
        data.append(']')
    }

    private fun writeVector4i(value: Vector4ic) {
        data.append('[')
        data.append(value.x())
        data.append(separator)
        data.append(value.y())
        data.append(separator)
        data.append(value.z())
        data.append(separator)
        data.append(value.w())
        data.append(']')
    }

    override fun writeVector2i(name: String, value: Vector2ic, force: Boolean) {
        if (force || value.x() != 0 || value.y() != 0) {
            writeAttributeStart(name, "v2i")
            writeVector2i(value)
        }
    }

    override fun writeVector2iArray(name: String, values: Array<Vector2ic>, force: Boolean) {
        writeArray(name, values, force, "v2i[]") { writeVector2i(it) }
    }

    override fun writeVector3i(name: String, value: Vector3ic, force: Boolean) {
        if (force || value.x() != 0 || value.y() != 0 || value.z() != 0) {
            writeAttributeStart(name, "v3i")
            writeVector3i(value)
        }
    }

    override fun writeVector3iArray(name: String, values: Array<Vector3ic>, force: Boolean) {
        writeArray(name, values, force, "v3i[]") { writeVector3i(it) }
    }

    override fun writeVector4i(name: String, value: Vector4ic, force: Boolean) {
        if (force || value.x() != 0 || value.y() != 0) {
            writeAttributeStart(name, "v3i")
            writeVector4i(value)
        }
    }

    override fun writeVector4iArray(name: String, values: Array<Vector4ic>, force: Boolean) {
        writeArray(name, values, force, "v4i[]") { writeVector4i(it) }
    }

    override fun writeVector2f(name: String, value: Vector2fc, force: Boolean) {
        if (force || value.x() != 0f || value.y() != 0f) {
            writeAttributeStart("v2", name)
            writeVector2f(value)
        }
    }

    override fun writeVector2fArray(name: String, values: Array<Vector2fc>, force: Boolean) {
        writeArray(name, values, force, "v2[]") { writeVector2f(it) }
    }

    override fun writeVector3f(name: String, value: Vector3fc, force: Boolean) {
        if (force || value.x() != 0f || value.y() != 0f || value.z() != 0f) {
            writeAttributeStart("v3", name)
            writeVector3f(value)
        }
    }

    override fun writeVector3fArray(name: String, values: Array<Vector3fc>, force: Boolean) {
        writeArray(name, values, force, "v3[]") { writeVector3f(it) }
    }

    override fun writeVector4f(name: String, value: Vector4fc, force: Boolean) {
        if (force || value.x() != 0f || value.y() != 0f || value.z() != 0f || value.w() != 0f) {
            writeAttributeStart("v4", name)
            writeVector4f(value)
        }
    }

    override fun writeVector4fArray(name: String, values: Array<Vector4fc>, force: Boolean) {
        writeArray(name, values, force, "v4[]") { writeVector4f(it) }
    }

    override fun writeVector2d(name: String, value: Vector2dc, force: Boolean) {
        if (force || value.x() != 0.0 || value.y() != 0.0) {
            writeAttributeStart("v2", name)
            writeVector2d(value)
        }
    }

    override fun writeVector2dArray(name: String, values: Array<Vector2dc>, force: Boolean) {
        writeArray(name, values, force, "v2d[]") { writeVector2d(it) }
    }

    override fun writeVector3d(name: String, value: Vector3dc, force: Boolean) {
        if (force || value.x() != 0.0 || value.y() != 0.0 || value.z() != 0.0) {
            writeAttributeStart("v3", name)
            writeVector3d(value)
        }
    }

    override fun writeVector3dArray(name: String, values: Array<Vector3dc>, force: Boolean) {
        writeArray(name, values, force, "v3d[]") { writeVector3d(it) }
    }

    override fun writeVector4d(name: String, value: Vector4dc, force: Boolean) {
        if (force || value.x() != 0.0 || value.y() != 0.0 || value.z() != 0.0 || value.w() != 0.0) {
            writeAttributeStart("v4", name)
            writeVector4d(value)
        }
    }

    override fun writeVector4dArray(name: String, values: Array<Vector4dc>, force: Boolean) {
        writeArray(name, values, force, "v4d[]") { writeVector4d(it) }
    }

    private inline fun <V> writeArray(
        name: String,
        values: Array<V>,
        force: Boolean,
        type: String,
        writeValue: (V) -> Unit
    ) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(type, name)
            open(true)
            data.append(values.size)
            for (it in values) {
                data.append(separator)
                writeValue(it)
            }
            close(true)
        }
    }

    override fun writeMatrix3x3f(name: String, value: Matrix3fc, force: Boolean) {
        writeAttributeStart("m3x3", name)
        data.append('[')
        val tmp = FloatArray(9)
        value.get(tmp)
        for (i in tmp.indices) {
            if (i > 0) data.append(',')
            append(tmp[i])
        }
        data.append(']')
    }

    override fun writeMatrix4x3f(name: String, value: Matrix4x3fc, force: Boolean) {
        writeAttributeStart("m4x3", name)
        data.append('[')
        val tmp = FloatArray(12)
        value.get(tmp)
        for (i in tmp.indices) {
            if (i > 0) data.append(',')
            append(tmp[i])
        }
        data.append(']')
    }

    override fun writeMatrix4x4f(name: String, value: Matrix4fc, force: Boolean) {
        writeAttributeStart("m4x4", name)
        data.append('[')
        val tmp = FloatArray(16)
        for (i in tmp.indices) {
            if (i > 0) data.append(',')
            append(tmp[i])
        }
        data.append(']')
    }

    override fun writeMatrix3x3d(name: String, value: Matrix3dc, force: Boolean) {
        writeAttributeStart("m3x3d", name)
        data.append('[')
        val tmp = DoubleArray(9)
        value.get(tmp)
        for (i in tmp.indices) {
            if (i > 0) data.append(',')
            append(tmp[i])
        }
        data.append(']')
    }

    override fun writeMatrix4x3d(name: String, value: Matrix4x3dc, force: Boolean) {
        writeAttributeStart("m4x3d", name)
        data.append('[')
        val tmp = DoubleArray(12)
        value.get(tmp)
        for (i in tmp.indices) {
            if (i > 0) data.append(',')
            append(tmp[i])
        }
        data.append(']')
    }

    override fun writeMatrix4x4d(name: String, value: Matrix4dc, force: Boolean) {
        writeAttributeStart("m4x4d", name)
        data.append('[')
        val tmp = DoubleArray(16)
        value.get(tmp)
        for (i in tmp.indices) {
            if (i > 0) data.append(',')
            append(tmp[i])
        }
        data.append(']')
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

    override fun writeObjectImpl(name: String?, value: ISaveable) {
        writeAttributeStart(value.className, name)
        open(false)
        if (name == null) {
            writeString("class")
            data.append(':')
            writeString(value.className)
            hasObject = true
        }
        val pointer = getPointer(value)!!
        if (usedPointers?.contains(pointer) != false) {// null oder true
            writeInt("*ptr", pointer)
        }
        value.save(this)
        close(false)
    }

    override fun <V : ISaveable> writeObjectArray(self: ISaveable?, name: String, values: Array<V>?, force: Boolean) {
        if (force || values?.isNotEmpty() == true) {
            if (values == null || values.isEmpty()) {
                writeAttributeStart("*[]", name)
                data.append("[0]")
            } else {
                val firstType = values.first().className
                val allHaveSameType = values.all { it.className == firstType }
                if (allHaveSameType) {
                    writeAttributeStart("$firstType[]", name)
                    open(true)
                    data.append(values.size)
                    for (obj in values) {
                        data.append(',')
                        // self is null, because later init is not allowed
                        writeObject(null, "", obj, true)
                    }
                    close(true)
                } else {
                    writeHeterogeneousArray(name, values)
                }
            }
        }
    }

    private fun <V : ISaveable> writeHeterogeneousArray(name: String, values: Array<V>) {
        writeAttributeStart("*[]", name)
        open(true)
        data.append(values.size)
        for (obj in values) {
            data.append(',')
            // self is null, because later init is not allowed
            writeObject(null, null, obj, true)
        }
        close(true)
    }

    override fun <V : ISaveable> writeObjectArray2D(
        self: ISaveable?,
        name: String,
        values: Array<Array<V>>,
        force: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun <V : ISaveable> writeHomogenousObjectArray(
        self: ISaveable?,
        name: String,
        values: Array<V>,
        force: Boolean
    ) = writeObjectArray(self, name, values, force)

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

        fun toText(data: List<ISaveable>, beautify: Boolean): String {
            val writer = TextWriter(beautify)
            for (entry in data) writer.add(entry)
            writer.writeAllInList()
            return writer.toString()
        }

        fun toText(data: ISaveable, beautify: Boolean): String {
            val writer = TextWriter(beautify)
            writer.add(data)
            writer.writeAllInList()
            return writer.toString()
        }

        /*fun toBuilder(data: ISaveable, beautify: Boolean): StringBuilder2 {
            val writer = TextWriter(beautify)
            writer.add(data)
            writer.writeAllInList()
            return writer.data
        }*/

    }


}