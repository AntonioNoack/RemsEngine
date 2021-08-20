package me.anno.io.text

import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.Maths.absMax
import me.anno.utils.Maths.min
import me.anno.utils.types.Strings
import me.anno.utils.types.Strings.isBlank2
import org.joml.*
import kotlin.math.abs

class TextWriter(beautify: Boolean) : BaseWriter(true) {

    private val separator = if (beautify) ", " else ","

    val data = StringBuilder(32)
    private var hasObject = false
    private var usedPointers: HashSet<Int>? = null

    private val tmp16 = FloatArray(16)
    private val tmp16d = DoubleArray(16)

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
            writeArray(values.size, values.indexOfLast { it }) {
                data.append(if (values[it]) '1' else '0')
            }
        }
    }

    override fun writeBooleanArray2D(name: String, values: Array<BooleanArray>, force: Boolean) {
        writeArray(name, values, force, "b[][]") { arr ->
            writeArray(arr.size, arr.indexOfLast { it }) {
                data.append(if (arr[it]) '1' else '0')
            }
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
            writeArray(values.size, values.indexOfLast { it.code != 0 }) {
                data.append(values[it].code)
            }
        }
    }

    override fun writeCharArray2D(name: String, values: Array<CharArray>, force: Boolean) {
        writeArray(name, values, force, "c[][]") { arr ->
            writeArray(arr.size, arr.indexOfLast { it.code != 0 }) {
                data.append(arr[it].code)
            }
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
            writeArray(values.size, values.indexOfLast { it != 0.toByte() }) {
                data.append(values[it].toString())
            }
        }
    }

    override fun writeByteArray2D(name: String, values: Array<ByteArray>, force: Boolean) {
        writeArray(name, values, force, "B[][]") { arr ->
            writeArray(arr.size, arr.indexOfLast { it != 0.toByte() }) {
                data.append(arr[it].toString())
            }
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
            writeArray(values.size, values.indexOfLast { it != 0.toShort() }) {
                data.append(values[it].toString())
            }
        }
    }

    override fun writeShortArray2D(name: String, values: Array<ShortArray>, force: Boolean) {
        writeArray(name, values, force, "s[][]") { arr ->
            writeArray(arr.size, arr.indexOfLast { it != 0.toShort() }) {
                data.append(arr[it].toString())
            }
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
            writeArray(values.size, values.indexOfLast { it != 0 }) {
                data.append(values[it])
            }
        }
    }

    override fun writeIntArray2D(name: String, values: Array<IntArray>, force: Boolean) {
        writeArray(name, values, force, "i[][]") { arr ->
            writeArray(arr.size, arr.indexOfLast { it != 0 }) {
                data.append(arr[it])
            }
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
            writeArray(values.size, values.indexOfLast { it != 0f }) {
                append(values[it])
            }
        }
    }

    override fun writeFloatArray2D(name: String, values: Array<FloatArray>, force: Boolean) {
        writeArray(name, values, force, "f[][]") { arr ->
            writeArray(arr.size, arr.indexOfLast { it != 0f }) {
                append(arr[it])
            }
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
            writeArray(values.size, values.indexOfLast { it != 0.0 }) {
                append(values[it])
            }
        }
    }

    override fun writeDoubleArray2D(name: String, values: Array<DoubleArray>, force: Boolean) {
        writeArray(name, values, force, "d[][]") { arr ->
            writeArray(arr.size, arr.indexOfLast { it != 0.0 }) {
                append(arr[it])
            }
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
        writeArray(name, values, force, "S[][]") { arr ->
            writeArray(arr.size, arr.size - 1) {
                writeString(arr[it])
            }
        }
    }

    override fun writeFile(name: String, value: FileReference?, force: Boolean, workspace: FileReference?) {
        if (force || (value != null && value != InvalidRef)) {
            writeAttributeStart("R", name)
            if (value == null || value == InvalidRef) data.append("null")
            else writeString(value.toLocalPath(workspace))
        }
    }

    override fun writeFileArray(name: String, values: Array<FileReference>, force: Boolean, workspace: FileReference?) {
        writeArray(name, values, force, "R[]") {
            writeString(it.toLocalPath(workspace))
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
            writeArray(values.size, values.indexOfLast { it != 0L }) {
                data.append(values[it])
            }
        }
    }

    override fun writeLongArray2D(name: String, values: Array<LongArray>, force: Boolean) {
        writeArray(name, values, force, "l[][]") { arr ->
            writeArray(arr.size, arr.indexOfLast { it != 0L }) {
                data.append(arr[it])
            }
        }
    }

    private fun writeVector2f(value: Vector2fc) {
        writeVector2f(value.x(), value.y())
    }

    private fun writeVector2f(x: Float, y: Float) {
        data.append('[')
        append(x)
        if (x != y) {
            data.append(separator)
            append(y)
        }
        data.append(']')
    }

    private fun writeVector3f(value: Vector3fc) {
        writeVector3f(value.x(), value.y(), value.z())
    }

    private fun writeVector3f(x: Float, y: Float, z: Float) {
        data.append('[')
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
        writeVector4f(value.x(), value.y(), value.z(), value.w())
    }

    private fun writeVector4f(x: Float, y: Float, z: Float, w: Float) {
        data.append('[')
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
        writeVector2d(value.x(), value.y())
    }

    private fun writeVector2d(x: Double, y: Double) {
        data.append('[')
        append(x)
        if (x != y) {
            data.append(separator)
            append(y)
        }
        data.append(']')
    }

    private fun writeVector3d(value: Vector3dc) {
        writeVector3d(value.x(), value.y(), value.z())
    }

    private fun writeVector3d(x: Double, y: Double, z: Double) {
        data.append('[')
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
        writeVector4d(value.x(), value.y(), value.z(), value.w())
    }

    private fun writeVector4d(x: Double, y: Double, z: Double, w: Double) {
        data.append('[')
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
            writeAttributeStart("v2d", name)
            writeVector2d(value)
        }
    }

    override fun writeVector2dArray(name: String, values: Array<Vector2dc>, force: Boolean) {
        writeArray(name, values, force, "v2d[]") { writeVector2d(it) }
    }

    override fun writeVector3d(name: String, value: Vector3dc, force: Boolean) {
        if (force || value.x() != 0.0 || value.y() != 0.0 || value.z() != 0.0) {
            writeAttributeStart("v3d", name)
            writeVector3d(value)
        }
    }

    override fun writeVector3dArray(name: String, values: Array<Vector3dc>, force: Boolean) {
        writeArray(name, values, force, "v3d[]") { writeVector3d(it) }
    }

    override fun writeVector4d(name: String, value: Vector4dc, force: Boolean) {
        if (force || value.x() != 0.0 || value.y() != 0.0 || value.z() != 0.0 || value.w() != 0.0) {
            writeAttributeStart("vd4", name)
            writeVector4d(value)
        }
    }

    override fun writeVector4dArray(name: String, values: Array<Vector4dc>, force: Boolean) {
        writeArray(name, values, force, "v4d[]") { writeVector4d(it) }
    }

    private inline fun writeArray(
        size: Int,
        lastIndex: Int,
        writeValue: (Int) -> Unit
    ) {
        open(true)
        data.append(size)
        for (i in 0 until min(lastIndex + 1, size)) {
            data.append(separator)
            writeValue(i)
        }
        close(true)
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
        val tmp = tmp16
        value.get(tmp)
        data.append('[')
        clamp3x3Relative(tmp)
        for (i in 0 until 9 step 3) {
            if (i > 0) data.append(',')
            writeVector3f(tmp[i], tmp[i + 1], tmp[i + 2])
        }
        data.append(']')
    }

    override fun writeMatrix4x3f(name: String, value: Matrix4x3fc, force: Boolean) {
        writeAttributeStart("m4x3", name)
        val tmp = tmp16
        value.get(tmp) // col major
        clamp4x3Relative(tmp)
        data.append('[')
        for (i in 0 until 12 step 3) {
            if (i > 0) data.append(',')
            writeVector3f(tmp[i], tmp[i + 1], tmp[i + 2])
        }
        data.append(']')
    }

    private fun clamp3x3Relative(tmp: FloatArray, scale: Float = 1e-7f) {
        // clamp values, which are 1e-7 below the scale -> won't impact anything, and saves space
        val sx = absMax(tmp[0], tmp[3], tmp[6]) * scale
        val sy = absMax(tmp[1], tmp[4], tmp[7]) * scale
        val sz = absMax(tmp[2], tmp[5], tmp[8]) * scale
        for (i in 0 until 9 step 3) {
            if (abs(tmp[i + 0]) < sx) tmp[i + 0] = 0f
            if (abs(tmp[i + 1]) < sy) tmp[i + 1] = 0f
            if (abs(tmp[i + 2]) < sz) tmp[i + 2] = 0f
        }
    }

    private fun clamp3x3Relative(tmp: DoubleArray, scale: Double = 1e-7) {
        // clamp values, which are 1e-7 below the scale -> won't impact anything, and saves space
        val sx = absMax(tmp[0], tmp[3], tmp[6]) * scale
        val sy = absMax(tmp[1], tmp[4], tmp[7]) * scale
        val sz = absMax(tmp[2], tmp[5], tmp[8]) * scale
        for (i in 0 until 9 step 3) {
            if (abs(tmp[i + 0]) < sx) tmp[i + 0] = 0.0
            if (abs(tmp[i + 1]) < sy) tmp[i + 1] = 0.0
            if (abs(tmp[i + 2]) < sz) tmp[i + 2] = 0.0
        }
    }

    private fun clamp4x3Relative(tmp: FloatArray, scale: Float = 1e-7f) {
        // clamp values, which are 1e-7 below the scale -> won't impact anything, and saves space
        val sx = absMax(tmp[0], tmp[3], tmp[6], tmp[9]) * scale
        val sy = absMax(tmp[1], tmp[4], tmp[7], tmp[10]) * scale
        val sz = absMax(tmp[2], tmp[5], tmp[8], tmp[11]) * scale
        for (i in 0 until 12 step 3) {
            if (abs(tmp[i + 0]) < sx) tmp[i + 0] = 0f
            if (abs(tmp[i + 1]) < sy) tmp[i + 1] = 0f
            if (abs(tmp[i + 2]) < sz) tmp[i + 2] = 0f
        }
    }

    private fun clamp4x3Relative(tmp: DoubleArray, scale: Double = 1e-7) {
        // clamp values, which are 1e-7 below the scale -> won't impact anything, and saves space
        val sx = absMax(tmp[0], tmp[3], tmp[6], tmp[9]) * scale
        val sy = absMax(tmp[1], tmp[4], tmp[7], tmp[10]) * scale
        val sz = absMax(tmp[2], tmp[5], tmp[8], tmp[11]) * scale
        for (i in 0 until 12 step 3) {
            if (abs(tmp[i + 0]) < sx) tmp[i + 0] = 0.0
            if (abs(tmp[i + 1]) < sy) tmp[i + 1] = 0.0
            if (abs(tmp[i + 2]) < sz) tmp[i + 2] = 0.0
        }
    }

    private fun clamp4x4Relative(tmp: FloatArray, scale: Float = 1e-7f) {
        // clamp values, which are 1e-7 below the scale -> won't impact anything, and saves space
        val sx = absMax(tmp[0], tmp[4], tmp[8], tmp[12]) * scale
        val sy = absMax(tmp[1], tmp[5], tmp[9], tmp[13]) * scale
        val sz = absMax(tmp[2], tmp[6], tmp[10], tmp[14]) * scale
        val sw = absMax(tmp[3], tmp[7], tmp[11], tmp[15]) * scale
        for (i in 0 until 16 step 4) {
            if (abs(tmp[i + 0]) < sx) tmp[i + 0] = 0f
            if (abs(tmp[i + 1]) < sy) tmp[i + 1] = 0f
            if (abs(tmp[i + 2]) < sz) tmp[i + 2] = 0f
            if (abs(tmp[i + 3]) < sw) tmp[i + 3] = 0f
        }
    }

    private fun clamp4x4Relative(tmp: DoubleArray, scale: Double = 1e-7) {
        // clamp values, which are 1e-7 below the scale -> won't impact anything, and saves space
        val sx = absMax(tmp[0], tmp[4], tmp[8], tmp[12]) * scale
        val sy = absMax(tmp[1], tmp[5], tmp[9], tmp[13]) * scale
        val sz = absMax(tmp[2], tmp[6], tmp[10], tmp[14]) * scale
        val sw = absMax(tmp[3], tmp[7], tmp[11], tmp[15]) * scale
        for (i in 0 until 16 step 4) {
            if (abs(tmp[i + 0]) < sx) tmp[i + 0] = 0.0
            if (abs(tmp[i + 1]) < sy) tmp[i + 1] = 0.0
            if (abs(tmp[i + 2]) < sz) tmp[i + 2] = 0.0
            if (abs(tmp[i + 3]) < sw) tmp[i + 3] = 0.0
        }
    }

    override fun writeMatrix4x4f(name: String, value: Matrix4fc, force: Boolean) {
        writeAttributeStart("m4x4", name)
        val tmp = tmp16
        value.get(tmp) // col major
        clamp4x4Relative(tmp)
        data.append('[')
        for (i in 0 until 16 step 4) {
            if (i > 0) data.append(',')
            writeVector4f(tmp[i], tmp[i + 1], tmp[i + 2], tmp[i + 3])
        }
        data.append(']')
    }

    override fun writeMatrix3x3d(name: String, value: Matrix3dc, force: Boolean) {
        writeAttributeStart("m3x3d", name)
        val tmp = tmp16d
        value.get(tmp)
        data.append('[')
        clamp3x3Relative(tmp)
        for (i in 0 until 9 step 3) {
            if (i > 0) data.append(',')
            writeVector3d(tmp[i], tmp[i + 1], tmp[i + 2])
        }
        data.append(']')
    }

    override fun writeMatrix4x3d(name: String, value: Matrix4x3dc, force: Boolean) {
        writeAttributeStart("m4x3d", name)
        val tmp = tmp16d
        value.get(tmp) // col major
        clamp4x3Relative(tmp)
        data.append('[')
        for (i in 0 until 12 step 3) {
            if (i > 0) data.append(',')
            writeVector3d(tmp[i], tmp[i + 1], tmp[i + 2])
        }
        data.append(']')
    }

    override fun writeMatrix4x4d(name: String, value: Matrix4dc, force: Boolean) {
        writeAttributeStart("m4x4d", name)
        val tmp = tmp16d
        value.get(tmp) // col major
        clamp4x4Relative(tmp)
        data.append('[')
        for (i in 0 until 16 step 4) {
            if (i > 0) data.append(',')
            writeVector4d(tmp[i], tmp[i + 1], tmp[i + 2], tmp[i + 3])
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

    override fun <V : ISaveable?> writeNullableObjectArray(
        self: ISaveable?,
        name: String,
        values: Array<V>?,
        force: Boolean
    ) {
        if (force || values?.isNotEmpty() == true) {
            if (values == null || values.isEmpty()) {
                writeAttributeStart("*[]", name)
                data.append("[0]")
            } else {
                val firstType = values.first()?.className
                val allHaveSameType = values.all { it?.className == firstType }
                if (firstType != null && allHaveSameType) {
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

    private fun <V : ISaveable?> writeHeterogeneousArray(name: String, values: Array<V>) {
        writeAttributeStart("*[]", name)
        writeArray(values.size, values.size) {
            writeObject(null, null, values[it], true)
        }
    }

    override fun <V : ISaveable> writeObjectArray2D(
        self: ISaveable?,
        name: String,
        values: Array<Array<V>>,
        force: Boolean
    ) {
        writeAttributeStart("*[][]", name)
        writeArray(values.size, values.size) { i ->
            val arr = values[i]
            writeArray(arr.size, arr.size) {
                writeObject(null,null,arr[it],true)
            }
        }
    }

    override fun <V : ISaveable?> writeHomogenousObjectArray(
        self: ISaveable?,
        name: String,
        values: Array<V>,
        force: Boolean
    ) = writeNullableObjectArray(self, name, values, force)

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

        fun save(data: ISaveable, beautify: Boolean, file: FileReference) {
            file.writeText(toText(data, beautify))
        }

        fun save(data: List<ISaveable>, beautify: Boolean, file: FileReference) {
            file.writeText(toText(data, beautify))
        }

        fun toBuilder(data: ISaveable, beautify: Boolean): StringBuilder {
            val writer = TextWriter(beautify)
            writer.add(data)
            writer.writeAllInList()
            return writer.data
        }

    }


}