package me.anno.utils.hpc

fun <V> threadLocal(generator: () -> V): ThreadLocal<V> = ThreadLocal.withInitial(generator)