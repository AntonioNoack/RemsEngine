package me.anno.utils

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
}