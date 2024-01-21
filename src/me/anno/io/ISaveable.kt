package me.anno.io

import me.anno.Build
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.inspector.CachedReflections
import me.anno.io.base.BaseWriter
import me.anno.io.base.UnknownClassException
import me.anno.io.files.FileReference
import me.anno.utils.OS
import me.anno.utils.structures.lists.Lists.firstOrNull2
import org.apache.logging.log4j.LogManager
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.reflect.KClass

/**
 * interface for everything that should be saveable;
 * please use Saveable.class, if possible
 * */
interface ISaveable {

    /**
     * write all data, which needs to be recovered, to the writer
     * */
    fun save(writer: BaseWriter) {}

    fun onReadingStarted() {}
    fun onReadingEnded() {}

    private fun warnMissingParam(name: String) {
        if (name == "*ptr") throw RuntimeException()
        LogManager.getLogger(Saveable::class).warn("Unknown param $className.$name")
    }

    fun readBoolean(name: String, value: Boolean) = readSomething(name, value)
    fun readBooleanArray(name: String, values: BooleanArray) = readSomething(name, values)
    fun readBooleanArray2D(name: String, values: Array<BooleanArray>) = readSomething(name, values)

    fun readByte(name: String, value: Byte) = readSomething(name, value)
    fun readByteArray(name: String, values: ByteArray) = readSomething(name, values)
    fun readByteArray2D(name: String, values: Array<ByteArray>) = readSomething(name, values)

    fun readChar(name: String, value: Char) = readSomething(name, value)
    fun readCharArray(name: String, values: CharArray) = readSomething(name, values)
    fun readCharArray2D(name: String, values: Array<CharArray>) = readSomething(name, values)

    fun readShort(name: String, value: Short) = readSomething(name, value)
    fun readShortArray(name: String, values: ShortArray) = readSomething(name, values)
    fun readShortArray2D(name: String, values: Array<ShortArray>) = readSomething(name, values)

    fun readInt(name: String, value: Int) = readSomething(name, value)
    fun readIntArray(name: String, values: IntArray) = readSomething(name, values)
    fun readIntArray2D(name: String, values: Array<IntArray>) = readSomething(name, values)

    fun readLong(name: String, value: Long) = readSomething(name, value)
    fun readLongArray(name: String, values: LongArray) = readSomething(name, values)
    fun readLongArray2D(name: String, values: Array<LongArray>) = readSomething(name, values)

    fun readFloat(name: String, value: Float) = readSomething(name, value)
    fun readFloatArray(name: String, values: FloatArray) = readSomething(name, values)
    fun readFloatArray2D(name: String, values: Array<FloatArray>) = readSomething(name, values)

    fun readDouble(name: String, value: Double) = readSomething(name, value)
    fun readDoubleArray(name: String, values: DoubleArray) = readSomething(name, values)
    fun readDoubleArray2D(name: String, values: Array<DoubleArray>) = readSomething(name, values)

    fun readString(name: String, value: String) = readSomething(name, value)
    fun readStringArray(name: String, values: Array<String>) = readSomething(name, values)
    fun readStringArray2D(name: String, values: Array<Array<String>>) = readSomething(name, values)

    fun readFile(name: String, value: FileReference) = readSomething(name, value)
    fun readFileArray(name: String, values: Array<FileReference>) = readSomething(name, values)
    fun readFileArray2D(name: String, values: Array<Array<FileReference>>) = readSomething(name, values)

    fun readObject(name: String, value: ISaveable?) = readSomething(name, value)
    fun readObjectArray(name: String, values: Array<ISaveable?>) = readSomething(name, values)
    fun readObjectArray2D(name: String, values: Array<Array<ISaveable?>>) = readSomething(name, values)

    fun readVector2f(name: String, value: Vector2f) = readSomething(name, value)
    fun readVector2fArray(name: String, values: Array<Vector2f>) = readSomething(name, values)
    fun readVector2fArray2D(name: String, values: Array<Array<Vector2f>>) = readSomething(name, values)

    fun readVector3f(name: String, value: Vector3f) = readSomething(name, value)
    fun readVector3fArray(name: String, values: Array<Vector3f>) = readSomething(name, values)
    fun readVector3fArray2D(name: String, values: Array<Array<Vector3f>>) = readSomething(name, values)

    fun readVector4f(name: String, value: Vector4f) = readSomething(name, value)
    fun readVector4fArray(name: String, values: Array<Vector4f>) = readSomething(name, values)
    fun readVector4fArray2D(name: String, values: Array<Array<Vector4f>>) = readSomething(name, values)

