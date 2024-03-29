package me.anno.io.json.saveable

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.SimpleType.BOOLEAN
import me.anno.io.json.saveable.SimpleType.CHAR
import me.anno.io.json.saveable.SimpleType.COLOR
import me.anno.io.json.saveable.SimpleType.DOUBLE
import me.anno.io.json.saveable.SimpleType.FLOAT
import me.anno.io.json.saveable.SimpleType.INT
import me.anno.io.json.saveable.SimpleType.LONG
import me.anno.io.json.saveable.SimpleType.REFERENCE
import me.anno.io.json.saveable.SimpleType.STRING
import me.anno.maths.Maths
import me.anno.utils.Color
import me.anno.utils.types.Strings
import me.anno.utils.types.Strings.isBlank2
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix2d
import org.joml.Matrix2f
import org.joml.Matrix3d
import org.joml.Matrix3f
import org.joml.Matrix3x2d
import org.joml.Matrix3x2f
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Planed
import org.joml.Planef
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4d
import org.joml.Vector4f
import org.joml.Vector4i
import kotlin.math.abs

abstract class JsonWriterBase(val workspace: FileReference) : BaseWriter(true) {

    private var hasObject = false

    private val tmp16f = FloatArray(16)
    private val tmp16d = DoubleArray(16)

    abstract fun append(v: Char)

    abstract fun append(v: String)

    abstract fun append(v: Int)

    abstract fun append(v: Long)

    open fun append(v: Byte): Unit = append(v.toInt())

    open fun append(v: Short): Unit = append(v.toInt())

    fun open(array: Boolean) {
        append(if (array) '[' else '{')
        hasObject = false
    }

    fun close(array: Boolean) {
        append(if (array) ']' else '}')
        hasObject = true
    }

    fun next() {
        if (hasObject) {
            append(',')
        }
        hasObject = true
    }

    private fun writeEscaped(value: String) {
        Strings.writeEscaped(value, this)
    }

    private fun writeString(value: String) {
        append('"')
        writeEscaped(value)
        append('"')
    }

    private fun writeTypeNameString(type: String, name: String?) {
        if (name != null) {
            append('"')
            writeEscaped(type)
            append(':')
            writeEscaped(name)
            append('"')
        }
    }

    private fun writeAttributeStart(type: String, name: String?) {
        if (name != null && !name.isBlank2()) {
            next()
            writeTypeNameString(type, name)
            append(':')
        }
    }

    private fun append(f: Float) {
        val str = f.toString()
        if (str.endsWith(".0")) {
            append(str.substring(0, str.length - 2))
        } else {
            append(str)
        }
    }

    private fun append(f: Double) {
        val str = f.toString()
        if (str.endsWith(".0")) {
            append(str.substring(0, str.length - 2))
        } else {
            append(str)
        }
    }

    override fun writeBoolean(name: String, value: Boolean, force: Boolean) {
        if (force || value) {
            writeAttributeStart(BOOLEAN.scalar, name)
            append(if (value) "true" else "false")
        }
    }

