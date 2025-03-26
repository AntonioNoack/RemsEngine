package me.anno.engine.inspector

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.ExtendableEnum
import me.anno.ecs.annotations.Group
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Order
import me.anno.ecs.annotations.Range
import me.anno.ui.input.EnumInput
import me.anno.utils.Reflections.getEnumById
import me.anno.utils.structures.Collections.filterIsInstance2
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.structures.lists.Lists.firstOrNull2
import me.anno.utils.types.AnyToInt
import me.anno.utils.types.Strings.camelCaseToTitle
import me.anno.utils.types.Strings.iff
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.toInt
import org.apache.logging.log4j.LogManager

class CachedProperty(
    val name: String,
    val instanceClass: Class<*>,
    val valueClass: Class<*>,
    val serialize: Boolean,
    val forceSaving: Boolean?,
    val annotations: List<Annotation>,
    val getter: (instance: Any) -> Any?,
    val setter: ((instance: Any, value: Any?) -> Unit)?
) {

    val range = annotations.firstInstanceOrNull(Range::class)
    val hideInInspector1 = annotations.mapNotNull { if (it is HideInInspector) hide(it, name, instanceClass) else null }
    val description = annotations.filterIsInstance2(Docs::class).joinToString("\n") { it.description }
    val order = annotations.firstInstanceOrNull(Order::class)?.index ?: 0
    val group = annotations.firstInstanceOrNull(Group::class)?.name ?: DEFAULT_GROUP

    fun hideInInspector(instance: Any): Boolean {
        return hideInInspector1.any2 { test -> test(instance) }
    }

    operator fun set(instance: Any, value: Any?): Boolean {
        if (setter == null) {
            LOGGER.warn("Cannot set {}", name)
            return false
        }
        if (!instanceClass.isInstance(instance)) throw IllegalArgumentException("Instance is not instance of $instanceClass, it is ${instance::class}")
        if (!valueClass.isInstance(value)) {
            if (value != null) {
                // if we have two arrays of different types, convert them
                val vcj = valueClass
                val icj = value.javaClass
                if (vcj.isArray && icj.isArray && value is Array<*>) {
                    // convert them
                    val newValue = java.lang.reflect.Array.newInstance(vcj.componentType, value.size)
                    @Suppress("UNCHECKED_CAST")
                    value.copyInto(newValue as Array<Any?>)
                    return set(instance, newValue)
                }
            }
        }
        return try {
            val oldValue = getter(instance)
            if (oldValue is Enum<*> && value !is Enum<*>) {
                // an enum, let's try our best to find the correct value
                val clazz = oldValue.javaClass
                val values = EnumInput.getEnumConstants(clazz)
                val parsingFailure = -1
                val id = AnyToInt.getInt(value, parsingFailure)
                val newValue = when {
                    id != parsingFailure -> {
                        val valueOrNull = getEnumById(clazz, id)
                        if (valueOrNull == null) {
                            LOGGER.warn("Missing $clazz with id $id")
                        }
                        valueOrNull ?: oldValue
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
                        val ordinal =
                            if (splitIndex < 0) -1
                            else (value as CharSequence).toInt(0, splitIndex)
                        val name = value.substring(splitIndex + 1)
                        val valueOrNull = values.firstOrNull { it.name == name }
                            ?: values.firstOrNull { it.name.equals(name, true) }
                            ?: getEnumById(clazz, id)
                            ?: getEnumById(clazz, ordinal)
                        // as a last resort, we could try to use the Levenshtein distance
                        if (valueOrNull == null) {
                            LOGGER.warn("Could not find appropriate enum value for value '$value' and class ${oldValue::class}")
                        }
                        valueOrNull ?: oldValue
                    }
                    else -> {
                        LOGGER.warn("Value $value cannot be converted to enum, type is incorrect")
                        oldValue
                    }
                }
                setter.invoke(instance, newValue)
            } else if (oldValue is ExtendableEnum && value !is ExtendableEnum) {
                // an enum, let's try our best to find the correct value
                val values = oldValue.values
                val clazz = oldValue.javaClass
                val id = AnyToInt.getInt(value, Int.MIN_VALUE)
                val valueOrNull = values.firstOrNull2 { it.id == id }
                if (valueOrNull == null) {
                    LOGGER.warn("Missing $clazz with id $id")
                }
                val newValue = valueOrNull ?: oldValue
                setter.invoke(instance, newValue)
            } else if (oldValue is Float && value is Number) {
                setter.invoke(instance, value.toFloat())
            } else if (oldValue is Docs && value is Number) {
                setter.invoke(instance, value.toDouble())
            } else setter.invoke(instance, value)
            true
        } catch (e: Exception) {
            LOGGER.error(
                "Error setting property '$name' with value of class " +
                        "'${if (value != null) value::class else null}' to instance of class '${instance::class}', " +
                        "properties class: '$valueClass'"
            )
            e.printStackTrace()
            false
        }
    }

    operator fun get(instance: Any): Any? {
        return try {
            getter(instance)
        } catch (e: Exception) {
            LOGGER.error("Setting property '$name' of ${instance::class}, but the properties class is '$valueClass'")
            e.printStackTrace()
            null
        }
    }

    override fun toString(): String {
        return "$name: $valueClass" +
                ", serialize".iff(serialize) +
                ", force-saving".iff(forceSaving == true) +
                ", $range".iff(range != null) +
                ", order: $order" +
                ", $group".iff(group != DEFAULT_GROUP) +
                ", $annotations"
    }

    companion object {
        const val DEFAULT_GROUP = ""
        private val LOGGER = LogManager.getLogger(CachedProperty::class)
        private fun hide(it: HideInInspector, name: String, instanceClazz: Class<*>): (Any) -> Boolean {
            if (it.hideIfVariableIsTrue.isBlank2()) return { _ -> true }
            else {
                val getter1 = try {
                    val fieldName0 = it.hideIfVariableIsTrue
                    instanceClazz.getMethod(
                        if (fieldName0.startsWith("is")) fieldName0
                        else "get${
                            fieldName0.camelCaseToTitle()
                                .replace(" ", "")
                        }"
                    )
                } catch (e: NoSuchMethodException) {
                    e.printStackTrace()
                    null
                }
                if (getter1 != null) return { instance -> getter1.invoke(instance) == true }
                else {
                    LOGGER.warn("Property ${it.hideIfVariableIsTrue} was not found to hide variable $name of $instanceClazz")
                    return { _: Any -> false }
                }
            }
        }
    }
}