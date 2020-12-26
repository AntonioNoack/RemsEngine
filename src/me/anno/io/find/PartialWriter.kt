package me.anno.io.find

import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f

/**
 * a class, that does nothing
 * */
abstract class PartialWriter(respectsDefaultValues: Boolean) : BaseWriter(respectsDefaultValues) {

    override fun writeBoolean(name: String, value: Boolean, force: Boolean) {}

    override fun writeByte(name: String, value: Byte, force: Boolean) {}

    override fun writeShort(name: String, value: Short, force: Boolean) {}

    override fun writeInt(name: String, value: Int, force: Boolean) {}

    override fun writeIntArray(name: String, value: IntArray, force: Boolean) {}

    override fun writeFloat(name: String, value: Float, force: Boolean) {}

    override fun writeFloatArray(name: String, value: FloatArray, force: Boolean) {}

    override fun writeDouble(name: String, value: Double, force: Boolean) {}

    override fun writeDoubleArray(name: String, value: DoubleArray, force: Boolean) {}

    override fun writeString(name: String, value: String?, force: Boolean) {}

    override fun writeLong(name: String, value: Long, force: Boolean) {}

    override fun writeLongArray(name: String, value: LongArray, force: Boolean) {}

    override fun writeVector2f(name: String, value: Vector2f, force: Boolean) {}

    override fun writeVector3f(name: String, value: Vector3f, force: Boolean) {}

    override fun writeVector4f(name: String, value: Vector4f, force: Boolean) {}

    override fun writeVector4d(name: String, value: Vector4d, force: Boolean) {}

    override fun writeNull(name: String?) {}

    override fun writePointer(name: String?, className: String, ptr: Int) {}

    override fun writeObjectImpl(name: String?, value: ISaveable) {
        value.save(this)
    }

    override fun <V : ISaveable> writeObjectArray(self: ISaveable?, name: String, elements: Array<V>, force: Boolean) {
        elements.forEach { it.save(this) }
    }

    override fun <V : ISaveable> writeHomogenousObjectArray(
        self: ISaveable?,
        name: String,
        elements: Array<V>,
        force: Boolean
    ) {
        elements.forEach { it.save(this) }
    }

    override fun writeVector2fArray(name: String, elements: Array<Vector2f>, force: Boolean) {}

    override fun writeVector3fArray(name: String, elements: Array<Vector3f>, force: Boolean) {}

    override fun writeVector4fArray(name: String, elements: Array<Vector4f>, force: Boolean) {}

    override fun writeListStart() {}

    override fun writeListEnd() {}

    override fun writeListSeparator() {}

    override fun writeBooleanArray(name: String, value: BooleanArray, force: Boolean) {}
    override fun writeByteArray(name: String, value: ByteArray, force: Boolean) {}
    override fun writeDoubleArray2D(name: String, value: Array<DoubleArray>, force: Boolean) {}
    override fun writeFloatArray2D(name: String, value: Array<FloatArray>, force: Boolean) {}
    override fun writeShortArray(name: String, value: ShortArray, force: Boolean) {}
    override fun writeStringArray(name: String, value: Array<String>, force: Boolean) {}

}