    override fun writeBooleanArray(name: String, values: BooleanArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(BOOLEAN.array, name)
            writeArray(values.size, values.indexOfLast { it }) {
                append(if (values[it]) '1' else '0')
            }
        }
    }

    override fun writeBooleanArray2D(name: String, values: Array<BooleanArray>, force: Boolean) {
        writeArray(name, values, force, BOOLEAN.array2d) { arr ->
            writeArray(arr.size, arr.indexOfLast { it }) {
                append(if (arr[it]) '1' else '0')
            }
        }
    }

    fun appendCharValue(value: Char) {
        when (value) {
            in 'A'..'Z', in 'a'..'z', in '0'..'9',
            in " _+-*/!§$%&()[]{}|~<>" -> {
                append('\"')
                append(value)
                append('\"')
            }
            else -> append(value.code)
        }
    }

    override fun writeChar(name: String, value: Char, force: Boolean) {
        if (force || value != 0.toChar()) {
            writeAttributeStart(CHAR.scalar, name)
            appendCharValue(value)
        }
    }

    override fun writeCharArray(name: String, values: CharArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(CHAR.array, name)
            writeArray(values.size, values.indexOfLast { it.code != 0 }) {
                appendCharValue(values[it])
            }
        }
    }

    override fun writeCharArray2D(name: String, values: Array<CharArray>, force: Boolean) {
        writeArray(name, values, force, CHAR.array2d) { arr ->
            writeArray(arr.size, arr.indexOfLast { it.code != 0 }) {
                appendCharValue(arr[it])
            }
        }
    }

    override fun writeByte(name: String, value: Byte, force: Boolean) {
        if (force || value != 0.toByte()) {
            writeAttributeStart(SimpleType.BYTE.scalar, name)
            append(value.toInt())
        }
    }

    override fun writeByteArray(name: String, values: ByteArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(SimpleType.BYTE.array, name)
            writeArray(values.size, values.indexOfLast { it != 0.toByte() }) {
                append(values[it].toInt())
            }
        }
    }

    override fun writeByteArray2D(name: String, values: Array<ByteArray>, force: Boolean) {
        writeArray(name, values, force, SimpleType.BYTE.array2d) { arr ->
            writeArray(arr.size, arr.indexOfLast { it != 0.toByte() }) {
                append(arr[it].toInt())
            }
        }
    }

    override fun writeShort(name: String, value: Short, force: Boolean) {
        if (force || value != 0.toShort()) {
            writeAttributeStart(SimpleType.SHORT.scalar, name)
            append(value)
        }
    }

    override fun writeShortArray(name: String, values: ShortArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(SimpleType.SHORT.array, name)
            writeArray(values.size, values.indexOfLast { it != 0.toShort() }) {
                append(values[it].toInt())
            }
        }
    }

    override fun writeShortArray2D(name: String, values: Array<ShortArray>, force: Boolean) {
        writeArray(name, values, force, SimpleType.SHORT.array2d) { arr ->
            writeArray(arr.size, arr.indexOfLast { it != 0.toShort() }) {
                append(arr[it].toInt())
            }
        }
    }

    override fun writeInt(name: String, value: Int, force: Boolean) {
        if (force || value != 0) {
            writeAttributeStart(SimpleType.INT.scalar, name)
            append(value)
        }
    }

    private fun appendColor(color: Int) {
        // write color in optimal hex format
        append('"') // be JSON compatible
        append('#')
        val c0 = color.and(0xf)
        val c1 = color.shr(4).and(0xf)
        val c2 = color.shr(8).and(0xf)
        val c3 = color.shr(12).and(0xf)
        val c4 = color.shr(16).and(0xf)
        val c5 = color.shr(20).and(0xf)
        val c6 = color.shr(24).and(0xf)
        val c7 = color.shr(28).and(0xf)
        val hex = Color.base36
        when {
            c0 == c1 && c2 == c3 && c4 == c5 && c6 == c7 -> {
                if (c6 == 15) { // #rgb
                    append(hex[c4])
                    append(hex[c2])
                    append(hex[c0])
                } else { // #argb
                    append(hex[c6])
                    append(hex[c4])
                    append(hex[c2])
                    append(hex[c0])
                }
            }
            c6 == 15 && c7 == 15 -> { // #rrggbb
                append(hex[c5])
                append(hex[c4])
                append(hex[c3])
                append(hex[c2])
                append(hex[c1])
                append(hex[c0])
            }
            else -> { // #aarrggbb
                append(hex[c7])
                append(hex[c6])
                append(hex[c5])
                append(hex[c4])
                append(hex[c3])
                append(hex[c2])
                append(hex[c1])
                append(hex[c0])
            }
        }
        append('"')
    }

    override fun writeColor(name: String, value: Int, force: Boolean) {
        if (force || value != 0) {
            writeAttributeStart(COLOR.scalar, name)
            appendColor(value)
        }
    }

    override fun writeIntArray(name: String, values: IntArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(INT.array, name)
            // 18-23ns/e
            writeArray(values.size, values.indexOfLast { it != 0 }) {
                append(values[it])
            }
        }
    }

    override fun writeColorArray(name: String, values: IntArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(COLOR.array, name)
            // 18-23ns/e
            writeArray(values.size, values.indexOfLast { it != 0 }) {
                appendColor(values[it])
            }
        }
    }

    override fun writeIntArray2D(name: String, values: Array<IntArray>, force: Boolean) {
        writeArray(name, values, force, INT.array2d) { arr ->
            writeArray(arr.size, arr.indexOfLast { it != 0 }) {
                append(arr[it])
            }
        }
    }

    override fun writeColorArray2D(name: String, values: Array<IntArray>, force: Boolean) {
        writeArray(name, values, force, COLOR.array2d) { arr ->
            writeArray(arr.size, arr.indexOfLast { it != 0 }) {
                appendColor(arr[it])
            }
        }
    }

    override fun writeFloat(name: String, value: Float, force: Boolean) {
        if (force || value != 0f) {
            writeAttributeStart(FLOAT.scalar, name)
            append(value)
        }
    }

    override fun writeFloatArray(name: String, values: FloatArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(FLOAT.array, name)
            writeArray(values.size, values.indexOfLast { it != 0f }) {
                append(values[it])
            }
        }
    }

    override fun writeFloatArray2D(name: String, values: Array<FloatArray>, force: Boolean) {
        writeArray(name, values, force, FLOAT.array2d) { arr ->
            writeArray(arr.size, arr.indexOfLast { it != 0f }) {
                append(arr[it])
            }
        }
    }

    override fun writeDouble(name: String, value: Double, force: Boolean) {
        if (force || value != 0.0) {
            writeAttributeStart(DOUBLE.scalar, name)
            append(value)
        }
    }

    override fun writeDoubleArray(name: String, values: DoubleArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(DOUBLE.array, name)
            writeArray(values.size, values.indexOfLast { it != 0.0 }) {
                append(values[it])
            }
        }
    }

    override fun writeDoubleArray2D(name: String, values: Array<DoubleArray>, force: Boolean) {
        writeArray(name, values, force, DOUBLE.array2d) { arr ->
            writeArray(arr.size, arr.indexOfLast { it != 0.0 }) {
                append(arr[it])
            }
        }
    }

    override fun writeString(name: String, value: String, force: Boolean) {
        if (force || value != "") {
            writeAttributeStart(STRING.scalar, name)
            writeString(value)
        }
    }

    override fun writeStringArray(name: String, values: Array<String>, force: Boolean) {
        writeArray(name, values, force, STRING.array) {
            writeString(it)
        }
    }

    override fun writeStringArray2D(name: String, values: Array<Array<String>>, force: Boolean) {
        writeArray2D(name, values, force, STRING.array2d, ::writeString)
    }

    private fun writeFile(value: FileReference?, workspace: FileReference) {
        if (value == null || value == InvalidRef) writeString("")
        else writeString(value.toLocalPath(workspace.ifUndefined(this.workspace)))
    }

    override fun writeFile(name: String, value: FileReference, force: Boolean, workspace: FileReference) {
        if (force || value != InvalidRef) {
            writeAttributeStart(REFERENCE.scalar, name)
            writeFile(value, workspace)
        }
    }

    override fun writeFileArray(name: String, values: Array<FileReference>, force: Boolean, workspace: FileReference) {
        writeArray(name, values, force, REFERENCE.array) {
            writeFile(it, workspace)
        }
    }

    override fun writeFileArray2D(
        name: String, values: Array<Array<FileReference>>,
        force: Boolean, workspace: FileReference
    ) {
        writeArray2D(name, values, force, REFERENCE.array2d) {
            writeFile(it, workspace)
        }
    }

    override fun writeLong(name: String, value: Long, force: Boolean) {
        if (force || value != 0L) {
            writeAttributeStart(LONG.scalar, name)
            append(value)
        }
    }

    override fun writeLongArray(name: String, values: LongArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(LONG.array, name)
            writeArray(values.size, values.indexOfLast { it != 0L }) {
                append(values[it])
            }
        }
    }

    override fun writeLongArray2D(name: String, values: Array<LongArray>, force: Boolean) {
        writeArray(name, values, force, LONG.array2d) { arr ->
            writeArray(arr.size, arr.indexOfLast { it != 0L }) {
                append(arr[it])
            }
        }
    }

    private fun writeVector2f(value: Vector2f) {
        writeVector2f(value.x, value.y)
    }

    private fun writeVector2f(x: Float, y: Float) {
        append('[')
        append(x)
        if (x != y) {
            append(',')
            append(y)
        }
        append(']')
    }

    private fun writeVector3f(value: Vector3f) {
        writeVector3f(value.x, value.y, value.z)
    }

    private fun writeVector3f(x: Float, y: Float, z: Float) {
        append('[')
        append(x)
        if (!(x == y && x == z)) {
            append(',')
            append(y)
            append(',')
            append(z)
        }
        append(']')
    }

    private fun writeVector4f(value: Vector4f) {
        writeVector4f(value.x, value.y, value.z, value.w)
    }

    private fun writeVector4f(x: Float, y: Float, z: Float, w: Float) {
        append('[')
        // compressed writing for gray scale values, which are typical
        val xyz = !(x == y && x == z)
        val xw = x != w
        append(x)
        if (xyz) {
            append(',')
            append(y)
            append(',')
            append(z)
        }
        if (xyz || xw) {
            append(',')
            append(w)
        }
        append(']')
    }

    private fun writeQuaternionf(value: Quaternionf) {
        append('[')
        val x = value.x
        val y = value.y
        val z = value.z
        val w = value.w
        append(x)
        append(',')
        append(y)
        append(',')
        append(z)
        append(',')
        append(w)
        append(']')
    }

    private fun writeQuaterniond(value: Quaterniond) {
        append('[')
        val x = value.x
        val y = value.y
        val z = value.z
        val w = value.w
        append(x)
        append(',')
        append(y)
        append(',')
        append(z)
        append(',')
        append(w)
        append(']')
    }

    override fun writeQuaternionf(name: String, value: Quaternionf, force: Boolean) {
        if (force || value.x != 0f || value.y != 0f || value.z != 0f || value.w != 1f) {
            writeAttributeStart("q4", name)
            writeQuaternionf(value)
        }
    }

    override fun writeQuaternionfArray(name: String, values: Array<Quaternionf>, force: Boolean) =
        writeArray(name, values, force, "q4[]", ::writeQuaternionf)

    override fun writeQuaternionfArray2D(name: String, values: Array<Array<Quaternionf>>, force: Boolean) =
        writeArray2D(name, values, force, "q4[][]", ::writeQuaternionf)

    override fun writeQuaterniondArray(name: String, values: Array<Quaterniond>, force: Boolean) =
        writeArray(name, values, force, "q4d[]", ::writeQuaterniond)

    override fun writeQuaterniondArray2D(name: String, values: Array<Array<Quaterniond>>, force: Boolean) =
        writeArray2D(name, values, force, "q4d[][]", ::writeQuaterniond)

    private fun writeVector2d(value: Vector2d) {
        writeVector2d(value.x, value.y)
    }

    private fun writeVector2d(x: Double, y: Double) {
        append('[')
        append(x)
        if (x != y) {
            append(',')
            append(y)
        }
        append(']')
    }

    private fun writeVector3d(value: Vector3d) {
        writeVector3d(value.x, value.y, value.z)
    }

    private fun writeVector3d(x: Double, y: Double, z: Double) {
        append('[')
        append(x)
        if (!(x == y && x == z)) {
            append(',')
            append(y)
            append(',')
            append(z)
        }
        append(']')
    }

    private fun writeVector4d(value: Vector4d) {
        writeVector4d(value.x, value.y, value.z, value.w)
    }

    private fun writeVector4d(x: Double, y: Double, z: Double, w: Double) {
        append('[')
        // compressed writing for gray scale values, which are typical
        val xyz = !(x == y && x == z)
        val xw = x != w
        append(x)
        if (xyz) {
            append(',')
            append(y)
            append(',')
            append(z)
        }
        if (xyz || xw) {
            append(',')
            append(w)
        }
        append(']')
    }

    override fun writeQuaterniond(name: String, value: Quaterniond, force: Boolean) {
        if (force || value.x != 0.0 || value.y != 0.0 || value.z != 0.0 || value.w != 1.0) {
            writeAttributeStart("q4d", name)
            writeQuaterniond(value)
        }
    }

    private fun writeVector2i(value: Vector2i) {
        append('[')
        append(value.x)
        append(',')
        append(value.y)
        append(']')
    }

    private fun writeVector3i(value: Vector3i) {
        append('[')
        append(value.x)
        append(',')
        append(value.y)
        append(',')
        append(value.z)
        append(']')
    }

    private fun writeVector4i(value: Vector4i) {
        append('[')
        append(value.x)
        append(',')
        append(value.y)
        append(',')
        append(value.z)
        append(',')
        append(value.w)
        append(']')
    }

    override fun writeVector2i(name: String, value: Vector2i, force: Boolean) {
        if (force || value.x != 0 || value.y != 0) {
            writeAttributeStart("v2i", name)
            writeVector2i(value)
        }
    }

    override fun writeVector2iArray(name: String, values: Array<Vector2i>, force: Boolean) =
        writeArray(name, values, force, "v2i[]", ::writeVector2i)

    override fun writeVector2iArray2D(name: String, values: Array<Array<Vector2i>>, force: Boolean) =
        writeArray2D(name, values, force, "v2i[][]", ::writeVector2i)

    override fun writeVector3i(name: String, value: Vector3i, force: Boolean) {
        if (force || value.x != 0 || value.y != 0 || value.z != 0) {
            writeAttributeStart("v3i", name)
            writeVector3i(value)
        }
    }

    override fun writeVector3iArray(name: String, values: Array<Vector3i>, force: Boolean) =
        writeArray(name, values, force, "v3i[]", ::writeVector3i)

    override fun writeVector3iArray2D(name: String, values: Array<Array<Vector3i>>, force: Boolean) =
        writeArray2D(name, values, force, "v3i[][]", ::writeVector3i)

    override fun writeVector4i(name: String, value: Vector4i, force: Boolean) {
        if (force || value.x != 0 || value.y != 0) {
            writeAttributeStart("v4i", name)
            writeVector4i(value)
        }
    }

    override fun writeVector4iArray(name: String, values: Array<Vector4i>, force: Boolean) =
        writeArray(name, values, force, "v4i[]", ::writeVector4i)

    override fun writeVector4iArray2D(name: String, values: Array<Array<Vector4i>>, force: Boolean) =
        writeArray2D(name, values, force, "v4i[][]", ::writeVector4i)

    override fun writeVector2f(name: String, value: Vector2f, force: Boolean) {
        if (force || value.x != 0f || value.y != 0f) {
            writeAttributeStart("v2", name)
            writeVector2f(value)
        }
    }

    override fun writeVector2fArray(name: String, values: Array<Vector2f>, force: Boolean) =
        writeArray(name, values, force, "v2[]", ::writeVector2f)

    override fun writeVector2fArray2D(name: String, values: Array<Array<Vector2f>>, force: Boolean) =
        writeArray2D(name, values, force, "v2[][]", ::writeVector2f)

    override fun writeVector3f(name: String, value: Vector3f, force: Boolean) {
        if (force || value.x != 0f || value.y != 0f || value.z != 0f) {
            writeAttributeStart("v3", name)
            writeVector3f(value)
        }
    }

    override fun writeVector3fArray(name: String, values: Array<Vector3f>, force: Boolean) =
        writeArray(name, values, force, "v3[]", ::writeVector3f)

    override fun writeVector3fArray2D(name: String, values: Array<Array<Vector3f>>, force: Boolean) =
        writeArray2D(name, values, force, "v3[][]", ::writeVector3f)

    override fun writeVector4f(name: String, value: Vector4f, force: Boolean) {
        if (force || value.x != 0f || value.y != 0f || value.z != 0f || value.w != 0f) {
            writeAttributeStart("v4", name)
            writeVector4f(value)
        }
    }

    override fun writeVector4fArray(name: String, values: Array<Vector4f>, force: Boolean) =
        writeArray(name, values, force, "v4[]", ::writeVector4f)

    override fun writeVector4fArray2D(name: String, values: Array<Array<Vector4f>>, force: Boolean) =
        writeArray2D(name, values, force, "v4[][]", ::writeVector4f)

    override fun writeVector2d(name: String, value: Vector2d, force: Boolean) {
        if (force || value.x != 0.0 || value.y != 0.0) {
            writeAttributeStart("v2d", name)
            writeVector2d(value)
        }
    }

    override fun writeVector2dArray(name: String, values: Array<Vector2d>, force: Boolean) =
        writeArray(name, values, force, "v2d[]", ::writeVector2d)

    override fun writeVector2dArray2D(name: String, values: Array<Array<Vector2d>>, force: Boolean) =
        writeArray2D(name, values, force, "v2d[][]", ::writeVector2d)

    override fun writeVector3d(name: String, value: Vector3d, force: Boolean) {
        if (force || value.x != 0.0 || value.y != 0.0 || value.z != 0.0) {
            writeAttributeStart("v3d", name)
            writeVector3d(value)
        }
    }

    override fun writeVector3dArray(name: String, values: Array<Vector3d>, force: Boolean) =
        writeArray(name, values, force, "v3d[]", ::writeVector3d)

    override fun writeVector3dArray2D(name: String, values: Array<Array<Vector3d>>, force: Boolean) =
        writeArray2D(name, values, force, "v3d[][]", ::writeVector3d)

    override fun writeVector4d(name: String, value: Vector4d, force: Boolean) {
        if (force || value.x != 0.0 || value.y != 0.0 || value.z != 0.0 || value.w != 0.0) {
            writeAttributeStart("vd4", name)
            writeVector4d(value)
        }
    }

    override fun writeVector4dArray(name: String, values: Array<Vector4d>, force: Boolean) =
        writeArray(name, values, force, "v4d[]", ::writeVector4d)

    override fun writeVector4dArray2D(name: String, values: Array<Array<Vector4d>>, force: Boolean) =
        writeArray2D(name, values, force, "v4d[][]", ::writeVector4d)

    private inline fun writeArray(
        size: Int,
        lastIndex: Int,
        writeValue: (Int) -> Unit
    ) {
        open(true)
        append(size)
        for (i in 0 until Maths.min(lastIndex + 1, size)) {
            append(',')
            writeValue(i)
        }
        close(true)
    }

    private inline fun <V> writeArray(
        name: String,
        values: Array<V>?,
        force: Boolean,
        type: String,
        writeValue: (V) -> Unit
    ) {
        if (values == null) {
            if (force) {
                writeAttributeStart(type, name)
                append("null")
            }
        } else {
            if (force || values.isNotEmpty()) {
                writeAttributeStart(type, name)
                open(true)
                append(values.size)
                for (it in values) {
                    append(',')
                    writeValue(it)
                }
                close(true)
            }
        }
    }

    private inline fun <V> writeArray2D(
        name: String, values: Array<Array<V>>,
        force: Boolean, type: String, writeValue: (V) -> Unit
    ) {
        writeArray(name, values, force, type) { array ->
            writeArray(array.size, array.size) {
                writeValue(array[it])
            }
        }
    }

    private fun writeMatrix3x3f(value: Matrix3f) {
        val tmp = tmp16f
        value.get(tmp)
        append('[')
        clamp3x3Relative(tmp)
        for (i in 0 until 9 step 3) {
            if (i > 0) append(',')
            writeVector3f(tmp[i], tmp[i + 1], tmp[i + 2])
        }
        append(']')
    }

    private fun writeMatrix4x3f(value: Matrix4x3f) {
        val tmp = tmp16f
        value.get(tmp) // col major
        clamp4x3Relative(tmp)
        append('[')
        for (i in 0 until 12 step 3) {
            if (i > 0) append(',')
            writeVector3f(tmp[i], tmp[i + 1], tmp[i + 2])
        }
        append(']')
    }

    private fun writeMatrix4x4f(value: Matrix4f) {
        val tmp = tmp16f
        value.get(tmp) // col major
        clamp4x4Relative(tmp)
        append('[')
        for (i in 0 until 16 step 4) {
            if (i > 0) append(',')
            writeVector4f(tmp[i], tmp[i + 1], tmp[i + 2], tmp[i + 3])
        }
        append(']')
    }

    private fun writeMatrix2x2f(value: Matrix2f) {
        val tmp = tmp16f
        value.get(tmp)
        append('[')
        clamp2x2Relative(tmp)
        for (i in 0 until 4 step 2) {
            if (i > 0) append(',')
            writeVector2f(tmp[i], tmp[i + 1])
        }
        append(']')
    }

    private fun writeMatrix3x2f(value: Matrix3x2f) {
        val tmp = tmp16f
        value.get(tmp)
        append('[')
        clamp3x2Relative(tmp)
        for (i in 0 until 6 step 2) {
            if (i > 0) append(',')
            writeVector2f(tmp[i], tmp[i + 1])
        }
        append(']')
    }

    override fun writeMatrix2x2f(name: String, value: Matrix2f, force: Boolean) {
        writeAttributeStart("m2x2", name)
        writeMatrix2x2f(value)
    }

    override fun writeMatrix3x2f(name: String, value: Matrix3x2f, force: Boolean) {
        writeAttributeStart("m3x2", name)
        writeMatrix3x2f(value)
    }

    override fun writeMatrix3x3f(name: String, value: Matrix3f, force: Boolean) {
        writeAttributeStart("m3x3", name)
        writeMatrix3x3f(value)
    }

    override fun writeMatrix4x3f(name: String, value: Matrix4x3f, force: Boolean) {
        writeAttributeStart("m4x3", name)
        writeMatrix4x3f(value)
    }

    override fun writeMatrix2x2fArray(name: String, values: Array<Matrix2f>, force: Boolean) =
        writeArray(name, values, force, "m2x2[]", ::writeMatrix2x2f)

    override fun writeMatrix3x2fArray(name: String, values: Array<Matrix3x2f>, force: Boolean) =
        writeArray(name, values, force, "m3x2[]", ::writeMatrix3x2f)

    override fun writeMatrix3x3fArray(name: String, values: Array<Matrix3f>, force: Boolean) =
        writeArray(name, values, force, "m3x3[]", ::writeMatrix3x3f)

    override fun writeMatrix4x3fArray(name: String, values: Array<Matrix4x3f>, force: Boolean) =
        writeArray(name, values, force, "m4x3[]", ::writeMatrix4x3f)

    override fun writeMatrix4x4fArray(name: String, values: Array<Matrix4f>, force: Boolean) =
        writeArray(name, values, force, "m4x4[]", ::writeMatrix4x4f)

    override fun writeMatrix2x2fArray2D(name: String, values: Array<Array<Matrix2f>>, force: Boolean) =
        writeArray2D(name, values, force, "m2x2[][]", ::writeMatrix2x2f)

    override fun writeMatrix3x2fArray2D(name: String, values: Array<Array<Matrix3x2f>>, force: Boolean) =
        writeArray2D(name, values, force, "m3x2[][]", ::writeMatrix3x2f)

    override fun writeMatrix3x3fArray2D(name: String, values: Array<Array<Matrix3f>>, force: Boolean) =
        writeArray2D(name, values, force, "m3x3[][]", ::writeMatrix3x3f)

    override fun writeMatrix4x3fArray2D(name: String, values: Array<Array<Matrix4x3f>>, force: Boolean) =
        writeArray2D(name, values, force, "m4x3[][]", ::writeMatrix4x3f)

    override fun writeMatrix4x4fArray2D(name: String, values: Array<Array<Matrix4f>>, force: Boolean) =
        writeArray2D(name, values, force, "m4x4[][]", ::writeMatrix4x4f)

    override fun writeMatrix4x4f(name: String, value: Matrix4f, force: Boolean) {
        writeAttributeStart("m4x4", name)
        writeMatrix4x4f(value)
    }

    private fun writeMatrix2x2d(value: Matrix2d) {
        val tmp = tmp16d
        value.get(tmp)
        append('[')
        clamp2x2Relative(tmp)
        for (i in 0 until 4 step 2) {
            if (i > 0) append(',')
            writeVector3d(tmp[i], tmp[i + 1], tmp[i + 2])
        }
        append(']')
    }

    private fun writeMatrix3x2d(value: Matrix3x2d) {
        val tmp = tmp16d
        value.get(tmp)
        append('[')
        clamp3x2Relative(tmp)
        for (i in 0 until 6 step 2) {
            if (i > 0) append(',')
            writeVector3d(tmp[i], tmp[i + 1], tmp[i + 2])
        }
        append(']')
    }

    private fun writeMatrix3x3d(value: Matrix3d) {
        val tmp = tmp16d
        value.get(tmp)
        append('[')
        clamp3x3Relative(tmp)
        for (i in 0 until 9 step 3) {
            if (i > 0) append(',')
            writeVector3d(tmp[i], tmp[i + 1], tmp[i + 2])
        }
        append(']')
    }

    private fun writeMatrix4x3d(value: Matrix4x3d) {
        val tmp = tmp16d
        value.get(tmp) // col major
        clamp4x3Relative(tmp)
        append('[')
        for (i in 0 until 12 step 3) {
            if (i > 0) append(',')
            writeVector3d(tmp[i], tmp[i + 1], tmp[i + 2])
        }
        append(']')
    }

    private fun writeMatrix4x4d(value: Matrix4d) {
        val tmp = tmp16d
        value.get(tmp) // col major
        clamp4x4Relative(tmp)
        append('[')
        for (i in 0 until 16 step 4) {
            if (i > 0) append(',')
            writeVector4d(tmp[i], tmp[i + 1], tmp[i + 2], tmp[i + 3])
        }
        append(']')
    }

    override fun writeMatrix2x2d(name: String, value: Matrix2d, force: Boolean) {
        writeAttributeStart("m2x2d", name)
        writeMatrix2x2d(value)
    }

    override fun writeMatrix3x2d(name: String, value: Matrix3x2d, force: Boolean) {
        writeAttributeStart("m3x2d", name)
        writeMatrix3x2d(value)
    }

    override fun writeMatrix3x3d(name: String, value: Matrix3d, force: Boolean) {
        writeAttributeStart("m3x3d", name)
        writeMatrix3x3d(value)
    }

    override fun writeMatrix4x3d(name: String, value: Matrix4x3d, force: Boolean) {
        writeAttributeStart("m4x3d", name)
        writeMatrix4x3d(value)
    }

    override fun writeMatrix4x4d(name: String, value: Matrix4d, force: Boolean) {
        writeAttributeStart("m4x4d", name)
        writeMatrix4x4d(value)
    }

    override fun writeMatrix2x2dArray(name: String, values: Array<Matrix2d>, force: Boolean) =
        writeArray(name, values, force, "m2x2d[]", ::writeMatrix2x2d)

    override fun writeMatrix3x2dArray(name: String, values: Array<Matrix3x2d>, force: Boolean) =
        writeArray(name, values, force, "m3x2d[]", ::writeMatrix3x2d)

    override fun writeMatrix3x3dArray(name: String, values: Array<Matrix3d>, force: Boolean) =
        writeArray(name, values, force, "m3x3d[]", ::writeMatrix3x3d)

    override fun writeMatrix4x3dArray(name: String, values: Array<Matrix4x3d>, force: Boolean) =
        writeArray(name, values, force, "m4x3d[]", ::writeMatrix4x3d)

    override fun writeMatrix4x4dArray(name: String, values: Array<Matrix4d>, force: Boolean) =
        writeArray(name, values, force, "m4x4d[]", ::writeMatrix4x4d)

    override fun writeMatrix2x2dArray2D(name: String, values: Array<Array<Matrix2d>>, force: Boolean) =
        writeArray2D(name, values, force, "m2x2d[][]", ::writeMatrix2x2d)

    override fun writeMatrix3x2dArray2D(name: String, values: Array<Array<Matrix3x2d>>, force: Boolean) =
        writeArray2D(name, values, force, "m3x2d[][]", ::writeMatrix3x2d)

    override fun writeMatrix3x3dArray2D(name: String, values: Array<Array<Matrix3d>>, force: Boolean) =
        writeArray2D(name, values, force, "m3x3d[][]", ::writeMatrix3x3d)

    override fun writeMatrix4x3dArray2D(name: String, values: Array<Array<Matrix4x3d>>, force: Boolean) =
        writeArray2D(name, values, force, "m4x3d[][]", ::writeMatrix4x3d)

    override fun writeMatrix4x4dArray2D(name: String, values: Array<Array<Matrix4d>>, force: Boolean) =
        writeArray2D(name, values, force, "m4x4[][]", ::writeMatrix4x4d)

    // clamp values, which are 1e-7 below the scale -> won't impact anything, and saves space
    private fun clamp2x2Relative(tmp: FloatArray, scale: Float = 1e-7f) {
        val sx = Maths.absMax(tmp[0], tmp[2]) * scale
        val sy = Maths.absMax(tmp[1], tmp[3]) * scale
        for (i in 0 until 4 step 2) {
            if (abs(tmp[i + 0]) < sx) tmp[i + 0] = 0f
            if (abs(tmp[i + 1]) < sy) tmp[i + 1] = 0f
        }
    }

    private fun clamp3x2Relative(tmp: FloatArray, scale: Float = 1e-7f) {
        val sx = Maths.absMax(tmp[0], tmp[2]) * scale
        val sy = Maths.absMax(tmp[1], tmp[3]) * scale
        for (i in 0 until 6 step 2) {
            if (abs(tmp[i + 0]) < sx) tmp[i + 0] = 0f
            if (abs(tmp[i + 1]) < sy) tmp[i + 1] = 0f
        }
    }

    private fun clamp2x2Relative(tmp: DoubleArray, scale: Double = 1e-7) {
        val sx = Maths.absMax(tmp[0], tmp[2]) * scale
        val sy = Maths.absMax(tmp[1], tmp[3]) * scale
        for (i in 0 until 4 step 2) {
            if (abs(tmp[i + 0]) < sx) tmp[i + 0] = 0.0
            if (abs(tmp[i + 1]) < sy) tmp[i + 1] = 0.0
        }
    }

    private fun clamp3x2Relative(tmp: DoubleArray, scale: Double = 1e-7) {
        val sx = Maths.absMax(tmp[0], tmp[2]) * scale
        val sy = Maths.absMax(tmp[1], tmp[3]) * scale
        for (i in 0 until 6 step 2) {
            if (abs(tmp[i + 0]) < sx) tmp[i + 0] = 0.0
            if (abs(tmp[i + 1]) < sy) tmp[i + 1] = 0.0
        }
    }

    private fun clamp3x3Relative(tmp: FloatArray, scale: Float = 1e-7f) {
        val sx = Maths.absMax(tmp[0], tmp[3], tmp[6]) * scale
        val sy = Maths.absMax(tmp[1], tmp[4], tmp[7]) * scale
        val sz = Maths.absMax(tmp[2], tmp[5], tmp[8]) * scale
        for (i in 0 until 9 step 3) {
            if (abs(tmp[i + 0]) < sx) tmp[i + 0] = 0f
            if (abs(tmp[i + 1]) < sy) tmp[i + 1] = 0f
            if (abs(tmp[i + 2]) < sz) tmp[i + 2] = 0f
        }
    }

    private fun clamp3x3Relative(tmp: DoubleArray, scale: Double = 1e-7) {
        val sx = Maths.absMax(tmp[0], tmp[3], tmp[6]) * scale
        val sy = Maths.absMax(tmp[1], tmp[4], tmp[7]) * scale
        val sz = Maths.absMax(tmp[2], tmp[5], tmp[8]) * scale
        for (i in 0 until 9 step 3) {
            if (abs(tmp[i + 0]) < sx) tmp[i + 0] = 0.0
            if (abs(tmp[i + 1]) < sy) tmp[i + 1] = 0.0
            if (abs(tmp[i + 2]) < sz) tmp[i + 2] = 0.0
        }
    }

    private fun clamp4x3Relative(tmp: FloatArray, scale: Float = 1e-7f) {
        val sx = Maths.absMax(tmp[0], tmp[3], tmp[6], tmp[9]) * scale
        val sy = Maths.absMax(tmp[1], tmp[4], tmp[7], tmp[10]) * scale
        val sz = Maths.absMax(tmp[2], tmp[5], tmp[8], tmp[11]) * scale
        for (i in 0 until 12 step 3) {
            if (abs(tmp[i + 0]) < sx) tmp[i + 0] = 0f
            if (abs(tmp[i + 1]) < sy) tmp[i + 1] = 0f
            if (abs(tmp[i + 2]) < sz) tmp[i + 2] = 0f
        }
    }

    private fun clamp4x3Relative(tmp: DoubleArray, scale: Double = 1e-7) {
        val sx = Maths.absMax(tmp[0], tmp[3], tmp[6], tmp[9]) * scale
        val sy = Maths.absMax(tmp[1], tmp[4], tmp[7], tmp[10]) * scale
        val sz = Maths.absMax(tmp[2], tmp[5], tmp[8], tmp[11]) * scale
        for (i in 0 until 12 step 3) {
            if (abs(tmp[i + 0]) < sx) tmp[i + 0] = 0.0
            if (abs(tmp[i + 1]) < sy) tmp[i + 1] = 0.0
            if (abs(tmp[i + 2]) < sz) tmp[i + 2] = 0.0
        }
    }

    private fun clamp4x4Relative(tmp: FloatArray, scale: Float = 1e-7f) {
        val sx = Maths.absMax(tmp[0], tmp[4], tmp[8], tmp[12]) * scale
        val sy = Maths.absMax(tmp[1], tmp[5], tmp[9], tmp[13]) * scale
        val sz = Maths.absMax(tmp[2], tmp[6], tmp[10], tmp[14]) * scale
        val sw = Maths.absMax(tmp[3], tmp[7], tmp[11], tmp[15]) * scale
        for (i in 0 until 16 step 4) {
            if (abs(tmp[i + 0]) < sx) tmp[i + 0] = 0f
            if (abs(tmp[i + 1]) < sy) tmp[i + 1] = 0f
            if (abs(tmp[i + 2]) < sz) tmp[i + 2] = 0f
            if (abs(tmp[i + 3]) < sw) tmp[i + 3] = 0f
        }
    }

    private fun clamp4x4Relative(tmp: DoubleArray, scale: Double = 1e-7) {
        val sx = Maths.absMax(tmp[0], tmp[4], tmp[8], tmp[12]) * scale
        val sy = Maths.absMax(tmp[1], tmp[5], tmp[9], tmp[13]) * scale
        val sz = Maths.absMax(tmp[2], tmp[6], tmp[10], tmp[14]) * scale
        val sw = Maths.absMax(tmp[3], tmp[7], tmp[11], tmp[15]) * scale
        for (i in 0 until 16 step 4) {
            if (abs(tmp[i + 0]) < sx) tmp[i + 0] = 0.0
            if (abs(tmp[i + 1]) < sy) tmp[i + 1] = 0.0
            if (abs(tmp[i + 2]) < sz) tmp[i + 2] = 0.0
            if (abs(tmp[i + 3]) < sw) tmp[i + 3] = 0.0
        }
    }

    private fun writeAABBf(value: AABBf) {
        append('[')
        append('[')
        append(value.minX)
        append(',')
        append(value.minY)
        append(',')
        append(value.minZ)
        append(']')
        append(',')
        append('[')
        append(value.maxX)
        append(',')
        append(value.maxY)
        append(',')
        append(value.maxZ)
        append(']')
        append(']')
    }

    private fun writeAABBd(value: AABBd) {
        append('[')
        append('[')
        append(value.minX)
        append(',')
        append(value.minY)
        append(',')
        append(value.minZ)
        append(']')
        append(',')
        append('[')
        append(value.maxX)
        append(',')
        append(value.maxY)
        append(',')
        append(value.maxZ)
        append(']')
        append(']')
    }

    override fun writeAABBf(name: String, value: AABBf, force: Boolean) {
        writeAttributeStart("AABBf", name)
        writeAABBf(value)
    }

    override fun writeAABBd(name: String, value: AABBd, force: Boolean) {
        writeAttributeStart("AABBd", name)
        writeAABBd(value)
    }

    override fun writeAABBfArray(name: String, values: Array<AABBf>, force: Boolean) =
        writeArray(name, values, force, "AABBf[]", ::writeAABBf)

    override fun writeAABBdArray(name: String, values: Array<AABBd>, force: Boolean) =
        writeArray(name, values, force, "AABBd[]", ::writeAABBd)

    override fun writeAABBfArray2D(name: String, values: Array<Array<AABBf>>, force: Boolean) =
        writeArray2D(name, values, force, "AABBf[][]", ::writeAABBf)

    override fun writeAABBdArray2D(name: String, values: Array<Array<AABBd>>, force: Boolean) =
        writeArray2D(name, values, force, "AABBd[][]", ::writeAABBd)

    fun writePlanef(value: Planef) {
        append('[')
        append(value.dirX)
        append(',')
        append(value.dirY)
        append(',')
        append(value.dirZ)
        append(',')
        append(value.distance)
        append(']')
    }

    fun writePlaned(value: Planed) {
        append('[')
        append(value.dirX)
        append(',')
        append(value.dirY)
        append(',')
        append(value.dirZ)
        append(',')
        append(value.distance)
        append(']')
    }

    override fun writePlanef(name: String, value: Planef, force: Boolean) {
        writeAttributeStart("p4", name)
        writePlanef(value)
    }

    override fun writePlaned(name: String, value: Planed, force: Boolean) {
        writeAttributeStart("p4d", name)
        writePlaned(value)
    }

    override fun writePlanefArray(name: String, values: Array<Planef>, force: Boolean) =
        writeArray(name, values, force, "p4[]", ::writePlanef)

    override fun writePlanedArray(name: String, values: Array<Planed>, force: Boolean) =
        writeArray(name, values, force, "p4d[]", ::writePlaned)

    override fun writePlanefArray2D(name: String, values: Array<Array<Planef>>, force: Boolean) =
        writeArray2D(name, values, force, "p4[][]", ::writePlanef)

    override fun writePlanedArray2D(name: String, values: Array<Array<Planed>>, force: Boolean) =
        writeArray2D(name, values, force, "p4d[][]", ::writePlaned)

    override fun writeNull(name: String?) {
        writeAttributeStart("?", name)
        append("null")
    }

    override fun writeListStart() {
        open(true)
    }

    override fun writeListSeparator() {
        append(',')
        hasObject = true
    }

    override fun writeListEnd() {
        close(true)
    }

    override fun writeObjectImpl(name: String?, value: Saveable) {
        writeAttributeStart(value.className, name)
        open(false)
        if (name == null) {
            writeString("class")
            append(':')
            writeString(value.className)
            hasObject = true
        }
        val pointer = getPointer(value)!!
        writeInt("*ptr", pointer)
        value.save(this)
        close(false)
    }

    override fun <V : Saveable> writeObjectArray(self: Saveable?, name: String, values: Array<V>?, force: Boolean) {
        if (force || values?.isNotEmpty() == true) {
            if (values.isNullOrEmpty()) {
                writeAttributeStart("*[]", name)
                append("[0]")
            } else {
                val firstType = values.first().className
                val allHaveSameType = values.all { it.className == firstType }
                if (allHaveSameType) {
                    writeAttributeStart("$firstType[]", name)
                    open(true)
                    append(values.size)
                    for (obj in values) {
                        append(',')
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

    override fun <V : Saveable?> writeNullableObjectArray(
        self: Saveable?,
        name: String,
        values: Array<V>?,
        force: Boolean
    ) {
        if (force || values?.isNotEmpty() == true) {
            if (values.isNullOrEmpty()) {
                writeAttributeStart("*[]", name)
                append("[0]")
            } else {
                val firstType = values.first()?.className
                val allHaveSameType = values.all { it?.className == firstType }
                if (firstType != null && allHaveSameType) {
                    writeAttributeStart("$firstType[]", name)
                    open(true)
                    append(values.size)
                    for (obj in values) {
                        append(',')
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

    private fun <V : Saveable?> writeHeterogeneousArray(name: String, values: Array<V>) {
        writeAttributeStart("*[]", name)
        writeArray(values.size, values.size) {
            writeObject(null, null, values[it], true)
        }
    }

    override fun <V : Saveable> writeObjectArray2D(
        self: Saveable?,
        name: String,
        values: Array<Array<V>>,
        force: Boolean
    ) {
        writeArray(name, values, force, "*[][]") { arr ->
            writeArray(arr.size, arr.size) {
                writeObject(null, null, arr[it], true)
            }
        }
    }

    override fun <V : Saveable?> writeHomogenousObjectArray(
        self: Saveable?,
        name: String,
        values: Array<V>,
        force: Boolean
    ) = writeNullableObjectArray(self, name, values, force)

    override fun writePointer(name: String?, className: String, ptr: Int, value: Saveable) {
        writeAttributeStart(className, name)
        append(ptr)
    }
}