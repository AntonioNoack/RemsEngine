package me.anno.io.binary

import me.anno.io.Streams.writeBE16
import me.anno.io.Streams.writeBE32
import me.anno.io.Streams.writeBE32F
import me.anno.io.Streams.writeBE64
import me.anno.io.Streams.writeBE64F
import me.anno.io.base.BaseWriter
import me.anno.io.binary.BinaryTypes.OBJECTS_HOMOGENOUS_ARRAY
import me.anno.io.binary.BinaryTypes.OBJECT_ARRAY
import me.anno.io.binary.BinaryTypes.OBJECT_ARRAY_2D
import me.anno.io.binary.BinaryTypes.OBJECT_IMPL
import me.anno.io.binary.BinaryTypes.OBJECT_LIST_UNKNOWN_LENGTH
import me.anno.io.binary.BinaryTypes.OBJECT_NULL
import me.anno.io.binary.BinaryTypes.OBJECT_PTR
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.SimpleType
import me.anno.io.saveable.Saveable
import me.anno.utils.types.Booleans.toInt
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
import java.io.OutputStream

open class BinaryWriter(val output: OutputStream, workspace: FileReference) : BaseWriter(workspace, true) {

    /**
     * max number of strings? idk...
     * typically we need only a few, but what if we need many?
     * */
    private val knownStrings = HashMap<String, Int>()

    private val knownNameTypes = HashMap<String, HashMap<NameType, Int>>()

    private var currentClass = ""
    private var currentNameTypes = knownNameTypes.getOrPut(currentClass, ::HashMap)

    private inline fun usingType(type: String, callback: () -> Unit) {
        val old1 = currentClass
        val old2 = currentNameTypes
        currentClass = type
        currentNameTypes = knownNameTypes.getOrPut(type) { HashMap() }
        callback()
        currentClass = old1
        currentNameTypes = old2
    }

    fun appendEfficientString(string: String?) {
        if (string == null) {
            output.writeBE32(-1)
        } else {
            val known = knownStrings[string]
            if (known != null) {
                output.writeBE32(known)
            } else {
                val bytes = string.encodeToByteArray()
                output.writeBE32(-2 - bytes.size)
                output.write(bytes)
                knownStrings[string] = knownStrings.size
            }
        }
    }

    private fun writeTypeString(value: String) {
        appendEfficientString(value)
    }

    fun writeAttributeStart(name: String, type: Int) {
        val nameType = NameType(name, type)
        val id = currentNameTypes[nameType]
        if (id != null) {
            // known -> shortcut
            output.writeBE32(id)
        } else {
            // not previously known -> create a new one
            output.writeBE32(-1)
            val newId = currentNameTypes.size
            currentNameTypes[nameType] = newId
            writeTypeString(name)
            output.write(type)
        }
    }

    override fun writeBoolean(name: String, value: Boolean, force: Boolean) {
        if (force || value) {
            writeAttributeStart(name, SimpleType.BOOLEAN.scalarId)
            output.write(value.toInt())
        }
    }

    override fun writeBooleanArray(name: String, values: BooleanArray, force: Boolean) {
        writeGenericList(name, values.size, force, SimpleType.BOOLEAN.scalarId + 1) {
            output.write(if (values[it]) 1 else 0)
        }
    }

    override fun writeBooleanArray2D(name: String, values: List<BooleanArray>, force: Boolean) {
        writeGenericList2D(name, values, force, SimpleType.BOOLEAN.scalarId + 2, { it.size }) { vs, i ->
            output.write(if (vs[i]) 1 else 0)
        }
    }

    override fun writeChar(name: String, value: Char, force: Boolean) {
        if (force || value != 0.toChar()) {
            writeAttributeStart(name, SimpleType.CHAR.scalarId)
            output.writeBE16(value.code)
        }
    }

    override fun writeCharArray(name: String, values: CharArray, force: Boolean) {
        writeGenericList(name, values.size, force, SimpleType.CHAR.scalarId + 1) {
            output.writeBE16(values[it].code)
        }
    }

    override fun writeCharArray2D(name: String, values: List<CharArray>, force: Boolean) {
        writeGenericList2D(name, values, force, SimpleType.CHAR.scalarId + 2, { it.size }) { vs, i ->
            output.writeBE16(vs[i].code)
        }
    }

    override fun writeByte(name: String, value: Byte, force: Boolean) {
        if (force || value != 0.toByte()) {
            writeAttributeStart(name, SimpleType.BYTE.scalarId)
            output.write(value.toInt())
        }
    }

