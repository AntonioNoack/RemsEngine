package me.anno.tests

import me.anno.utils.hpc.HeavyProcessing

fun main() {
    // should return (5,1)
    println(HeavyProcessing.splitWork(50, 10, 5))
    // should return (7,1)
    println(HeavyProcessing.splitWork(50, 10, 7))
    // should return (4,2)
    println(HeavyProcessing.splitWork(50, 10, 8))
}