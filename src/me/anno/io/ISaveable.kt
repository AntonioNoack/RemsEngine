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
    fun readVector3f(name: String, value: Vector3f)
    fun readVector4f(name: String, value: Vector4f)

    fun readVector2fArray(name: String, values: Array<Vector2f>)
    fun readVector3fArray(name: String, values: Array<Vector3f>)
    fun readVector4fArray(name: String, values: Array<Vector4f>)

    fun readVector2d(name: String, value: Vector2d)
    fun readVector3d(name: String, value: Vector3d)
    fun readVector4d(name: String, value: Vector4d)

    fun readVector2dArray(name: String, values: Array<Vector2d>)
    fun readVector3dArray(name: String, values: Array<Vector3d>)
    fun readVector4dArray(name: String, values: Array<Vector4d>)

    fun readVector2i(name: String, value: Vector2i)
    fun readVector3i(name: String, value: Vector3i)
    fun readVector4i(name: String, value: Vector4i)

    fun readVector2iArray(name: String, values: Array<Vector2i>)
    fun readVector3iArray(name: String, values: Array<Vector3i>)
    fun readVector4iArray(name: String, values: Array<Vector4i>)


    // read matrices
    // array versions? idk...
    fun readMatrix3x3f(name: String, value: Matrix3f)
    fun readMatrix4x3f(name: String, value: Matrix4x3f)
    fun readMatrix4x4f(name: String, value: Matrix4f)

    fun readMatrix3x3d(name: String, value: Matrix3d)
    fun readMatrix4x3d(name: String, value: Matrix4x3d)
    fun readMatrix4x4d(name: String, value: Matrix4d)

    fun readQuaternionf(name: String, value: Quaternionf)
    fun readQuaterniond(name: String, value: Quaterniond)

    /**
     * can saving be ignored?, because this is default anyways?
     * */
    fun isDefaultValue(): Boolean

    /**
     * tries to insert value into all properties with matching name
     * returns true on success
     * */
    fun readSerializableProperty(name: String, value: Any?): Boolean {
        val reflections = getReflections()
        return reflections.set(this, name, value)
    }

    fun saveSerializableProperties(writer: BaseWriter) {
        val reflections = getReflections()
        for ((name, field) in reflections.properties) {
            val value = field.getter.call(this)
            // todo if the type is explicitely given, however not deductable (empty array), and the saving is forced,
            // todo use the field.type
            writer.writeSomething(this, name, value, field.forceSaving ?: value is Boolean)
        }
    }

    fun getReflections(): CachedReflections {
        val clazz = this::class
        return reflectionCache.getOrPut(clazz) { CachedReflections(this, clazz) }
    }

    companion object {

        private val reflectionCache = ConcurrentHashMap<KClass<*>, CachedReflections>()

        class RegistryEntry(val sampleInstance: ISaveable, val generator: () -> ISaveable) {
            constructor(generator: () -> ISaveable) : this(generator(), generator)

            fun generate() = generator()
        }

        val objectTypeRegistry = HashMap<String, RegistryEntry>()

        @JvmStatic
        fun registerCustomClass(className: String, constructor: () -> ISaveable) {
            objectTypeRegistry[className] = RegistryEntry(constructor)
        }

        @JvmStatic
        fun registerCustomClass(instance0: ISaveable) {
            val className = instance0.className
            val constructor = instance0.javaClass
            objectTypeRegistry[className] = RegistryEntry(instance0) { constructor.newInstance() }
        }

        @JvmStatic
        fun registerCustomClass(constructor: () -> ISaveable) {
            val instance0 = constructor()
            val className = instance0.className
            objectTypeRegistry[className] = RegistryEntry(instance0, constructor)
        }

        @JvmStatic
        fun registerCustomClass(clazz: Class<ISaveable>) {
            val constructor = clazz.getConstructor()
            val instance0 = constructor.newInstance()
            val className = instance0.className
            objectTypeRegistry[className] = RegistryEntry(instance0) { constructor.newInstance() }
        }

        @JvmStatic
        fun registerCustomClass(className: String, clazz: Class<ISaveable>) {
            val constructor = clazz.getConstructor()
            objectTypeRegistry[className] = RegistryEntry { constructor.newInstance() }
        }

    }

}