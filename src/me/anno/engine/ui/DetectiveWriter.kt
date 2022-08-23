package me.anno.engine.ui

import me.anno.io.ISaveable
import me.anno.io.files.FileReference
import me.anno.io.find.PartialWriter
import org.joml.*

/**
 * collects all written properties including their type;
 * is used in the editor for ISaveables that are not Inspectables
 * */
class DetectiveWriter(val dst: HashMap<String, Pair<String, Any?>>) : PartialWriter(false) {

    override fun writeBoolean(name: String, value: Boolean, force: Boolean) {
        dst[name] = "Boolean" to value
    }

    override fun writeBooleanArray(name: String, values: BooleanArray, force: Boolean) {
        dst[name] = "BooleanArray" to values
    }

    override fun writeBooleanArray2D(name: String, values: Array<BooleanArray>, force: Boolean) {
        dst[name] = "Array<BooleanArray>" to values
    }

    override fun writeChar(name: String, value: Char, force: Boolean) {
        dst[name] = "Char" to value
    }

    override fun writeCharArray(name: String, values: CharArray, force: Boolean) {
        dst[name] = "CharArray" to values
    }

    override fun writeCharArray2D(name: String, values: Array<CharArray>, force: Boolean) {
        dst[name] = "Array<CharArray>" to values
    }

    override fun writeByte(name: String, value: Byte, force: Boolean) {
        dst[name] = "Byte" to value
    }

    override fun writeByteArray(name: String, values: ByteArray, force: Boolean) {
        dst[name] = "ByteArray" to values
    }

    override fun writeByteArray2D(name: String, values: Array<ByteArray>, force: Boolean) {
        dst[name] = "Array<ByteArray>" to values
    }

    override fun writeShort(name: String, value: Short, force: Boolean) {
        dst[name] = "Short" to value
    }

    override fun writeShortArray(name: String, values: ShortArray, force: Boolean) {
        dst[name] = "ShortArray" to values
    }

    override fun writeShortArray2D(name: String, values: Array<ShortArray>, force: Boolean) {
        dst[name] = "Array<ShortArray>" to values
    }

    override fun writeInt(name: String, value: Int, force: Boolean) {
        dst[name] = "Int" to value
    }

    override fun writeIntArray(name: String, values: IntArray, force: Boolean) {
        dst[name] = "IntArray" to values
    }

    override fun writeIntArray2D(name: String, values: Array<IntArray>, force: Boolean) {
        dst[name] = "Array<IntArray>" to values
    }

    override fun writeColor(name: String, value: Int, force: Boolean) {
        dst[name] = "Color4" to value
    }

    override fun writeColorArray(name: String, values: IntArray, force: Boolean) {
        dst[name] = "Array<Color4>" to values
    }

    override fun writeColorArray2D(name: String, values: Array<IntArray>, force: Boolean) {
        dst[name] = "Array<Array<Color4>>" to values
    }

    override fun writeLong(name: String, value: Long, force: Boolean) {
        dst[name] = "Long" to value
    }

    override fun writeLongArray(name: String, values: LongArray, force: Boolean) {
        dst[name] = "LongArray" to values
    }

    override fun writeLongArray2D(name: String, values: Array<LongArray>, force: Boolean) {
        dst[name] = "Array<LongArray>" to values
    }

    override fun writeFloat(name: String, value: Float, force: Boolean) {
        dst[name] = "Float" to value
    }

    override fun writeFloatArray(name: String, values: FloatArray?, force: Boolean) {
        dst[name] = "FloatArray" to values
    }

    override fun writeFloatArray2D(name: String, values: Array<FloatArray>?, force: Boolean) {
        dst[name] = "Array<FloatArray>" to values
    }

    override fun writeDouble(name: String, value: Double, force: Boolean) {
        dst[name] = "Double" to value
    }

    override fun writeDoubleArray(name: String, values: DoubleArray, force: Boolean) {
        dst[name] = "DoubleArray" to values
    }

    override fun writeDoubleArray2D(name: String, values: Array<DoubleArray>, force: Boolean) {
        dst[name] = "Array<DoubleArray>" to values
    }

    override fun writeString(name: String, value: String?, force: Boolean) {
        dst[name] = "String" to value
    }

    override fun writeStringArray(name: String, values: Array<String>, force: Boolean) {
        dst[name] = "Array<String>" to values
    }

    override fun writeStringArray2D(name: String, values: Array<Array<String>>, force: Boolean) {
        dst[name] = "Array<Array<String>>" to values
    }

    override fun writeVector2f(name: String, value: Vector2f, force: Boolean) {
        dst[name] = "Vector2f" to value
    }

    override fun writeVector3f(name: String, value: Vector3f, force: Boolean) {
        dst[name] = "Vector3f" to value
    }

    override fun writeVector4f(name: String, value: Vector4f, force: Boolean) {
        dst[name] = "Vector4f" to value
    }

    override fun writeVector2fArray(name: String, values: Array<Vector2f>, force: Boolean) {
        dst[name] = "Array<Vector2f>" to values
    }

