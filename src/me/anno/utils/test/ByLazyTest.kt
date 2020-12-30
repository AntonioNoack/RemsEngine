package me.anno.utils.test

val lazy by lazy {
    println("invoked by lazy")
    null
}

fun main() {

    println(lazy)
    println(lazy)
    println(lazy)

}