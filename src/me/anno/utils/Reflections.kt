package me.anno.utils

import me.anno.ui.input.EnumInput.Companion.getEnumConstants
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

object Reflections {

    /**
     * returns super class or null if class = Any::class = Object::class, or class is an interface
     * */
    @JvmStatic
    fun getParentClass(clazz: KClass<*>): KClass<*>? {
        return getParentClasses(clazz).firstOrNull()
    }

    /**
     * returns super class and interfaces
     * */
    @JvmStatic
    fun getParentClasses(clazz: KClass<*>): List<KClass<*>> {
        return clazz.superclasses
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
        } catch (ignored: NoSuchFieldException) {
            null
        }
    }

    private fun getEnumIdMethod(clazz: Class<*>): Method? {
        // must be separate for our JVM without exceptions
        return try {
            clazz.getMethod("getId")
        } catch (ignored: NoSuchMethodException) {
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

    private val enumByClass = HashMap<Class<*>, Map<Int, Enum<*>>>()
    private fun getEnumByIdMap(clazz: Class<*>): Map<Int, Enum<*>> {
        return enumByClass.getOrPut(clazz) {
            val constants = getEnumConstants(clazz)
            val getter = getEnumIdGetter(clazz)
            constants.associateBy { getter(it) }
        }
    }

    fun getEnumById(clazz: Class<*>, id: Int): Enum<*>? {
        return getEnumByIdMap(clazz)[id]
    }
}