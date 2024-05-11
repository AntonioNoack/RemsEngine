package me.anno.engine.inspector

import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.DebugWarning
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.utils.OS
import me.anno.utils.types.Strings.titlecase
import org.apache.logging.log4j.LogManager
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.isAccessible

class CachedReflections(
    val clazz: KClass<*>,
    val allProperties: Map<String, CachedProperty>,
    val debugActions: List<KFunction<*>>,
    val debugProperties: List<KProperty<*>>,
    val debugWarnings: List<KProperty<*>>
) {

    constructor(clazz: KClass<*>) : this(
        clazz,
        getMemberProperties(clazz),
        getDebugActions(clazz),
        getDebugProperties(clazz),
        getDebugWarnings(clazz)
    )

    val propertiesByClass by lazy {
        getPropertiesByDeclaringClass(clazz, allProperties)
    }

    val propertiesByClassList by lazy {
        propertiesByClass.flatMap { it.second }
    }

    val serializedProperties = allProperties.filter { it.value.serialize }

    // todo use this
    // val executeInEditMode = annotations.any { it is ExecuteInEditMode }

    /**
     * updates the property in the instance
     * returns true on success
     * */
    operator fun set(self: Any, name: String, value: Any?): Boolean {
        val property = allProperties[name] ?: return false
        val value2 = if (value is Array<*> && property[self] is List<*>) {
            value.toList()
        } else if (property.valueClass.isEnum && value is Int) {
            val values = property.valueClass.enumConstants
            values.firstOrNull { getEnumId(it) == value } ?: values.first()
        } else value
        return try {
            property.set(self, value2)
        } catch (e: IllegalArgumentException) {
            LOGGER.warn(
                "$e for ${self::class}.$name, " +
                        "it's ${if (value2 != null) value2::class else null}"
            )
            false
        }
    }

    operator fun get(self: Any, name: String): Any? {
        val property = allProperties[name] ?: return null
        return property[self]
    }

    operator fun get(name: String) = allProperties[name]

    companion object {

        private val LOGGER = LogManager.getLogger(CachedReflections::class)

        fun getDebugActions(clazz: KClass<*>): List<KFunction<*>> {
            if (OS.isWeb) return emptyList()
            var list = emptyList<KFunction<*>>()
            for (item in clazz.memberFunctions) {
                if (item.annotations.any { it is DebugAction }) {
                    item.isAccessible = true // it's debug, so we're allowed to access it
                    if (list is MutableList) list.add(item)
                    else list = arrayListOf(item)
                }
            }
            return list
        }

        fun getDebugProperties(clazz: KClass<*>): List<KProperty<*>> {
            if (OS.isWeb) return emptyList()
            var list = emptyList<KProperty<*>>()
            for (item in clazz.memberProperties) {
                if (item.annotations.any { it is DebugProperty }) {
                    item.isAccessible = true // it's debug, so we're allowed to access it
                    if (list is MutableList) list.add(item)
                    else list = arrayListOf(item)
                }
            }
            return list
        }

        fun getDebugWarnings(clazz: KClass<*>): List<KProperty<*>> {
            if (OS.isWeb) return emptyList()
            var list = emptyList<KProperty<*>>()
            for (item in clazz.memberProperties) {
                if (item.annotations.any { it is DebugWarning }) {
                    item.isAccessible = true // it's debug, so we're allowed to access it
                    if (list is MutableList) list.add(item)
                    else list = arrayListOf(item)
                }
            }
            return list
        }

        fun getMemberProperties(clazz: KClass<*>): Map<String, CachedProperty> {
            val jc = clazz.java
            return findProperties(
                allFields(jc, ArrayList()).filter { !Modifier.isStatic(it.modifiers) },
                allMethods(jc, ArrayList())
                    .filter { !Modifier.isStatic(it.modifiers) || it.name.endsWith("\$annotations") })
        }

        fun allFields(clazz: Class<*>, dst: ArrayList<Field>): List<Field> {
            dst.addAll(clazz.declaredFields)
            val superClass = clazz.superclass
            if (superClass != null) allFields(superClass, dst)
            return dst
        }

        fun allMethods(clazz: Class<*>, dst: ArrayList<Method>): List<Method> {
            dst.addAll(clazz.declaredMethods)
            val superClass = clazz.superclass
            if (superClass != null) allMethods(superClass, dst)
            return dst
        }

        private fun listClasses(clazz: KClass<*>): List<KClass<*>> {
            val classes = ArrayList<KClass<*>>()
            classes.add(clazz)
            while (true) {
                val lastClass = classes.last()
                val superClasses = lastClass.superclasses
                if (superClasses.isEmpty()) break
                classes.addAll(superClasses)
            }
            return classes
        }

        fun getPropertiesByDeclaringClass(
            classes: List<KClass<*>>, allProperties: Map<String, CachedProperty>
        ): List<Pair<KClass<*>, List<String>>> {
            // the earlier something is found, the better
            val doneNames = HashSet<String>()
            val result = ArrayList<Pair<KClass<*>, List<String>>>(classes.size)
            val targetSize = allProperties.size
            for (ci in classes.indices) {
                val clazz2 = classes[ci]
                val partialResult = ArrayList<String>()
                val reflections = clazz2.declaredMemberProperties
                    .filterIsInstance<KMutableProperty1<*, *>>()
                for (pi in reflections.indices) {
                    val property = reflections[pi]
                    val name = property.name
                    if (name !in allProperties) continue // not serialized
                    if (doneNames.add(name)) {
                        partialResult.add(name)
                    }
                }
                partialResult.sort()
                result.add(clazz2 to partialResult)
                if (doneNames.size >= targetSize) break // done :)
            }
            return result
        }

        fun getPropertiesByDeclaringClass(
            clazz: KClass<*>, allProperties: Map<String, CachedProperty>
        ): List<Pair<KClass<*>, List<String>>> {
            val classes = listClasses(clazz)
            return getPropertiesByDeclaringClass(classes, allProperties)
        }

        fun findProperties(
            properties: List<Field>,
            methods: List<Method>,
        ): Map<String, CachedProperty> {
            // this is great: declaredMemberProperties in only what was changes, so we can really create listener lists :)
            val map = HashMap<String, CachedProperty>()
            for (index in properties.indices) {
                val field = properties[index]
                val annotations = field.annotations.toMutableList()
                val fieldCap = field.name.titlecase()
                val kotlinAnnotationName = "get$fieldCap\$annotations"
                val m = methods.firstOrNull { it.name == kotlinAnnotationName }
                if (m != null) annotations += m.annotations.toList()
                val serial = annotations.firstOrNull { it is SerializedProperty } as? SerializedProperty
                val notSerial = annotations.firstOrNull { it is NotSerializedProperty }
                val getterName = if (fieldCap.startsWith("Is")) field.name
                else "get$fieldCap"
                val setterName = if (fieldCap.startsWith("Is")) "set${fieldCap.substring(2).titlecase()}"
                else "set$fieldCap"
                val isPublic = Modifier.isPublic(field.modifiers) ||
                        (methods.any { Modifier.isPublic(it.modifiers) && it.name == getterName } &&
                                methods.any { Modifier.isPublic(it.modifiers) && it.name == setterName })
                val serialize = serial != null || (isPublic && notSerial == null)
                var name = serial?.name
                if (name.isNullOrEmpty()) name = field.name
                if (name in map) continue
                try {
                    map[name!!] = saveField(field, name, serial, serialize, annotations.toList())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            for (index in methods.indices) {
                val getterMethod = methods[index]
                val name = getterMethod.name
                if (getterMethod.parameterCount == 0 &&
                    name.startsWith("get") &&
                    '$' !in name &&
                    name.length > 3 &&
                    name[3] in 'A'..'Z'
                ) {
                    val subName = name.substring(3)
                    val setterName = "s" + name.substring(1)
                    val betterName = subName[0].lowercase() + subName.substring(1)
                    if (betterName !in map &&
                        map.none { it.key.equals(subName, true) } // might have an upper case start letter...
                    ) {
                        val setterMethod = methods.firstOrNull {
                            it.name == setterName && it.parameterCount == 1 &&
                                    it.parameters[0].type == getterMethod.returnType
                        } ?: continue
                        val annotations = getterMethod.annotations.toMutableList()
                        val kotlinAnnotationName = "$name\$annotations"
                        val m = methods.firstOrNull { it.name == kotlinAnnotationName }
                        if (m != null) annotations += m.annotations.toList()
                        val serial = annotations.firstOrNull { it is SerializedProperty } as? SerializedProperty
                        val notSerial = annotations.firstOrNull { it is NotSerializedProperty }
                        val isPublic = Modifier.isPublic(getterMethod.modifiers)
                        val serialize = serial != null || (isPublic && notSerial == null)
                        map[betterName] = saveField(
                            getterMethod.declaringClass, getterMethod.returnType,
                            betterName, serial, serialize, annotations,
                            getterMethod::invoke, setterMethod::invoke
                        )
                    }
                }
            }
            return map
        }

        private fun saveField(
            field: Field,
            name: String,
            serial: SerializedProperty?,
            serialize: Boolean,
            annotations: List<Annotation>
        ): CachedProperty {
            // save the field
            field.isAccessible = true
            val setterMethod = try {
                field.declaringClass.getMethod("set${name.titlecase()}", field.type)
            } catch (e: NoSuchMethodException) {
                null
            }
            val getterMethod = try {
                field.declaringClass.getMethod("get${name.titlecase()}")
            } catch (e: NoSuchMethodException) {
                null
            }
            return saveField(
                field.declaringClass,
                field.type, name, serial,
                serialize, annotations,
                if (getterMethod != null && getterMethod.returnType == field.type) { it -> getterMethod.invoke(it) }
                else field::get,
                if (setterMethod != null) { i, v -> setterMethod.invoke(i, v) }
                else { i, v -> field.set(i, v) })
        }

        private fun saveField(
            instanceClass: Class<*>,
            valueClass: Class<*>,
            name: String,
            serial: SerializedProperty?,
            serialize: Boolean,
            annotations: List<Annotation>,
            getter: (instance: Any) -> Any?,
            setter: (instance: Any, value: Any?) -> Unit
        ): CachedProperty {
            // save the field
            val forceSaving = serial?.forceSaving ?: (valueClass == Boolean::class.java)
            return CachedProperty(
                name, instanceClass, valueClass, serialize, forceSaving,
                annotations, getter, setter
            )
        }

        fun getEnumId(value: Any): Int? {
            // todo why is this not saved as an input for nodes when cloning???
            return try {
                value.javaClass.getField("id").get(value) as? Int
            } catch (ignored: NoSuchFieldException) {
                try {
                    value.javaClass.getMethod("getId").invoke(value) as? Int
                } catch (ignored: NoSuchMethodException) {
                    null
                }
            }
        }
    }
}