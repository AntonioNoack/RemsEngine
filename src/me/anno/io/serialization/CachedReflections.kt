package me.anno.io.serialization

import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.DebugWarning
import me.anno.utils.OS
import me.anno.utils.strings.StringHelper.titlecase
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

    val annotations =
        if (OS.isWeb) clazz.java.annotations.toList()
        else clazz.annotations

    val propertiesByClass by lazy {
        getPropertiesByDeclaringClass(clazz, allProperties)
    }

    val propertiesByClassList by lazy {
        propertiesByClass.map { it.second }.flatten()
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
        val value2 = if (value is Array<*> && property[self] !is Array<*>) {
            value.toList()
        } else value
        return property.set(self, value2)
    }

    operator fun get(self: Any, name: String): Any? {
        val property = allProperties[name] ?: return null
        return property[self]
    }

    operator fun get(name: String) = allProperties[name]

    companion object {

        fun getDebugActions(clazz: KClass<*>): List<KFunction<*>> {
            if (OS.isWeb) return emptyList()
            var list = emptyList<KFunction<*>>()
            for (func in clazz.memberFunctions) {
                if (func.annotations.any { it is DebugAction }) {
                    if (list is MutableList) list.add(func)
                    else list = arrayListOf(func)
                }
            }
            return list
        }

        fun getDebugProperties(clazz: KClass<*>): List<KProperty<*>> {
            if (OS.isWeb) return emptyList()
            var list = emptyList<KProperty<*>>()
            for (func in clazz.memberProperties) {
                if (func.annotations.any { it is DebugProperty }) {
                    if (list is MutableList) list.add(func)
                    else list = arrayListOf(func)
                }
            }
            return list
        }

        fun getDebugWarnings(clazz: KClass<*>): List<KProperty<*>> {
            if (OS.isWeb) return emptyList()
            var list = emptyList<KProperty<*>>()
            for (func in clazz.memberProperties) {
                if (func.annotations.any { it is DebugWarning }) {
                    if (list is MutableList) list.add(func)
                    else list = arrayListOf(func)
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

        fun allFields(clazz: Class<*>, list: ArrayList<Field>): List<Field> {
            list.addAll(clazz.declaredFields)
            val superClass = clazz.superclass
            if (superClass != null) allFields(superClass, list)
            return list
        }

        fun allMethods(clazz: Class<*>, list: ArrayList<Method>): List<Method> {
            list.addAll(clazz.declaredMethods)
            val superClass = clazz.superclass
            if (superClass != null) allMethods(superClass, list)
            return list
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
                if (name == null || name.isEmpty()) name = field.name
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
                if (name.startsWith("get") &&
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
                        val setterMethod = methods.firstOrNull { it.name == setterName } ?: continue
                        val annotations = getterMethod.annotations.toMutableList()
                        val kotlinAnnotationName = "$name\$annotations"
                        val m = methods.firstOrNull { it.name == kotlinAnnotationName }
                        if (m != null) annotations += m.annotations.toList()
                        val serial = annotations.firstOrNull { it is SerializedProperty } as? SerializedProperty
                        val notSerial = annotations.firstOrNull { it is NotSerializedProperty }
                        val isPublic = Modifier.isPublic(getterMethod.modifiers)
                        val serialize = serial != null || (isPublic && notSerial == null)
                        map[betterName] = saveField(
                            getterMethod.returnType.kotlin, betterName, serial, serialize,
                            annotations, getterMethod::invoke, setterMethod::invoke
                        )
                    }
                }
            }
            return map
        }

        private fun saveField(
            field: Field, name: String, serial: SerializedProperty?,
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
                field.type.kotlin, name, serial,
                serialize, annotations,
                {
                    if (getterMethod != null) getterMethod.invoke(it)
                    else field.get(it)
                }, { i, v ->
                    if (setterMethod != null) setterMethod.invoke(i, v)
                    else field.set(i, v)
                }
            )
        }

        private fun saveField(
            valueClass: KClass<*>, name: String, serial: SerializedProperty?,
            serialize: Boolean,
            annotations: List<Annotation>,
            getter: (Any?) -> Any?,
            setter: (Any?, Any?) -> Unit
        ): CachedProperty {
            // save the field
            val forceSaving = serial?.forceSaving ?: (valueClass == Boolean::class)
            return CachedProperty(
                name, valueClass, serialize, forceSaving,
                annotations, getter, setter
            )
        }
    }

}
