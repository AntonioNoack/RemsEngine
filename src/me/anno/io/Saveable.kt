package me.anno.io

import me.anno.io.base.BaseWriter
import me.anno.io.text.TextWriter
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

abstract class Saveable {

    // for references from object to objects
    var uuid = 0L

    abstract fun getClassName(): String

    /**
     a guess, small objects shouldn't contain large ones
     (e.g. human containing building vs building containing human)
    **/
    abstract fun getApproxSize(): Int

    open fun save(writer: BaseWriter){
        writer.writeLong("uuid", uuid)
    }

    open fun onReadingStarted(){}
    open fun onReadingEnded(){}

    fun warnMissingParam(name: String) = println("Unknown param ${getClassName()}.$name")

    open fun readBool(name: String, value: Boolean) = readSomething(name, value)
    open fun readByte(name: String, value: Byte) = readSomething(name, value)
    open fun readShort(name: String, value: Short) = readSomething(name, value)
    open fun readInt(name: String, value: Int) = readSomething(name, value)
    open fun readFloat(name: String, value: Float) = readSomething(name, value)
    open fun readDouble(name: String, value: Double) = readSomething(name, value)
    open fun readLong(name: String, value: Long){
        when(name){
            "uuid" -> uuid = value
            else -> readSomething(name, value)
        }
    }
    open fun readIntArray(name: String, value: IntArray) = readSomething(name, value)
    open fun readString(name: String, value: String) = readSomething(name, value)
    open fun readArray(name: String, value: List<Saveable>) = readSomething(name, value)
    open fun readFloatArray(name: String, value: FloatArray) = readSomething(name, value)
    open fun readFloatArray2D(name: String, value: Array<FloatArray>) = readSomething(name, value)

    open fun readObject(name: String, value: Saveable?) = readSomething(name, value)

    open fun readVector2(name: String, value: Vector2f) = readSomething(name, value)
    open fun readVector3(name: String, value: Vector3f) = readSomething(name, value)
    open fun readVector4(name: String, value: Vector4f) = readSomething(name, value)

    open fun readSomething(name: String, value: Any?) = warnMissingParam(name)

    override fun toString(): String = TextWriter.toText(this, true)

}