package me.anno.io.find

import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import org.joml.*

/**
 * a BaseWriter, with the default behaviour of ignoring everything;
 * this is used in Rem's Studio as a way to detect references (whether a pointer needs to be added as such to a file)
 * */
abstract class PartialWriter(canSkipDefaultValues: Boolean) : BaseWriter(canSkipDefaultValues) {

    override fun writeBoolean(name: String, value: Boolean, force: Boolean) {}
    override fun writeBooleanArray(name: String, values: BooleanArray, force: Boolean) {}
    override fun writeBooleanArray2D(name: String, values: Array<BooleanArray>, force: Boolean) {}

    override fun writeChar(name: String, value: Char, force: Boolean) {}
    override fun writeCharArray(name: String, values: CharArray, force: Boolean) {}
    override fun writeCharArray2D(name: String, values: Array<CharArray>, force: Boolean) {}

    override fun writeByte(name: String, value: Byte, force: Boolean) {}
    override fun writeByteArray(name: String, values: ByteArray, force: Boolean) {}
    override fun writeByteArray2D(name: String, values: Array<ByteArray>, force: Boolean) {}

    override fun writeShort(name: String, value: Short, force: Boolean) {}
    override fun writeShortArray(name: String, values: ShortArray, force: Boolean) {}
    override fun writeShortArray2D(name: String, values: Array<ShortArray>, force: Boolean) {}

    override fun writeInt(name: String, value: Int, force: Boolean) {}
    override fun writeIntArray(name: String, values: IntArray, force: Boolean) {}
    override fun writeIntArray2D(name: String, values: Array<IntArray>, force: Boolean) {}

    override fun writeColor(name: String, value: Int, force: Boolean) {}
    override fun writeColorArray(name: String, values: IntArray, force: Boolean) {}
    override fun writeColorArray2D(name: String, values: Array<IntArray>, force: Boolean) {}

    override fun writeLong(name: String, value: Long, force: Boolean) {}
    override fun writeLongArray(name: String, values: LongArray, force: Boolean) {}
    override fun writeLongArray2D(name: String, values: Array<LongArray>, force: Boolean) {}

    override fun writeFloat(name: String, value: Float, force: Boolean) {}
    override fun writeFloatArray(name: String, values: FloatArray?, force: Boolean) {}
    override fun writeFloatArray2D(name: String, values: Array<FloatArray>?, force: Boolean) {}

    override fun writeDouble(name: String, value: Double, force: Boolean) {}
    override fun writeDoubleArray(name: String, values: DoubleArray, force: Boolean) {}
    override fun writeDoubleArray2D(name: String, values: Array<DoubleArray>, force: Boolean) {}

    override fun writeString(name: String, value: String?, force: Boolean) {}
    override fun writeStringArray(name: String, values: Array<String>, force: Boolean) {}
    override fun writeStringArray2D(name: String, values: Array<Array<String>>, force: Boolean) {}

    override fun writeFile(name: String, value: FileReference?, force: Boolean, workspace: FileReference?) {}
    override fun writeFileArray(name: String, values: Array<FileReference>, force: Boolean, workspace: FileReference?) {
    }

    override fun writeVector2f(name: String, value: Vector2fc, force: Boolean) {}
    override fun writeVector3f(name: String, value: Vector3fc, force: Boolean) {}
    override fun writeVector4f(name: String, value: Vector4fc, force: Boolean) {}

    override fun writeVector2fArray(name: String, values: Array<Vector2f>, force: Boolean) {}
    override fun writeVector3fArray(name: String, values: Array<Vector3f>, force: Boolean) {}
    override fun writeVector4fArray(name: String, values: Array<Vector4f>, force: Boolean) {}

    override fun writeVector2d(name: String, value: Vector2dc, force: Boolean) {}
    override fun writeVector3d(name: String, value: Vector3dc, force: Boolean) {}
    override fun writeVector4d(name: String, value: Vector4dc, force: Boolean) {}

    override fun writeVector2dArray(name: String, values: Array<Vector2d>, force: Boolean) {}
    override fun writeVector3dArray(name: String, values: Array<Vector3d>, force: Boolean) {}
    override fun writeVector4dArray(name: String, values: Array<Vector4d>, force: Boolean) {}

    override fun writeMatrix2x2f(name: String, value: Matrix2fc, force: Boolean) {}
    override fun writeMatrix3x2f(name: String, value: Matrix3x2fc, force: Boolean) {}
    override fun writeMatrix3x3f(name: String, value: Matrix3fc, force: Boolean) {}
    override fun writeMatrix4x3f(name: String, value: Matrix4x3fc, force: Boolean) {}
    override fun writeMatrix4x4f(name: String, value: Matrix4fc, force: Boolean) {}

    override fun writeMatrix2x2fArray(name: String, values: Array<Matrix2fc>, force: Boolean) {}
    override fun writeMatrix3x2fArray(name: String, values: Array<Matrix3x2fc>, force: Boolean) {}
    override fun writeMatrix3x3fArray(name: String, values: Array<Matrix3fc>, force: Boolean) {}
    override fun writeMatrix4x3fArray(name: String, values: Array<Matrix4x3fc>, force: Boolean) {}
    override fun writeMatrix4x4fArray(name: String, values: Array<Matrix4fc>, force: Boolean) {}

