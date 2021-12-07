package me.anno.utils

import org.joml.*

object Intersections {

    fun getStrictLineIntersection(
        s0: Vector2fc, e0: Vector2fc, s1: Vector2fc, e1: Vector2fc
    ): Vector2f? {
        val result = Vector2f()
        val intersects = Intersectionf.intersectLineLine(
            s0.x(), s0.y(),
            e0.x(), e0.y(),
            s1.x(), s1.y(),
            e1.x(), e1.y(), result
        )
        if(!intersects) return null
        val d1 = s0.distance(result)
        val d2 = e0.distance(result)
        val d3 = s1.distance(result)
        val d4 = e1.distance(result)
        val innerDistance = d1+d2+d3+d4
        val totalDistance = s0.distance(e0) + s1.distance(e1)
        val isInside = innerDistance < 1.00001f * totalDistance
        val margin = totalDistance * 0.0001f
        val isVeryClose = d1 <= margin || d2 <= margin || d3 <= margin || d4 <= margin
        return if(isInside && !isVeryClose){
            // ("$margin, all larger: $d1 $d2 $d3 $d4, $innerDistance < $totalDistance")
            result
        } else null
    }


    fun getStrictLineIntersection(
        s0: Vector2dc, e0: Vector2dc, s1: Vector2dc, e1: Vector2dc
    ): Vector2d? {
        val result = Vector2d()
        val intersects = Intersectiond.intersectLineLine(
            s0.x(), s0.y(),
            e0.x(), e0.y(),
            s1.x(), s1.y(),
            e1.x(), e1.y(), result
        )
        if(!intersects) return null
        val d1 = s0.distance(result)
        val d2 = e0.distance(result)
        val d3 = s1.distance(result)
        val d4 = e1.distance(result)
        val innerDistance = d1+d2+d3+d4
        val totalDistance = s0.distance(e0) + s1.distance(e1)
        val isInside = innerDistance < 1.00001f * totalDistance
        val margin = totalDistance * 0.0001f
        val isVeryClose = d1 <= margin || d2 <= margin || d3 <= margin || d4 <= margin
        return if(isInside && !isVeryClose){
            // ("$margin, all larger: $d1 $d2 $d3 $d4, $innerDistance < $totalDistance")
            result
        } else null
    }

}