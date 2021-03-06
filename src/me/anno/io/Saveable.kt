package me.anno.io

import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.text.TextWriter
import me.anno.studio.StudioBase
import org.apache.logging.log4j.LogManager
import org.joml.*

open class Saveable : ISaveable {

    override fun save(writer: BaseWriter) {}

    override fun onReadingStarted() {}
    override fun onReadingEnded() {}

    private fun warnMissingParam(name: String) {
        if (name == "*ptr") throw RuntimeException()
        LogManager.getLogger(Saveable::class).warn("Unknown param $className.$name")
    }

    override fun readBoolean(name: String, value: Boolean) = readSomething(name, value)
    override fun readBooleanArray(name: String, values: BooleanArray) = readSomething(name, values)
    override fun readBooleanArray2D(name: String, values: Array<BooleanArray>) = readSomething(name, values)

    override fun readByte(name: String, value: Byte) = readSomething(name, value)
    override fun readByteArray(name: String, values: ByteArray) = readSomething(name, values)
    override fun readByteArray2D(name: String, values: Array<ByteArray>) = readSomething(name, values)

    override fun readChar(name: String, value: Char) = readSomething(name, value)
    override fun readCharArray(name: String, values: CharArray) = readSomething(name, values)
    override fun readCharArray2D(name: String, values: Array<CharArray>) = readSomething(name, values)

    override fun readShort(name: String, value: Short) = readSomething(name, value)
    override fun readShortArray(name: String, values: ShortArray) = readSomething(name, values)
    override fun readShortArray2D(name: String, values: Array<ShortArray>) = readSomething(name, values)

    override fun readInt(name: String, value: Int) = readSomething(name, value)
    override fun readIntArray(name: String, values: IntArray) = readSomething(name, values)
    override fun readIntArray2D(name: String, values: Array<IntArray>) = readSomething(name, values)

    override fun readLong(name: String, value: Long) = readSomething(name, value)
    override fun readLongArray(name: String, values: LongArray) = readSomething(name, values)
    override fun readLongArray2D(name: String, values: Array<LongArray>) = readSomething(name, values)

    override fun readFloat(name: String, value: Float) = readSomething(name, value)
    override fun readFloatArray(name: String, values: FloatArray) = readSomething(name, values)
    override fun readFloatArray2D(name: String, values: Array<FloatArray>) = readSomething(name, values)

    override fun readDouble(name: String, value: Double) = readSomething(name, value)
    override fun readDoubleArray(name: String, values: DoubleArray) = readSomething(name, values)
    override fun readDoubleArray2D(name: String, values: Array<DoubleArray>) = readSomething(name, values)

    override fun readString(name: String, value: String?) = readSomething(name, value)
    override fun readStringArray(name: String, values: Array<String>) = readSomething(name, values)
    override fun readStringArray2D(name: String, values: Array<Array<String>>) = readSomething(name, values)

    override fun readFile(name: String, value: FileReference) = readSomething(name, value)
    override fun readFileArray(name: String, values: Array<FileReference>) = readSomething(name, values)
    override fun readFileArray2D(name: String, values: Array<Array<FileReference>>) = readSomething(name, values)

    override fun readObject(name: String, value: ISaveable?) = readSomething(name, value)
    override fun readObjectArray(name: String, values: Array<ISaveable?>) = readSomething(name, values)
    override fun readObjectArray2D(name: String, values: Array<Array<ISaveable?>>) = readSomething(name, values)

    override fun readVector2f(name: String, value: Vector2f) = readSomething(name, value)
    override fun readVector2fArray(name: String, values: Array<Vector2f>) = readSomething(name, values)
    override fun readVector2fArray2D(name: String, values: Array<Array<Vector2f>>) = readSomething(name, values)

    override fun readVector3f(name: String, value: Vector3f) = readSomething(name, value)
    override fun readVector3fArray(name: String, values: Array<Vector3f>) = readSomething(name, values)
    override fun readVector3fArray2D(name: String, values: Array<Array<Vector3f>>) = readSomething(name, values)

    override fun readVector4f(name: String, value: Vector4f) = readSomething(name, value)
    override fun readVector4fArray(name: String, values: Array<Vector4f>) = readSomething(name, values)
    override fun readVector4fArray2D(name: String, values: Array<Array<Vector4f>>) = readSomething(name, values)

    override fun readVector2d(name: String, value: Vector2d) = readSomething(name, value)
    override fun readVector2dArray(name: String, values: Array<Vector2d>) = readSomething(name, values)
    override fun readVector2dArray2D(name: String, values: Array<Array<Vector2d>>) = readSomething(name, values)

    override fun readVector3d(name: String, value: Vector3d) = readSomething(name, value)
    override fun readVector3dArray(name: String, values: Array<Vector3d>) = readSomething(name, values)
    override fun readVector3dArray2D(name: String, values: Array<Array<Vector3d>>) = readSomething(name, values)

    override fun readVector4d(name: String, value: Vector4d) = readSomething(name, value)
    override fun readVector4dArray(name: String, values: Array<Vector4d>) = readSomething(name, values)
    override fun readVector4dArray2D(name: String, values: Array<Array<Vector4d>>) = readSomething(name, values)

    override fun readVector2i(name: String, value: Vector2i) = readSomething(name, value)
    override fun readVector2iArray(name: String, values: Array<Vector2i>) = readSomething(name, values)
    override fun readVector2iArray2D(name: String, values: Array<Array<Vector2i>>) = readSomething(name, values)

