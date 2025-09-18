package me.anno.maths.patterns

import me.anno.maths.Maths.TAUf
import me.anno.maths.MinMax.max
import me.anno.maths.Maths.sq
import org.joml.Vector3i
import kotlin.math.atan2
import kotlin.math.round

/**
 * patterns that, e.g., can be used for chunk loading
 * */
object SpiralPattern {

    /**
     * radius is inclusive
     * */
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
            val angle01 = atan2(it.z.toFloat(), it.x.toFloat()) / TAUf
            val turnIndex = round((distance + angle01) / increment)
            turnIndex - angle01
        }
        return list
    }

    /**
     * radius is inclusive
     * */
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

    /**
     * radius is inclusive
     * */
    @Suppress("unused")
    fun spiral3d(radius: Int, fillBlock: Boolean): List<Vector3i> {
        val result = ArrayList<Vector3i>()
        val maxRadius = if (fillBlock) radius * 2 else radius
        for (r in 0..maxRadius) {
            if (r < radius) {
                spiral2d(r, 0, false, result)
            }
            // first build floor, then ceiling
            for (dy in 1..radius) {
                val r2 = r - dy
                if (r2 in 0..radius) {
                    spiral2d(r2, -dy, false, result)
                }
            }
            for (dy in 1..radius) {
                val r2 = r - dy
                if (r2 in 0..radius) {
                    spiral2d(r2, +dy, false, result)
                }
            }
        }
        return result
    }

    /**
     * radius is inclusive
     * */
    @Suppress("unused")
    fun spiral2dStack(radius: Int, y0: Int, y1: Int, full: Boolean): List<Vector3i> {
        val result = ArrayList<Vector3i>()
        for (sample in spiral2d(radius, y0, full)) {
            result.add(sample)
            for (y in y0 + 1 until y1) {
                val clone = Vector3i(sample)
                clone.y = y
                result.add(clone)
            }
        }
        return result
    }

    /**
     * radius is inclusive
     * */
    fun spiral2d(radius: Int, y: Int, full: Boolean): List<Vector3i> {
        val size =
            if (full) sq(radius * 2 + 1)
            else max((radius * 2) * 4, 1)
        val result = ArrayList<Vector3i>(size)
        spiral2d(radius, y, full, result)
        return result
    }

    /**
     * radius is inclusive
     * */
    fun spiral2d(radius: Int, y: Int, full: Boolean, result: ArrayList<Vector3i>) {
        if (full) {
            for (r in 0..radius) {
                spiral2d(r, y, false, result)
            }
        } else if (radius == 0) {
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
    }
}