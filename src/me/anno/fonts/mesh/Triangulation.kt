package me.anno.fonts.mesh

import org.joml.Vector2d
import org.joml.Vector2f
import org.the3deers.util.EarCut

object Triangulation {

    fun ringToTriangles2(points: List<Vector2f>, name: String): List<Vector2f> {
        val joint = FloatArray(points.size * 2)
        points.forEachIndexed { index, vector2d -> joint[index*2] = vector2d.x; joint[index*2+1] = vector2d.y }
        val indices = EarCut.earcut(joint, intArrayOf(), 2)
        return indices.map { index -> points[index] }
    }

    fun ringToTriangles(points: List<Vector2d>, name: String): List<Vector2d> {
        val joint = FloatArray(points.size * 2)
        points.forEachIndexed { index, vector2d -> joint[index*2] = vector2d.x.toFloat(); joint[index*2+1] = vector2d.y.toFloat() }
        val indices = EarCut.earcut(joint, intArrayOf(), 2)
        return indices.map { index -> points[index] }
    }

}