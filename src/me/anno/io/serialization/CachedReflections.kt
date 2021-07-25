package me.anno.io.serialization

import me.anno.ecs.annotations.ExecuteInEditMode
import me.anno.io.ISaveable
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible

class CachedReflections(val clazz: KClass<*>, val properties: Map<String, CachedProperty>) {

    constructor(instance: Any) : this(instance, instance::class)

    constructor(instance: Any, clazz: KClass<*>) : this(clazz, extractProperties(instance, clazz))

    val annotations = clazz.annotations

    val executeInEditMode = annotations.any { it is ExecuteInEditMode }


    /**
     * updates the property in the instance
     * returns true on success
     * */
    operator fun set(self: ISaveable, name: String, value: Any?): Boolean {
        val property = properties[name] ?: return false
        property[self] = value
        return true
    }

    operator fun get(self: ISaveable, name: String): Any? {
        val property = properties[name] ?: return null
        return property[self]
    }

    operator fun get(name: String) = properties[name]

    companion object {

        private val LOGGER = LogManager.getLogger(CachedProperty::class)

        fun extractProperties(instance: Any, clazz: KClass<*>): Map<String, CachedProperty> {
            val properties = clazz.declaredMemberProperties.filterIsInstance<KMutableProperty1<*, *>>()
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
