package me.anno.io.serialization

import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Range
import me.anno.engine.IProperty
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

class CachedProperty(
    val name: String,
    val clazz: KClass<*>,
    val forceSaving: Boolean?,
    val annotations: List<Annotation>,
    val getter: KProperty1.Getter<*, *>,
    val setter: KMutableProperty1.Setter<*, *>
) {

    val range = annotations.filterIsInstance<Range>().firstOrNull()
    val hideInInspector = annotations.any { it is HideInInspector }

    operator fun set(instance: Any, value: Any?) {
        try {
            setter.call(instance, value)
        } catch (e: Exception) {
            LOGGER.warn("Setting $name of ${instance::class}, but the properties class is $clazz")
            e.printStackTrace()
        }
    }

    operator fun get(instance: Any): Any? {
        return try {
            getter.call(instance)
        } catch (e: Exception) {
            LOGGER.warn("Setting $name of ${instance::class}, but the properties class is $clazz")
            e.printStackTrace()
            null
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(CachedProperty::class)
    }

}
