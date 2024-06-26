package me.anno.engine.inspector

import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.DebugWarning
import me.anno.ecs.annotations.EditorField
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.utils.structures.lists.Lists.partition1
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.types.Strings.titlecase
import org.apache.logging.log4j.LogManager
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.isAccessible

class CachedReflections(
    val clazz: KClass<*>,
    val allProperties: Map<String, CachedProperty>,
    val debugActions: List<KFunction<*>>,
    val debugProperties: List<CachedProperty>,
    val editorFields: List<CachedProperty>,
    val debugWarnings: List<CachedProperty>,
) {

    constructor(clazz: KClass<*>) : this(clazz, getMemberProperties(clazz))
    constructor(clazz: KClass<*>, allProperties: Map<String, CachedProperty>) : this(
        clazz, allProperties,
        getDebugActions(clazz),
        getDebugProperties(allProperties),
        getEditorFields(allProperties),
        getDebugWarnings(allProperties)
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

        private fun <V> List<V>.hasMember(clazz: KClass<*>): Boolean {
            for (i in indices) {
                if (clazz.isInstance(this[i])) {
                    return true
                }
            }
            return false
        }

        fun getDebugActions(clazz: KClass<*>): List<KFunction<*>> {
            var list = emptyList<KFunction<*>>()
            for (item in clazz.memberFunctions) {
                if (item.annotations.hasMember(DebugAction::class)) {
                    item.isAccessible = true // it's debug, so we're allowed to access it
                    if (list is MutableList) list.add(item)
                    else list = arrayListOf(item)
                }
            }
            // selectParent shall be first
            (list as? ArrayList)?.partition1 { it.name == "selectParent" }
            return list
        }

        fun getDebugProperties(properties: Map<String, CachedProperty>): List<CachedProperty> {
            return getAnnotatedFields(properties, DebugProperty::class)
        }

        fun getEditorFields(properties: Map<String, CachedProperty>): List<CachedProperty> {
            return getAnnotatedFields(properties, EditorField::class)
        }

        fun getDebugWarnings(properties: Map<String, CachedProperty>): List<CachedProperty> {
            return getAnnotatedFields(properties, DebugWarning::class)
        }

        fun getAnnotatedFields(allProperties: Map<String, CachedProperty>, clazz1: KClass<*>): List<CachedProperty> {
            return allProperties.values.filter { it.annotations.hasMember(clazz1) }
        }

        fun getMemberProperties(clazz: KClass<*>): Map<String, CachedProperty> {
            val jc = clazz.java
            return findProperties(
                allFields(jc, ArrayList())
                    .filter { !Modifier.isStatic(it.modifiers) },
                allMethods(jc, ArrayList())
                    .filter { !Modifier.isStatic(it.modifiers) || it.name.endsWith("\$annotations") })
        }

        private val declaredFields = LazyMap(Class<*>::getDeclaredFields)
        private val declaredMethods = LazyMap(Class<*>::getDeclaredMethods)

        fun allFields(clazz: Class<*>, dst: ArrayList<Field>): List<Field> {
            dst.addAll(declaredFields[clazz])
            val superClass = clazz.superclass
            if (superClass != null) allFields(superClass, dst)
            return dst
        }

        fun allMethods(clazz: Class<*>, dst: ArrayList<Method>): List<Method> {
            dst.addAll(declaredMethods[clazz])
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
            findMethodProperties(map, methods, "get", "set", false)
            findMethodProperties(map, methods, "is", "set", true)
            return map
        }

        fun findMethodProperties(
            map: HashMap<String, CachedProperty>,
            methods: List<Method>,
            getterPrefix: String, setterPrefix: String,
            includePrefixInName: Boolean
        ): Map<String, CachedProperty> {
            // this is great: declaredMemberProperties in only what was changes, so we can really create listener lists :)
            for (index in methods.indices) {
                val getterMethod = methods[index]
                val name = getterMethod.name
                if (getterMethod.parameterCount == 0 &&
                    name.startsWith(getterPrefix) &&
                    '$' !in name &&
                    name.length > getterPrefix.length &&
                    name[getterPrefix.length] in 'A'..'Z'
                ) {
                    val betterName = if (includePrefixInName) name else {
                        name[getterPrefix.length].lowercaseChar() +
                                name.substring(getterPrefix.length + 1)
                    }
                    if (betterName !in map) {
                        val setterName = setterPrefix + name.substring(getterPrefix.length)
                        val setterMethod = methods.firstOrNull {
                            it.name == setterName && it.parameterCount == 1 &&
                                    it.parameters[0].type == getterMethod.returnType
                        } ?: continue
                        val annotations = getterMethod.annotations.toMutableList()
                        val kotlinAnnotationName = "$name\$annotations" // todo is this still correct???
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