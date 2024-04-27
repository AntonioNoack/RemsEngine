package me.anno.graph.octtree

import kotlin.math.max

fun getMaxComponent(x: Double, y: Double): Int = if (y >= x) 1 else 0
fun getMaxComponent(x: Double, y: Double, z: Double): Int = if (z >= max(x, y)) 2 else getMaxComponent(x, y)
fun getMaxComponent(x: Double, y: Double, z: Double, w: Double): Int =
    if (w >= max(x, max(y, z))) 3 else getMaxComponent(x, y)
