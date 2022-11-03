package me.anno.io.serialization

import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.DebugWarning
import me.anno.ecs.annotations.ExecuteInEditMode
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible


// todo fix: get<Name>() is missing in Java, e.g. position, scale
class CachedReflections(
    val clazz: KClass<*>,
    val allProperties: Map<String, CachedProperty>,
    val declaredProperties: Map<String, CachedProperty>,
    val debugActions: List<KFunction<*>>,
    val debugProperties: List<KProperty<*>>,
    val debugWarnings: List<KProperty<*>>
) {

    @Suppress("unused")
    constructor(instance: Any) : this(instance, instance::class)

    constructor(instance: Any, clazz: KClass<*>) : this(
        clazz,
        getMemberProperties(instance, clazz),
        getDeclaredMemberProperties(instance, clazz),
        getDebugActions(clazz),
        getDebugProperties(clazz),
        getDebugWarnings(clazz)
    )

    val annotations =
        if (OS.isWeb) clazz.java.annotations.toList()
        else clazz.annotations

    val propertiesByClass = lazy {
        getPropertiesByDeclaringClass(clazz, allProperties)
    }

    // todo use this
    val executeInEditMode = annotations.any { it is ExecuteInEditMode }

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

        fun getMemberProperties(instance: Any, clazz: KClass<*>): Map<String, CachedProperty> {
            /* return try {
                 findProperties(instance, clazz, clazz.memberProperties.filterIsInstance<KMutableProperty1<*, *>>())
             } catch (e: ClassCastException) {*/
            val jc = clazz.java
            return findProperties(instance, allFields(jc, ArrayList()))
            // }
        }

        fun allFields(clazz: Class<*>, list: ArrayList<Field>): List<Field> {
            list.addAll(clazz.declaredFields)
            val superClass = clazz.superclass
            if (superClass != null) allFields(superClass, list)
            return list
        }

        fun getDeclaredMemberProperties(instance: Any, clazz: KClass<*>): Map<String, CachedProperty> {
            return if (OS.isWeb) {
                emptyMap()
            } else {
                val properties = clazz.declaredMemberProperties.filterIsInstance<KMutableProperty1<*, *>>()
                findProperties(instance, clazz, properties)
            }
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
            instance: Any,
            clazz: KClass<*>,
            properties: List<KMutableProperty1<*, *>>
        ): Map<String, CachedProperty> {
            // this is great: declaredMemberProperties in only what was changes, so we can really create listener lists :)
            val map = HashMap<String, CachedProperty>()
            for (index in properties.indices) {
                val field = properties[index]
                val isPublic = field.visibility == KVisibility.PUBLIC
                val serial = field.findAnnotation<SerializedProperty>()
                val notSerial = field.findAnnotation<NotSerializedProperty>()
                val serialize = serial != null || (isPublic && notSerial == null)
                try {
                    // save the field
                    var name = serial?.name
                    if (name == null || name.isEmpty()) name = field.name
                    if (name in map) continue
                    // make sure we can access it
                    val setter = field.setter
                    setter.isAccessible = true
                    val getter = field.getter
                    getter.isAccessible = true
                    val value = getter.call(instance)
                    val forceSaving = serial?.forceSaving ?: (value is Boolean)
                    val property = CachedProperty(
                        name, clazz, serialize, forceSaving,
                        field.annotations, { getter.call(it) }, { i, v -> setter.call(i, v) }
                    )
                    map[name] = property
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return map
        }

        fun findProperties(
            instance: Any,
            properties: List<Field>
        ): Map<String, CachedProperty> {
            // this is great: declaredMemberProperties in only what was changes, so we can really create listener lists :)
            val map = HashMap<String, CachedProperty>()
            for (index in properties.indices) {
                val field = properties[index]
                val annotations = field.annotations
                val serial = annotations.firstOrNull { it is SerializedProperty } as? SerializedProperty
                val notSerial = annotations.firstOrNull { it is NotSerializedProperty }
                val isPublic = Modifier.isPublic(field.modifiers)
                val serialize = serial != null || (isPublic && notSerial == null)
                var name = serial?.name
                if (name == null || name.isEmpty()) name = field.name
                if (name in map) continue
                try {
                    saveField(instance, field, name!!, serial, serialize, annotations.toList(), map)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return map
        }

        private fun saveField(
            instance: Any, field: Field, name: String,
            serial: SerializedProperty?,
            serialize: Boolean,
            annotations: List<Annotation>,
            map: MutableMap<String, CachedProperty>
        ) {
            // save the field
            field.isAccessible = true
            val value = field.get(instance)
            val forceSaving = serial?.forceSaving ?: (value is Boolean)
            val property = CachedProperty(
                name, field::class, serialize, forceSaving,
                annotations, {
                    field.get(it)
                }, { i, v ->
                    field.set(i, v)
                }
            )
            map[name] = property
        }
    }


}
