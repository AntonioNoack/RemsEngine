package me.anno.io.find

import me.anno.io.saveable.Saveable
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

    override fun writeBooleanArray2D(name: String, values: List<BooleanArray>, force: Boolean) =
        put(name, "List<BooleanArray>", values)

    override fun writeCharArray2D(name: String, values: List<CharArray>, force: Boolean) =
        put(name, "List<CharArray>", values)

    override fun writeByteArray2D(name: String, values: List<ByteArray>, force: Boolean) =
        put(name, "List<ByteArray>", values)

    override fun writeShortArray2D(name: String, values: List<ShortArray>, force: Boolean) =
        put(name, "List<ShortArray>", values)

    override fun writeIntArray2D(name: String, values: List<IntArray>, force: Boolean) =
        put(name, "List<IntArray>", values)

    override fun writeColor(name: String, value: Int, force: Boolean) =
        put(name, "Color4", value)

    override fun writeColorArray(name: String, values: IntArray, force: Boolean) =
        put(name, "List<Color4>", values)

    override fun writeColorArray2D(name: String, values: List<IntArray>, force: Boolean) =
        put(name, "List<List<Color4>>", values)

    override fun writeLongArray2D(name: String, values: List<LongArray>, force: Boolean) =
        put(name, "List<LongArray>", values)

    override fun writeFloatArray2D(name: String, values: List<FloatArray>, force: Boolean) =
        put(name, "List<FloatArray>", values)

    override fun writeDoubleArray2D(name: String, values: List<DoubleArray>, force: Boolean) =
        put(name, "List<DoubleArray>", values)

    override fun writeStringList(name: String, values: List<String>, force: Boolean) =
        put(name, "List<String>", values)

    override fun writeStringList2D(name: String, values: List<List<String>>, force: Boolean) =
        put(name, "List<List<String>>", values)

    override fun writeVector2fList(name: String, values: List<Vector2f>, force: Boolean) =
        put(name, "List<Vector2f>", values)

    override fun writeVector3fList(name: String, values: List<Vector3f>, force: Boolean) =
        put(name, "List<Vector3f>", values)

    override fun writeVector4fList(name: String, values: List<Vector4f>, force: Boolean) =
        put(name, "List<Vector4f>", values)

    override fun writeVector2fList2D(name: String, values: List<List<Vector2f>>, force: Boolean) =
        put(name, "List<List<Vector2f>>", values)

    override fun writeVector3fList2D(name: String, values: List<List<Vector3f>>, force: Boolean) =
        put(name, "List<List<Vector3f>>", values)

    override fun writeVector4fList2D(name: String, values: List<List<Vector4f>>, force: Boolean) =
        put(name, "List<List<Vector4f>>", values)

    override fun writeVector2dList(name: String, values: List<Vector2d>, force: Boolean) =
        put(name, "List<List<Vector2f>>", values)

    override fun writeVector3dList(name: String, values: List<Vector3d>, force: Boolean) =
        put(name, "List<List<Vector3f>>", values)

    override fun writeVector4dList(name: String, values: List<Vector4d>, force: Boolean) =
        put(name, "List<List<Vector4f>>", values)

    override fun writeVector2dList2D(name: String, values: List<List<Vector2d>>, force: Boolean) =
        put(name, "List<List<Vector2d>>", values)

    override fun writeVector3dList2D(name: String, values: List<List<Vector3d>>, force: Boolean) =
        put(name, "List<List<Vector3d>>", values)

    override fun writeVector4dList2D(name: String, values: List<List<Vector4d>>, force: Boolean) =
        put(name, "List<List<Vector4d>>", values)

    override fun writeVector2i(name: String, value: Vector2i, force: Boolean) =
        put(name, "Vector2i", value)

    override fun writeVector3i(name: String, value: Vector3i, force: Boolean) =
        put(name, "Vector3i", value)

    override fun writeVector4i(name: String, value: Vector4i, force: Boolean) =
        put(name, "Vector4i", value)

    override fun writeVector2iList(name: String, values: List<Vector2i>, force: Boolean) =
        put(name, "List<Vector2i>", values)

    override fun writeVector3iList(name: String, values: List<Vector3i>, force: Boolean) =
        put(name, "List<Vector3i>", values)

    override fun writeVector4iList(name: String, values: List<Vector4i>, force: Boolean) =
        put(name, "List<Vector4i>", values)

    override fun writeVector2iList2D(name: String, values: List<List<Vector2i>>, force: Boolean) =
        put(name, "List<List<Vector2i>>", values)

    override fun writeVector3iList2D(name: String, values: List<List<Vector3i>>, force: Boolean) =
        put(name, "List<List<Vector3i>>", values)

    override fun writeVector4iList2D(name: String, values: List<List<Vector4i>>, force: Boolean) =
        put(name, "List<List<Vector4i>>", values)

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

    override fun writeMatrix2x2fList(name: String, values: List<Matrix2f>, force: Boolean) =
        put(name, "List<Matrix2x2>", values)

    override fun writeMatrix3x2fList(name: String, values: List<Matrix3x2f>, force: Boolean) =
        put(name, "List<Matrix3x2>", values)

    override fun writeMatrix3x3fList(name: String, values: List<Matrix3f>, force: Boolean) =
        put(name, "List<Matrix3x3>", values)

    override fun writeMatrix4x3fList(name: String, values: List<Matrix4x3f>, force: Boolean) =
        put(name, "List<Matrix4x3>", values)

    override fun writeMatrix4x4fList(name: String, values: List<Matrix4f>, force: Boolean) =
        put(name, "List<Matrix4x4>", values)

    override fun writeMatrix2x2fList2D(name: String, values: List<List<Matrix2f>>, force: Boolean) =
        put(name, "List<List<Matrix2x2>>", values)

    override fun writeMatrix3x2fList2D(name: String, values: List<List<Matrix3x2f>>, force: Boolean) =
        put(name, "List<List<Matrix3x2>>", values)

    override fun writeMatrix3x3fList2D(name: String, values: List<List<Matrix3f>>, force: Boolean) =
        put(name, "List<List<Matrix3x3>>", values)

    override fun writeMatrix4x3fList2D(name: String, values: List<List<Matrix4x3f>>, force: Boolean) =
        put(name, "List<List<Matrix4x3>>", values)

    override fun writeMatrix4x4fList2D(name: String, values: List<List<Matrix4f>>, force: Boolean) =
        put(name, "List<List<Matrix4x4>>", values)

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

    override fun writeMatrix2x2dList(name: String, values: List<Matrix2d>, force: Boolean) =
        put(name, "List<Matrix2x2d>", values)

    override fun writeMatrix3x2dList(name: String, values: List<Matrix3x2d>, force: Boolean) =
        put(name, "List<Matrix3x2d>", values)

    override fun writeMatrix3x3dList(name: String, values: List<Matrix3d>, force: Boolean) =
        put(name, "List<Matrix3x3d>", values)

    override fun writeMatrix4x3dList(name: String, values: List<Matrix4x3d>, force: Boolean) =
        put(name, "List<Matrix4x3d>", values)

    override fun writeMatrix4x4dList(name: String, values: List<Matrix4d>, force: Boolean) =
        put(name, "List<Matrix4x4d>", values)

    override fun writeMatrix2x2dList2D(name: String, values: List<List<Matrix2d>>, force: Boolean) =
        put(name, "List<List<Matrix2x2d>>", values)

    override fun writeMatrix3x2dList2D(name: String, values: List<List<Matrix3x2d>>, force: Boolean) =
        put(name, "List<List<Matrix3x2d>>", values)

    override fun writeMatrix3x3dList2D(name: String, values: List<List<Matrix3d>>, force: Boolean) =
        put(name, "List<List<Matrix3x3d>>", values)

    override fun writeMatrix4x3dList2D(name: String, values: List<List<Matrix4x3d>>, force: Boolean) =
        put(name, "List<List<Matrix4x3d>>", values)

    override fun writeMatrix4x4dList2D(name: String, values: List<List<Matrix4d>>, force: Boolean) =
        put(name, "List<List<Matrix4x4d>>", values)

    override fun writeQuaternionfList(name: String, values: List<Quaternionf>, force: Boolean) =
        put(name, "List<Quaternionf>", values)

    override fun writeQuaterniondList(name: String, values: List<Quaterniond>, force: Boolean) =
        put(name, "List<Quaterniond>", values)

    override fun writeQuaternionfList2D(name: String, values: List<List<Quaternionf>>, force: Boolean) =
        put(name, "List<List<Quaternionf>>", values)

    override fun writeQuaterniondList2D(name: String, values: List<List<Quaterniond>>, force: Boolean) =
        put(name, "List<List<Quaterniond>>", values)

    override fun writeAABBfList(name: String, values: List<AABBf>, force: Boolean) =
        put(name, "List<AABBf>", values)

    override fun writeAABBdList(name: String, values: List<AABBd>, force: Boolean) =
        put(name, "List<AABBd>", values)

    override fun writeAABBfList2D(name: String, values: List<List<AABBf>>, force: Boolean) =
        put(name, "List<List<AABBf>>", values)

    override fun writeAABBdList2D(name: String, values: List<List<AABBd>>, force: Boolean) =
        put(name, "List<List<AABBd>>", values)

    override fun writePlanefList(name: String, values: List<Planef>, force: Boolean) =
        put(name, "List<Planef>", values)

    override fun writePlanedList(name: String, values: List<Planed>, force: Boolean) =
        put(name, "List<Planed>", values)

    override fun writePlanefList2D(name: String, values: List<List<Planef>>, force: Boolean) =
        put(name, "List<List<Planef>>", values)

    override fun writePlanedList2D(name: String, values: List<List<Planed>>, force: Boolean) =
        put(name, "List<List<Planed>>", values)

    override fun writeFileList(name: String, values: List<FileReference>, force: Boolean) =
        put(name, "List<FileReference>", values)

    override fun writeFileList2D(name: String, values: List<List<FileReference>>, force: Boolean) =
        put(name, "List<List<FileReference>>", values)

    override fun writeNull(name: String?) {
        if (name != null) {
            put(name, "null", null)
        }
    }

    override fun writeObjectImpl(name: String?, value: Saveable) {
        // maybe... idk...
    }

    override fun <V : Saveable?> writeNullableObjectList(
        self: Saveable?, name: String,
        values: List<V>, force: Boolean
    ) = put(name, "List<Object?>", values)

    override fun <V : Saveable> writeObjectList(self: Saveable?, name: String, values: List<V>, force: Boolean) =
        put(name, "List<Object>", values)

    override fun <V : Saveable> writeObjectList2D(
        self: Saveable?,
        name: String,
        values: List<List<V>>,
        force: Boolean
    ) = put(name, "List<List<Object>>", values)

    override fun <V : Saveable?> writeHomogenousObjectList(
        self: Saveable?,
        name: String,
        values: List<V>,
        force: Boolean
    ) = put(name, "List<List<Object>>", values)
}