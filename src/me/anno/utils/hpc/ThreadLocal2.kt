package me.anno.utils.hpc

class ThreadLocal2<V>(private val generator: () -> V) : ThreadLocal<V>() {
    override fun initialValue(): V = generator()
}