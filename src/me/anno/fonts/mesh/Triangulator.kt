package me.anno.fonts.mesh

import me.anno.utils.getSideSign
import org.joml.Vector2d

object Triangulator {

    // todo convert non-convex shape into convex shapes?
    // todo remove linear segments

    fun ringToTriangles(points: List<Vector2d>): List<Vector2d> {
        return makeConvex(points)
    }

    fun makeConvex(points0: List<Vector2d>): List<Vector2d> {

        var points = points0.toMutableList()
        var i = 0
        var isNegative = 0
        while(i < points.size){
            val p0 = points[i]
            val p1 = points[(i+1) % points.size]
            val p2 = points[(i+2) % points.size]
            val sign = p0.getSideSign(p1, p2)
            when {
                sign < 0.0 -> {
                    isNegative++
                    i++
                }
                sign > 0.0 -> {
                    i++
                }
                else -> {
                    points.removeAt((i+1) % points.size)
                }
            }
        }

        if(isNegative >= points.size/2){
            points = points.asReversed()
        }

        return Triangulator0.ringToTriangles(points)

    }


}