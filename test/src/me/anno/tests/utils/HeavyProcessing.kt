package me.anno.tests.utils

import me.anno.utils.hpc.HeavyProcessing.splitWork

fun main() {
    // should return (5,1)
    println(splitWork(50, 10, 5))
    // should return (7,1)
    println(splitWork(50, 10, 7))
    // should return (4,2)
    println(splitWork(50, 10, 8))
}