    override fun readVector3i(name: String, value: Vector3i) = readSomething(name, value)
    override fun readVector3iArray(name: String, values: Array<Vector3i>) = readSomething(name, values)
    override fun readVector3iArray2D(name: String, values: Array<Array<Vector3i>>) = readSomething(name, values)

    override fun readVector4i(name: String, value: Vector4i) = readSomething(name, value)
    override fun readVector4iArray(name: String, values: Array<Vector4i>) = readSomething(name, values)
    override fun readVector4iArray2D(name: String, values: Array<Array<Vector4i>>) = readSomething(name, values)

    override fun readMatrix2x2f(name: String, value: Matrix2f) = readSomething(name, value)
    override fun readMatrix3x2f(name: String, value: Matrix3x2f) = readSomething(name, value)
    override fun readMatrix3x3f(name: String, value: Matrix3f) = readSomething(name, value)
    override fun readMatrix4x3f(name: String, value: Matrix4x3f) = readSomething(name, value)
    override fun readMatrix4x4f(name: String, value: Matrix4f) = readSomething(name, value)
    override fun readMatrix2x2fArray(name: String, values: Array<Matrix2f>) = readSomething(name, values)
    override fun readMatrix3x2fArray(name: String, values: Array<Matrix3x2f>) = readSomething(name, values)
    override fun readMatrix3x3fArray(name: String, values: Array<Matrix3f>) = readSomething(name, values)
    override fun readMatrix4x3fArray(name: String, values: Array<Matrix4x3f>) = readSomething(name, values)
    override fun readMatrix4x4fArray(name: String, values: Array<Matrix4f>) = readSomething(name, values)
    override fun readMatrix2x2fArray2D(name: String, values: Array<Array<Matrix2f>>) = readSomething(name, values)
    override fun readMatrix3x2fArray2D(name: String, values: Array<Array<Matrix3x2f>>) = readSomething(name, values)
    override fun readMatrix3x3fArray2D(name: String, values: Array<Array<Matrix3f>>) = readSomething(name, values)
    override fun readMatrix4x3fArray2D(name: String, values: Array<Array<Matrix4x3f>>) = readSomething(name, values)
    override fun readMatrix4x4fArray2D(name: String, values: Array<Array<Matrix4f>>) = readSomething(name, values)

    override fun readMatrix2x2d(name: String, value: Matrix2d) = readSomething(name, value)
    override fun readMatrix3x2d(name: String, value: Matrix3x2d) = readSomething(name, value)
    override fun readMatrix3x3d(name: String, value: Matrix3d) = readSomething(name, value)
    override fun readMatrix4x3d(name: String, value: Matrix4x3d) = readSomething(name, value)
    override fun readMatrix4x4d(name: String, value: Matrix4d) = readSomething(name, value)
    override fun readMatrix2x2dArray(name: String, values: Array<Matrix2d>) = readSomething(name, values)
    override fun readMatrix3x2dArray(name: String, values: Array<Matrix3x2d>) = readSomething(name, values)
    override fun readMatrix3x3dArray(name: String, values: Array<Matrix3d>) = readSomething(name, values)
    override fun readMatrix4x3dArray(name: String, values: Array<Matrix4x3d>) = readSomething(name, values)
    override fun readMatrix4x4dArray(name: String, values: Array<Matrix4d>) = readSomething(name, values)
    override fun readMatrix2x2dArray2D(name: String, values: Array<Array<Matrix2d>>) = readSomething(name, values)
    override fun readMatrix3x2dArray2D(name: String, values: Array<Array<Matrix3x2d>>) = readSomething(name, values)
    override fun readMatrix3x3dArray2D(name: String, values: Array<Array<Matrix3d>>) = readSomething(name, values)
    override fun readMatrix4x3dArray2D(name: String, values: Array<Array<Matrix4x3d>>) = readSomething(name, values)
    override fun readMatrix4x4dArray2D(name: String, values: Array<Array<Matrix4d>>) = readSomething(name, values)

    override fun readQuaternionf(name: String, value: Quaternionf) = readSomething(name, value)
    override fun readQuaternionfArray(name: String, values: Array<Quaternionf>) = readSomething(name, values)
    override fun readQuaternionfArray2D(name: String, values: Array<Array<Quaternionf>>) = readSomething(name, values)

    override fun readQuaterniond(name: String, value: Quaterniond) = readSomething(name, value)
    override fun readQuaterniondArray(name: String, values: Array<Quaterniond>) = readSomething(name, values)
    override fun readQuaterniondArray2D(name: String, values: Array<Array<Quaterniond>>) = readSomething(name, values)

    override fun readAABBf(name: String, value: AABBf) = readSomething(name, value)
    override fun readAABBd(name: String, value: AABBd) = readSomething(name, value)

    override fun readPlanef(name: String, value: Planef) = readSomething(name, value)
    override fun readPlaned(name: String, value: Planed) = readSomething(name, value)

    override fun readMap(name: String, value: Map<Any?, Any?>) = readSomething(name, value)

    open fun readSomething(name: String, value: Any?) = warnMissingParam(name)

    override fun isDefaultValue(): Boolean = false
    override val approxSize: Int = 100
    override val className: String = javaClass.simpleName

    override fun toString(): String = TextWriter.toText(this, StudioBase.workspace)// + "@${super.toString()}"

}