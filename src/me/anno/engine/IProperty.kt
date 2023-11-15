package me.anno.engine

import me.anno.ui.Panel

/**
 * Setting, Getting, and Resetting for ComponentUI and NullableInput
 * */
interface IProperty<V> {

    val annotations: List<Annotation>

    fun set(panel: Panel?, value: V, mask: Int)
    fun set(panel: Panel?, value: V) = set(panel, value, -1)

    fun get(): V

    fun getDefault(): V

    fun reset(panel: Panel?): V

    fun init(panel: Panel?)

}