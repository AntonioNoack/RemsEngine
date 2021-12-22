package me.anno.io.serialization

import me.anno.ecs.annotations.*
import me.anno.ui.input.EnumInput
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmName

class CachedProperty(
    val name: String,
    val clazz: KClass<*>,
    val serialize: Boolean,
    val forceSaving: Boolean?,
    val annotations: List<Annotation>,
    val getter: KProperty1.Getter<*, *>,
    val setter: KMutableProperty1.Setter<*, *>
) {

    val range = annotations.filterIsInstance<Range>().firstOrNull()
    val hideInInspector = annotations.any { it is HideInInspector }
    val description = annotations.filterIsInstance<Docs>().joinToString("\n") { it.description }
    val order = annotations.filterIsInstance<Order>().firstOrNull()?.index ?: 0
    val group = annotations.filterIsInstance<Group>().firstOrNull()?.name

    operator fun set(instance: Any, value: Any?): Boolean {
        return try {
            val oldValue = getter.call(instance)
            if (oldValue is Enum<*> && value !is Enum<*>) {
                // an enum, let's try our best to find the correct value
                val values = EnumInput.getEnumConstants(oldValue.javaClass)
                val newValue = when (value) {
                    is Byte -> values[value.toInt().and(0xff)]
                    is UByte -> values[value.toInt()]
                    is Short -> values[value.toInt().and(0xffff)]
                    is UShort -> values[value.toInt()]
                    is Int -> values[value]
                    is UInt -> values[value.toInt()]
                    is Long -> values[value.toInt()]
                    is ULong -> values[value.toInt()]
                    is Float -> values[value.toInt()]
                    is Double -> values[value.toInt()]
                    is String -> {
                        /**
                         * try to match the old value with the existing enums
                         * properties, which are tested, in order
                         * 1. name matches, case sensitive
                         * 2. name matches, case insensitive
                         * 3. enum.id exists now && enum.id matches old ordinal
                         * 4. ordinal matches
                         * */
                        val splitIndex = value.indexOf('/')
                        val ordinal = if (splitIndex < 0) -1 else value.substring(0, splitIndex).toInt()
                        val name = value.substring(splitIndex + 1)
                        val idGetter = oldValue::class.memberProperties
                            .firstOrNull { it.name == "id" }
                        values.firstOrNull { it as Enum<*>; it.name == name }
                            ?: values.firstOrNull { it as Enum<*>; it.name.equals(name, true) }
                            ?: (if (idGetter != null) values.firstOrNull { idGetter.call(it) == ordinal } else null)
                            ?: (if (ordinal > -1) values.firstOrNull { it as Enum<*>; it.ordinal == ordinal } else null)
                            // as a last resort, we could try to use the Levenshtein distance
                            ?: throw RuntimeException("Could not find appropriate enum value for value '$value' and class ${oldValue::class}")
                    }
                    else -> throw RuntimeException("Value $value cannot be converted to enum, type is incorrect")
                }
                setter.call(instance, newValue)
            } else setter.call(instance, value)
            true
        } catch (e: Exception) {
            LOGGER.error("Setting property '$name' with value of class '${value?.javaClass?.name}' to instance of class '${instance::class.jvmName}', properties class: '$clazz'")
            e.printStackTrace()
            false
        }
    }

    operator fun get(instance: Any): Any? {
        return try {
            getter.call(instance)
        } catch (e: Exception) {
            LOGGER.error("Setting property '$name' of ${instance::class.jvmName}, but the properties class is '$clazz'")
            e.printStackTrace()
            null
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(CachedProperty::class)
    }

}