    fun readVector2d(name: String, value: Vector2d) = readSomething(name, value)
    fun readVector2dArray(name: String, values: Array<Vector2d>) = readSomething(name, values)
    fun readVector2dArray2D(name: String, values: Array<Array<Vector2d>>) = readSomething(name, values)

    fun readVector3d(name: String, value: Vector3d) = readSomething(name, value)
    fun readVector3dArray(name: String, values: Array<Vector3d>) = readSomething(name, values)
    fun readVector3dArray2D(name: String, values: Array<Array<Vector3d>>) = readSomething(name, values)

    fun readVector4d(name: String, value: Vector4d) = readSomething(name, value)
    fun readVector4dArray(name: String, values: Array<Vector4d>) = readSomething(name, values)
    fun readVector4dArray2D(name: String, values: Array<Array<Vector4d>>) = readSomething(name, values)

    fun readVector2i(name: String, value: Vector2i) = readSomething(name, value)
    fun readVector2iArray(name: String, values: Array<Vector2i>) = readSomething(name, values)
    fun readVector2iArray2D(name: String, values: Array<Array<Vector2i>>) = readSomething(name, values)

    fun readVector3i(name: String, value: Vector3i) = readSomething(name, value)
    fun readVector3iArray(name: String, values: Array<Vector3i>) = readSomething(name, values)
    fun readVector3iArray2D(name: String, values: Array<Array<Vector3i>>) = readSomething(name, values)

    fun readVector4i(name: String, value: Vector4i) = readSomething(name, value)
    fun readVector4iArray(name: String, values: Array<Vector4i>) = readSomething(name, values)
    fun readVector4iArray2D(name: String, values: Array<Array<Vector4i>>) = readSomething(name, values)

    fun readMatrix2x2f(name: String, value: Matrix2f) = readSomething(name, value)
    fun readMatrix3x2f(name: String, value: Matrix3x2f) = readSomething(name, value)
    fun readMatrix3x3f(name: String, value: Matrix3f) = readSomething(name, value)
    fun readMatrix4x3f(name: String, value: Matrix4x3f) = readSomething(name, value)
    fun readMatrix4x4f(name: String, value: Matrix4f) = readSomething(name, value)
    fun readMatrix2x2fArray(name: String, values: Array<Matrix2f>) = readSomething(name, values)
    fun readMatrix3x2fArray(name: String, values: Array<Matrix3x2f>) = readSomething(name, values)
    fun readMatrix3x3fArray(name: String, values: Array<Matrix3f>) = readSomething(name, values)
    fun readMatrix4x3fArray(name: String, values: Array<Matrix4x3f>) = readSomething(name, values)
    fun readMatrix4x4fArray(name: String, values: Array<Matrix4f>) = readSomething(name, values)
    fun readMatrix2x2fArray2D(name: String, values: Array<Array<Matrix2f>>) = readSomething(name, values)
    fun readMatrix3x2fArray2D(name: String, values: Array<Array<Matrix3x2f>>) = readSomething(name, values)
    fun readMatrix3x3fArray2D(name: String, values: Array<Array<Matrix3f>>) = readSomething(name, values)
    fun readMatrix4x3fArray2D(name: String, values: Array<Array<Matrix4x3f>>) = readSomething(name, values)
    fun readMatrix4x4fArray2D(name: String, values: Array<Array<Matrix4f>>) = readSomething(name, values)

    fun readMatrix2x2d(name: String, value: Matrix2d) = readSomething(name, value)
    fun readMatrix3x2d(name: String, value: Matrix3x2d) = readSomething(name, value)
    fun readMatrix3x3d(name: String, value: Matrix3d) = readSomething(name, value)
    fun readMatrix4x3d(name: String, value: Matrix4x3d) = readSomething(name, value)
    fun readMatrix4x4d(name: String, value: Matrix4d) = readSomething(name, value)
    fun readMatrix2x2dArray(name: String, values: Array<Matrix2d>) = readSomething(name, values)
    fun readMatrix3x2dArray(name: String, values: Array<Matrix3x2d>) = readSomething(name, values)
    fun readMatrix3x3dArray(name: String, values: Array<Matrix3d>) = readSomething(name, values)
    fun readMatrix4x3dArray(name: String, values: Array<Matrix4x3d>) = readSomething(name, values)
    fun readMatrix4x4dArray(name: String, values: Array<Matrix4d>) = readSomething(name, values)
    fun readMatrix2x2dArray2D(name: String, values: Array<Array<Matrix2d>>) = readSomething(name, values)
    fun readMatrix3x2dArray2D(name: String, values: Array<Array<Matrix3x2d>>) = readSomething(name, values)
    fun readMatrix3x3dArray2D(name: String, values: Array<Array<Matrix3d>>) = readSomething(name, values)
    fun readMatrix4x3dArray2D(name: String, values: Array<Array<Matrix4x3d>>) = readSomething(name, values)
    fun readMatrix4x4dArray2D(name: String, values: Array<Array<Matrix4d>>) = readSomething(name, values)

