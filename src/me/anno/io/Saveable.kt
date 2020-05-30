package me.anno.io

import me.anno.io.base.BaseWriter
import me.anno.io.text.TextWriter
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

abstract class Saveable: ISaveable {

    // for references from object to objects
    override var uuid = 0L

    // abstract fun getClassName(): String
    // abstract fun getApproxSize(): Int

    override fun save(writer: BaseWriter){
        writer.writeLong("uuid", uuid)
    }

    override fun onReadingStarted(){}
    override fun onReadingEnded(){}

    fun warnMissingParam(name: String) = println("Unknown param ${getClassName()}.$name")

    override fun readBool(name: String, value: Boolean) = readSomething(name, value)
    override fun readByte(name: String, value: Byte) = readSomething(name, value)
    override fun readShort(name: String, value: Short) = readSomething(name, value)
    override fun readInt(name: String, value: Int) = readSomething(name, value)
    override fun readFloat(name: String, value: Float) = readSomething(name, value)
    override fun readDouble(name: String, value: Double) = readSomething(name, value)
    override fun readLong(name: String, value: Long){
        when(name){
            "uuid" -> uuid = value
            else -> readSomething(name, value)
        }
    }
    override fun readIntArray(name: String, value: IntArray) = readSomething(name, value)
    override fun readString(name: String, value: String) = readSomething(name, value)
    override fun readArray(name: String, value: List<Saveable>) = readSomething(name, value)
    override fun readFloatArray(name: String, value: FloatArray) = readSomething(name, value)
    override fun readFloatArray2D(name: String, value: Array<FloatArray>) = readSomething(name, value)

    override fun readObject(name: String, value: Saveable?) = readSomething(name, value)

    override fun readVector2(name: String, value: Vector2f) = readSomething(name, value)
    override fun readVector3(name: String, value: Vector3f) = readSomething(name, value)
    override fun readVector4(name: String, value: Vector4f) = readSomething(name, value)

    open fun readSomething(name: String, value: Any?) = warnMissingParam(name)

    override fun toString(): String = TextWriter.toText(this, true)

}