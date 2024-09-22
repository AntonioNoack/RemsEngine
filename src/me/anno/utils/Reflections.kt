package me.anno.utils

import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.jvmName

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
}