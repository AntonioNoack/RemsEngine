package me.anno.io.find

import me.anno.io.ISaveable
import me.anno.io.files.FileReference
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

/**
 * collects all written properties including their type;
 * is used in the editor for ISaveables that are not Inspectables
 * */
class DetectiveWriter(val dst: HashMap<String, Pair<String, Any?>>) : PartialWriter(false) {

    private fun put(name: String, type: String, value: Any?) {
        dst[name] = type to value
    }

    override fun writeSomething(name: String, value: Any?, force: Boolean) {
        val type = if (value != null) value::class.simpleName!! else "?"
        put(name, type, value)
    }

    override fun writeBooleanArray2D(name: String, values: Array<BooleanArray>, force: Boolean) =
        put(name, "Array<BooleanArray>", values)

    override fun writeCharArray2D(name: String, values: Array<CharArray>, force: Boolean) =
        put(name, "Array<CharArray>", values)

    override fun writeByteArray2D(name: String, values: Array<ByteArray>, force: Boolean) =
        put(name, "Array<ByteArray>", values)

    override fun writeShortArray2D(name: String, values: Array<ShortArray>, force: Boolean) =
        put(name, "Array<ShortArray>", values)

    override fun writeIntArray2D(name: String, values: Array<IntArray>, force: Boolean) =
        put(name, "Array<IntArray>", values)

    override fun writeColor(name: String, value: Int, force: Boolean) =
        put(name, "Color4", value)

    override fun writeColorArray(name: String, values: IntArray, force: Boolean) =
        put(name, "Array<Color4>", values)

    override fun writeColorArray2D(name: String, values: Array<IntArray>, force: Boolean) =
        put(name, "Array<Array<Color4>>", values)

    override fun writeLongArray2D(name: String, values: Array<LongArray>, force: Boolean) =
        put(name, "Array<LongArray>", values)

    override fun writeFloatArray2D(name: String, values: Array<FloatArray>, force: Boolean) =
        put(name, "Array<FloatArray>", values)

    override fun writeDoubleArray2D(name: String, values: Array<DoubleArray>, force: Boolean) =
        put(name, "Array<DoubleArray>", values)

    override fun writeStringArray(name: String, values: Array<String>, force: Boolean) =
        put(name, "Array<String>", values)

    override fun writeStringArray2D(name: String, values: Array<Array<String>>, force: Boolean) =
        put(name, "Array<Array<String>>", values)

    override fun writeVector2fArray(name: String, values: Array<Vector2f>, force: Boolean) =
        put(name, "Array<Vector2f>", values)

    override fun writeVector3fArray(name: String, values: Array<Vector3f>, force: Boolean) =
        put(name, "Array<Vector3f>", values)

    override fun writeVector4fArray(name: String, values: Array<Vector4f>, force: Boolean) =
        put(name, "Array<Vector4f>", values)

    override fun writeVector2fArray2D(name: String, values: Array<Array<Vector2f>>, force: Boolean) =
        put(name, "Array<Array<Vector2f>>", values)

    override fun writeVector3fArray2D(name: String, values: Array<Array<Vector3f>>, force: Boolean) =
        put(name, "Array<Array<Vector3f>>", values)

    override fun writeVector4fArray2D(name: String, values: Array<Array<Vector4f>>, force: Boolean) =
        put(name, "Array<Array<Vector4f>>", values)

    override fun writeVector2dArray(name: String, values: Array<Vector2d>, force: Boolean) =
        put(name, "Array<Array<Vector2f>>", values)

    override fun writeVector3dArray(name: String, values: Array<Vector3d>, force: Boolean) =
        put(name, "Array<Array<Vector3f>>", values)

    override fun writeVector4dArray(name: String, values: Array<Vector4d>, force: Boolean) =
        put(name, "Array<Array<Vector4f>>", values)

    override fun writeVector2dArray2D(name: String, values: Array<Array<Vector2d>>, force: Boolean) =
        put(name, "Array<Array<Vector2d>>", values)

    override fun writeVector3dArray2D(name: String, values: Array<Array<Vector3d>>, force: Boolean) =
        put(name, "Array<Array<Vector3d>>", values)

    override fun writeVector4dArray2D(name: String, values: Array<Array<Vector4d>>, force: Boolean) =
        put(name, "Array<Array<Vector4d>>", values)

    override fun writeVector2i(name: String, value: Vector2i, force: Boolean) =
        put(name, "Vector2i", value)

    override fun writeVector3i(name: String, value: Vector3i, force: Boolean) =
        put(name, "Vector3i", value)

