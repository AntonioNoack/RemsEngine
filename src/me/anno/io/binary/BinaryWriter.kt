package me.anno.io.binary

import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.binary.BinaryTypes.AABB32
import me.anno.io.binary.BinaryTypes.AABB64
import me.anno.io.binary.BinaryTypes.BOOL
import me.anno.io.binary.BinaryTypes.BOOL_ARRAY
import me.anno.io.binary.BinaryTypes.BOOL_ARRAY_2D
import me.anno.io.binary.BinaryTypes.BYTE
import me.anno.io.binary.BinaryTypes.BYTE_ARRAY
import me.anno.io.binary.BinaryTypes.BYTE_ARRAY_2D
import me.anno.io.binary.BinaryTypes.CHAR
import me.anno.io.binary.BinaryTypes.CHAR_ARRAY
import me.anno.io.binary.BinaryTypes.CHAR_ARRAY_2D
import me.anno.io.binary.BinaryTypes.DOUBLE
import me.anno.io.binary.BinaryTypes.DOUBLE_ARRAY
import me.anno.io.binary.BinaryTypes.DOUBLE_ARRAY_2D
import me.anno.io.binary.BinaryTypes.FILE
import me.anno.io.binary.BinaryTypes.FILE_ARRAY
import me.anno.io.binary.BinaryTypes.FLOAT
import me.anno.io.binary.BinaryTypes.FLOAT_ARRAY
import me.anno.io.binary.BinaryTypes.FLOAT_ARRAY_2D
import me.anno.io.binary.BinaryTypes.INT
import me.anno.io.binary.BinaryTypes.INT_ARRAY
import me.anno.io.binary.BinaryTypes.INT_ARRAY_2D
import me.anno.io.binary.BinaryTypes.LONG
import me.anno.io.binary.BinaryTypes.LONG_ARRAY
import me.anno.io.binary.BinaryTypes.LONG_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX2X2D
import me.anno.io.binary.BinaryTypes.MATRIX2X2D_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX2X2D_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX2X2F
import me.anno.io.binary.BinaryTypes.MATRIX2X2F_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX2X2F_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX3X2D
import me.anno.io.binary.BinaryTypes.MATRIX3X2D_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX3X2D_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX3X2F
import me.anno.io.binary.BinaryTypes.MATRIX3X2F_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX3X2F_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX3X3D
import me.anno.io.binary.BinaryTypes.MATRIX3X3D_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX3X3D_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX3X3F
import me.anno.io.binary.BinaryTypes.MATRIX3X3F_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX3X3F_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX4X3D
import me.anno.io.binary.BinaryTypes.MATRIX4X3D_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX4X3D_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX4X3F
import me.anno.io.binary.BinaryTypes.MATRIX4X3F_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX4X3F_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX4X4D
import me.anno.io.binary.BinaryTypes.MATRIX4X4D_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX4X4D_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX4X4F
import me.anno.io.binary.BinaryTypes.MATRIX4X4F_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX4X4F_ARRAY_2D
import me.anno.io.binary.BinaryTypes.OBJECTS_HOMOGENOUS_ARRAY
import me.anno.io.binary.BinaryTypes.OBJECT_ARRAY
import me.anno.io.binary.BinaryTypes.OBJECT_ARRAY_2D
import me.anno.io.binary.BinaryTypes.OBJECT_IMPL
import me.anno.io.binary.BinaryTypes.OBJECT_LIST_UNKNOWN_LENGTH
import me.anno.io.binary.BinaryTypes.OBJECT_NULL
import me.anno.io.binary.BinaryTypes.OBJECT_PTR
import me.anno.io.binary.BinaryTypes.PLANE32
import me.anno.io.binary.BinaryTypes.PLANE64
import me.anno.io.binary.BinaryTypes.QUATERNION32
import me.anno.io.binary.BinaryTypes.QUATERNION32_ARRAY
import me.anno.io.binary.BinaryTypes.QUATERNION32_ARRAY_2D
import me.anno.io.binary.BinaryTypes.QUATERNION64
import me.anno.io.binary.BinaryTypes.QUATERNION64_ARRAY
import me.anno.io.binary.BinaryTypes.QUATERNION64_ARRAY_2D
import me.anno.io.binary.BinaryTypes.SHORT
import me.anno.io.binary.BinaryTypes.SHORT_ARRAY
import me.anno.io.binary.BinaryTypes.SHORT_ARRAY_2D
import me.anno.io.binary.BinaryTypes.STRING
import me.anno.io.binary.BinaryTypes.STRING_ARRAY
import me.anno.io.binary.BinaryTypes.STRING_ARRAY_2D
import me.anno.io.binary.BinaryTypes.VECTOR2D
import me.anno.io.binary.BinaryTypes.VECTOR2D_ARRAY
import me.anno.io.binary.BinaryTypes.VECTOR2F
import me.anno.io.binary.BinaryTypes.VECTOR2F_ARRAY
import me.anno.io.binary.BinaryTypes.VECTOR2I
import me.anno.io.binary.BinaryTypes.VECTOR2I_ARRAY
import me.anno.io.binary.BinaryTypes.VECTOR3D
import me.anno.io.binary.BinaryTypes.VECTOR3D_ARRAY
import me.anno.io.binary.BinaryTypes.VECTOR3F
import me.anno.io.binary.BinaryTypes.VECTOR3F_ARRAY
import me.anno.io.binary.BinaryTypes.VECTOR3I
import me.anno.io.binary.BinaryTypes.VECTOR3I_ARRAY
import me.anno.io.binary.BinaryTypes.VECTOR4D
import me.anno.io.binary.BinaryTypes.VECTOR4D_ARRAY
import me.anno.io.binary.BinaryTypes.VECTOR4F
import me.anno.io.binary.BinaryTypes.VECTOR4F_ARRAY
import me.anno.io.binary.BinaryTypes.VECTOR4I
import me.anno.io.binary.BinaryTypes.VECTOR4I_ARRAY
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.types.Booleans.toInt
import org.joml.*
import java.io.DataOutputStream

