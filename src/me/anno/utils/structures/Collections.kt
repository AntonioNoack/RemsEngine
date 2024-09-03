package me.anno.utils.structures

import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * utility functions for collections;
 * currently only used for cross product
 * */
@Suppress("unused")
object Collections {

    @JvmStatic
    fun <A, B, R : MutableCollection<Pair<A, B>>> Collection<A>.cross(
        other: Collection<B>, dst: R
    ): R = crossMap(other, dst, ::Pair)

    @JvmStatic
    fun <A, B, C, R : MutableCollection<C>> Collection<A>.crossMap(
        other: Collection<B>, dst: R, transform: (A, B) -> C
    ): R {
        if (dst is ArrayList<*>) {
            dst.ensureCapacity(size * other.size)
        }
        for (a in this) {
            for (b in other) {
                dst += transform(a, b)
            }
        }
        return dst
    }

    @JvmStatic
    fun <A, B, C, R : MutableCollection<Triple<A, B, C>>> Collection<A>.cross(
        other: Collection<B>, other2: Collection<C>, dst: R
    ): R = crossMap(other, other2, dst, ::Triple)

    @JvmStatic
    fun <A, B, C, D, R : MutableCollection<D>> Collection<A>.crossMap(
        other: Collection<B>, other2: Collection<C>, dst: R, transform: (A, B, C) -> D
    ): R {
        if (dst is ArrayList<*>) {
            dst.ensureCapacity(size * other.size * other2.size)
        }
        for (a in this) {
            for (b in other) {
                for (c in other2) {
                    dst += transform(a, b, c)
                }
            }
        }
        return dst
    }

    @JvmStatic
    fun <V> MutableCollection<V>.setContains(element: V, shallContain: Boolean): Boolean {
        return if (shallContain) add(element)
        else remove(element)
    }

    @JvmStatic
    fun <V> MutableSet<V>.toggleContains(element: V) {
        // if it was already contained = no change, then remove it
        if (!add(element)) remove(element)
    }

    /**
     * non-inline filterIsInstance()
     * */
    @JvmStatic
    fun <V : Any> Iterable<*>.filterIsInstance2(clazz: KClass<V>): List<V> {
        return mapNotNull { clazz.safeCast(it) }
    }
}