    override fun writeVector4i(name: String, value: Vector4i, force: Boolean) =
        put(name, "Vector4i", value)

    override fun writeVector2iArray(name: String, values: Array<Vector2i>, force: Boolean) =
        put(name, "Array<Vector2i>", values)

    override fun writeVector3iArray(name: String, values: Array<Vector3i>, force: Boolean) =
        put(name, "Array<Vector3i>", values)

    override fun writeVector4iArray(name: String, values: Array<Vector4i>, force: Boolean) =
        put(name, "Array<Vector4i>", values)

    override fun writeVector2iArray2D(name: String, values: Array<Array<Vector2i>>, force: Boolean) =
        put(name, "Array<Array<Vector2i>>", values)

    override fun writeVector3iArray2D(name: String, values: Array<Array<Vector3i>>, force: Boolean) =
        put(name, "Array<Array<Vector3i>>", values)

    override fun writeVector4iArray2D(name: String, values: Array<Array<Vector4i>>, force: Boolean) =
        put(name, "Array<Array<Vector4i>>", values)

    override fun writeMatrix2x2f(name: String, value: Matrix2f, force: Boolean) =
        put(name, "Matrix2x2", value)

    override fun writeMatrix3x2f(name: String, value: Matrix3x2f, force: Boolean) =
        put(name, "Matrix3x2", value)

    override fun writeMatrix3x3f(name: String, value: Matrix3f, force: Boolean) =
        put(name, "Matrix3x3", value)

    override fun writeMatrix4x3f(name: String, value: Matrix4x3f, force: Boolean) =
        put(name, "Matrix4x3", value)

    override fun writeMatrix4x4f(name: String, value: Matrix4f, force: Boolean) =
        put(name, "Matrix4x4", value)

    override fun writeMatrix2x2fArray(name: String, values: Array<Matrix2f>, force: Boolean) =
        put(name, "Array<Matrix2x2>", values)

    override fun writeMatrix3x2fArray(name: String, values: Array<Matrix3x2f>, force: Boolean) =
        put(name, "Array<Matrix3x2>", values)

    override fun writeMatrix3x3fArray(name: String, values: Array<Matrix3f>, force: Boolean) =
        put(name, "Array<Matrix3x3>", values)

    override fun writeMatrix4x3fArray(name: String, values: Array<Matrix4x3f>, force: Boolean) =
        put(name, "Array<Matrix4x3>", values)

    override fun writeMatrix4x4fArray(name: String, values: Array<Matrix4f>, force: Boolean) =
        put(name, "Array<Matrix4x4>", values)

    override fun writeMatrix2x2fArray2D(name: String, values: Array<Array<Matrix2f>>, force: Boolean) =
        put(name, "Array<Array<Matrix2x2>>", values)

    override fun writeMatrix3x2fArray2D(name: String, values: Array<Array<Matrix3x2f>>, force: Boolean) =
        put(name, "Array<Array<Matrix3x2>>", values)

    override fun writeMatrix3x3fArray2D(name: String, values: Array<Array<Matrix3f>>, force: Boolean) =
        put(name, "Array<Array<Matrix3x3>>", values)

    override fun writeMatrix4x3fArray2D(name: String, values: Array<Array<Matrix4x3f>>, force: Boolean) =
        put(name, "Array<Array<Matrix4x3>>", values)

    override fun writeMatrix4x4fArray2D(name: String, values: Array<Array<Matrix4f>>, force: Boolean) =
        put(name, "Array<Array<Matrix4x4>>", values)

    override fun writeMatrix2x2d(name: String, value: Matrix2d, force: Boolean) =
        put(name, "Matrix2x2d", value)

    override fun writeMatrix3x2d(name: String, value: Matrix3x2d, force: Boolean) =
        put(name, "Matrix3x2d", value)

    override fun writeMatrix3x3d(name: String, value: Matrix3d, force: Boolean) =
        put(name, "Matrix3x3d", value)

    override fun writeMatrix4x3d(name: String, value: Matrix4x3d, force: Boolean) =
        put(name, "Matrix4x3d", value)

    override fun writeMatrix4x4d(name: String, value: Matrix4d, force: Boolean) =
        put(name, "Matrix4x4d", value)

    override fun writeMatrix2x2dArray(name: String, values: Array<Matrix2d>, force: Boolean) =
        put(name, "Array<Matrix2x2d>", values)

    override fun writeMatrix3x2dArray(name: String, values: Array<Matrix3x2d>, force: Boolean) =
        put(name, "Array<Matrix3x2d>", values)

