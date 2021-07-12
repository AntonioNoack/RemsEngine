package me.anno.engine

interface IProperty<V> {

    val annotations: List<Annotation>

    fun set(value: V)
    fun get(): V

    fun getDefault(): V

    fun reset(): V

}