    override fun writeVector3fArray(name: String, values: Array<Vector3f>, force: Boolean) {
        dst[name] = "Array<Vector3f>" to values
    }

    override fun writeVector4fArray(name: String, values: Array<Vector4f>, force: Boolean) {
        dst[name] = "Array<Vector4f>" to values
    }

    override fun writeVector2d(name: String, value: Vector2d, force: Boolean) {
        dst[name] = "Vector2d" to value
    }

    override fun writeVector3d(name: String, value: Vector3d, force: Boolean) {
        dst[name] = "Vector3d" to value
    }

    override fun writeVector4d(name: String, value: Vector4d, force: Boolean) {
        dst[name] = "Vector4d" to value
    }

    override fun writeVector2dArray(name: String, values: Array<Vector2d>, force: Boolean) {
        dst[name] = "Array<Array<Vector2f>>" to values
    }

    override fun writeVector3dArray(name: String, values: Array<Vector3d>, force: Boolean) {
        dst[name] = "Array<Array<Vector3f>>" to values
    }

    override fun writeVector4dArray(name: String, values: Array<Vector4d>, force: Boolean) {
        dst[name] = "Array<Array<Vector4f>>" to values
    }

    override fun writeVector2i(name: String, value: Vector2i, force: Boolean) {
        dst[name] = "Vector2i" to value
    }

    override fun writeVector3i(name: String, value: Vector3i, force: Boolean) {
        dst[name] = "Vector3i" to value
    }

    override fun writeVector4i(name: String, value: Vector4i, force: Boolean) {
        dst[name] = "Vector4i" to value
    }

    override fun writeVector2iArray(name: String, values: Array<Vector2i>, force: Boolean) {
        dst[name] = "Array<Vector2i>" to values
    }

    override fun writeVector3iArray(name: String, values: Array<Vector3i>, force: Boolean) {
        dst[name] = "Array<Vector3i>" to values
    }

    override fun writeVector4iArray(name: String, values: Array<Vector4i>, force: Boolean) {
        dst[name] = "Array<Vector4i>" to values
    }

    override fun writeMatrix2x2f(name: String, value: Matrix2f, force: Boolean) {
        dst[name] = "Matrix2x2" to value
    }

    override fun writeMatrix3x2f(name: String, value: Matrix3x2f, force: Boolean) {
        dst[name] = "Matrix3x2" to value
    }

    override fun writeMatrix3x3f(name: String, value: Matrix3f, force: Boolean) {
        dst[name] = "Matrix3x3" to value
    }

    override fun writeMatrix4x3f(name: String, value: Matrix4x3f, force: Boolean) {
        dst[name] = "Matrix4x3" to value
    }

    override fun writeMatrix4x4f(name: String, value: Matrix4f, force: Boolean) {
        dst[name] = "Matrix4x4" to value
    }

    override fun writeMatrix2x2fArray(name: String, values: Array<Matrix2f>, force: Boolean) {
        dst[name] = "Array<Matrix2x2>" to values
    }

    override fun writeMatrix3x2fArray(name: String, values: Array<Matrix3x2f>, force: Boolean) {
        dst[name] = "Array<Matrix3x2>" to values
    }

    override fun writeMatrix3x3fArray(name: String, values: Array<Matrix3f>, force: Boolean) {
        dst[name] = "Array<Matrix3x3>" to values
    }

    override fun writeMatrix4x3fArray(name: String, values: Array<Matrix4x3f>, force: Boolean) {
        dst[name] = "Array<Matrix4x3>" to values
    }

    override fun writeMatrix4x4fArray(name: String, values: Array<Matrix4f>, force: Boolean) {
        dst[name] = "Array<Matrix4x4>" to values
    }

    override fun writeMatrix2x2fArray2D(name: String, values: Array<Array<Matrix2f>>, force: Boolean) {
        dst[name] = "Array<Array<Matrix2x2>>" to values
    }

    override fun writeMatrix3x2fArray2D(name: String, values: Array<Array<Matrix3x2f>>, force: Boolean) {
        dst[name] = "Array<Array<Matrix3x2>>" to values
    }

    override fun writeMatrix3x3fArray2D(name: String, values: Array<Array<Matrix3f>>, force: Boolean) {
        dst[name] = "Array<Array<Matrix3x3>>" to values
    }

    override fun writeMatrix4x3fArray2D(name: String, values: Array<Array<Matrix4x3f>>, force: Boolean) {
        dst[name] = "Array<Array<Matrix4x3>>" to values
    }

    override fun writeMatrix4x4fArray2D(name: String, values: Array<Array<Matrix4f>>, force: Boolean) {
        dst[name] = "Array<Array<Matrix4x4>>" to values
    }

    override fun writeMatrix2x2d(name: String, value: Matrix2d, force: Boolean) {
        dst[name] = "Matrix2x2d" to value
    }

    override fun writeMatrix3x2d(name: String, value: Matrix3x2d, force: Boolean) {
        dst[name] = "Matrix3x2d" to value
    }

    override fun writeMatrix3x3d(name: String, value: Matrix3d, force: Boolean) {
        dst[name] = "Matrix3x3d" to value
    }

