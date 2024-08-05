package me.anno.utils

import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

object Reflections {
    @JvmStatic
    fun getParentClass(clazz: KClass<*>): KClass<*>? {
        return clazz.superclasses.firstOrNull()
    }
}