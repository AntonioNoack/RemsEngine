package me.anno.engine.inspector

import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.DebugWarning
import me.anno.ecs.annotations.EditorField
import me.anno.ecs.annotations.Order
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.io.saveable.Saveable
import me.anno.utils.Reflections.getEnumById
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.types.Strings.camelCaseToTitle
import me.anno.utils.types.Strings.isNotBlank2
import me.anno.utils.types.Strings.titlecase
import org.apache.logging.log4j.LogManager
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

class CachedReflections private constructor(
    val clazz: Class<*>,
    val allProperties: Map<String, CachedProperty>,
    val debugActions: List<DebugActionInstance>,
    val debugProperties: List<CachedProperty>,
    val editorFields: List<CachedProperty>,
    val debugWarnings: List<CachedProperty>,
) {

    constructor(clazz: Class<*>) : this(clazz, getMemberProperties(clazz))
    private constructor(clazz: Class<*>, allProperties: Map<String, CachedProperty>) : this(
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
            getEnumById(property.valueClass, value) ?: return false
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

        fun getDebugActions(clazz: Class<*>): List<DebugActionInstance> {
            var list = emptyList<DebugActionInstance>()
            val debugAnnotationClass = DebugAction::class.java
            val orderAnnotationClass = Order::class.java
            for (method in clazz.methods) {
                val debugAction = method.getAnnotation(debugAnnotationClass)
                if (debugAction != null) {
                    method.isAccessible = true // it's debug, so we're allowed to access it
                    val order = method.getAnnotation(orderAnnotationClass)?.index ?: 0
                    val title = debugAction.title.ifEmpty { method.name.camelCaseToTitle() }
                    val item = DebugActionInstance(method, title, order)
                    if (list is MutableList) list.add(item)
                    else list = arrayListOf(item)
                }
            }
            // selectParent shall be first, the rest shall be sorted
            (list as? ArrayList)?.sort()
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

        fun getMemberProperties(clazz: Class<*>): Map<String, CachedProperty> {
            return findProperties(
                clazz,
                allFields(clazz, ArrayList())
                    .filter { !Modifier.isStatic(it.modifiers) },
                allMethods(clazz, ArrayList())
                    .filter { !Modifier.isStatic(it.modifiers) || it.name.endsWith("\$annotations") })
        }

        private val declaredFields = LazyMap(Class<*>::getDeclaredFields)
        private val declaredMethods = LazyMap(Class<*>::getDeclaredMethods)

        fun allFields(clazz0: Class<*>, dst: ArrayList<Field>): List<Field> {
            var clazz = clazz0
            while (true) {
                dst.addAll(declaredFields[clazz])
                clazz = clazz.superclass ?: return dst
            }
        }

        fun allMethods(clazz0: Class<*>, dst: ArrayList<Method>): List<Method> {
            var clazz = clazz0
            while (true) {
                dst.addAll(declaredMethods[clazz])
                clazz = clazz.superclass ?: return dst
            }
        }

        private fun sortProperties(input: Collection<CachedProperty>): List<CachedProperty> {
            return input.sortedBy { it.name }
        }

        fun getPropertiesByDeclaringClass(
            clazz: Class<*>, allProperties: Map<String, CachedProperty>
        ): List<Pair<Class<*>, List<CachedProperty>>> {
            val superClass = clazz.superclass
                ?: return listOf(clazz to sortProperties(allProperties.values))
            val superReflections = Saveable.getReflectionsByClass(superClass)
            val superPropertyNames = superReflections.allProperties
            val customProperties = allProperties.filter { it.key !in superPropertyNames }.values
            return superReflections.propertiesByClass + (clazz to sortProperties(customProperties))
        }

        fun getGetterName(fieldName: String, fieldCap: CharSequence): String {
            return if (fieldCap.startsWith("Is")) fieldName
            else "get$fieldCap"
        }

        fun getSetterName(fieldName: String, fieldCap: CharSequence): String {
            return if (fieldCap.startsWith("Is")) "set${fieldCap.substring(2).titlecase()}"
            else "set$fieldCap"
        }

        fun findProperties(
            clazz: Class<*>,
            properties: List<Field>,
            methods: List<Method>,
        ): Map<String, CachedProperty> {

            // this is great: declaredMemberProperties in only what was changes, so we can really create listener lists :)
            val map = HashMap<String, CachedProperty>()
            val publicMethodNames = methods
                .filter { Modifier.isPublic(it.modifiers) }
                .map { it.name }.toHashSet()
            for (index in properties.indices) {
                val field = properties[index]
                val annotations = field.annotations.toMutableList()
                val fieldCap = field.name.titlecase()
                val getterName = getGetterName(field.name, fieldCap)
                val kotlinAnnotationName = "$getterName\$annotations"
                val m = methods.firstOrNull { it.name == kotlinAnnotationName }
                if (m != null) annotations += m.annotations.toList()
                val serial = annotations.firstOrNull { it is SerializedProperty } as? SerializedProperty
                val notSerial = annotations.firstOrNull { it is NotSerializedProperty }
                val setterName = getSetterName(field.name, fieldCap)
                val isPublic = Modifier.isPublic(field.modifiers) ||
                        (getterName in publicMethodNames && setterName in publicMethodNames)
                val serialize = serial != null || (isPublic && notSerial == null)
                var name = serial?.name
                if (name.isNullOrBlank()) name = field.name
                if (name in map || name == null) continue
                try {
                    map[name] = saveField(clazz, field, field.name, name, serial, serialize, annotations.toList())
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
                    var betterName = if (includePrefixInName) name else {
                        name[getterPrefix.length].lowercaseChar() +
                                name.substring(getterPrefix.length + 1)
                    }
                    val annotations = getterMethod.annotations.toMutableList()
                    val kotlinAnnotationName = "$name\$annotations" // todo is this still correct???
                    val m = methods.firstOrNull { it.name == kotlinAnnotationName }
                    if (m != null) annotations += m.annotations.toList()
                    val serial = annotations.firstOrNull { it is SerializedProperty } as? SerializedProperty
                    if (serial != null && serial.name.isNotBlank2()) {
                        betterName = serial.name
                    }
                    if (betterName !in map) {
                        val setterName = setterPrefix + name.substring(getterPrefix.length)
                        val setterMethod = methods.firstOrNull {
                            it.name == setterName && it.parameterCount == 1 &&
                                    it.parameterTypes[0] == getterMethod.returnType
                        }
                        val notSerial = annotations.firstOrNull { it is NotSerializedProperty }
                        val isPublic = Modifier.isPublic(getterMethod.modifiers)
                        val serialize = (serial != null || (isPublic && notSerial == null)) && setterMethod != null
                        getterMethod.isAccessible = true
                        setterMethod?.isAccessible = true
                        map[betterName] = saveField(
                            getterMethod.declaringClass, getterMethod.returnType,
                            betterName, serial, serialize, annotations,
                            getterMethod::invoke,
                            if (setterMethod != null) { i, v -> setterMethod(i, v) }
                            else null
                        )
                    }
                }
            }
            return map
        }

        private fun saveField(
            clazz: Class<*>,
            field: Field,
            javaName: String, savedName: String,
            serial: SerializedProperty?,
            serialize: Boolean,
            annotations: List<Annotation>
        ): CachedProperty {
            // save the field
            field.isAccessible = true
            val capName = javaName.titlecase()
            val setterMethod = try {
                clazz.getMethod(getSetterName(javaName, capName), field.type)
            } catch (e: NoSuchMethodException) {
                null
            }
            if (serialize && setterMethod == null) {
                LOGGER.warn("Missing setter for $clazz.$javaName")
            }
            val getterMethod = try {
                clazz.getMethod(getGetterName(javaName, capName))
            } catch (e: NoSuchMethodException) {
                null
            }
            return saveField(
                clazz, field.type, savedName, serial,
                serialize && setterMethod != null, annotations,
                createGetter(getterMethod, field),
                createSetter(setterMethod, field)
            )
        }

        private fun createGetter(getterMethod: Method?, field: Field): (Any) -> Any? {
            return if (getterMethod != null && getterMethod.returnType == field.type) getterMethod::invoke
            else field::get
        }

        private fun createSetter(setterMethod: Method?, field: Field): (Any, Any?) -> Unit {
            return if (setterMethod != null) setterMethod::invoke
            else { i, v -> field.set(i, v) }
        }

        private fun saveField(
            instanceClass: Class<*>,
            valueClass: Class<*>,
            name: String,
            serial: SerializedProperty?,
            serialize: Boolean,
            annotations: List<Annotation>,
            getter: (instance: Any) -> Any?,
            setter: ((instance: Any, value: Any?) -> Unit)?
        ): CachedProperty {
            // save the field
            val forceSaving = serial?.forceSaving ?: (valueClass == Boolean::class.java)
            return CachedProperty(
                name, instanceClass, valueClass, serialize, forceSaving,
                annotations, getter, setter
            )
        }
    }
}