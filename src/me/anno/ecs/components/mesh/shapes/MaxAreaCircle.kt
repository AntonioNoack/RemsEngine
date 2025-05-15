package me.anno.ecs.components.mesh.shapes

import me.anno.maths.Maths.max
import me.anno.maths.Maths.posMod
import me.anno.utils.assertions.assertTrue

/**
 * Thin triangles cause extra pixels to be evaluated and are therefore a waste on planar surfaces.
 * So when creating a circle, use this class to create as well shaped triangles as possible
 * without adding any extra indices.
 * */
object MaxAreaCircle {

    fun numCircleIndices(n: Int): Int {
        return max(n - 2, 0) * 3
    }

    fun flipIndices(dst: IntArray, i0: Int, i1: Int) {
        val endI = (i0 + i1) ushr 1
        val endJ = i1 + i0 - 1
        for (i in i0 until endI) {
            val j = endJ - i
            val tmp = dst[i]
            dst[i] = dst[j]
            dst[j] = tmp
        }
    }

    fun createCircleIndices(n: Int, iOffset: Int, dst: IntArray, dstOffset: Int, flip: Boolean) {

        if (n < 3) return
        val a = n / 3
        val b = (2 * n) / 3

        // fill in main triangle
        var dstI = dstOffset
        dst[dstI++] = 0
        dst[dstI++] = a
        dst[dstI++] = b

        @Suppress("KotlinConstantConditions")
        assertTrue(a > 0)
        assertTrue(b > a)
        assertTrue(n > b)

        // fill in remaining small triangles
        if (a > 1) {
            createThirdsIndices(0, a, n, dst, dstI)
            dstI += 3 * (a - 1)
        }

        if (b > a + 1) {
            createThirdsIndices(a, b, n, dst, dstI)
            dstI += 3 * (b - a - 1)
        }

        if (n > b + 1) {
            createThirdsIndices(b, n, n, dst, dstI)
            dstI += 3 * (n - b - 1)
        }

        for (i in dstOffset until dstI) {
            dst[i] += iOffset
        }

        if (!flip) flipIndices(dst, dstOffset, dstI)
    }

    // use the stack instead? depth is just log2(n), so not too bad
    private fun createThirdsIndices(
        i0: Int, i1: Int, n: Int,
        dst: IntArray, dstOffset: Int
    ) {

        val middle = (i0 + i1) ushr 1
        if (middle == i0 || middle == i1) return

        // add main triangle
        var dstI = dstOffset
        dst[dstI++] = i0
        dst[dstI++] = middle
        dst[dstI++] = posMod(i1, n)

        if (middle > i0 + 1) {
            // add remaining corners
            createThirdsIndices(i0, middle, n, dst, dstI)
            dstI += 3 * (middle - i0 - 1)
        }

        if (i1 > middle + 1) {
            createThirdsIndices(middle, i1, n, dst, dstI)
            // dstI += 3 * (i1 - middle - 1)
        }
    }
}