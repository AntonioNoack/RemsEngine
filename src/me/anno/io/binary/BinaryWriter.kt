package me.anno.io.binary

import me.anno.io.Saveable
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
                val bytes = string.encodeToByteArray()
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
        val nameType = NameType(name, type)
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
            writeAttributeStart(name, SimpleType.BOOLEAN.scalarId)
            output.writeByte(value.toInt())
        }
    }

    override fun writeBooleanArray(name: String, values: BooleanArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.BOOLEAN.scalarId + 1)
            output.writeInt(values.size)
            for (v in values) output.write(if (v) 1 else 0)
        }
    }

    override fun writeBooleanArray2D(name: String, values: Array<BooleanArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.BOOLEAN.scalarId + 2)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) output.write(if (v) 1 else 0)
            }
        }
    }

    override fun writeChar(name: String, value: Char, force: Boolean) {
        if (force || value != 0.toChar()) {
            writeAttributeStart(name, SimpleType.CHAR.scalarId)
            output.writeChar(value.code)
        }
    }

    override fun writeCharArray(name: String, values: CharArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.CHAR.scalarId + 1)
            output.writeInt(values.size)
            for (c in values) output.writeChar(c.code)
        }
    }

    override fun writeCharArray2D(name: String, values: Array<CharArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.CHAR.scalarId + 2)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) output.writeChar(v.code)
            }
        }
    }


    override fun writeByte(name: String, value: Byte, force: Boolean) {
        if (force || value != 0.toByte()) {
            writeAttributeStart(name, SimpleType.BYTE.scalarId)
            output.writeByte(value.toInt())
        }
    }

    override fun writeByteArray(name: String, values: ByteArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.BYTE.scalarId + 1)
            output.writeInt(values.size)
            output.write(values)
        }
    }

    override fun writeByteArray2D(name: String, values: Array<ByteArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.BYTE.scalarId + 2)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                output.write(vs)
            }
        }
    }

    override fun writeShort(name: String, value: Short, force: Boolean) {
        if (force || value != 0.toShort()) {
            writeAttributeStart(name, SimpleType.SHORT.scalarId)
            output.writeShort(value.toInt())
        }
    }

    override fun writeShortArray(name: String, values: ShortArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.SHORT.scalarId + 1)
            output.writeInt(values.size)
            for (v in values) output.writeShort(v.toInt())
        }
    }

    override fun writeShortArray2D(name: String, values: Array<ShortArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.SHORT.scalarId + 2)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) output.writeShort(v.toInt())
            }
        }
    }

    override fun writeInt(name: String, value: Int, force: Boolean) {
        if (force || value != 0) {
            writeAttributeStart(name, SimpleType.INT.scalarId)
            output.writeInt(value)
        }
    }

    override fun writeColor(name: String, value: Int, force: Boolean) {
        writeInt(name, value, force)
    }

    override fun writeIntArray(name: String, values: IntArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.INT.scalarId + 1)
            output.writeInt(values.size)
            for (v in values) output.writeInt(v)
        }
    }

    override fun writeColorArray(name: String, values: IntArray, force: Boolean) {
        writeIntArray(name, values, force)
    }

    override fun writeIntArray2D(name: String, values: Array<IntArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.INT.scalarId + 2)
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
            writeAttributeStart(name, SimpleType.LONG.scalarId)
            output.writeLong(value)
        }
    }

    override fun writeLongArray(name: String, values: LongArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.LONG.scalarId + 1)
            output.writeInt(values.size)
            for (v in values) output.writeLong(v)
        }
    }

    override fun writeLongArray2D(name: String, values: Array<LongArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.LONG.scalarId + 2)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) output.writeLong(v)
            }
        }
    }

    override fun writeFloat(name: String, value: Float, force: Boolean) {
        if (force || value != 0f) {
            writeAttributeStart(name, SimpleType.FLOAT.scalarId)
            output.writeFloat(value)
        }
    }

    override fun writeFloatArray(name: String, values: FloatArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.FLOAT.scalarId + 1)
            output.writeInt(values.size)
            for (v in values) output.writeFloat(v)
        }
    }

    override fun writeFloatArray2D(name: String, values: Array<FloatArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.FLOAT.scalarId + 2)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) output.writeFloat(v)
            }
        }
    }

    override fun writeDouble(name: String, value: Double, force: Boolean) {
        if (force || value != 0.0) {
            writeAttributeStart(name, SimpleType.DOUBLE.scalarId)
            output.writeDouble(value)
        }
    }

    override fun writeDoubleArray(name: String, values: DoubleArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.DOUBLE.scalarId + 1)
            output.writeInt(values.size)
            for (v in values) output.writeDouble(v)
        }
    }

    override fun writeDoubleArray2D(name: String, values: Array<DoubleArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.DOUBLE.scalarId + 2)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) output.writeDouble(v)
            }
        }
    }

    override fun writeString(name: String, value: String, force: Boolean) {
        if (force || value != "") {
            writeAttributeStart(name, SimpleType.STRING.scalarId)
            writeEfficientString(value)
        }
    }

    override fun writeStringArray(name: String, values: Array<String>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.STRING.scalarId + 1)
            output.writeInt(values.size)
            for (v in values) writeEfficientString(v)
        }
    }

    override fun writeStringArray2D(name: String, values: Array<Array<String>>, force: Boolean) {
        writeGenericArray2D(name, values, force, SimpleType.STRING.scalarId + 2) {
            writeEfficientString(it)
        }
    }

    override fun writeFile(name: String, value: FileReference, force: Boolean, workspace: FileReference) {
        if (force || value != InvalidRef) {
            writeAttributeStart(name, SimpleType.REFERENCE.scalarId)
            writeEfficientString(value.toLocalPath(workspace))
        }
    }

    override fun writeFileArray(name: String, values: Array<FileReference>, force: Boolean, workspace: FileReference) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SimpleType.REFERENCE.scalarId + 1)
            output.writeInt(values.size)
            for (v in values) writeEfficientString(v.toLocalPath(workspace))
        }
    }

    override fun writeFileArray2D(
        name: String, values: Array<Array<FileReference>>,
        force: Boolean, workspace: FileReference
    ) {
        writeGenericArray2D(name, values, force, SimpleType.REFERENCE.scalarId + 2) { v ->
            writeEfficientString(v.toLocalPath(workspace))
        }
    }

    private fun writeVector2f(value: Vector2f) {
        output.writeFloat(value.x)
        output.writeFloat(value.y)
    }

    private fun writeVector3f(value: Vector3f) {
        output.writeFloat(value.x)
        output.writeFloat(value.y)
        output.writeFloat(value.z)
    }

    private fun writeVector4f(value: Vector4f) {
        output.writeFloat(value.x)
        output.writeFloat(value.y)
        output.writeFloat(value.z)
        output.writeFloat(value.w)
    }

    override fun writeVector2f(name: String, value: Vector2f, force: Boolean) {
        if (force || (value.x != 0f && value.y != 0f)) {
            writeAttributeStart(name, SimpleType.VECTOR2F.scalarId)
            writeVector2f(value)
        }
    }

    override fun writeVector2fArray(name: String, values: Array<Vector2f>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.VECTOR2F.scalarId + 1, ::writeVector2f)

    override fun writeVector2fArray2D(name: String, values: Array<Array<Vector2f>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.VECTOR2F.scalarId + 2, ::writeVector2f)

    override fun writeVector3f(name: String, value: Vector3f, force: Boolean) {
        if (force || (value.x != 0f || value.y != 0f || value.z != 0f)) {
            writeAttributeStart(name, SimpleType.VECTOR3F.scalarId)
            writeVector3f(value)
        }
    }

    override fun writeVector3fArray(name: String, values: Array<Vector3f>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.VECTOR3F.scalarId + 1, ::writeVector3f)

    override fun writeVector3fArray2D(name: String, values: Array<Array<Vector3f>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.VECTOR3F.scalarId + 2, ::writeVector3f)

    override fun writeVector4f(name: String, value: Vector4f, force: Boolean) {
        if (force || (value.x != 0f || value.y != 0f || value.z != 0f || value.w != 0f)) {
            writeAttributeStart(name, SimpleType.VECTOR4F.scalarId)
            writeVector4f(value)
        }
    }

    override fun writeVector4fArray(name: String, values: Array<Vector4f>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.VECTOR4F.scalarId + 1, ::writeVector4f)

    override fun writeVector4fArray2D(name: String, values: Array<Array<Vector4f>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.VECTOR4F.scalarId + 2, ::writeVector4f)

    private fun writeVector2d(value: Vector2d) {
        output.writeDouble(value.x)
        output.writeDouble(value.y)
    }

    private fun writeVector3d(value: Vector3d) {
        output.writeDouble(value.x)
        output.writeDouble(value.y)
        output.writeDouble(value.z)
    }

    private fun writeVector4d(value: Vector4d) {
        output.writeDouble(value.x)
        output.writeDouble(value.y)
        output.writeDouble(value.z)
        output.writeDouble(value.w)
    }

    override fun writeVector2d(name: String, value: Vector2d, force: Boolean) {
        if (force || (value.x != 0.0 || value.y != 0.0)) {
            writeAttributeStart(name, SimpleType.VECTOR2D.scalarId)
            writeVector2d(value)
        }
    }

    override fun writeVector2dArray(name: String, values: Array<Vector2d>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.VECTOR2D.scalarId + 1, ::writeVector2d)

    override fun writeVector2dArray2D(name: String, values: Array<Array<Vector2d>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.VECTOR2D.scalarId + 2, ::writeVector2d)

    override fun writeVector3d(name: String, value: Vector3d, force: Boolean) {
        if (force || (value.x != 0.0 || value.y != 0.0 || value.z != 0.0)) {
            writeAttributeStart(name, SimpleType.VECTOR3D.scalarId)
            writeVector3d(value)
        }
    }

    override fun writeVector3dArray(name: String, values: Array<Vector3d>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.VECTOR3D.scalarId + 1, ::writeVector3d)

    override fun writeVector3dArray2D(name: String, values: Array<Array<Vector3d>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.VECTOR3D.scalarId + 2, ::writeVector3d)

    override fun writeVector4d(name: String, value: Vector4d, force: Boolean) {
        if (force || (value.x != 0.0 || value.y != 0.0 || value.z != 0.0 || value.w != 0.0)) {
            writeAttributeStart(name, SimpleType.VECTOR4D.scalarId)
            writeVector4d(value)
        }
    }

    override fun writeVector4dArray(name: String, values: Array<Vector4d>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.VECTOR4D.scalarId + 1, ::writeVector4d)

    override fun writeVector4dArray2D(name: String, values: Array<Array<Vector4d>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.VECTOR4D.scalarId + 2, ::writeVector4d)

    private fun writeVector2i(value: Vector2i) {
        output.writeInt(value.x)
        output.writeInt(value.y)
    }

    private fun writeVector3i(value: Vector3i) {
        output.writeInt(value.x)
        output.writeInt(value.y)
        output.writeInt(value.z)
    }

    private fun writeVector4i(value: Vector4i) {
        output.writeInt(value.x)
        output.writeInt(value.y)
        output.writeInt(value.z)
        output.writeInt(value.w)
    }

    override fun writeVector2i(name: String, value: Vector2i, force: Boolean) {
        if (force || (value.x != 0 || value.y != 0)) {
            writeAttributeStart(name, SimpleType.VECTOR2I.scalarId)
            writeVector2i(value)
        }
    }

    override fun writeVector3i(name: String, value: Vector3i, force: Boolean) {
        if (force || (value.x != 0 || value.y != 0 || value.z != 0)) {
            writeAttributeStart(name, SimpleType.VECTOR3I.scalarId)
            writeVector3i(value)
        }
    }

    override fun writeVector4i(name: String, value: Vector4i, force: Boolean) {
        if (force || (value.x != 0 || value.y != 0 || value.z != 0 || value.w != 0)) {
            writeAttributeStart(name, SimpleType.VECTOR4I.scalarId)
            writeVector4i(value)
        }
    }

    override fun writeVector2iArray(name: String, values: Array<Vector2i>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.VECTOR2I.scalarId + 1, ::writeVector2i)

    override fun writeVector3iArray(name: String, values: Array<Vector3i>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.VECTOR3I.scalarId + 1, ::writeVector3i)

    override fun writeVector4iArray(name: String, values: Array<Vector4i>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.VECTOR4I.scalarId + 1, ::writeVector4i)

    override fun writeVector2iArray2D(name: String, values: Array<Array<Vector2i>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.VECTOR2I.scalarId + 2, ::writeVector2i)

    override fun writeVector3iArray2D(name: String, values: Array<Array<Vector3i>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.VECTOR3I.scalarId + 2, ::writeVector3i)

    override fun writeVector4iArray2D(name: String, values: Array<Array<Vector4i>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.VECTOR4I.scalarId + 2, ::writeVector4i)

    private fun writeQuaternionf(value: Quaternionf) {
        output.writeFloat(value.x)
        output.writeFloat(value.y)
        output.writeFloat(value.z)
        output.writeFloat(value.w)
    }

    private fun writeQuaterniond(value: Quaterniond) {
        output.writeDouble(value.x)
        output.writeDouble(value.y)
        output.writeDouble(value.z)
        output.writeDouble(value.w)
    }

    override fun writeQuaternionf(name: String, value: Quaternionf, force: Boolean) {
        if (force || (value.x != 0f || value.y != 0f || value.z != 0f || value.w != 1f)) {
            writeAttributeStart(name, SimpleType.QUATERNIONF.scalarId)
            writeQuaternionf(value)
        }
    }

    override fun writeQuaterniond(name: String, value: Quaterniond, force: Boolean) {
        if (force || (value.x != 0.0 || value.y != 0.0 || value.z != 0.0 || value.w != 1.0)) {
            writeAttributeStart(name, SimpleType.QUATERNIOND.scalarId)
            writeQuaterniond(value)
        }
    }

    override fun writeQuaternionfArray(name: String, values: Array<Quaternionf>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.QUATERNIONF.scalarId + 1) { writeQuaternionf(it) }

    override fun writeQuaternionfArray2D(name: String, values: Array<Array<Quaternionf>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.QUATERNIONF.scalarId + 2, ::writeQuaternionf)

    override fun writeQuaterniondArray(name: String, values: Array<Quaterniond>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.QUATERNIOND.scalarId + 1, ::writeQuaterniond)

    override fun writeQuaterniondArray2D(name: String, values: Array<Array<Quaterniond>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.QUATERNIOND.scalarId + 2, ::writeQuaterniond)

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
        writeAttributeStart(name, SimpleType.MATRIX2X2F.scalarId)
        writeMatrix(value)
    }

    override fun writeMatrix3x2f(name: String, value: Matrix3x2f, force: Boolean) {
        writeAttributeStart(name, SimpleType.MATRIX3X2F.scalarId)
        writeMatrix(value)
    }

    override fun writeMatrix3x3f(name: String, value: Matrix3f, force: Boolean) {
        writeAttributeStart(name, SimpleType.MATRIX3X3F.scalarId)
        writeMatrix(value)
    }

    override fun writeMatrix4x3f(name: String, value: Matrix4x3f, force: Boolean) {
        writeAttributeStart(name, SimpleType.MATRIX4X3F.scalarId)
        writeMatrix(value)
    }

    override fun writeMatrix4x4f(name: String, value: Matrix4f, force: Boolean) {
        writeAttributeStart(name, SimpleType.MATRIX4X4F.scalarId)
        writeMatrix(value)
    }

    override fun writeMatrix2x2fArray(name: String, values: Array<Matrix2f>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.MATRIX2X2F.scalarId + 1) { writeMatrix(it) }

    override fun writeMatrix3x2fArray(name: String, values: Array<Matrix3x2f>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.MATRIX3X2F.scalarId + 1) { writeMatrix(it) }

    override fun writeMatrix3x3fArray(name: String, values: Array<Matrix3f>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.MATRIX3X3F.scalarId + 1) { writeMatrix(it) }

    override fun writeMatrix4x3fArray(name: String, values: Array<Matrix4x3f>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.MATRIX4X3F.scalarId + 1) { writeMatrix(it) }

    override fun writeMatrix4x4fArray(name: String, values: Array<Matrix4f>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.MATRIX4X4F.scalarId + 1) { writeMatrix(it) }

    override fun writeMatrix2x2fArray2D(name: String, values: Array<Array<Matrix2f>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.MATRIX2X2F.scalarId + 2) { writeMatrix(it) }

    override fun writeMatrix3x2fArray2D(name: String, values: Array<Array<Matrix3x2f>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.MATRIX3X2F.scalarId + 2) { writeMatrix(it) }

    override fun writeMatrix3x3fArray2D(name: String, values: Array<Array<Matrix3f>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.MATRIX3X3F.scalarId + 2) { writeMatrix(it) }

    override fun writeMatrix4x3fArray2D(name: String, values: Array<Array<Matrix4x3f>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.MATRIX4X3F.scalarId + 2) { writeMatrix(it) }

    override fun writeMatrix4x4fArray2D(name: String, values: Array<Array<Matrix4f>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.MATRIX4X4F.scalarId + 2) { writeMatrix(it) }

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
        writeAttributeStart(name, SimpleType.MATRIX2X2D.scalarId)
        writeMatrix(value)
    }

    override fun writeMatrix3x2d(name: String, value: Matrix3x2d, force: Boolean) {
        writeAttributeStart(name, SimpleType.MATRIX3X2D.scalarId)
        writeMatrix(value)
    }

    override fun writeMatrix3x3d(name: String, value: Matrix3d, force: Boolean) {
        writeAttributeStart(name, SimpleType.MATRIX3X3D.scalarId)
        writeMatrix(value)
    }

    override fun writeMatrix4x3d(name: String, value: Matrix4x3d, force: Boolean) {
        writeAttributeStart(name, SimpleType.MATRIX4X3D.scalarId)
        writeMatrix(value)
    }

    override fun writeMatrix4x4d(name: String, value: Matrix4d, force: Boolean) {
        writeAttributeStart(name, SimpleType.MATRIX4X4D.scalarId)
        writeMatrix(value)
    }

    override fun writeMatrix2x2dArray(name: String, values: Array<Matrix2d>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.MATRIX2X2D.scalarId + 1) { writeMatrix(it) }

    override fun writeMatrix3x2dArray(name: String, values: Array<Matrix3x2d>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.MATRIX3X2D.scalarId + 1) { writeMatrix(it) }

    override fun writeMatrix3x3dArray(name: String, values: Array<Matrix3d>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.MATRIX3X3D.scalarId + 1) { writeMatrix(it) }

    override fun writeMatrix4x3dArray(name: String, values: Array<Matrix4x3d>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.MATRIX4X3D.scalarId + 1) { writeMatrix(it) }

    override fun writeMatrix4x4dArray(name: String, values: Array<Matrix4d>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.MATRIX4X4D.scalarId + 1) { writeMatrix(it) }

    override fun writeMatrix2x2dArray2D(name: String, values: Array<Array<Matrix2d>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.MATRIX2X2D.scalarId + 2) { writeMatrix(it) }

    override fun writeMatrix3x2dArray2D(name: String, values: Array<Array<Matrix3x2d>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.MATRIX3X2D.scalarId + 2) { writeMatrix(it) }

    override fun writeMatrix3x3dArray2D(name: String, values: Array<Array<Matrix3d>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.MATRIX3X3D.scalarId + 2) { writeMatrix(it) }

    override fun writeMatrix4x3dArray2D(name: String, values: Array<Array<Matrix4x3d>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.MATRIX4X3D.scalarId + 2) { writeMatrix(it) }

    override fun writeMatrix4x4dArray2D(name: String, values: Array<Array<Matrix4d>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.MATRIX4X4D.scalarId + 2) { writeMatrix(it) }

    private fun writeAABBf(value: AABBf) {
        output.writeFloat(value.minX)
        output.writeFloat(value.minY)
        output.writeFloat(value.minZ)
        output.writeFloat(value.maxX)
        output.writeFloat(value.maxY)
        output.writeFloat(value.maxZ)
    }

    private fun writeAABBd(value: AABBd) {
        output.writeDouble(value.minX)
        output.writeDouble(value.minY)
        output.writeDouble(value.minZ)
        output.writeDouble(value.maxX)
        output.writeDouble(value.maxY)
        output.writeDouble(value.maxZ)
    }

    override fun writeAABBf(name: String, value: AABBf, force: Boolean) {
        writeAttributeStart(name, SimpleType.AABBF.scalarId)
        writeAABBf(value)
    }

    override fun writeAABBd(name: String, value: AABBd, force: Boolean) {
        writeAttributeStart(name, SimpleType.AABBD.scalarId)
        writeAABBd(value)
    }

    override fun writeAABBfArray(name: String, values: Array<AABBf>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.AABBF.scalarId + 1, ::writeAABBf)

    override fun writeAABBdArray(name: String, values: Array<AABBd>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.AABBD.scalarId + 1, ::writeAABBd)

    override fun writeAABBfArray2D(name: String, values: Array<Array<AABBf>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.AABBF.scalarId + 2, ::writeAABBf)

    override fun writeAABBdArray2D(name: String, values: Array<Array<AABBd>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.AABBD.scalarId + 2, ::writeAABBd)

    private fun writePlanef(value: Planef) {
        output.writeFloat(value.dirX)
        output.writeFloat(value.dirY)
        output.writeFloat(value.dirZ)
        output.writeFloat(value.distance)
    }

    private fun writePlaned(value: Planed) {
        output.writeDouble(value.dirX)
        output.writeDouble(value.dirY)
        output.writeDouble(value.dirZ)
        output.writeDouble(value.distance)
    }

    override fun writePlanef(name: String, value: Planef, force: Boolean) {
        writeAttributeStart(name, SimpleType.PLANEF.scalarId)
        writePlanef(value)
    }

    override fun writePlaned(name: String, value: Planed, force: Boolean) {
        writeAttributeStart(name, SimpleType.PLANED.scalarId)
        writePlaned(value)
    }

    override fun writePlanefArray(name: String, values: Array<Planef>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.PLANEF.scalarId + 1, ::writePlanef)

    override fun writePlanedArray(name: String, values: Array<Planed>, force: Boolean) =
        writeGenericArray(name, values, force, SimpleType.PLANED.scalarId + 1, ::writePlaned)

    override fun writePlanefArray2D(name: String, values: Array<Array<Planef>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.PLANEF.scalarId + 2, ::writePlanef)

    override fun writePlanedArray2D(name: String, values: Array<Array<Planed>>, force: Boolean) =
        writeGenericArray2D(name, values, force, SimpleType.PLANED.scalarId + 2, ::writePlaned)

    override fun writeNull(name: String?) {
        if (name != null) writeAttributeStart(name, OBJECT_NULL)
        else output.write(OBJECT_NULL)
    }

    override fun writePointer(name: String?, className: String, ptr: Int, value: Saveable) {
        if (name != null) writeAttributeStart(name, OBJECT_PTR)
        else output.write(OBJECT_PTR)
        output.writeInt(ptr)
    }

    private fun writeObjectEnd() {
        output.writeInt(-2)
    }

    override fun writeObjectImpl(name: String?, value: Saveable) {
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
        values: Array<V>,
        force: Boolean,
        type: Int,
        writeInstance: (V) -> Unit
    ) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, type)
            output.writeInt(values.size)
            for (index in values.indices) {
                writeInstance(values[index])
            }
        }
    }

    fun <V> writeGenericArray2D(
        name: String,
        values: Array<Array<V>>,
        force: Boolean,
        type: Int,
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

    override fun <V : Saveable> writeObjectArray(self: Saveable?, name: String, values: Array<V>?, force: Boolean) {
        if (force || values?.isNotEmpty() == true) {
            if (!values.isNullOrEmpty()) {
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

    override fun <V : Saveable?> writeNullableObjectArray(
        self: Saveable?,
        name: String,
        values: Array<V>?,
        force: Boolean
    ) {
        if (force || values?.isNotEmpty() == true) {
            if (!values.isNullOrEmpty()) {
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

    override fun <V : Saveable> writeObjectArray2D(
        self: Saveable?,
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

    override fun <V : Saveable> writeObjectList(self: Saveable?, name: String, values: List<V>?, force: Boolean) {
        writeGenericList(name, values, force) {
            writeObject(null, null, it, true)
        }
    }

    override fun <V : Saveable?> writeHomogenousObjectArray(
        self: Saveable?,
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