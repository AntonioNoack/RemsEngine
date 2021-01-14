package me.anno.io

import me.anno.io.base.BaseWriter
import me.anno.io.text.TextWriter
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

abstract class Saveable : ISaveable {

    override fun save(writer: BaseWriter) {}

    override fun onReadingStarted() {}
    override fun onReadingEnded() {}

    private fun warnMissingParam(name: String) {
        if (name == "*ptr") throw RuntimeException()
        LogManager.getLogger(Saveable::class).warn("Unknown param ${getClassName()}.$name")
    }

    override fun readBoolean(name: String, value: Boolean) = readSomething(name, value)
    override fun readBooleanArray(name: String, value: BooleanArray) = readSomething(name, value)

    override fun readByte(name: String, value: Byte) = readSomething(name, value)
    override fun readByteArray(name: String, value: ByteArray) = readSomething(name, value)

    override fun readShort(name: String, value: Short) = readSomething(name, value)
    override fun readShortArray(name: String, value: ShortArray) = readSomething(name, value)

    override fun readInt(name: String, value: Int) = readSomething(name, value)
    override fun readIntArray(name: String, value: IntArray) = readSomething(name, value)

    override fun readFloat(name: String, value: Float) = readSomething(name, value)
    override fun readFloatArray(name: String, value: FloatArray) = readSomething(name, value)
    override fun readFloatArray2D(name: String, value: Array<FloatArray>) = readSomething(name, value)

    override fun readDouble(name: String, value: Double) = readSomething(name, value)
    override fun readDoubleArray(name: String, value: DoubleArray) = readSomething(name, value)
    override fun readDoubleArray2D(name: String, value: Array<DoubleArray>) = readSomething(name, value)

    override fun readLong(name: String, value: Long) = readSomething(name, value)
    override fun readLongArray(name: String, value: LongArray) = readSomething(name, value)

    override fun readString(name: String, value: String) = readSomething(name, value)
    override fun readStringArray(name: String, value: Array<String>) = readSomething(name, value)

    override fun readObject(name: String, value: ISaveable?) = readSomething(name, value)
    override fun readObjectArray(name: String, value: Array<ISaveable>) = readSomething(name, value)

    override fun readVector2f(name: String, value: Vector2f) = readSomething(name, value)
    override fun readVector2fArray(name: String, value: Array<Vector2f>) = readSomething(name, value)
    override fun readVector3f(name: String, value: Vector3f) = readSomething(name, value)
    override fun readVector3fArray(name: String, value: Array<Vector3f>) = readSomething(name, value)
    override fun readVector4f(name: String, value: Vector4f) = readSomething(name, value)
    override fun readVector4fArray(name: String, value: Array<Vector4f>) = readSomething(name, value)

    open fun readSomething(name: String, value: Any?) = warnMissingParam(name)

    override fun toString(): String = TextWriter.toText(this, true)// + "@${super.toString()}"

}