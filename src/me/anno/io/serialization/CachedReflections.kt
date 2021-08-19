package me.anno.io.serialization

import me.anno.ecs.annotations.ExecuteInEditMode
import me.anno.io.ISaveable
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.isAccessible

class CachedReflections(
    val clazz: KClass<*>,
    val allProperties: Map<String, CachedProperty>,
    val declaredProperties: Map<String, CachedProperty>
) {

    constructor(instance: Any) : this(instance, instance::class)

    constructor(instance: Any, clazz: KClass<*>) : this(
        clazz,
        getMemberProperties(instance, clazz),
        getDeclaredMemberProperties(instance, clazz)
    )

    val annotations = clazz.annotations

    val propertiesByClass = lazy {
        getPropertiesByDeclaringClass(clazz, allProperties)
    }

    val executeInEditMode = annotations.any { it is ExecuteInEditMode }

    /**
     * updates the property in the instance
     * returns true on success
     * */
    operator fun set(self: ISaveable, name: String, value: Any?): Boolean {
        val property = allProperties[name] ?: return false
        if (value is Array<*> && property[self] !is Array<*>) {
            property[self] = value.toList()
        } else {
            property[self] = value
        }
        return true
    }

    operator fun get(self: ISaveable, name: String): Any? {
        val property = allProperties[name] ?: return null
        return property[self]
    }

    operator fun get(name: String) = allProperties[name]


    companion object {

        private val LOGGER = LogManager.getLogger(CachedProperty::class)

        fun getMemberProperties(instance: Any, clazz: KClass<*>): Map<String, CachedProperty> {
            return findProperties(instance, clazz, clazz.memberProperties.filterIsInstance<KMutableProperty1<*, *>>())
        }

        fun getDeclaredMemberProperties(instance: Any, clazz: KClass<*>): Map<String, CachedProperty> {
            return findProperties(
                instance,
                clazz,
                clazz.declaredMemberProperties.filterIsInstance<KMutableProperty1<*, *>>()
            )
        }

        fun getPropertiesByDeclaringClass(
            clazz: KClass<*>, allProperties: Map<String, CachedProperty>
        ): List<Pair<KClass<*>, List<String>>> {
            val classes = ArrayList<KClass<*>>()
            classes.add(clazz)
            while (true) {
                val lastClass = classes.last()
                val superClasses = lastClass.superclasses
                if (superClasses.isEmpty()) break
                classes.addAll(superClasses)
            }
            val classesWithIndex = classes.withIndex().toList()
            val reflections = classes.map { clazz2 ->
                clazz2.declaredMemberProperties.filterIsInstance<KMutableProperty1<*, *>>()
            }
            // the earlier something is found, the better
            val doneNames = HashSet<String>()
            val result = ArrayList<Pair<KClass<*>, List<String>>>(classes.size)
            val targetSize = allProperties.size
            for (classWithIndex in classesWithIndex) {
                val partialResult = ArrayList<String>()
                val index = classWithIndex.index
                for (property in reflections[index]) {
                    val name = property.name
                    if (name !in allProperties) continue // not serialized
                    if (name !in doneNames) {
                        partialResult.add(name)
                    }
                }
                doneNames.addAll(partialResult)
                result.add(classWithIndex.value to partialResult)
                if (doneNames.size >= targetSize) break // done :)
            }
            return result
        }

        fun findProperties(
            instance: Any,
            clazz: KClass<*>,
            properties: List<KMutableProperty1<*, *>>
        ): Map<String, CachedProperty> {
            // todo this is great: declaredMemberProperties in only what was changes, so we can really create listener lists :)
            val map = HashMap<String, CachedProperty>()
            properties.map { field ->
                val isPublic = field.visibility == KVisibility.PUBLIC
                val serial = field.findAnnotation<SerializedProperty>()
                val needsSerialization =
                    serial != null || (isPublic && field.findAnnotation<NotSerializedProperty>() == null)
                if (needsSerialization) {
                    try {
                        // save the field
                        var name = serial?.name
                        if (name == null || name.isEmpty()) name = field.name
                        // make sure we can access it
                        val setter = field.setter
                        setter.isAccessible = true
                        val getter = field.getter
                        getter.isAccessible = true
                        val value = getter.call(instance)
                        val forceSaving = serial?.forceSaving ?: value is Boolean
                        val property = CachedProperty(name, clazz, forceSaving, field.annotations, getter, setter)
                        if (name in map) LOGGER.warn("Property $name appears twice in $clazz")
                        map[name] = property
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            return map
        }
    }

}
