package me.anno.maths.patterns

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
    fun spiral3d(radius: Int, fillBlock: Boolean): Sequence<Vector3i> {
        return sequence {
            for (r in 0 until if (fillBlock) radius * 2 else radius) {
                if (r < radius) yieldAll(spiral2d(r, 0, false))
                // first build floor, then ceiling
                for (dy in 1 until radius) {
                    val r2 = r - dy
                    if (r2 < radius) {
                        yieldAll(spiral2d(r2, -dy, false))
                    }
                }
                for (dy in 1 until radius) {
                    val r2 = r - dy
                    if (r2 < radius) {
                        yieldAll(spiral2d(r2, +dy, false))
                    }
                }
            }
        }
    }

    fun spiral2dStack(radius: Int, y0: Int, y1: Int, full: Boolean): Sequence<Vector3i> {
        return sequence {
            val base = spiral2d(radius, y0, full).iterator()
            while (base.hasNext()) {
                val sample = base.next()
                yield(sample)
                for (y in y0 + 1 until y1) {
                    val clone = Vector3i(sample)
                    clone.y = y
                    yield(clone)
                }
            }
        }
    }

    fun spiral2d(radius: Int, y: Int, full: Boolean): Sequence<Vector3i> {
        return sequence {
            if (full) {
                for (r in 0 until radius) {
                    yieldAll(spiral2d(r, y, false))
                }
            } else if (radius == 0) {
                yield(Vector3i(0, y, 0))
            } else {
                // center to right on the top
                for (x in 0 until radius) {
                    yield(Vector3i(x, y, radius))
                }
                // top to bottom on the right
                for (z in radius downTo -radius + 1) {
                    yield(Vector3i(radius, y, z))
                }
                // right to left at the bottom
                for (x in radius downTo -radius + 1) {
                    yield(Vector3i(x, y, -radius))
                }
                // bottom to top at the left
                for (z in -radius until radius) {
                    yield(Vector3i(-radius, y, z))
                }
                for (x in -radius until 0) {
                    yield(Vector3i(x, y, radius))
                }
            }
        }
    }
}