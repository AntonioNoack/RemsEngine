package me.anno.maths.patterns

import me.anno.image.ImageWriter
import me.anno.maths.Maths.sq
import org.joml.Vector3i
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.round

/**
 * patterns that, e.g., can be used for chunk loading
 * */
object SpiralPattern {

    fun roundSpiral2d(radius: Int, y: Int, fillBlock: Boolean, increment: Float = 1.5f): List<Vector3i> {
        val size = 2 * radius + 1
        val list = ArrayList<Vector3i>(size * size * size)
        val rsq = sq(radius + 0.5f).toInt()
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                if (fillBlock || x * x + z * z < rsq) {
                    list.add(Vector3i(x, y, z))
                }
            }
        }
        list.sortBy {
            // compute spiral index
            // compute spiral angle
            val distance = it.distance(0, y, 0)
            val angle01 = atan2(it.z.toFloat(), it.x.toFloat()) / (PI * 2.0).toFloat()
            val turnIndex = round((distance + angle01) / increment)
            turnIndex - angle01
        }
        return list
    }

    @Suppress("unused")
    fun sortedBlock(radius: Int): List<Vector3i> {
        val size = 2 * radius + 1
        val list = ArrayList<Vector3i>(size * size * size)
        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in radius..radius) {
                    list.add(Vector3i(x, y, z))
                }
            }
        }
        list.sortBy { it.lengthSquared() }
        return list
    }

    @Suppress("unused")
    fun spiral3d(radius: Int, fillBlock: Boolean): List<Vector3i> {
        val size = 2 * radius + 1
        val result = ArrayList<Vector3i>(size * size * size)
        for (r in 0 until if (fillBlock) radius * 2 else radius) {
            if (r < radius) spiral2d(r, 0, result)
            // first build floor, then ceiling
            for (dy in 1 until radius) {
                val r2 = r - dy
                if (r2 < radius) spiral2d(r2, -dy, result)
            }
            for (dy in 1 until radius) {
                val r2 = r - dy
                if (r2 < radius) spiral2d(r2, +dy, result)
            }
        }
        return result
    }

    fun spiral2d(radius: Int, y: Int, result: ArrayList<Vector3i> = ArrayList((8 * radius + 4))): List<Vector3i> {
        if (radius == 0) {
            result.add(Vector3i(0, y, 0))
        } else {
            // center to right on the top
            for (x in 0 until radius) {
                result.add(Vector3i(x, y, radius))
            }
            // top to bottom on the right
            for (z in radius downTo -radius + 1) {
                result.add(Vector3i(radius, y, z))
            }
            // right to left at the bottom
            for (x in radius downTo -radius + 1) {
                result.add(Vector3i(x, y, -radius))
            }
            // bottom to top at the left
            for (z in -radius until radius) {
                result.add(Vector3i(-radius, y, z))
            }
            for (x in -radius until 0) {
                result.add(Vector3i(x, y, radius))
            }
        }
        return result
    }


}