    override fun writeMatrix3x3dArray(name: String, values: Array<Matrix3d>, force: Boolean) =
        put(name, "Array<Matrix3x3d>", values)

    override fun writeMatrix4x3dArray(name: String, values: Array<Matrix4x3d>, force: Boolean) =
        put(name, "Array<Matrix4x3d>", values)

    override fun writeMatrix4x4dArray(name: String, values: Array<Matrix4d>, force: Boolean) =
        put(name, "Array<Matrix4x4d>", values)

    override fun writeMatrix2x2dArray2D(name: String, values: Array<Array<Matrix2d>>, force: Boolean) =
        put(name, "Array<Array<Matrix2x2d>>", values)

    override fun writeMatrix3x2dArray2D(name: String, values: Array<Array<Matrix3x2d>>, force: Boolean) =
        put(name, "Array<Array<Matrix3x2d>>", values)

    override fun writeMatrix3x3dArray2D(name: String, values: Array<Array<Matrix3d>>, force: Boolean) =
        put(name, "Array<Array<Matrix3x3d>>", values)

    override fun writeMatrix4x3dArray2D(name: String, values: Array<Array<Matrix4x3d>>, force: Boolean) =
        put(name, "Array<Array<Matrix4x3d>>", values)

    override fun writeMatrix4x4dArray2D(name: String, values: Array<Array<Matrix4d>>, force: Boolean) =
        put(name, "Array<Array<Matrix4x4d>>", values)

    override fun writeQuaternionfArray(name: String, values: Array<Quaternionf>, force: Boolean) =
        put(name, "Array<Quaternionf>", values)

    override fun writeQuaterniondArray(name: String, values: Array<Quaterniond>, force: Boolean) =
        put(name, "Array<Quaterniond>", values)

    override fun writeQuaternionfArray2D(name: String, values: Array<Array<Quaternionf>>, force: Boolean) =
        put(name, "Array<Array<Quaternionf>>", values)

    override fun writeQuaterniondArray2D(name: String, values: Array<Array<Quaterniond>>, force: Boolean) =
        put(name, "Array<Array<Quaterniond>>", values)

    override fun writeAABBfArray(name: String, values: Array<AABBf>, force: Boolean) =
        put(name, "Array<AABBf>", values)

    override fun writeAABBdArray(name: String, values: Array<AABBd>, force: Boolean) =
        put(name, "Array<AABBd>", values)

    override fun writeAABBfArray2D(name: String, values: Array<Array<AABBf>>, force: Boolean) =
        put(name, "Array<Array<AABBf>>", values)

    override fun writeAABBdArray2D(name: String, values: Array<Array<AABBd>>, force: Boolean) =
        put(name, "Array<Array<AABBd>>", values)

    override fun writePlanefArray(name: String, values: Array<Planef>, force: Boolean) =
        put(name, "Array<Planef>", values)

    override fun writePlanedArray(name: String, values: Array<Planed>, force: Boolean) =
        put(name, "Array<Planed>", values)

    override fun writePlanefArray2D(name: String, values: Array<Array<Planef>>, force: Boolean) =
        put(name, "Array<Array<Planef>>", values)

    override fun writePlanedArray2D(name: String, values: Array<Array<Planed>>, force: Boolean) =
        put(name, "Array<Array<Planed>>", values)

    override fun writeFileArray(name: String, values: Array<FileReference>, force: Boolean, workspace: FileReference) =
        put(name, "Array<FileReference>", values)

    override fun writeFileArray2D(
        name: String, values: Array<Array<FileReference>>,
        force: Boolean, workspace: FileReference
    ) = put(name, "Array<Array<FileReference>>", values)

    override fun writeNull(name: String?) {
        name ?: return
        put(name, "null", null)
    }

    override fun writeObjectImpl(name: String?, value: ISaveable) {
        // maybe... idk...
    }

    override fun <V : ISaveable?> writeNullableObjectArray(
        self: ISaveable?,
        name: String,
        values: Array<V>?,
        force: Boolean
    ) = put(name, "Array<Object?>", values)

    override fun <V : ISaveable> writeObjectArray(self: ISaveable?, name: String, values: Array<V>?, force: Boolean) =
        put(name, "Array<Object>", values)

    override fun <V : ISaveable> writeObjectArray2D(
        self: ISaveable?,
        name: String,
        values: Array<Array<V>>,
        force: Boolean
    ) = put(name, "Array<Array<Object>>", values)

    override fun <V : ISaveable?> writeHomogenousObjectArray(
        self: ISaveable?,
        name: String,
        values: Array<V>,
        force: Boolean
    ) = put(name, "Array<Array<Object>>", values)
}