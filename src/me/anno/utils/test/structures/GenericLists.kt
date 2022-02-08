package me.anno.utils.test.structures

import me.anno.utils.Warning.unused

fun main() {
    testList(listOf(1, 2, 3))
    testList(listOf("a", "b", "c"))
    secondTest(listOf("cool house") as List<*>)
}

fun testList(list: List<*>) {
    val clazz = list::class
    // LOGGER.info(clazz.nestedClasses)
    // LOGGER.info(clazz.objectInstance)
    println(clazz.typeParameters)
    println(clazz)
}

// to do idea: our own kotlin derivative & language with explicit type information?
inline fun <reified T> secondTest(sample: List<T>) {
    unused(sample)
    println(T::class)
}