package me.anno.utils

import me.anno.ui.input.EnumInput.Companion.getEnumConstants
import speiger.primitivecollections.HashUtil.initialSize
import speiger.primitivecollections.IntToObjectHashMap
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

object Reflections {

    private val superClassCache = ConcurrentHashMap<KClass<*>, List<KClass<*>>>()
    private val parentClassCache = ConcurrentHashMap<KClass<*>, KClass<*>>()

    /**
     * returns super class or null if class = Any::class = Object::class, or class is an interface
     * */
    @JvmStatic
    fun getParentClass(clazz: KClass<*>): KClass<*>? {
        return parentClassCache.getOrPut(clazz) {
            getParentClasses(clazz).firstOrNull { superClass ->
                !superClass.java.isInterface
            }
        }
    }

    /**
     * returns super class and interfaces
     * */
    @JvmStatic
    fun getParentClasses(clazz: KClass<*>): List<KClass<*>> {
        return superClassCache.getOrPut(clazz) { clazz.superclasses }
    }

    @JvmStatic
    fun <V : Any> getBaseConstructor(clazz: KClass<V>): () -> V {
        return { clazz.java.newInstance() }
        // pure kotlin solution:
        /*val constructor = clazz.constructors.first {
            it.parameters.isEmpty()
        }
        return { constructor.call() as V }*/
    }


    private fun getEnumIdField(clazz: Class<*>): Field? {
        // must be separate for our JVM without exceptions
        return try {
            clazz.getField("id")
        } catch (_: NoSuchFieldException) {
            null
        }
    }

    private fun getEnumIdMethod(clazz: Class<*>): Method? {
        // must be separate for our JVM without exceptions
        return try {
            clazz.getMethod("getId")
        } catch (_: NoSuchMethodException) {
            null
        }
    }

    fun getEnumId(value: Any): Int {
        // todo why is this not saved as an input for nodes when cloning???
        val clazz = value.javaClass
        val getter = getEnumIdGetter(clazz)
        return getter(value)
    }

    private val enumIdByClass = HashMap<Class<*>, (Any?) -> Int>()
    private fun getEnumIdGetter0(clazz: Class<*>): (Any?) -> Int {
        val field = getEnumIdField(clazz)
        if (field != null) return { field.get(it) as? Int ?: getOrdinal(it) }
        val method = getEnumIdMethod(clazz)
        if (method != null) return { method.invoke(it) as? Int ?: getOrdinal(it) }
        return Reflections::getOrdinal
    }

    private fun getOrdinal(value: Any?): Int {
        return (value as? Enum<*>)?.ordinal ?: -1
    }

    private fun getEnumIdGetter(clazz: Class<*>): (Any?) -> Int {
        return enumIdByClass.getOrPut(clazz) {
            getEnumIdGetter0(clazz)
        }
    }

    private val enumByClass = HashMap<Class<*>, IntToObjectHashMap<Enum<*>>>()
    private fun getEnumByIdMap(clazz: Class<*>): IntToObjectHashMap<Enum<*>> {
        return enumByClass.getOrPut(clazz) {
            val enumValues = getEnumConstants(clazz)
            val getter = getEnumIdGetter(clazz)
            val minSize = initialSize(enumValues.size)
            val idToEnum = IntToObjectHashMap<Enum<*>>(minSize)
            for (i in enumValues.indices) {
                val constant = enumValues[i]
                idToEnum.put(getter(constant), constant)
            }
            idToEnum
        }
    }

    fun getEnumById(clazz: Class<*>, id: Int): Enum<*>? {
        return getEnumByIdMap(clazz)[id]
    }
}