    fun readQuaternionf(name: String, value: Quaternionf) = readSomething(name, value)
    fun readQuaternionfArray(name: String, values: Array<Quaternionf>) = readSomething(name, values)
    fun readQuaternionfArray2D(name: String, values: Array<Array<Quaternionf>>) = readSomething(name, values)

    fun readQuaterniond(name: String, value: Quaterniond) = readSomething(name, value)
    fun readQuaterniondArray(name: String, values: Array<Quaterniond>) = readSomething(name, values)
    fun readQuaterniondArray2D(name: String, values: Array<Array<Quaterniond>>) = readSomething(name, values)

    fun readAABBf(name: String, value: AABBf) = readSomething(name, value)
    fun readAABBfArray(name: String, values: Array<AABBf>) = readSomething(name, values)
    fun readAABBfArray2D(name: String, values: Array<Array<AABBf>>) = readSomething(name, values)
    fun readAABBd(name: String, value: AABBd) = readSomething(name, value)
    fun readAABBdArray(name: String, values: Array<AABBd>) = readSomething(name, values)
    fun readAABBdArray2D(name: String, values: Array<Array<AABBd>>) = readSomething(name, values)

    fun readPlanef(name: String, value: Planef) = readSomething(name, value)
    fun readPlanefArray(name: String, values: Array<Planef>) = readSomething(name, values)
    fun readPlanefArray2D(name: String, values: Array<Array<Planef>>) = readSomething(name, values)
    fun readPlaned(name: String, value: Planed) = readSomething(name, value)
    fun readPlanedArray(name: String, values: Array<Planed>) = readSomething(name, values)
    fun readPlanedArray2D(name: String, values: Array<Array<Planed>>) = readSomething(name, values)

    fun readMap(name: String, value: Map<Any?, Any?>) = readSomething(name, value)

    fun readSomething(name: String, value: Any?) = warnMissingParam(name)

    /**
     * class id for saving instances of this class
     * needs to be unique for that class, and needs to be registered
     * */
    val className: String
        get() {
            val clazz = this::class
            return clazz.simpleName ?: clazz.toString()
        }

    /**
     * a guess, small objects shouldn't contain large ones
     * (e.g., human containing building vs building containing human)
     * */
    val approxSize: Int get() = 100