class BinaryWriter(val output: DataOutputStream) : BaseWriter(true) {

    /**
     * max number of strings? idk...
     * typically we need only a few, but what if we need many?
     * */
    private val knownStrings = HashMap<String, Int>()

    private val knownNameTypes = HashMap<String, HashMap<NameType, Int>>()

    private var currentClass = ""
    private var currentNameTypes = knownNameTypes.getOrPut(currentClass) { HashMap() }

    private fun usingType(type: String, run: () -> Unit) {
        val old1 = currentClass
        val old2 = currentNameTypes
        currentClass = type
        currentNameTypes = knownNameTypes.getOrPut(type) { HashMap() }
        run()
        currentClass = old1
        currentNameTypes = old2
    }

    private fun writeEfficientString(string: String?) {
        if (string == null) {
            output.writeInt(-1)
        } else {
            val known = knownStrings[string] ?: -1
            if (known >= 0) {
                output.writeInt(known)
            } else {
                val bytes = string.toByteArray()
                output.writeInt(-2 - bytes.size)
                output.write(bytes)
                knownStrings[string] = knownStrings.size
            }
        }
    }

    private fun writeTypeString(value: String) {
        writeEfficientString(value)
    }

    private fun writeAttributeStart(name: String, type: Int) {
        val nameType = NameType(name, type.toChar())
        val id = currentNameTypes[nameType] ?: -1
        if (id >= 0) {
            // known -> short cut
            output.writeInt(id)
        } else {
            // not previously known -> create a new one
            output.writeInt(-1)
            val newId = currentNameTypes.size
            currentNameTypes[nameType] = newId
            writeTypeString(name)
            output.writeByte(type)
        }
    }

    override fun writeBoolean(name: String, value: Boolean, force: Boolean) {
        if (force || value) {
            writeAttributeStart(name, BOOL)
            output.writeByte(value.toInt())
        }
    }