    override fun writeByteArray(name: String, values: ByteArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.BYTE.scalarId + 1)
            output.writeBE32(values.size)
            output.write(values)
        }
    }

    override fun writeByteArray2D(name: String, values: List<ByteArray>, force: Boolean) {
        writeGenericList(name, values, force, SimpleType.BYTE.scalarId + 2) { vs ->
            output.writeBE32(vs.size)
            output.write(vs)
        }
    }

    override fun writeShort(name: String, value: Short, force: Boolean) {
        if (force || value != 0.toShort()) {
            writeAttributeStart(name, SimpleType.SHORT.scalarId)
            output.writeBE16(value.toInt())
        }
    }

    override fun writeShortArray(name: String, values: ShortArray, force: Boolean) {
        writeGenericList(name, values.size, force, SimpleType.SHORT.scalarId + 1) {
            output.writeBE16(values[it].toInt())
        }
    }

    override fun writeShortArray2D(name: String, values: List<ShortArray>, force: Boolean) {
        writeGenericList2D(name, values, force, SimpleType.SHORT.scalarId + 2, { it.size }) { vs, i ->
            output.writeBE16(vs[i].toInt())
        }
    }

    override fun writeInt(name: String, value: Int, force: Boolean) {
        if (force || value != 0) {
            writeAttributeStart(name, SimpleType.INT.scalarId)
            output.writeBE32(value)
        }
    }

    override fun writeColor(name: String, value: Int, force: Boolean) {
        writeInt(name, value, force)
    }

    override fun writeIntArray(name: String, values: IntArray, force: Boolean) {
        writeGenericList(name, values.size, force, SimpleType.INT.scalarId + 1) {
            output.writeBE32(values[it])
        }
    }

    override fun writeColorArray(name: String, values: IntArray, force: Boolean) {
        writeIntArray(name, values, force)
    }

    override fun writeIntArray2D(name: String, values: List<IntArray>, force: Boolean) {
        writeGenericList2D(name, values, force, SimpleType.INT.scalarId + 2, { it.size }) { vs, i ->
            output.writeBE32(vs[i])
        }
    }

    override fun writeColorArray2D(name: String, values: List<IntArray>, force: Boolean) {
        writeIntArray2D(name, values, force)
    }

    override fun writeLong(name: String, value: Long, force: Boolean) {
        if (force || value != 0L) {
            writeAttributeStart(name, SimpleType.LONG.scalarId)
            output.writeBE64(value)
        }
    }

    override fun writeLongArray(name: String, values: LongArray, force: Boolean) {
        writeGenericList(name, values.size, force, SimpleType.LONG.scalarId + 1) {
            output.writeBE64(values[it])
        }
    }

    override fun writeLongArray2D(name: String, values: List<LongArray>, force: Boolean) {
        writeGenericList2D(name, values, force, SimpleType.LONG.scalarId + 2, { it.size }) { vs, i ->
            output.writeBE64(vs[i])
        }
    }

    override fun writeFloat(name: String, value: Float, force: Boolean) {
        if (force || value != 0f) {
            writeAttributeStart(name, SimpleType.FLOAT.scalarId)
            output.writeBE32F(value)
        }
    }

    override fun writeFloatArray(name: String, values: FloatArray, force: Boolean) {
        writeGenericList(name, values.size, force, SimpleType.FLOAT.scalarId + 1) {
            output.writeBE32F(values[it])
        }
    }

    override fun writeFloatArray2D(name: String, values: List<FloatArray>, force: Boolean) {
        writeGenericList2D(name, values, force, SimpleType.FLOAT.scalarId + 2, { it.size }) { vs, i ->
            output.writeBE32F(vs[i])
        }
    }

    override fun writeDouble(name: String, value: Double, force: Boolean) {
        if (force || value != 0.0) {
            writeAttributeStart(name, SimpleType.DOUBLE.scalarId)
            output.writeBE64F(value)
        }
    }

    override fun writeDoubleArray(name: String, values: DoubleArray, force: Boolean) {
        writeGenericList(name, values.size, force, SimpleType.DOUBLE.scalarId + 1) {
            output.writeBE64F(values[it])
        }
    }

    override fun writeDoubleArray2D(name: String, values: List<DoubleArray>, force: Boolean) {
        writeGenericList2D(name, values, force, SimpleType.DOUBLE.scalarId + 2, { it.size }) { vs, i ->
            output.writeBE64F(vs[i])
        }
    }

    override fun writeString(name: String, value: String, force: Boolean) {
        if (force || value != "") {
            writeAttributeStart(name, SimpleType.STRING.scalarId)
            appendEfficientString(value)
        }
    }

    override fun writeStringList(name: String, values: List<String>, force: Boolean) {
        writeGenericList(name, values, force, SimpleType.STRING.scalarId + 1, ::appendEfficientString)
    }

    override fun writeStringList2D(name: String, values: List<List<String>>, force: Boolean) {
        writeGenericList2D(name, values, force, SimpleType.STRING.scalarId + 2, ::appendEfficientString)
    }

    private fun appendFile(value: FileReference) {
        val mapped = resourceMap[value] ?: value
        appendEfficientString(mapped.toLocalPath(workspace))
    }

    override fun writeFile(name: String, value: FileReference, force: Boolean) {
        if (force || value != InvalidRef) {
            writeAttributeStart(name, SimpleType.REFERENCE.scalarId)
            appendFile(value)
        }
    }

    override fun writeFileList(name: String, values: List<FileReference>, force: Boolean) {
        writeGenericList(name, values, force, SimpleType.REFERENCE.scalarId + 1, ::appendFile)
    }

    override fun writeFileList2D(name: String, values: List<List<FileReference>>, force: Boolean) {
        writeGenericList2D(name, values, force, SimpleType.REFERENCE.scalarId + 2, ::appendFile)
    }

    private fun appendVector2f(value: Vector2f) {
        output.writeBE32F(value.x)
        output.writeBE32F(value.y)
    }

    private fun appendVector3f(value: Vector3f) {
        output.writeBE32F(value.x)
        output.writeBE32F(value.y)
        output.writeBE32F(value.z)
    }

    private fun appendVector4f(value: Vector4f) {
        output.writeBE32F(value.x)
        output.writeBE32F(value.y)
        output.writeBE32F(value.z)
        output.writeBE32F(value.w)
    }

    override fun writeVector2f(name: String, value: Vector2f, force: Boolean) {
        if (force || (value.x != 0f && value.y != 0f)) {
            writeAttributeStart(name, SimpleType.VECTOR2F.scalarId)
            appendVector2f(value)
        }
    }

    override fun writeVector2fList(name: String, values: List<Vector2f>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.VECTOR2F.scalarId + 1, ::appendVector2f)

    override fun writeVector2fList2D(name: String, values: List<List<Vector2f>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.VECTOR2F.scalarId + 2, ::appendVector2f)

    override fun writeVector3f(name: String, value: Vector3f, force: Boolean) {
        if (force || (value.x != 0f || value.y != 0f || value.z != 0f)) {
            writeAttributeStart(name, SimpleType.VECTOR3F.scalarId)
            appendVector3f(value)
        }
    }

    override fun writeVector3fList(name: String, values: List<Vector3f>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.VECTOR3F.scalarId + 1, ::appendVector3f)

    override fun writeVector3fList2D(name: String, values: List<List<Vector3f>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.VECTOR3F.scalarId + 2, ::appendVector3f)

    override fun writeVector4f(name: String, value: Vector4f, force: Boolean) {
        if (force || (value.x != 0f || value.y != 0f || value.z != 0f || value.w != 0f)) {
            writeAttributeStart(name, SimpleType.VECTOR4F.scalarId)
            appendVector4f(value)
        }
    }

    override fun writeVector4fList(name: String, values: List<Vector4f>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.VECTOR4F.scalarId + 1, ::appendVector4f)

    override fun writeVector4fList2D(name: String, values: List<List<Vector4f>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.VECTOR4F.scalarId + 2, ::appendVector4f)

    private fun appendVector2d(value: Vector2d) {
        output.writeBE64F(value.x)
        output.writeBE64F(value.y)
    }

    private fun appendVector3d(value: Vector3d) {
        output.writeBE64F(value.x)
        output.writeBE64F(value.y)
        output.writeBE64F(value.z)
    }

    private fun appendVector4d(value: Vector4d) {
        output.writeBE64F(value.x)
        output.writeBE64F(value.y)
        output.writeBE64F(value.z)
        output.writeBE64F(value.w)
    }

    override fun writeVector2d(name: String, value: Vector2d, force: Boolean) {
        if (force || (value.x != 0.0 || value.y != 0.0)) {
            writeAttributeStart(name, SimpleType.VECTOR2D.scalarId)
            appendVector2d(value)
        }
    }

    override fun writeVector2dList(name: String, values: List<Vector2d>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.VECTOR2D.scalarId + 1, ::appendVector2d)

    override fun writeVector2dList2D(name: String, values: List<List<Vector2d>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.VECTOR2D.scalarId + 2, ::appendVector2d)

    override fun writeVector3d(name: String, value: Vector3d, force: Boolean) {
        if (force || (value.x != 0.0 || value.y != 0.0 || value.z != 0.0)) {
            writeAttributeStart(name, SimpleType.VECTOR3D.scalarId)
            appendVector3d(value)
        }
    }

    override fun writeVector3dList(name: String, values: List<Vector3d>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.VECTOR3D.scalarId + 1, ::appendVector3d)

    override fun writeVector3dList2D(name: String, values: List<List<Vector3d>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.VECTOR3D.scalarId + 2, ::appendVector3d)

    override fun writeVector4d(name: String, value: Vector4d, force: Boolean) {
        if (force || (value.x != 0.0 || value.y != 0.0 || value.z != 0.0 || value.w != 0.0)) {
            writeAttributeStart(name, SimpleType.VECTOR4D.scalarId)
            appendVector4d(value)
        }
    }

    override fun writeVector4dList(name: String, values: List<Vector4d>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.VECTOR4D.scalarId + 1, ::appendVector4d)

    override fun writeVector4dList2D(name: String, values: List<List<Vector4d>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.VECTOR4D.scalarId + 2, ::appendVector4d)

    private fun appendVector2i(value: Vector2i) {
        output.writeBE32(value.x)
        output.writeBE32(value.y)
    }

    private fun appendVector3i(value: Vector3i) {
        output.writeBE32(value.x)
        output.writeBE32(value.y)
        output.writeBE32(value.z)
    }

    private fun appendVector4i(value: Vector4i) {
        output.writeBE32(value.x)
        output.writeBE32(value.y)
        output.writeBE32(value.z)
        output.writeBE32(value.w)
    }

    override fun writeVector2i(name: String, value: Vector2i, force: Boolean) {
        if (force || (value.x != 0 || value.y != 0)) {
            writeAttributeStart(name, SimpleType.VECTOR2I.scalarId)
            appendVector2i(value)
        }
    }

    override fun writeVector3i(name: String, value: Vector3i, force: Boolean) {
        if (force || (value.x != 0 || value.y != 0 || value.z != 0)) {
            writeAttributeStart(name, SimpleType.VECTOR3I.scalarId)
            appendVector3i(value)
        }
    }

    override fun writeVector4i(name: String, value: Vector4i, force: Boolean) {
        if (force || (value.x != 0 || value.y != 0 || value.z != 0 || value.w != 0)) {
            writeAttributeStart(name, SimpleType.VECTOR4I.scalarId)
            appendVector4i(value)
        }
    }

    override fun writeVector2iList(name: String, values: List<Vector2i>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.VECTOR2I.scalarId + 1, ::appendVector2i)

    override fun writeVector3iList(name: String, values: List<Vector3i>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.VECTOR3I.scalarId + 1, ::appendVector3i)

    override fun writeVector4iList(name: String, values: List<Vector4i>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.VECTOR4I.scalarId + 1, ::appendVector4i)

    override fun writeVector2iList2D(name: String, values: List<List<Vector2i>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.VECTOR2I.scalarId + 2, ::appendVector2i)

    override fun writeVector3iList2D(name: String, values: List<List<Vector3i>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.VECTOR3I.scalarId + 2, ::appendVector3i)

    override fun writeVector4iList2D(name: String, values: List<List<Vector4i>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.VECTOR4I.scalarId + 2, ::appendVector4i)

    private fun appendQuaternionf(value: Quaternionf) {
        output.writeBE32F(value.x)
        output.writeBE32F(value.y)
        output.writeBE32F(value.z)
        output.writeBE32F(value.w)
    }

    private fun appendQuaterniond(value: Quaterniond) {
        output.writeBE64F(value.x)
        output.writeBE64F(value.y)
        output.writeBE64F(value.z)
        output.writeBE64F(value.w)
    }

    override fun writeQuaternionf(name: String, value: Quaternionf, force: Boolean) {
        if (force || (value.x != 0f || value.y != 0f || value.z != 0f || value.w != 1f)) {
            writeAttributeStart(name, SimpleType.QUATERNIONF.scalarId)
            appendQuaternionf(value)
        }
    }

    override fun writeQuaterniond(name: String, value: Quaterniond, force: Boolean) {
        if (force || (value.x != 0.0 || value.y != 0.0 || value.z != 0.0 || value.w != 1.0)) {
            writeAttributeStart(name, SimpleType.QUATERNIOND.scalarId)
            appendQuaterniond(value)
        }
    }

    override fun writeQuaternionfList(name: String, values: List<Quaternionf>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.QUATERNIONF.scalarId + 1) { appendQuaternionf(it) }

    override fun writeQuaternionfList2D(name: String, values: List<List<Quaternionf>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.QUATERNIONF.scalarId + 2, ::appendQuaternionf)

    override fun writeQuaterniondList(name: String, values: List<Quaterniond>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.QUATERNIOND.scalarId + 1, ::appendQuaterniond)

    override fun writeQuaterniondList2D(name: String, values: List<List<Quaterniond>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.QUATERNIOND.scalarId + 2, ::appendQuaterniond)

    private fun appendMatrix(value: Matrix2f) {
        output.writeBE32F(value.m00)
        output.writeBE32F(value.m01)
        output.writeBE32F(value.m10)
        output.writeBE32F(value.m11)
    }

    private fun appendMatrix(value: Matrix3x2f) {
        output.writeBE32F(value.m00)
        output.writeBE32F(value.m01)
        output.writeBE32F(value.m10)
        output.writeBE32F(value.m11)
        output.writeBE32F(value.m20)
        output.writeBE32F(value.m21)
    }

    private fun appendMatrix(value: Matrix3f) {
        output.writeBE32F(value.m00)
        output.writeBE32F(value.m01)
        output.writeBE32F(value.m02)
        output.writeBE32F(value.m10)
        output.writeBE32F(value.m11)
        output.writeBE32F(value.m12)
        output.writeBE32F(value.m20)
        output.writeBE32F(value.m21)
        output.writeBE32F(value.m22)
    }

    private fun appendMatrix(value: Matrix4x3f) {
        output.writeBE32F(value.m00)
        output.writeBE32F(value.m01)
        output.writeBE32F(value.m02)
        output.writeBE32F(value.m10)
        output.writeBE32F(value.m11)
        output.writeBE32F(value.m12)
        output.writeBE32F(value.m20)
        output.writeBE32F(value.m21)
        output.writeBE32F(value.m22)
        output.writeBE32F(value.m30)
        output.writeBE32F(value.m31)
        output.writeBE32F(value.m32)
    }

    private fun appendMatrix(value: Matrix4f) {
        output.writeBE32F(value.m00)
        output.writeBE32F(value.m01)
        output.writeBE32F(value.m02)
        output.writeBE32F(value.m03)
        output.writeBE32F(value.m10)
        output.writeBE32F(value.m11)
        output.writeBE32F(value.m12)
        output.writeBE32F(value.m13)
        output.writeBE32F(value.m20)
        output.writeBE32F(value.m21)
        output.writeBE32F(value.m22)
        output.writeBE32F(value.m23)
        output.writeBE32F(value.m30)
        output.writeBE32F(value.m31)
        output.writeBE32F(value.m32)
        output.writeBE32F(value.m33)
    }

    override fun writeMatrix2x2f(name: String, value: Matrix2f, force: Boolean) {
        writeAttributeStart(name, SimpleType.MATRIX2X2F.scalarId)
        appendMatrix(value)
    }

    override fun writeMatrix3x2f(name: String, value: Matrix3x2f, force: Boolean) {
        writeAttributeStart(name, SimpleType.MATRIX3X2F.scalarId)
        appendMatrix(value)
    }

    override fun writeMatrix3x3f(name: String, value: Matrix3f, force: Boolean) {
        writeAttributeStart(name, SimpleType.MATRIX3X3F.scalarId)
        appendMatrix(value)
    }

    override fun writeMatrix4x3f(name: String, value: Matrix4x3f, force: Boolean) {
        writeAttributeStart(name, SimpleType.MATRIX4X3F.scalarId)
        appendMatrix(value)
    }

    override fun writeMatrix4x4f(name: String, value: Matrix4f, force: Boolean) {
        writeAttributeStart(name, SimpleType.MATRIX4X4F.scalarId)
        appendMatrix(value)
    }

    override fun writeMatrix2x2fList(name: String, values: List<Matrix2f>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.MATRIX2X2F.scalarId + 1) { appendMatrix(it) }

    override fun writeMatrix3x2fList(name: String, values: List<Matrix3x2f>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.MATRIX3X2F.scalarId + 1) { appendMatrix(it) }

    override fun writeMatrix3x3fList(name: String, values: List<Matrix3f>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.MATRIX3X3F.scalarId + 1) { appendMatrix(it) }

    override fun writeMatrix4x3fList(name: String, values: List<Matrix4x3f>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.MATRIX4X3F.scalarId + 1) { appendMatrix(it) }

    override fun writeMatrix4x4fList(name: String, values: List<Matrix4f>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.MATRIX4X4F.scalarId + 1) { appendMatrix(it) }

    override fun writeMatrix2x2fList2D(name: String, values: List<List<Matrix2f>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.MATRIX2X2F.scalarId + 2) { appendMatrix(it) }

    override fun writeMatrix3x2fList2D(name: String, values: List<List<Matrix3x2f>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.MATRIX3X2F.scalarId + 2) { appendMatrix(it) }

    override fun writeMatrix3x3fList2D(name: String, values: List<List<Matrix3f>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.MATRIX3X3F.scalarId + 2) { appendMatrix(it) }

    override fun writeMatrix4x3fList2D(name: String, values: List<List<Matrix4x3f>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.MATRIX4X3F.scalarId + 2) { appendMatrix(it) }

    override fun writeMatrix4x4fList2D(name: String, values: List<List<Matrix4f>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.MATRIX4X4F.scalarId + 2) { appendMatrix(it) }

    private fun appendMatrix(value: Matrix2d) {
        output.writeBE64F(value.m00)
        output.writeBE64F(value.m01)
        output.writeBE64F(value.m10)
        output.writeBE64F(value.m11)
    }

    private fun appendMatrix(value: Matrix3x2d) {
        output.writeBE64F(value.m00)
        output.writeBE64F(value.m01)
        output.writeBE64F(value.m10)
        output.writeBE64F(value.m11)
        output.writeBE64F(value.m20)
        output.writeBE64F(value.m21)
    }

    private fun appendMatrix(value: Matrix3d) {
        output.writeBE64F(value.m00)
        output.writeBE64F(value.m01)
        output.writeBE64F(value.m02)
        output.writeBE64F(value.m10)
        output.writeBE64F(value.m11)
        output.writeBE64F(value.m12)
        output.writeBE64F(value.m20)
        output.writeBE64F(value.m21)
        output.writeBE64F(value.m22)
    }

    private fun appendMatrix(value: Matrix4x3d) {
        output.writeBE64F(value.m00)
        output.writeBE64F(value.m01)
        output.writeBE64F(value.m02)
        output.writeBE64F(value.m10)
        output.writeBE64F(value.m11)
        output.writeBE64F(value.m12)
        output.writeBE64F(value.m20)
        output.writeBE64F(value.m21)
        output.writeBE64F(value.m22)
        output.writeBE64F(value.m30)
        output.writeBE64F(value.m31)
        output.writeBE64F(value.m32)
    }

    private fun appendMatrix(value: Matrix4d) {
        output.writeBE64F(value.m00)
        output.writeBE64F(value.m01)
        output.writeBE64F(value.m02)
        output.writeBE64F(value.m03)
        output.writeBE64F(value.m10)
        output.writeBE64F(value.m11)
        output.writeBE64F(value.m12)
        output.writeBE64F(value.m13)
        output.writeBE64F(value.m20)
        output.writeBE64F(value.m21)
        output.writeBE64F(value.m22)
        output.writeBE64F(value.m23)
        output.writeBE64F(value.m30)
        output.writeBE64F(value.m31)
        output.writeBE64F(value.m32)
        output.writeBE64F(value.m33)
    }

    override fun writeMatrix2x2d(name: String, value: Matrix2d, force: Boolean) {
        writeAttributeStart(name, SimpleType.MATRIX2X2D.scalarId)
        appendMatrix(value)
    }

    override fun writeMatrix3x2d(name: String, value: Matrix3x2d, force: Boolean) {
        writeAttributeStart(name, SimpleType.MATRIX3X2D.scalarId)
        appendMatrix(value)
    }

    override fun writeMatrix3x3d(name: String, value: Matrix3d, force: Boolean) {
        writeAttributeStart(name, SimpleType.MATRIX3X3D.scalarId)
        appendMatrix(value)
    }

    override fun writeMatrix4x3d(name: String, value: Matrix4x3d, force: Boolean) {
        writeAttributeStart(name, SimpleType.MATRIX4X3D.scalarId)
        appendMatrix(value)
    }

    override fun writeMatrix4x4d(name: String, value: Matrix4d, force: Boolean) {
        writeAttributeStart(name, SimpleType.MATRIX4X4D.scalarId)
        appendMatrix(value)
    }

    override fun writeMatrix2x2dList(name: String, values: List<Matrix2d>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.MATRIX2X2D.scalarId + 1) { appendMatrix(it) }

    override fun writeMatrix3x2dList(name: String, values: List<Matrix3x2d>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.MATRIX3X2D.scalarId + 1) { appendMatrix(it) }

    override fun writeMatrix3x3dList(name: String, values: List<Matrix3d>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.MATRIX3X3D.scalarId + 1) { appendMatrix(it) }

    override fun writeMatrix4x3dList(name: String, values: List<Matrix4x3d>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.MATRIX4X3D.scalarId + 1) { appendMatrix(it) }

    override fun writeMatrix4x4dList(name: String, values: List<Matrix4d>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.MATRIX4X4D.scalarId + 1) { appendMatrix(it) }

    override fun writeMatrix2x2dList2D(name: String, values: List<List<Matrix2d>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.MATRIX2X2D.scalarId + 2) { appendMatrix(it) }

    override fun writeMatrix3x2dList2D(name: String, values: List<List<Matrix3x2d>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.MATRIX3X2D.scalarId + 2) { appendMatrix(it) }

    override fun writeMatrix3x3dList2D(name: String, values: List<List<Matrix3d>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.MATRIX3X3D.scalarId + 2) { appendMatrix(it) }

    override fun writeMatrix4x3dList2D(name: String, values: List<List<Matrix4x3d>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.MATRIX4X3D.scalarId + 2) { appendMatrix(it) }

    override fun writeMatrix4x4dList2D(name: String, values: List<List<Matrix4d>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.MATRIX4X4D.scalarId + 2) { appendMatrix(it) }

    private fun writeAABBf(value: AABBf) {
        output.writeBE32F(value.minX)
        output.writeBE32F(value.minY)
        output.writeBE32F(value.minZ)
        output.writeBE32F(value.maxX)
        output.writeBE32F(value.maxY)
        output.writeBE32F(value.maxZ)
    }

    private fun writeAABBd(value: AABBd) {
        output.writeBE64F(value.minX)
        output.writeBE64F(value.minY)
        output.writeBE64F(value.minZ)
        output.writeBE64F(value.maxX)
        output.writeBE64F(value.maxY)
        output.writeBE64F(value.maxZ)
    }

    override fun writeAABBf(name: String, value: AABBf, force: Boolean) {
        writeAttributeStart(name, SimpleType.AABBF.scalarId)
        writeAABBf(value)
    }

    override fun writeAABBd(name: String, value: AABBd, force: Boolean) {
        writeAttributeStart(name, SimpleType.AABBD.scalarId)
        writeAABBd(value)
    }

    override fun writeAABBfList(name: String, values: List<AABBf>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.AABBF.scalarId + 1, ::writeAABBf)

    override fun writeAABBdList(name: String, values: List<AABBd>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.AABBD.scalarId + 1, ::writeAABBd)

    override fun writeAABBfList2D(name: String, values: List<List<AABBf>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.AABBF.scalarId + 2, ::writeAABBf)

    override fun writeAABBdList2D(name: String, values: List<List<AABBd>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.AABBD.scalarId + 2, ::writeAABBd)

    private fun appendPlanef(value: Planef) {
        output.writeBE32F(value.dirX)
        output.writeBE32F(value.dirY)
        output.writeBE32F(value.dirZ)
        output.writeBE32F(value.distance)
    }

    private fun appendPlaned(value: Planed) {
        output.writeBE64F(value.dirX)
        output.writeBE64F(value.dirY)
        output.writeBE64F(value.dirZ)
        output.writeBE64F(value.distance)
    }

    override fun writePlanef(name: String, value: Planef, force: Boolean) {
        writeAttributeStart(name, SimpleType.PLANEF.scalarId)
        appendPlanef(value)
    }

    override fun writePlaned(name: String, value: Planed, force: Boolean) {
        writeAttributeStart(name, SimpleType.PLANED.scalarId)
        appendPlaned(value)
    }

    override fun writePlanefList(name: String, values: List<Planef>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.PLANEF.scalarId + 1, ::appendPlanef)

    override fun writePlanedList(name: String, values: List<Planed>, force: Boolean) =
        writeGenericList(name, values, force, SimpleType.PLANED.scalarId + 1, ::appendPlaned)

    override fun writePlanefList2D(name: String, values: List<List<Planef>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.PLANEF.scalarId + 2, ::appendPlanef)

    override fun writePlanedList2D(name: String, values: List<List<Planed>>, force: Boolean) =
        writeGenericList2D(name, values, force, SimpleType.PLANED.scalarId + 2, ::appendPlaned)

    override fun writeNull(name: String?) {
        if (name != null) writeAttributeStart(name, OBJECT_NULL)
        else output.write(OBJECT_NULL)
    }

    override fun writePointer(name: String?, className: String, ptr: Int, value: Saveable) {
        if (name != null) writeAttributeStart(name, OBJECT_PTR)
        else output.write(OBJECT_PTR)
        output.writeBE32(ptr)
    }

    private fun writeObjectEnd() {
        output.writeBE32(-2)
    }

    override fun writeObjectImpl(name: String?, value: Saveable) {
        if (name != null) writeAttributeStart(name, OBJECT_IMPL)
        else output.write(OBJECT_IMPL)
        usingType(value.className) {
            writeTypeString(currentClass)
            output.writeBE32(getPointer(value)!!)
            value.save(this)
            writeObjectEnd()
        }
    }

    inline fun writeGenericList(
        name: String, size: Int, force: Boolean,
        type: Int, writeInstance: (Int) -> Unit
    ) {
        if (force || size > 0) {
            writeAttributeStart(name, type)
            output.writeBE32(size)
            for (index in 0 until size) {
                writeInstance(index)
            }
        }
    }

    inline fun <V> writeGenericList(
        name: String, values: List<V>, force: Boolean,
        type: Int, writeInstance: (V) -> Unit
    ) {
        writeGenericList(name, values.size, force, type) {
            writeInstance(values[it])
        }
    }

    inline fun <V> writeGenericList2D(
        name: String, values: List<V>, force: Boolean,
        type: Int, getSize: (V) -> Int, writeInstance: (V, Int) -> Unit
    ) {
        writeGenericList(name, values, force, type) { vs ->
            val size = getSize(vs)
            output.writeBE32(size)
            for (j in 0 until size) {
                writeInstance(vs, j)
            }
        }
    }

    inline fun <V> writeGenericList2D(
        name: String, values: List<List<V>>, force: Boolean,
        type: Int, writeInstance: (V) -> Unit
    ) {
        writeGenericList2D(name, values, force, type, { it.size }, { list, idx ->
            writeInstance(list[idx])
        })
    }

    override fun <V : Saveable> writeObjectList(self: Saveable?, name: String, values: List<V>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            if (values.isNotEmpty()) {
                val firstType = values.first().className
                val allSameType = values.all { it.className == firstType }
                if (allSameType) {
                    writeHomogenousObjectList(self, name, values, force)
                } else {
                    writeGenericList(name, values, force, OBJECT_ARRAY) {
                        writeObject(null, null, it, true)
                    }
                }
            } else {
                writeAttributeStart(name, OBJECT_ARRAY)
                output.writeBE32(0)
            }
        }
    }

    override fun <V : Saveable?> writeNullableObjectList(
        self: Saveable?, name: String,
        values: List<V>, force: Boolean
    ) {
        if (force || values.isNotEmpty()) {
            if (values.isNotEmpty()) {
                val firstType = values.first()?.className
                val allSameType = values.all { it?.className == firstType }
                if (firstType != null && allSameType) {
                    writeHomogenousObjectList(self, name, values, force)
                } else {
                    writeGenericList(name, values, force, OBJECT_ARRAY) {
                        writeObject(null, null, it, true)
                    }
                }
            } else {
                writeAttributeStart(name, OBJECT_ARRAY)
                output.writeBE32(0)
            }
        }
    }

    override fun <V : Saveable> writeObjectList2D(
        self: Saveable?,
        name: String,
        values: List<List<V>>,
        force: Boolean
    ) {
        writeGenericList(name, values, force, OBJECT_ARRAY_2D) { list ->
            output.writeBE32(list.size)
            for (i in list.indices) {
                writeObject(null, null, list[i], true)
            }
        }
    }

    override fun <V : Saveable?> writeHomogenousObjectList(
        self: Saveable?, name: String,
        values: List<V>, force: Boolean
    ) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, OBJECTS_HOMOGENOUS_ARRAY)
            val type = values.firstOrNull()?.className ?: ""
            usingType(type) {
                writeTypeString(type)
                output.writeBE32(values.size)
                for (i in values.indices) {
                    values[i]!!.save(this)
                    writeObjectEnd()
                }
            }
        }
    }

    override fun writeListStart() {
        writeAttributeStart("", OBJECT_LIST_UNKNOWN_LENGTH)
    }

    override fun writeListEnd() {
        output.write(LIST_END)
    }

    override fun writeListSeparator() {
        output.write(LIST_SEPARATOR)
    }

    override fun flush() {
        output.flush()
    }

    override fun close() {
        output.close()
    }

    companion object {
        const val LIST_SEPARATOR = 17
        const val LIST_END = 37
    }
}