    /**
     * can saving be ignored?, because this is default anyway?
     * */
    fun isDefaultValue(): Boolean = false

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
        for ((name, field) in reflections.allProperties) {
            if (field.serialize) {
                val value = field.getter(this)
                val type = (field.annotations.firstOrNull2 { it is Type } as? Type)?.type
                if (type != null) {
                    writer.writeSomething(this, type, name, value, field.forceSaving ?: (value is Boolean))
                } else {
                    writer.writeSomething(this, name, value, field.forceSaving ?: (value is Boolean))
                }
            }
        }
    }

    fun getReflections(): CachedReflections {
        val clazz = this::class
        return reflectionCache.getOrPut(clazz) { CachedReflections(clazz) }
    }

    operator fun get(propertyName: String): Any? {
        return getReflections()[this, propertyName]
    }

    operator fun set(propertyName: String, value: Any?): Boolean {
        val reflections = getReflections()
        return reflections.set(this, propertyName, value)
    }

    companion object {

        private val LOGGER = LogManager.getLogger(ISaveable::class)
        private val reflectionCache: MutableMap<KClass<*>, CachedReflections> =
            if (OS.isWeb) HashMap()
            else ConcurrentHashMap()

        fun getReflections(instance: Any): CachedReflections {
            val clazz = instance::class
            return reflectionCache.getOrPut(clazz) { CachedReflections(clazz) }
        }

        fun get(instance: Any, name: String): Any? {
            return getReflections(instance)[instance, name]
        }

        fun set(instance: Any, name: String, value: Any?): Boolean {
            val reflections = getReflections(instance)
            return reflections.set(instance, name, value)
        }

        class RegistryEntry(
            val sampleInstance: ISaveable,
            private val generator: (() -> ISaveable)? = null
        ) {
            private val clazz = sampleInstance.javaClass
            fun generate(): ISaveable = generator?.invoke() ?: clazz.newInstance()
        }

        fun createOrNull(type: String): ISaveable? {
            return objectTypeRegistry[type]?.generate()
        }

        fun create(type: String): ISaveable {
            return objectTypeRegistry[type]?.generate() ?: throw UnknownClassException(type)
        }

        fun getSample(type: String) = objectTypeRegistry[type]?.sampleInstance

        fun getClass(type: String): Class<out ISaveable>? {
            val instance = objectTypeRegistry[type]?.sampleInstance ?: return superTypeRegistry[type]
            return instance.javaClass
        }

        fun getByClass(clazz: KClass<*>): RegistryEntry? {
            return objectTypeByClass[clazz]
        }

        fun getByClass(clazz: Class<*>): RegistryEntry? {
            return objectTypeByClass[clazz]
        }

        fun <V : ISaveable> getInstanceOf(clazz: KClass<V>): Map<String, RegistryEntry> {
            return objectTypeRegistry.filterValues { clazz.isInstance(it.sampleInstance) }
        }

        val objectTypeRegistry = HashMap<String, RegistryEntry>()
        private val objectTypeByClass = HashMap<Any, RegistryEntry>()
        private val superTypeRegistry = HashMap<String, Class<out ISaveable>>()

        fun checkInstance(instance0: ISaveable) {
            if (Build.isDebug && instance0 is PrefabSaveable) {
                val clone = try {
                    instance0.clone()
                } catch (ignored: Exception) {
                    null
                }
                if (clone != null && clone::class != instance0::class) {
                    throw RuntimeException("${instance0::class}.clone() is incorrect, returns ${clone::class} instead")
                }
            }
        }

        fun registerSuperClasses(clazz0: Class<out ISaveable>) {
            var clazz = clazz0
            while (true) {
                superTypeRegistry[clazz.simpleName] = clazz
                @Suppress("unchecked_cast")
                clazz = (clazz.superclass ?: break) as Class<out ISaveable>
            }
        }

        @JvmStatic
        fun registerCustomClass(className: String, constructor: () -> ISaveable): RegistryEntry {
            val instance0 = constructor()
            checkInstance(instance0)
            return register(className, RegistryEntry(instance0, constructor))
        }

        @JvmStatic
        fun registerCustomClass(sample: ISaveable): RegistryEntry {
            checkInstance(sample)
            val className = sample.className
            return if (sample is PrefabSaveable) {
                register(className, RegistryEntry(sample) { sample.clone() })
            } else {
                register(className, RegistryEntry(sample))
            }
        }

        @JvmStatic
        fun registerCustomClass(constructor: () -> ISaveable): RegistryEntry {
            val instance0 = constructor()
            val className = instance0.className
            val entry = register(className, RegistryEntry(instance0, constructor))
            // dangerous to be done after
            // but this allows us to skip the full implementation of clone() everywhere
            checkInstance(instance0)
            return entry
        }

        @JvmStatic
        fun <V : ISaveable> registerCustomClass(clazz: KClass<V>): RegistryEntry {
            return registerCustomClass(null as String?, clazz)
        }

        @JvmStatic
        fun <V : ISaveable> registerCustomClass(className: String?, clazz: KClass<V>): RegistryEntry {
            val constructor = clazz.java.getConstructor()
            val sample = try {
                constructor.newInstance()
            } catch (e: InstantiationException) {
                throw IllegalArgumentException("$clazz is missing constructor without parameters", e)
            }
            checkInstance(sample)
            return register(className ?: sample.className, RegistryEntry(sample))
        }

        private fun register(className: String, entry: RegistryEntry): RegistryEntry {
            val clazz = entry.sampleInstance::class
            val oldInstance = objectTypeRegistry[className]?.sampleInstance
            if (oldInstance != null && oldInstance::class != clazz) {
                LOGGER.warn(
                    "Overriding registered class {} from type {} with {}",
                    className, oldInstance::class, clazz
                )
            }
            LOGGER.info("Registering {}", className)
            objectTypeRegistry[className] = entry
            objectTypeByClass[clazz] = entry
            objectTypeByClass[clazz.java] = entry
            registerSuperClasses(entry.sampleInstance.javaClass)
            return entry
        }
    }
}