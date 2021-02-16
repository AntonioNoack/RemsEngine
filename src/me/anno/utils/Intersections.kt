package me.anno.utils

import org.joml.*

object Intersections {

    fun getStrictLineIntersection(
        v1: Vector2fc, v2: Vector2fc, v3: Vector2fc, v4: Vector2fc
    ): Vector2f? {
        val result = Vector2f()
        val intersects = Intersectionf.intersectLineLine(
            v1.x(), v1.y(),
            v2.x(), v2.y(),
            v3.x(), v3.y(),
            v4.x(), v4.y(), result
        )
        if(!intersects) return null
        val d1 = v1.distance(result)
        val d2 = v2.distance(result)
        val d3 = v3.distance(result)
        val d4 = v4.distance(result)
        val innerDistance = d1+d2+d3+d4
        val totalDistance = v1.distance(v2) + v3.distance(v4)
        val isInside = innerDistance < 1.00001f * totalDistance
        val margin = totalDistance * 0.0001f
        val isVeryClose = d1 <= margin || d2 <= margin || d3 <= margin || d4 <= margin
        return if(isInside && !isVeryClose){
            // ("$margin, all larger: $d1 $d2 $d3 $d4, $innerDistance < $totalDistance")
            result
        } else null
    }


    fun getStrictLineIntersection(
        v1: Vector2dc, v2: Vector2dc, v3: Vector2dc, v4: Vector2dc
    ): Vector2d? {
        val result = Vector2d()
        val intersects = Intersectiond.intersectLineLine(
            v1.x(), v1.y(),
            v2.x(), v2.y(),
            v3.x(), v3.y(),
            v4.x(), v4.y(), result
        )
        if(!intersects) return null
        val d1 = v1.distance(result)
        val d2 = v2.distance(result)
        val d3 = v3.distance(result)
        val d4 = v4.distance(result)
        val innerDistance = d1+d2+d3+d4
        val totalDistance = v1.distance(v2) + v3.distance(v4)
        val isInside = innerDistance < 1.00001f * totalDistance
        val margin = totalDistance * 0.0001f
        val isVeryClose = d1 <= margin || d2 <= margin || d3 <= margin || d4 <= margin
        return if(isInside && !isVeryClose){
            // ("$margin, all larger: $d1 $d2 $d3 $d4, $innerDistance < $totalDistance")
            result
        } else null
    }

}