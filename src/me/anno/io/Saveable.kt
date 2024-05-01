package me.anno.io

import me.anno.Build
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.EngineBase
import me.anno.engine.inspector.CachedReflections
import me.anno.io.base.BaseWriter
import me.anno.io.base.UnknownClassException
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.utils.OS
import me.anno.utils.structures.lists.Lists.firstOrNull2
import org.apache.logging.log4j.LogManager
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

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
        if (name == "*ptr") throw RuntimeException()
        LogManager.getLogger(Saveable::class).warn("Unknown param $className.$name")
    }

    open fun setProperty(name: String, value: Any?) = warnMissingParam(name)

    /**
     * class id for saving instances of this class
     * needs to be unique for that class, and needs to be registered
     * */
    open val className: String
        get() {
            val clazz = this::class.java
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

    companion object {

        private val LOGGER = LogManager.getLogger(Saveable::class)
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
            val sampleInstance: Saveable,
            private val generator: (() -> Saveable)? = null
        ) {
            private val clazz = sampleInstance.javaClass
            fun generate(): Saveable = generator?.invoke() ?: clazz.newInstance()
        }

        fun createOrNull(type: String): Saveable? {
            return objectTypeRegistry[type]?.generate()
        }

        fun create(type: String): Saveable {
            return objectTypeRegistry[type]?.generate() ?: throw UnknownClassException(type)
        }

        fun getSample(type: String) = objectTypeRegistry[type]?.sampleInstance

        fun getClass(type: String): KClass<out Saveable>? {
            val instance = objectTypeRegistry[type]?.sampleInstance ?: return superTypeRegistry[type]
            return instance::class
        }

        fun getByClass(clazz: KClass<*>): RegistryEntry? {
            return objectTypeByClass[clazz]
        }

        fun <V : Saveable> getInstanceOf(clazz: KClass<V>): Map<String, RegistryEntry> {
            return objectTypeRegistry.filterValues { clazz.isInstance(it.sampleInstance) }
        }

        val objectTypeRegistry = HashMap<String, RegistryEntry>()
        private val objectTypeByClass = HashMap<KClass<out Saveable>, RegistryEntry>()
        private val superTypeRegistry = HashMap<String, KClass<out Saveable>>()

        fun checkInstance(instance0: Saveable) {
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

        fun registerSuperClasses(clazz0: KClass<out Saveable>) {
            var clazz = clazz0
            while (true) {
                superTypeRegistry[clazz.simpleName!!] = clazz
                @Suppress("unchecked_cast")
                clazz = (clazz.superclasses.firstOrNull() ?: break) as KClass<out Saveable>
            }
        }

        @JvmStatic
        fun registerCustomClass(className: String, constructor: () -> Saveable): RegistryEntry {
            val instance0 = constructor()
            checkInstance(instance0)
            return register(className, RegistryEntry(instance0, constructor))
        }

        @JvmStatic
        fun registerCustomClass(sample: Saveable): RegistryEntry {
            checkInstance(sample)
            val className = sample.className
            return if (sample is PrefabSaveable) {
                register(className, RegistryEntry(sample) { sample.clone() })
            } else {
                register(className, RegistryEntry(sample))
            }
        }

        @JvmStatic
        fun registerCustomClass(constructor: () -> Saveable): RegistryEntry {
            val instance0 = constructor()
            val className = instance0.className
            val entry = register(className, RegistryEntry(instance0, constructor))
            // dangerous to be done after
            // but this allows us to skip the full implementation of clone() everywhere
            checkInstance(instance0)
            return entry
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
            registerSuperClasses(entry.sampleInstance::class)
            return entry
        }
    }
}