    override fun writeBooleanArray(name: String, values: BooleanArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, BOOL_ARRAY)
            output.writeInt(values.size)
            for (v in values) output.write(if (v) 1 else 0)
        }
    }

    override fun writeBooleanArray2D(name: String, values: Array<BooleanArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, BOOL_ARRAY_2D)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) output.write(if (v) 1 else 0)
            }
        }
    }

    override fun writeChar(name: String, value: Char, force: Boolean) {
        if (force || value != 0.toChar()) {
            writeAttributeStart(name, CHAR)
            output.writeChar(value.code)
        }
    }

    override fun writeCharArray(name: String, values: CharArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, CHAR_ARRAY)
            output.writeInt(values.size)
            for (c in values) output.writeChar(c.code)
        }
    }

    override fun writeCharArray2D(name: String, values: Array<CharArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, CHAR_ARRAY_2D)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) output.writeChar(v.code)
            }
        }
    }


    override fun writeByte(name: String, value: Byte, force: Boolean) {
        if (force || value != 0.toByte()) {
            writeAttributeStart(name, BYTE)
            output.writeByte(value.toInt())
        }
    }

    override fun writeByteArray(name: String, values: ByteArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, BYTE_ARRAY)
            output.writeInt(values.size)
            output.write(values)
        }
    }

    override fun writeByteArray2D(name: String, values: Array<ByteArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, BYTE_ARRAY_2D)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                output.write(vs)
            }
        }
    }

    override fun writeShort(name: String, value: Short, force: Boolean) {
        if (force || value != 0.toShort()) {
            writeAttributeStart(name, SHORT)
            output.writeShort(value.toInt())
        }
    }

    override fun writeShortArray(name: String, values: ShortArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SHORT_ARRAY)
            output.writeInt(values.size)
            for (v in values) output.writeShort(v.toInt())
        }
    }

    override fun writeShortArray2D(name: String, values: Array<ShortArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SHORT_ARRAY_2D)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) output.writeShort(v.toInt())
            }
        }
    }

    override fun writeInt(name: String, value: Int, force: Boolean) {
        if (force || value != 0) {
            writeAttributeStart(name, INT)
            output.writeInt(value)
        }
    }

    override fun writeColor(name: String, value: Int, force: Boolean) {
        writeInt(name, value, force)
    }

    override fun writeIntArray(name: String, values: IntArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, INT_ARRAY)
            output.writeInt(values.size)
            for (v in values) output.writeInt(v)
        }
    }

    override fun writeColorArray(name: String, values: IntArray, force: Boolean) {
        writeIntArray(name, values, force)
    }

    override fun writeIntArray2D(name: String, values: Array<IntArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, INT_ARRAY_2D)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) output.writeInt(v)
            }
        }
    }

    override fun writeColorArray2D(name: String, values: Array<IntArray>, force: Boolean) {
        writeIntArray2D(name, values, force)
    }

    override fun writeLong(name: String, value: Long, force: Boolean) {
        if (force || value != 0L) {
            writeAttributeStart(name, LONG)
            output.writeLong(value)
        }
    }

    override fun writeLongArray(name: String, values: LongArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, LONG_ARRAY)
            output.writeInt(values.size)
            for (v in values) output.writeLong(v)
        }
    }

    override fun writeLongArray2D(name: String, values: Array<LongArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, LONG_ARRAY_2D)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) output.writeLong(v)
            }
        }
    }

    override fun writeFloat(name: String, value: Float, force: Boolean) {
        if (force || value != 0f) {
            writeAttributeStart(name, FLOAT)
            output.writeFloat(value)
        }
    }

    override fun writeFloatArray(name: String, values: FloatArray?, force: Boolean) {
        if (values == null) {
            writeNull(name)
        } else if (force || values.isNotEmpty()) {
            writeAttributeStart(name, FLOAT_ARRAY)
            output.writeInt(values.size)
            for (v in values) output.writeFloat(v)
        }
    }

    override fun writeFloatArray2D(name: String, values: Array<FloatArray>?, force: Boolean) {
        if (values == null) {
            writeNull(name)
        } else if (force || values.isNotEmpty()) {
            writeAttributeStart(name, FLOAT_ARRAY_2D)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) output.writeFloat(v)
            }
        }
    }

    override fun writeDouble(name: String, value: Double, force: Boolean) {
        if (force || value != 0.0) {
            writeAttributeStart(name, DOUBLE)
            output.writeDouble(value)
        }
    }

    override fun writeDoubleArray(name: String, values: DoubleArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, DOUBLE_ARRAY)
            output.writeInt(values.size)
            for (v in values) output.writeDouble(v)
        }
    }

    override fun writeDoubleArray2D(name: String, values: Array<DoubleArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, DOUBLE_ARRAY_2D)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) output.writeDouble(v)
            }
        }
    }

    override fun writeString(name: String, value: String?, force: Boolean) {
        if (force || value != null) {
            writeAttributeStart(name, STRING)
            writeEfficientString(value)
        }
    }

    override fun writeStringArray(name: String, values: Array<String>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, STRING_ARRAY)
            output.writeInt(values.size)
            for (v in values) writeEfficientString(v)
        }
    }

    override fun writeStringArray2D(name: String, values: Array<Array<String>>, force: Boolean) {
        writeGenericArray2D(name, STRING_ARRAY_2D, values, force) {
            writeEfficientString(it)
        }
    }

    override fun writeFile(name: String, value: FileReference?, force: Boolean, workspace: FileReference?) {
        if (force || (value != null && value != InvalidRef)) {
            writeAttributeStart(name, FILE)
            writeEfficientString(value?.toLocalPath(workspace))
        }
    }

    override fun writeFileArray(name: String, values: Array<FileReference>, force: Boolean, workspace: FileReference?) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, FILE_ARRAY)
            output.writeInt(values.size)
            for (v in values) writeEfficientString(v.toLocalPath(workspace))
        }
    }

    override fun writeVector2f(name: String, value: Vector2f, force: Boolean) {
        if (force || (value.x != 0f && value.y != 0f)) {
            writeAttributeStart(name, VECTOR2F)
            output.writeFloat(value.x)
            output.writeFloat(value.y)
        }
    }

    override fun writeVector2fArray(name: String, values: Array<Vector2f>, force: Boolean) {
        writeGenericArray(name, values, force, VECTOR2F_ARRAY) { v ->
            output.writeFloat(v.x)
            output.writeFloat(v.y)
        }
    }

    override fun writeVector3f(name: String, value: Vector3f, force: Boolean) {
        if (force || (value.x != 0f || value.y != 0f || value.z != 0f)) {
            writeAttributeStart(name, VECTOR3F)
            output.writeFloat(value.x)
            output.writeFloat(value.y)
            output.writeFloat(value.z)
        }
    }

    override fun writeVector3fArray(name: String, values: Array<Vector3f>, force: Boolean) {
        writeGenericArray(name, values, force, VECTOR3F_ARRAY) {
            output.writeFloat(it.x)
            output.writeFloat(it.y)
            output.writeFloat(it.z)
        }
    }

    override fun writeVector4f(name: String, value: Vector4f, force: Boolean) {
        if (force || (value.x != 0f || value.y != 0f || value.z != 0f || value.w != 0f)) {
            writeAttributeStart(name, VECTOR4F)
            output.writeFloat(value.x)
            output.writeFloat(value.y)
            output.writeFloat(value.z)
            output.writeFloat(value.w)
        }
    }

    override fun writeVector4fArray(name: String, values: Array<Vector4f>, force: Boolean) {
        writeGenericArray(name, values, force, VECTOR4F_ARRAY) {
            output.writeFloat(it.x)
            output.writeFloat(it.y)
            output.writeFloat(it.z)
            output.writeFloat(it.w)
        }
    }

    override fun writeVector2d(name: String, value: Vector2d, force: Boolean) {
        if (force || (value.x != 0.0 || value.y != 0.0)) {
            writeAttributeStart(name, VECTOR2D)
            output.writeDouble(value.x)
            output.writeDouble(value.y)
        }
    }

    override fun writeVector2dArray(name: String, values: Array<Vector2d>, force: Boolean) {
        writeGenericArray(name, values, force, VECTOR2D_ARRAY) { v ->
            output.writeDouble(v.x)
            output.writeDouble(v.y)
        }
    }

    override fun writeVector3d(name: String, value: Vector3d, force: Boolean) {
        if (force || (value.x != 0.0 || value.y != 0.0 || value.z != 0.0)) {
            writeAttributeStart(name, VECTOR3D)
            output.writeDouble(value.x)
            output.writeDouble(value.y)
            output.writeDouble(value.z)
        }
    }

    override fun writeVector3dArray(name: String, values: Array<Vector3d>, force: Boolean) {
        writeGenericArray(name, values, force, VECTOR3D_ARRAY) { v ->
            output.writeDouble(v.x)
            output.writeDouble(v.y)
            output.writeDouble(v.z)
        }
    }

    override fun writeVector4d(name: String, value: Vector4d, force: Boolean) {
        if (force || (value.x != 0.0 || value.y != 0.0 || value.z != 0.0 || value.w != 0.0)) {
            writeAttributeStart(name, VECTOR4D)
            output.writeDouble(value.x)
            output.writeDouble(value.y)
            output.writeDouble(value.z)
            output.writeDouble(value.w)
        }
    }

    override fun writeVector4dArray(name: String, values: Array<Vector4d>, force: Boolean) {
        writeGenericArray(name, values, force, VECTOR4D_ARRAY) { v ->
            output.writeDouble(v.x)
            output.writeDouble(v.y)
            output.writeDouble(v.z)
            output.writeDouble(v.w)
        }
    }

    override fun writeVector2i(name: String, value: Vector2i, force: Boolean) {
        if (force || (value.x != 0 || value.y != 0)) {
            writeAttributeStart(name, VECTOR2I)
            output.writeInt(value.x)
            output.writeInt(value.y)
        }
    }

    override fun writeVector3i(name: String, value: Vector3i, force: Boolean) {
        if (force || (value.x != 0 || value.y != 0 || value.z != 0)) {
            writeAttributeStart(name, VECTOR3I)
            output.writeInt(value.x)
            output.writeInt(value.y)
            output.writeInt(value.z)
        }
    }

    override fun writeVector4i(name: String, value: Vector4i, force: Boolean) {
        if (force || (value.x != 0 || value.y != 0 || value.z != 0 || value.w != 0)) {
            writeAttributeStart(name, VECTOR4I)
            output.writeInt(value.x)
            output.writeInt(value.y)
            output.writeInt(value.z)
            output.writeInt(value.w)
        }
    }

    override fun writeVector2iArray(name: String, values: Array<Vector2i>, force: Boolean) =
        writeGenericArray(name, values, force, VECTOR2I_ARRAY) {
            output.writeInt(it.x)
            output.writeInt(it.y)
        }

    override fun writeVector3iArray(name: String, values: Array<Vector3i>, force: Boolean) =
        writeGenericArray(name, values, force, VECTOR3I_ARRAY) {
            output.writeInt(it.x)
            output.writeInt(it.y)
            output.writeInt(it.z)
        }

    override fun writeVector4iArray(name: String, values: Array<Vector4i>, force: Boolean) =
        writeGenericArray(name, values, force, VECTOR4I_ARRAY) {
            output.writeInt(it.x)
            output.writeInt(it.y)
            output.writeInt(it.z)
            output.writeInt(it.w)
        }

    private fun writeQuaternion(value: Quaternionf) {
        output.writeFloat(value.x)
        output.writeFloat(value.y)
        output.writeFloat(value.z)
        output.writeFloat(value.w)
    }

    private fun writeQuaternion(value: Quaterniond) {
        output.writeDouble(value.x)
        output.writeDouble(value.y)
        output.writeDouble(value.z)
        output.writeDouble(value.w)
    }

    override fun writeQuaternionf(name: String, value: Quaternionf, force: Boolean) {
        if (force || (value.x != 0f || value.y != 0f || value.z != 0f || value.w != 1f)) {
            writeAttributeStart(name, QUATERNION32)
            writeQuaternion(value)
        }
    }

    override fun writeQuaterniond(name: String, value: Quaterniond, force: Boolean) {
        if (force || (value.x != 0.0 || value.y != 0.0 || value.z != 0.0 || value.w != 1.0)) {
            writeAttributeStart(name, QUATERNION64)
            writeQuaternion(value)
        }
    }

    override fun writeQuaternionfArray(name: String, values: Array<Quaternionf>, force: Boolean) =
        writeGenericArray(name, values, force, QUATERNION32_ARRAY) { writeQuaternion(it) }

    override fun writeQuaternionfArray2D(name: String, values: Array<Array<Quaternionf>>, force: Boolean) =
        writeGenericArray2D(name, QUATERNION32_ARRAY_2D, values, force) {
            writeQuaternion(it)
        }

    override fun writeQuaterniondArray(name: String, values: Array<Quaterniond>, force: Boolean) =
        writeGenericArray(name, values, force, QUATERNION64_ARRAY) { writeQuaternion(it) }

    override fun writeQuaterniondArray2D(name: String, values: Array<Array<Quaterniond>>, force: Boolean) =
        writeGenericArray2D(name, QUATERNION64_ARRAY_2D, values, force) {
            writeQuaternion(it)
        }

    private fun writeMatrix(value: Matrix2f) {
        output.writeFloat(value.m00)
        output.writeFloat(value.m01)
        output.writeFloat(value.m10)
        output.writeFloat(value.m11)
    }

    private fun writeMatrix(value: Matrix3x2f) {
        output.writeFloat(value.m00)
        output.writeFloat(value.m01)
        output.writeFloat(value.m10)
        output.writeFloat(value.m11)
        output.writeFloat(value.m20)
        output.writeFloat(value.m21)
    }

    private fun writeMatrix(value: Matrix3f) {
        output.writeFloat(value.m00)
        output.writeFloat(value.m01)
        output.writeFloat(value.m02)
        output.writeFloat(value.m10)
        output.writeFloat(value.m11)
        output.writeFloat(value.m12)
        output.writeFloat(value.m20)
        output.writeFloat(value.m21)
        output.writeFloat(value.m22)
    }

    private fun writeMatrix(value: Matrix4x3f) {
        output.writeFloat(value.m00)
        output.writeFloat(value.m01)
        output.writeFloat(value.m02)
        output.writeFloat(value.m10)
        output.writeFloat(value.m11)
        output.writeFloat(value.m12)
        output.writeFloat(value.m20)
        output.writeFloat(value.m21)
        output.writeFloat(value.m22)
        output.writeFloat(value.m30)
        output.writeFloat(value.m31)
        output.writeFloat(value.m32)
    }

    private fun writeMatrix(value: Matrix4f) {
        output.writeFloat(value.m00)
        output.writeFloat(value.m01)
        output.writeFloat(value.m02)
        output.writeFloat(value.m03)
        output.writeFloat(value.m10)
        output.writeFloat(value.m11)
        output.writeFloat(value.m12)
        output.writeFloat(value.m13)
        output.writeFloat(value.m20)
        output.writeFloat(value.m21)
        output.writeFloat(value.m22)
        output.writeFloat(value.m23)
        output.writeFloat(value.m30)
        output.writeFloat(value.m31)
        output.writeFloat(value.m32)
        output.writeFloat(value.m33)
    }

    override fun writeMatrix2x2f(name: String, value: Matrix2f, force: Boolean) {
        writeAttributeStart(name, MATRIX2X2F)
        writeMatrix(value)
    }

    override fun writeMatrix3x2f(name: String, value: Matrix3x2f, force: Boolean) {
        writeAttributeStart(name, MATRIX3X2F)
        writeMatrix(value)
    }

    override fun writeMatrix3x3f(name: String, value: Matrix3f, force: Boolean) {
        writeAttributeStart(name, MATRIX3X3F)
        writeMatrix(value)
    }

    override fun writeMatrix4x3f(name: String, value: Matrix4x3f, force: Boolean) {
        writeAttributeStart(name, MATRIX4X3F)
        writeMatrix(value)
    }

    override fun writeMatrix4x4f(name: String, value: Matrix4f, force: Boolean) {
        writeAttributeStart(name, MATRIX4X4F)
        writeMatrix(value)
    }

    override fun writeMatrix2x2fArray(name: String, values: Array<Matrix2f>, force: Boolean) =
        writeGenericArray(name, values, force, MATRIX2X2F_ARRAY) { writeMatrix(it) }

    override fun writeMatrix3x2fArray(name: String, values: Array<Matrix3x2f>, force: Boolean) =
        writeGenericArray(name, values, force, MATRIX3X2F_ARRAY) { writeMatrix(it) }

    override fun writeMatrix3x3fArray(name: String, values: Array<Matrix3f>, force: Boolean) =
        writeGenericArray(name, values, force, MATRIX3X3F_ARRAY) { writeMatrix(it) }

    override fun writeMatrix4x3fArray(name: String, values: Array<Matrix4x3f>, force: Boolean) =
        writeGenericArray(name, values, force, MATRIX4X3F_ARRAY) { writeMatrix(it) }

    override fun writeMatrix4x4fArray(name: String, values: Array<Matrix4f>, force: Boolean) =
        writeGenericArray(name, values, force, MATRIX4X4F_ARRAY) { writeMatrix(it) }

    override fun writeMatrix2x2fArray2D(name: String, values: Array<Array<Matrix2f>>, force: Boolean) =
        writeGenericArray2D(name, MATRIX2X2F_ARRAY_2D, values, force) { writeMatrix(it) }

    override fun writeMatrix3x2fArray2D(name: String, values: Array<Array<Matrix3x2f>>, force: Boolean) =
        writeGenericArray2D(name, MATRIX3X2F_ARRAY_2D, values, force) { writeMatrix(it) }

    override fun writeMatrix3x3fArray2D(name: String, values: Array<Array<Matrix3f>>, force: Boolean) =
        writeGenericArray2D(name, MATRIX3X3F_ARRAY_2D, values, force) { writeMatrix(it) }

    override fun writeMatrix4x3fArray2D(name: String, values: Array<Array<Matrix4x3f>>, force: Boolean) =
        writeGenericArray2D(name, MATRIX4X3F_ARRAY_2D, values, force) { writeMatrix(it) }

    override fun writeMatrix4x4fArray2D(name: String, values: Array<Array<Matrix4f>>, force: Boolean) =
        writeGenericArray2D(name, MATRIX4X4F_ARRAY_2D, values, force) { writeMatrix(it) }

    override fun writeMatrix2x2dArray2D(name: String, values: Array<Array<Matrix2d>>, force: Boolean) =
        writeGenericArray2D(name, MATRIX2X2D_ARRAY_2D, values, force) { writeMatrix(it) }

    override fun writeMatrix3x2dArray2D(name: String, values: Array<Array<Matrix3x2d>>, force: Boolean) =
        writeGenericArray2D(name, MATRIX3X2D_ARRAY_2D, values, force) { writeMatrix(it) }

    override fun writeMatrix3x3dArray2D(name: String, values: Array<Array<Matrix3d>>, force: Boolean) =
        writeGenericArray2D(name, MATRIX3X3D_ARRAY_2D, values, force) { writeMatrix(it) }

    override fun writeMatrix4x3dArray2D(name: String, values: Array<Array<Matrix4x3d>>, force: Boolean) =
        writeGenericArray2D(name, MATRIX4X3D_ARRAY_2D, values, force) { writeMatrix(it) }

    override fun writeMatrix4x4dArray2D(name: String, values: Array<Array<Matrix4d>>, force: Boolean) =
        writeGenericArray2D(name, MATRIX4X4D_ARRAY_2D, values, force) { writeMatrix(it) }

    private fun writeMatrix(value: Matrix2d) {
        output.writeDouble(value.m00)
        output.writeDouble(value.m01)
        output.writeDouble(value.m10)
        output.writeDouble(value.m11)
    }

    private fun writeMatrix(value: Matrix3x2d) {
        output.writeDouble(value.m00)
        output.writeDouble(value.m01)
        output.writeDouble(value.m10)
        output.writeDouble(value.m11)
        output.writeDouble(value.m20)
        output.writeDouble(value.m21)
    }

    private fun writeMatrix(value: Matrix3d) {
        output.writeDouble(value.m00)
        output.writeDouble(value.m01)
        output.writeDouble(value.m02)
        output.writeDouble(value.m10)
        output.writeDouble(value.m11)
        output.writeDouble(value.m12)
        output.writeDouble(value.m20)
        output.writeDouble(value.m21)
        output.writeDouble(value.m22)
    }

    private fun writeMatrix(value: Matrix4x3d) {
        output.writeDouble(value.m00)
        output.writeDouble(value.m01)
        output.writeDouble(value.m02)
        output.writeDouble(value.m10)
        output.writeDouble(value.m11)
        output.writeDouble(value.m12)
        output.writeDouble(value.m20)
        output.writeDouble(value.m21)
        output.writeDouble(value.m22)
        output.writeDouble(value.m30)
        output.writeDouble(value.m31)
        output.writeDouble(value.m32)
    }

    private fun writeMatrix(value: Matrix4d) {
        output.writeDouble(value.m00)
        output.writeDouble(value.m01)
        output.writeDouble(value.m02)
        output.writeDouble(value.m03)
        output.writeDouble(value.m10)
        output.writeDouble(value.m11)
        output.writeDouble(value.m12)
        output.writeDouble(value.m13)
        output.writeDouble(value.m20)
        output.writeDouble(value.m21)
        output.writeDouble(value.m22)
        output.writeDouble(value.m23)
        output.writeDouble(value.m30)
        output.writeDouble(value.m31)
        output.writeDouble(value.m32)
        output.writeDouble(value.m33)
    }

    override fun writeMatrix2x2d(name: String, value: Matrix2d, force: Boolean) {
        writeAttributeStart(name, MATRIX2X2D)
        writeMatrix(value)
    }

    override fun writeMatrix3x2d(name: String, value: Matrix3x2d, force: Boolean) {
        writeAttributeStart(name, MATRIX3X2D)
        writeMatrix(value)
    }

    override fun writeMatrix3x3d(name: String, value: Matrix3d, force: Boolean) {
        writeAttributeStart(name, MATRIX3X3D)
        writeMatrix(value)
    }

    override fun writeMatrix4x3d(name: String, value: Matrix4x3d, force: Boolean) {
        writeAttributeStart(name, MATRIX4X3D)
        writeMatrix(value)
    }

    override fun writeMatrix4x4d(name: String, value: Matrix4d, force: Boolean) {
        writeAttributeStart(name, MATRIX4X4D)
        writeMatrix(value)
    }

    override fun writeMatrix2x2dArray(name: String, values: Array<Matrix2d>, force: Boolean) =
        writeGenericArray(name, values, force, MATRIX2X2D_ARRAY) { writeMatrix(it) }

    override fun writeMatrix3x2dArray(name: String, values: Array<Matrix3x2d>, force: Boolean) =
        writeGenericArray(name, values, force, MATRIX3X2D_ARRAY) { writeMatrix(it) }

    override fun writeMatrix3x3dArray(name: String, values: Array<Matrix3d>, force: Boolean) =
        writeGenericArray(name, values, force, MATRIX3X3D_ARRAY) { writeMatrix(it) }

    override fun writeMatrix4x3dArray(name: String, values: Array<Matrix4x3d>, force: Boolean) =
        writeGenericArray(name, values, force, MATRIX4X3D_ARRAY) { writeMatrix(it) }

    override fun writeMatrix4x4dArray(name: String, values: Array<Matrix4d>, force: Boolean) =
        writeGenericArray(name, values, force, MATRIX4X4D_ARRAY) { writeMatrix(it) }

    override fun writeAABBf(name: String, value: AABBf, force: Boolean) {
        writeAttributeStart(name, AABB32)
        output.writeFloat(value.minX)
        output.writeFloat(value.minY)
        output.writeFloat(value.minZ)
        output.writeFloat(value.maxX)
        output.writeFloat(value.maxY)
        output.writeFloat(value.maxZ)
    }

    override fun writeAABBd(name: String, value: AABBd, force: Boolean) {
        writeAttributeStart(name, AABB64)
        output.writeDouble(value.minX)
        output.writeDouble(value.minY)
        output.writeDouble(value.minZ)
        output.writeDouble(value.maxX)
        output.writeDouble(value.maxY)
        output.writeDouble(value.maxZ)
    }

    override fun writePlanef(name: String, value: Planef, force: Boolean) {
        writeAttributeStart(name, PLANE32)
        output.writeFloat(value.a)
        output.writeFloat(value.b)
        output.writeFloat(value.c)
        output.writeFloat(value.d)
    }

    override fun writePlaned(name: String, value: Planed, force: Boolean) {
        writeAttributeStart(name, PLANE64)
        output.writeDouble(value.a)
        output.writeDouble(value.b)
        output.writeDouble(value.c)
        output.writeDouble(value.d)
    }

    override fun writeNull(name: String?) {
        if (name != null) writeAttributeStart(name, OBJECT_NULL)
        else output.write(OBJECT_NULL)
    }

    override fun writePointer(name: String?, className: String, ptr: Int, value: ISaveable) {
        if (name != null) writeAttributeStart(name, OBJECT_PTR)
        else output.write(OBJECT_PTR)
        output.writeInt(ptr)
    }

    private fun writeObjectEnd() {
        output.writeInt(-2)
    }

    override fun writeObjectImpl(name: String?, value: ISaveable) {
        if (name != null) writeAttributeStart(name, OBJECT_IMPL)
        else output.write(OBJECT_IMPL)
        usingType(value.className) {
            writeTypeString(currentClass)
            output.writeInt(getPointer(value)!!)
            value.save(this)
            writeObjectEnd()
        }
    }

    private inline fun <V> writeGenericArray(
        name: String,
        elements: Array<V>,
        force: Boolean,
        type: Int,
        writeInstance: (V) -> Unit
    ) {
        if (force || elements.isNotEmpty()) {
            writeAttributeStart(name, type)
            output.writeInt(elements.size)
            for (index in elements.indices) {
                writeInstance(elements[index])
            }
        }
    }

    fun <V> writeGenericArray2D(
        name: String,
        type: Int,
        values: Array<Array<V>>,
        force: Boolean,
        writeInstance: (V) -> Unit
    ) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, type)
            output.writeInt(values.size)
            for (i in values.indices) {
                val vs = values[i]
                output.writeInt(vs.size)
                for (j in vs.indices) writeInstance(vs[j])
            }
        }
    }

    private inline fun <V> writeGenericList(
        name: String,
        elements: List<V>?,
        force: Boolean,
        writeInstance: (V) -> Unit
    ) {
        if (force || elements?.isNotEmpty() == true) {
            writeAttributeStart(name, OBJECT_ARRAY)
            output.writeInt(elements?.size ?: 0)
            elements?.forEach { element ->
                writeInstance(element)
            }
        }
    }

    override fun <V : ISaveable> writeObjectArray(self: ISaveable?, name: String, values: Array<V>?, force: Boolean) {
        if (force || values?.isNotEmpty() == true) {
            if (values != null && values.isNotEmpty()) {
                val firstType = values.first().className
                val allSameType = values.all { it.className == firstType }
                if (allSameType) {
                    writeHomogenousObjectArray(self, name, values, force)
                } else {
                    writeGenericArray(name, values, force, OBJECT_ARRAY) {
                        writeObject(null, null, it, true)
                    }
                }
            } else {
                writeAttributeStart(name, OBJECT_ARRAY)
                output.writeInt(0)
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
            if (values != null && values.isNotEmpty()) {
                val firstType = values.first()?.className
                val allSameType = values.all { it?.className == firstType }
                if (firstType != null && allSameType) {
                    writeHomogenousObjectArray(self, name, values, force)
                } else {
                    writeGenericArray(name, values, force, OBJECT_ARRAY) {
                        writeObject(null, null, it, true)
                    }
                }
            } else {
                writeAttributeStart(name, OBJECT_ARRAY)
                output.writeInt(0)
            }
        }
    }

    override fun <V : ISaveable> writeObjectArray2D(
        self: ISaveable?,
        name: String,
        values: Array<Array<V>>,
        force: Boolean
    ) {
        writeGenericArray(name, values, force, OBJECT_ARRAY_2D) {
            output.writeInt(it.size)
            for (i in it.indices) {
                writeObject(null, null, it[i], true)
            }
        }
    }

    override fun <V : ISaveable> writeObjectList(self: ISaveable?, name: String, values: List<V>?, force: Boolean) {
        writeGenericList(name, values, force) {
            writeObject(null, null, it, true)
        }
    }

    override fun <V : ISaveable?> writeHomogenousObjectArray(
        self: ISaveable?,
        name: String,
        values: Array<V>,
        force: Boolean
    ) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, OBJECTS_HOMOGENOUS_ARRAY)
            writeTypeString(values.firstOrNull()?.className ?: "")
            output.writeInt(values.size)
            for (element in values) {
                element!!.save(this)
                writeObjectEnd()
            }
        }
    }

    override fun writeListStart() {
        writeAttributeStart("", OBJECT_LIST_UNKNOWN_LENGTH)
    }

    override fun writeListEnd() {
        output.write(37)
    }

    override fun writeListSeparator() {
        output.write(17)
    }
}