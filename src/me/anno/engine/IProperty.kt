package me.anno.engine

import me.anno.ui.base.Panel

interface IProperty<V> {

    val annotations: List<Annotation>

    fun set(panel: Panel?, value: V)
    fun get(): V

    fun getDefault(): V

    fun reset(panel: Panel?): V

    fun init(panel: Panel?)

}