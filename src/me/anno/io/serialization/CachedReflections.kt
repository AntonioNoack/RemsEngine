package me.anno.io.serialization

import me.anno.io.ISaveable
import me.anno.utils.LOGGER
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible

class CachedReflections(val properties: Map<String, CachedProperty>) {

    constructor(instance: Any) : this(instance::class)

    constructor(clazz: KClass<*>) : this(extractProperties(clazz))

    /**
     * updates the property in the instance
     * returns true on success
     * */
    fun set(self: ISaveable, name: String, value: Any?): Boolean {
        val property = properties[name] ?: return false
        property.setter.call(self, value)
        return true
    }

    fun get(self: ISaveable, name: String): Any? {
        val property = properties[name] ?: return null
        return property.getter.call(self)
    }

    companion object {
        fun extractProperties(clazz: KClass<*>): Map<String, CachedProperty> {
            val properties = clazz.declaredMemberProperties.filterIsInstance<KMutableProperty1<*, *>>()
            val map = HashMap<String, CachedProperty>()
            properties.map { field ->
                val isPublic = field.visibility == KVisibility.PUBLIC
                val serial = field.findAnnotation<SerializableProperty>()
                val needsSerialization =
                    serial != null || (isPublic && field.findAnnotation<NotSerializableProperty>() == null)
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
                        val value = getter.call(this)
                        val forceSaving = serial?.forceSaving ?: value is Boolean
                        val property = CachedProperty(forceSaving, getter, setter)
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