    override fun writeMatrix2x2fArray2D(name: String, values: Array<Array<Matrix2fc>>, force: Boolean) {}
    override fun writeMatrix3x2fArray2D(name: String, values: Array<Array<Matrix3x2fc>>, force: Boolean) {}
    override fun writeMatrix3x3fArray2D(name: String, values: Array<Array<Matrix3fc>>, force: Boolean) {}
    override fun writeMatrix4x3fArray2D(name: String, values: Array<Array<Matrix4x3fc>>, force: Boolean) {}
    override fun writeMatrix4x4fArray2D(name: String, values: Array<Array<Matrix4fc>>, force: Boolean) {}

    override fun writeMatrix2x2d(name: String, value: Matrix2dc, force: Boolean) {}
    override fun writeMatrix3x2d(name: String, value: Matrix3x2dc, force: Boolean) {}
    override fun writeMatrix3x3d(name: String, value: Matrix3dc, force: Boolean) {}
    override fun writeMatrix4x3d(name: String, value: Matrix4x3dc, force: Boolean) {}
    override fun writeMatrix4x4d(name: String, value: Matrix4dc, force: Boolean) {}

    override fun writeMatrix2x2dArray(name: String, values: Array<Matrix2dc>, force: Boolean) {}
    override fun writeMatrix3x2dArray(name: String, values: Array<Matrix3x2dc>, force: Boolean) {}
    override fun writeMatrix3x3dArray(name: String, values: Array<Matrix3dc>, force: Boolean) {}
    override fun writeMatrix4x3dArray(name: String, values: Array<Matrix4x3dc>, force: Boolean) {}
    override fun writeMatrix4x4dArray(name: String, values: Array<Matrix4dc>, force: Boolean) {}

    override fun writeMatrix2x2dArray2D(name: String, values: Array<Array<Matrix2dc>>, force: Boolean) {}
    override fun writeMatrix3x2dArray2D(name: String, values: Array<Array<Matrix3x2dc>>, force: Boolean) {}
    override fun writeMatrix3x3dArray2D(name: String, values: Array<Array<Matrix3dc>>, force: Boolean) {}
    override fun writeMatrix4x3dArray2D(name: String, values: Array<Array<Matrix4x3dc>>, force: Boolean) {}
    override fun writeMatrix4x4dArray2D(name: String, values: Array<Array<Matrix4dc>>, force: Boolean) {}

    override fun writeQuaternionf(name: String, value: Quaternionf, force: Boolean) {}
    override fun writeQuaterniond(name: String, value: Quaterniond, force: Boolean) {}

    override fun writeVector2i(name: String, value: Vector2ic, force: Boolean) {}
    override fun writeVector3i(name: String, value: Vector3ic, force: Boolean) {}
    override fun writeVector4i(name: String, value: Vector4ic, force: Boolean) {}

    override fun writeVector2iArray(name: String, values: Array<Vector2i>, force: Boolean) {}
    override fun writeVector3iArray(name: String, values: Array<Vector3i>, force: Boolean) {}
    override fun writeVector4iArray(name: String, values: Array<Vector4i>, force: Boolean) {}

    override fun writeQuaternionfArray(name: String, values: Array<Quaternionf>, force: Boolean) {}
    override fun writeQuaternionfArray2D(name: String, values: Array<Array<Quaternionf>>, force: Boolean) {}

    override fun writeQuaterniondArray(name: String, values: Array<Quaterniond>, force: Boolean) {}
    override fun writeQuaterniondArray2D(name: String, values: Array<Array<Quaterniond>>, force: Boolean) {}

    override fun writeAABBf(name: String, value: AABBf, force: Boolean) {}
    override fun writeAABBd(name: String, value: AABBd, force: Boolean) {}

    override fun writeNull(name: String?) {}
    override fun writePointer(name: String?, className: String, ptr: Int, value: ISaveable) {}

    override fun writePlanef(name: String, value: Planef, force: Boolean) {}
    override fun writePlaned(name: String, value: Planed, force: Boolean) {}

    override fun writeObjectImpl(name: String?, value: ISaveable) {
        value.save(this)
    }

    override fun <V : ISaveable> writeObjectArray(self: ISaveable?, name: String, values: Array<V>?, force: Boolean) {
        values ?: return
        for (obj in values) obj.save(this)
    }

    override fun <V : ISaveable> writeObjectArray2D(
        self: ISaveable?,
        name: String,
        values: Array<Array<V>>,
        force: Boolean
    ) {
        for (objects in values) {
            for (obj in objects) {
                obj.save(this)
            }
        }
    }

    override fun <V : ISaveable?> writeNullableObjectArray(
        self: ISaveable?,
        name: String,
        values: Array<V>?,
        force: Boolean
    ) {
        values?.forEach { it?.save(this) }
    }

    override fun <V : ISaveable?> writeHomogenousObjectArray(
        self: ISaveable?,
        name: String,
        values: Array<V>,
        force: Boolean
    ) {
        values.forEach { it?.save(this) }
    }

    override fun writeListStart() {}
    override fun writeListEnd() {}
    override fun writeListSeparator() {}


}