    override fun writeMatrix4x3d(name: String, value: Matrix4x3d, force: Boolean) {
        dst[name] = "Matrix4x3d" to value
    }

    override fun writeMatrix4x4d(name: String, value: Matrix4d, force: Boolean) {
        dst[name] = "Matrix4x4d" to value
    }

    override fun writeMatrix2x2dArray(name: String, values: Array<Matrix2d>, force: Boolean) {
        dst[name] = "Array<Matrix2x2d>" to values
    }

    override fun writeMatrix3x2dArray(name: String, values: Array<Matrix3x2d>, force: Boolean) {
        dst[name] = "Array<Matrix3x2d>" to values
    }

    override fun writeMatrix3x3dArray(name: String, values: Array<Matrix3d>, force: Boolean) {
        dst[name] = "Array<Matrix3x3d>" to values
    }

    override fun writeMatrix4x3dArray(name: String, values: Array<Matrix4x3d>, force: Boolean) {
        dst[name] = "Array<Matrix4x3d>" to values
    }

    override fun writeMatrix4x4dArray(name: String, values: Array<Matrix4d>, force: Boolean) {
        dst[name] = "Array<Matrix4x4d>" to values
    }

    override fun writeMatrix2x2dArray2D(name: String, values: Array<Array<Matrix2d>>, force: Boolean) {
        dst[name] = "Array<Array<Matrix2x2d>>" to values
    }

    override fun writeMatrix3x2dArray2D(name: String, values: Array<Array<Matrix3x2d>>, force: Boolean) {
        dst[name] = "Array<Array<Matrix3x2d>>" to values
    }

    override fun writeMatrix3x3dArray2D(name: String, values: Array<Array<Matrix3d>>, force: Boolean) {
        dst[name] = "Array<Array<Matrix3x3d>>" to values
    }

    override fun writeMatrix4x3dArray2D(name: String, values: Array<Array<Matrix4x3d>>, force: Boolean) {
        dst[name] = "Array<Array<Matrix4x3d>>" to values
    }

    override fun writeMatrix4x4dArray2D(name: String, values: Array<Array<Matrix4d>>, force: Boolean) {
        dst[name] = "Array<Array<Matrix4x4d>>" to values
    }

    override fun writeQuaternionf(name: String, value: Quaternionf, force: Boolean) {
        dst[name] = "Quaternion" to value
    }

    override fun writeQuaterniond(name: String, value: Quaterniond, force: Boolean) {
        dst[name] = "Quaterniond" to value
    }

    override fun writeQuaternionfArray(name: String, values: Array<Quaternionf>, force: Boolean) {
        dst[name] = "Array<Quaternion>" to values
    }

    override fun writeQuaternionfArray2D(name: String, values: Array<Array<Quaternionf>>, force: Boolean) {
        dst[name] = "Array<Array<Quaternion>>" to values
    }

    override fun writeQuaterniondArray(name: String, values: Array<Quaterniond>, force: Boolean) {
        dst[name] = "Array<Quaterniond>" to values
    }

    override fun writeQuaterniondArray2D(name: String, values: Array<Array<Quaterniond>>, force: Boolean) {
        dst[name] = "Array<Array<Quaterniond>>" to values
    }

    override fun writeAABBf(name: String, value: AABBf, force: Boolean) {
        dst[name] = "AABBf" to value
    }

    override fun writeAABBd(name: String, value: AABBd, force: Boolean) {
        dst[name] = "AABBd" to value
    }

    override fun writePlanef(name: String, value: Planef, force: Boolean) {
        dst[name] = "Planef" to value
    }

    override fun writePlaned(name: String, value: Planed, force: Boolean) {
        dst[name] = "Planed" to value
    }

    override fun writeFile(name: String, value: FileReference?, force: Boolean, workspace: FileReference?) {
        dst[name] = "FileReference" to value
    }

    override fun writeFileArray(name: String, values: Array<FileReference>, force: Boolean, workspace: FileReference?) {
        dst[name] = "Array<FileReference>" to values
    }

    override fun writeNull(name: String?) {
        name ?: return
        dst[name] = "null" to null
    }

    override fun writeObjectImpl(name: String?, value: ISaveable) {
        // maybe... idk...
    }

    override fun <V : ISaveable?> writeNullableObjectArray(
        self: ISaveable?,
        name: String,
        values: Array<V>?,
        force: Boolean
    ) {
        dst[name] = "Array<Object?>" to values
    }

    override fun <V : ISaveable> writeObjectArray(self: ISaveable?, name: String, values: Array<V>?, force: Boolean) {
        dst[name] = "Array<Object>" to values
    }

    override fun <V : ISaveable> writeObjectArray2D(
        self: ISaveable?,
        name: String,
        values: Array<Array<V>>,
        force: Boolean
    ) {
        dst[name] = "Array<Array<Object>>" to values
    }

    override fun <V : ISaveable?> writeHomogenousObjectArray(
        self: ISaveable?,
        name: String,
        values: Array<V>,
        force: Boolean
    ) {
        dst[name] = "Array<Array<Object>>" to values
    }

}