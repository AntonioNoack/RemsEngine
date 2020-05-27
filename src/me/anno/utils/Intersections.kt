package me.anno.utils

import org.joml.Intersectionf
import org.joml.Vector2f

fun getStrictLineIntersection(
    v1: Vector2f, v2: Vector2f, v3: Vector2f, v4: Vector2f
): Vector2f? {
    val result = Vector2f()
    val intersects = Intersectionf.intersectLineLine(
        v1.x, v1.y,
        v2.x, v2.y,
        v3.x, v3.y,
        v4.x, v4.y, result
    )
    if(!intersects) return null
    val d1 = v1.distance(result)
    val d2 = v2.distance(result)
    val d3 = v3.distance(result)
    val d4 = v4.distance(result)
    val innerDistance = d1+d2+d3+d4
    val totalDistance = v1.distance(v2) + v3.distance(v4)
    val isInside = innerDistance < 1.00001f * totalDistance
    val margin = totalDistance * 0.00001f
    val isVeryClose = d1 <= margin || d2 <= margin || d3 <= margin || d4 <= margin
    return if(isInside && !isVeryClose){
        println("$margin, all larger: $d1 $d2 $d3 $d4, $innerDistance < $totalDistance")
        result
    } else null
}
