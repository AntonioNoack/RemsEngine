package me.anno.io

import me.anno.io.base.BaseWriter
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

interface ISaveable {

    // for references from object to objects
    var uuid: Long

    fun getClassName(): String

    /**
    a guess, small objects shouldn't contain large ones
    (e.g. human containing building vs building containing human)
     **/ 
    fun getApproxSize(): Int

    fun save(writer: BaseWriter)

    fun onReadingStarted(){}
    fun onReadingEnded(){}

    fun readBool(name: String, value: Boolean)
    fun readByte(name: String, value: Byte)
    fun readShort(name: String, value: Short)
    fun readInt(name: String, value: Int)
    fun readFloat(name: String, value: Float)
    fun readDouble(name: String, value: Double)
    fun readLong(name: String, value: Long)

    fun readIntArray(name: String, value: IntArray)
    fun readString(name: String, value: String)
    fun readArray(name: String, value: List<ISaveable>)
    fun readFloatArray(name: String, value: FloatArray)
    fun readFloatArray2D(name: String, value: Array<FloatArray>)

    fun readObject(name: String, value: ISaveable?)

    fun readVector2(name: String, value: Vector2f)
    fun readVector3(name: String, value: Vector3f)
    fun readVector4(name: String, value: Vector4f)

    /**
     * is not guaranteed to contain no nulls, even if is in file like that;
     * reader must wait for onReadingEnded()
     * */
    fun readObjectList(name: String, values: List<ISaveable?>)

    companion object {

        val objectTypeRegistry = HashMap<String, () -> ISaveable>()

        @JvmStatic
        fun registerCustomClass(className: String, constructor: () -> ISaveable){
            objectTypeRegistry[className] = constructor
        }

    }

}