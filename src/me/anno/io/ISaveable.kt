package me.anno.io

import me.anno.io.base.BaseWriter
import me.anno.io.serialization.CachedReflections
import org.joml.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.reflect.KClass

interface ISaveable {

    /**
     * class id for saving instances of this class
     * needs to be unique for that class, and needs to be registered
     * */
    val className: String

    /**
     * a guess, small objects shouldn't contain large ones
     * (e.g. human containing building vs building containing human)
     * */
    val approxSize: Int

    /**
     * write all data, which needs to be recovered, to the writer
     * */
    fun save(writer: BaseWriter)

    fun onReadingStarted() {}
    fun onReadingEnded() {}

    fun readBoolean(name: String, value: Boolean)
    fun readBooleanArray(name: String, values: BooleanArray)
    fun readBooleanArray2D(name: String, values: Array<BooleanArray>)

    fun readByte(name: String, value: Byte)
    fun readByteArray(name: String, values: ByteArray)
    fun readByteArray2D(name: String, values: Array<ByteArray>)

    fun readShort(name: String, value: Short)
    fun readShortArray(name: String, values: ShortArray)
    fun readShortArray2D(name: String, values: Array<ShortArray>)

    fun readInt(name: String, value: Int)
    fun readIntArray(name: String, values: IntArray)
    fun readIntArray2D(name: String, values: Array<IntArray>)

    fun readLong(name: String, value: Long)
    fun readLongArray(name: String, values: LongArray)
    fun readLongArray2D(name: String, values: Array<LongArray>)

    fun readFloat(name: String, value: Float)
    fun readFloatArray(name: String, values: FloatArray)
    fun readFloatArray2D(name: String, values: Array<FloatArray>)

    fun readDouble(name: String, value: Double)
    fun readDoubleArray(name: String, values: DoubleArray)
    fun readDoubleArray2D(name: String, values: Array<DoubleArray>)

    fun readString(name: String, value: String)
    fun readStringArray(name: String, values: Array<String>)
    fun readStringArray2D(name: String, values: Array<Array<String>>)

    fun readObject(name: String, value: ISaveable?)
    fun readObjectArray(name: String, values: Array<ISaveable?>)
    fun readObjectArray2D(name: String, values: Array<Array<ISaveable?>>)

    fun readVector2f(name: String, value: Vector2f)
    fun readVector2fArray(name: String, values: Array<Vector2f>)

    fun readVector3f(name: String, value: Vector3f)
    fun readVector3fArray(name: String, values: Array<Vector3f>)

    fun readVector4f(name: String, value: Vector4f)
    fun readVector4fArray(name: String, values: Array<Vector4f>)

    fun readVector2d(name: String, value: Vector2d)
    fun readVector2dArray(name: String, values: Array<Vector2d>)

    fun readVector3d(name: String, value: Vector3d)
    fun readVector3dArray(name: String, values: Array<Vector3d>)

    fun readVector4d(name: String, value: Vector4d)
    fun readVector4dArray(name: String, values: Array<Vector4d>)

    // read matrices
    // array versions? idk...
    fun readMatrix3x3f(name: String, value: Matrix3f)
    fun readMatrix4x3f(name: String, value: Matrix4x3f)
    fun readMatrix4x4f(name: String, value: Matrix4f)

    fun readMatrix3x3d(name: String, value: Matrix3d)
    fun readMatrix4x3d(name: String, value: Matrix4x3d)
    fun readMatrix4x4d(name: String, value: Matrix4d)

    /**
     * can saving be ignored?, because this is default anyways?
     * */
    fun isDefaultValue(): Boolean

    /**
     * tries to insert value into all properties with matching name
     * returns true on success
     * */
    fun readSerializableProperty(name: String, value: Any?): Boolean {
        val clazz = this::class
        val reflections = reflectionCache.getOrPut(clazz) { CachedReflections(clazz) }
        return reflections.set(this, name, value)
    }

    fun saveSerializableProperties(writer: BaseWriter) {
        val clazz = this::class
        val reflections = reflectionCache.getOrPut(clazz) { CachedReflections(clazz) }
        for ((name, field) in reflections.properties) {
            val value = field.getter.call(name)
            // todo if the type is explicitely given, however not deductable (empty array), and the saving is forced,
            // todo use the field.type
            writer.writeSomething(this, name, value, field.forceSaving ?: value is Boolean)
        }
    }

    companion object {

        val reflectionCache = ConcurrentHashMap<KClass<*>, CachedReflections>()

        val objectTypeRegistry = HashMap<String, () -> ISaveable>()

        @JvmStatic
        fun registerCustomClass(className: String, constructor: () -> ISaveable) {
            objectTypeRegistry[className] = constructor
        }

    }

}