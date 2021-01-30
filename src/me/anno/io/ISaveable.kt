package me.anno.io

import me.anno.io.base.BaseWriter
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.*
import kotlin.collections.HashMap

interface ISaveable {

    fun getClassName(): String

    /**
    a guess, small objects shouldn't contain large ones
    (e.g. human containing building vs building containing human)
     **/ 
    fun getApproxSize(): Int

    fun save(writer: BaseWriter)

    fun onReadingStarted(){}
    fun onReadingEnded(){}

    fun readBoolean(name: String, value: Boolean)
    fun readBooleanArray(name: String, values: BooleanArray)

    fun readByte(name: String, value: Byte)
    fun readByteArray(name: String, values: ByteArray)

    fun readShort(name: String, value: Short)
    fun readShortArray(name: String, values: ShortArray)

    fun readInt(name: String, value: Int)
    fun readIntArray(name: String, values: IntArray)

    fun readLong(name: String, value: Long)
    fun readLongArray(name: String, values: LongArray)

    fun readFloat(name: String, value: Float)
    fun readFloatArray(name: String, values: FloatArray)
    fun readFloatArray2D(name: String, values: Array<FloatArray>)

    fun readDouble(name: String, value: Double)
    fun readDoubleArray(name: String, values: DoubleArray)
    fun readDoubleArray2D(name: String, values: Array<DoubleArray>)

    fun readString(name: String, value: String)
    fun readStringArray(name: String, values: Array<String>)

    fun readObject(name: String, value: ISaveable?)
    fun readObjectArray(name: String, values: Array<ISaveable?>)

    fun readVector2f(name: String, value: Vector2f)
    fun readVector2fArray(name: String, values: Array<Vector2f>)

    fun readVector3f(name: String, value: Vector3f)
    fun readVector3fArray(name: String, values: Array<Vector3f>)

    fun readVector4f(name: String, value: Vector4f)
    fun readVector4fArray(name: String, values: Array<Vector4f>)

    /**
     * can saving be ignored?, because this is default anyways?
     * */
    fun isDefaultValue(): Boolean

    companion object {

        val objectTypeRegistry = HashMap<String, () -> ISaveable>()

        @JvmStatic
        fun registerCustomClass(className: String, constructor: () -> ISaveable){
            objectTypeRegistry[className] = constructor
        }

    }

}