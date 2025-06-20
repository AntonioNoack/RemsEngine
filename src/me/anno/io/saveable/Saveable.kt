package me.anno.io.saveable

import me.anno.Build
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.EngineBase
import me.anno.engine.inspector.CachedReflections
import me.anno.io.base.BaseWriter
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.utils.structures.lists.Lists.firstOrNull2
import org.apache.logging.log4j.LogManager
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * everything that should be saveable
 *
 * you just need to override setProperty() as a loader and store them in save(writer: BaseWriter)
 * if you want to use an easier, but likely slower path, you can use readSerializedProperty() and saveSerializableProperties()
 * */
open class Saveable {

    /**
     * write all data, which needs to be recovered, to the writer
     * */
    open fun save(writer: BaseWriter) {}

    open fun onReadingStarted() {}
    open fun onReadingEnded() {}

    private fun warnMissingParam(name: String) {
        LOGGER.warn("Unknown param '$className'.'$name'")
    }

    open fun setProperty(name: String, value: Any?) {
        warnMissingParam(name)
    }

    /**
     * class id for saving instances of this class
     * needs to be unique for that class, and needs to be registered
     * */
    open val className: String
        get() {
            val clazz = this.javaClass
            return clazz.simpleName ?: clazz.toString()
        }

    /**
     * a guess, small objects shouldn't contain large ones
     * (e.g., human containing building vs building containing human)
     * */
    open val approxSize: Int get() = 100

    /**
     * can saving be ignored?, because this is default anyway?
     * */
    open fun isDefaultValue(): Boolean = false

    /**
     * tries to insert value into all properties with matching name
     * returns true on success
     * */
    fun setSerializableProperty(name: String, value: Any?): Boolean {
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
        return getReflections(this)
    }

    open operator fun get(propertyName: String): Any? {
        return getReflections()[this, propertyName]
    }

    open operator fun set(propertyName: String, value: Any?): Boolean {
        val reflections = getReflections()
        return reflections.set(this, propertyName, value)
    }

    override fun toString(): String {
        return JsonStringWriter.toText(this, EngineBase.workspace)
    }

    open fun clone(): Saveable {
        val workspace = InvalidRef
        val asText = JsonStringWriter.toText(this, workspace)
        return JsonStringReader.readFirst(asText, workspace, this::class, safely = true)
    }

    companion object {

        private val LOGGER = LogManager.getLogger(Saveable::class)
        private val reflectionCache: MutableMap<Class<*>, CachedReflections> = ConcurrentHashMap()

        fun getReflections(instance: Any): CachedReflections {
            return getReflectionsByClass(instance::class.java)
        }

        fun getReflectionsByClass(clazz: Class<*>): CachedReflections {
            return reflectionCache.getOrPut(clazz) { CachedReflections(clazz) }
        }

        interface IRegistryEntry {
            val sampleInstance: Saveable
            val classPath: String
            fun generate(): Saveable
        }

        class RegistryEntry(
            override val sampleInstance: Saveable,
            private val generator: (() -> Saveable)? = null
        ) : IRegistryEntry {
            private val clazz = sampleInstance.javaClass
            override val classPath: String get() = clazz.name
            override fun generate(): Saveable = generator?.invoke() ?: clazz.newInstance()
        }

        fun createOrNull(type: String): Saveable? {
            return try {
                objectTypeRegistry[type]?.generate()
            } catch (e: Throwable) {
                LOGGER.warn("Failed to create $type", e)
                null
            }
        }

        fun create(type: String): Saveable {
            return objectTypeRegistry[type]?.generate() ?: UnknownSaveable(type)
        }

        fun getSample(type: String) = objectTypeRegistry[type]?.sampleInstance

        fun getClass(type: String): KClass<out Saveable> {
            val instance = objectTypeRegistry[type]?.sampleInstance
            return if (instance != null) instance::class else UnknownSaveable::class
        }

        fun isRegistered(type: String): Boolean {
            return type in objectTypeRegistry
        }

        fun getByClass(clazz: KClass<*>): IRegistryEntry? {
            return objectTypeByClass[clazz.java.name]
        }

        fun <V : Saveable> getInstanceOf(clazz: KClass<V>): Map<String, IRegistryEntry> {
            return objectTypeRegistry.filterValues {
                clazz.isInstance(it.sampleInstance)
            }
        }

        val objectTypeRegistry = HashMap<String, IRegistryEntry>()
        private val objectTypeByClass = HashMap<String, IRegistryEntry>()

        fun checkInstance(instance0: Saveable) {
            if (Build.isDebug && instance0 is PrefabSaveable) {
                val clone = try {
                    instance0.clone()
                } catch (_: Exception) {
                    null
                }
                if (clone != null && clone::class != instance0::class) {
                    LOGGER.warn("${instance0::class}.clone() is incorrect, returns ${clone::class} instead")
                }
            }
        }

        @JvmStatic
        fun registerCustomClass(sample: Saveable): RegistryEntry {
            checkInstance(sample)
            val className = sample.className
            val entry = if (sample is PrefabSaveable) {
                RegistryEntry(sample) { sample.clone() }
            } else RegistryEntry(sample)
            return registerCustomClass(className, entry)
        }

        @JvmStatic
        fun registerCustomClass(sample: Saveable, constructor: () -> Saveable): RegistryEntry {
            val className = sample.className
            val entry = registerCustomClass(className, RegistryEntry(sample, constructor))
            // dangerous to be done after
            // but this allows us to skip the full implementation of clone() everywhere
            checkInstance(sample)
            return entry
        }

        @JvmStatic
        fun registerCustomClass(constructor: () -> Saveable): RegistryEntry {
            val instance0 = constructor()
            return registerCustomClass(instance0, constructor)
        }

        @JvmStatic
        fun <V : Saveable> registerCustomClass(clazz: KClass<V>): RegistryEntry {
            return registerCustomClass(null as String?, clazz)
        }

        @JvmStatic
        fun <V : Saveable> registerCustomClass(className: String?, clazz: KClass<V>): RegistryEntry {
            val sample = try {
                clazz.java.newInstance()
            } catch (e: InstantiationException) {
                throw IllegalArgumentException("$clazz is missing constructor without parameters", e)
            }
            checkInstance(sample)
            return registerCustomClass(className ?: sample.className, RegistryEntry(sample))
        }

        fun <Entry : IRegistryEntry> registerCustomClass(
            className: String, entry: Entry,
            print: Boolean = true
        ): Entry {
            val clazz = entry.classPath
            val oldInstance = objectTypeRegistry[className]?.classPath
            if (oldInstance != null && oldInstance != clazz) {
                LOGGER.warn("Overriding registered class {} from type {} with {}", className, oldInstance, clazz)
            }
            if (print) {
                LOGGER.info("Registering {}", className)
            }
            objectTypeRegistry[className] = entry
            objectTypeByClass[clazz] = entry
            return entry
        }
    }
}