package me.anno.utils.structures.sets

fun main(){

    val set = ParallelHashSet<Int>()
    set.add(1)
    set.add(16)
    set.add(-5)

    set.process { println(it) }

}