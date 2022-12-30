package me.anno.io.serialization

import me.anno.ecs.annotations.*
import me.anno.ui.input.EnumInput
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.types.Strings.isBlank2
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
    val getter: (Any) -> Any?,
    val setter: (Any, Any?) -> Unit
) {

    val range = annotations.firstInstanceOrNull<Range>()
    val hideInInspector = annotations.mapNotNull { if (it is HideInInspector) hide(it, name, clazz) else null }
    val description = annotations.filterIsInstance<Docs>().joinToString("\n") { it.description }
    val order = annotations.firstInstanceOrNull<Order>()?.index ?: 0
    val group = annotations.firstInstanceOrNull<Group>()?.name

    operator fun set(instance: Any, value: Any?): Boolean {
        return try {
            val oldValue = getter(instance)
            if (oldValue is Enum<*> && value !is Enum<*>) {
                // an enum, let's try our best to find the correct value
                val values = EnumInput.getEnumConstants(oldValue.javaClass)
                val index = when (value) {
                    is Byte -> value.toUByte().toInt()
                    is UByte -> value.toInt()
                    is Short -> value.toUShort().toInt()
                    is UShort -> value.toInt()
                    is Int -> value
                    is UInt -> value.toInt()
                    is Long -> value.toInt()
                    is ULong -> value.toInt()
                    is Float -> value.toInt()
                    is Double -> value.toInt()
                    else -> Int.MIN_VALUE
                }
                if (index != Int.MIN_VALUE && index !in values.indices)
                    LOGGER.warn("Index $index out of bounds! 0 until ${values.size} for ${oldValue.javaClass}")
                val newValue = when {
                    index != Int.MIN_VALUE -> {
                        // todo use hashmap or array or sth like that for lookup
                        @Suppress("unchecked_cast")
                        val idProperty = (oldValue::class as KClass<Any>)
                            .memberProperties
                            .firstOrNull { it.name == "id" }
                        if (idProperty != null) {
                            values.firstOrNull { idProperty.get(it) == index } ?: values[0]
                        } else values.getOrNull(index) ?: values[0]
                    }
                    value is String -> {
                        /**
                         * try to match the old value with the existing enums
                         * properties, which are tested, in order
                         * 1. name matches, case-sensitive
                         * 2. name matches, case-insensitive
                         * 3. enum.id exists now && enum.id matches old ordinal
                         * 4. ordinal matches
                         * */
                        @Suppress("GrazieInspection")
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
                setter(instance, newValue)
            } else setter(instance, value)
            true
        } catch (e: Exception) {
            LOGGER.error("Error setting property '$name' with value of class '${value?.javaClass?.name}' to instance of class '${instance::class.jvmName}', properties class: '$clazz'")
            e.printStackTrace()
            false
        }
    }

    operator fun get(instance: Any): Any? {
        return try {
            getter(instance)
        } catch (e: Exception) {
            LOGGER.error("Setting property '$name' of ${instance::class.jvmName}, but the properties class is '$clazz'")
            e.printStackTrace()
            null
        }
    }

    override fun toString(): String {
        return "$name: ${clazz.jvmName}" +
                (if (serialize) ", serialize" else "") +
                (if (forceSaving == true) ", force-saving" else "") +
                (if (range != null) ", $range" else "") +
                ", order: $order" +
                (if (group != null) ", $group" else "") +
                ", $annotations"
    }

    companion object {
        private val LOGGER = LogManager.getLogger(CachedProperty::class)
        private fun hide(it: HideInInspector, name: String, clazz: KClass<*>): (Any) -> Boolean {
            if (it.hideIfVariableIsTrue.isBlank2()) return { _ -> false }
            else {
                @Suppress("unchecked_cast")
                val getter1 = clazz.memberProperties
                    .firstOrNull { p -> p.name == it.hideIfVariableIsTrue } as? KProperty1<Any, Boolean>
                if (getter1 != null) return { instance -> getter1.invoke(instance) }
                else {
                    LOGGER.warn("Property ${it.hideIfVariableIsTrue} was not found to hide variable $name of $clazz")
                    return { _: Any -> false }
                }
            }
        }
    }

}
