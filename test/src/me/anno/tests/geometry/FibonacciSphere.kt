package me.anno.tests.geometry

import me.anno.maths.geometry.FibonacciSphere
import me.anno.utils.types.Vectors.print

fun main() {
    val pts = FibonacciSphere.create(12)
    pts.forEach {
        println(it.print())
    }
}