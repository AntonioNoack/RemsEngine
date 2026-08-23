package me.anno.maths.geometry

import me.anno.maths.Packing.pack64
import me.anno.utils.structures.arrays.IntArrayList
import kotlin.math.min

/**
 * Simple shelf-based rectangle packing.
 *
 * Rectangles are packed left-to-right into shelves. A shelf has a fixed
 * height equal to the tallest rectangle placed into it. Once a rectangle
 * has been placed into a shelf, the shelf height does not change (much).
 *
 * This is intentionally a simple and predictable algorithm. It avoids
 * the potentially expensive skyline updates/scans of SkylinePacking.
 */
class ShelfPacking(
    val width: Int,
    val height: Int,
    val heightTolerance: Int = 0,
    val extraChecks: Int = 16,
) {

    companion object {
        private const val Y0 = 0
        private const val X0 = 1
        private const val Y1 = 2
        private const val STRIDE = 2
    }

    private val shelves = IntArrayList()

    init {
        // last shelf is 0 tall
        shelves.add(0)
    }

    fun clear() {
        shelves.clear()
        shelves.add(0)
    }

    fun allocate(rectWidth: Int, rectHeight: Int): Long {
        if (rectWidth <= 0 || rectHeight <= 0) return 0L
        if (rectWidth > width || rectHeight > height) return -1L

        // find first (best*) fitting shelf
        var i = 0
        while (i + STRIDE < shelves.size) {
            val shelfY = shelves[i + Y0]
            if (shelfY + rectHeight > height) return -1L // impossible to find a spot

            val shelfHeight = shelves[i + Y1] - shelfY
            val shelfX = shelves[i + X0]
            if (rectHeight <= shelfHeight &&
                shelfX + rectWidth <= width
            ) return findBestShelfAt(i, shelfHeight, rectWidth, rectHeight)

            i += STRIDE
        }

        // check whether we can grow the last shelf
        if (shelves.size >= 3) {
            i -= STRIDE
            val shelfY = shelves[i + Y0]
            val shelfHeight = shelves[i + Y1] - shelfY
            val shelfX = shelves[i + X0]
            if (rectHeight <= shelfHeight + heightTolerance &&
                rectHeight <= height - shelfY && // implicitly checked, too
                shelfX + rectWidth <= width
            ) {
                shelves[i + X0] = shelfX + rectWidth
                shelves[i + Y1] = shelfY + rectHeight
                return pack64(shelfX, shelfY)
            }
        }

        // Create a new shelf.
        val y = shelves.last()
        if (y + rectHeight > height) {
            return -1L
        }

        shelves.add(rectWidth) // x
        shelves.add(y + rectHeight) // next y
        return pack64(0, y)
    }

    private fun findBestShelfAt(i0: Int, shelfHeight: Int, rectWidth: Int, rectHeight: Int): Long {
        var i = i0
        var bestI = i
        var bestHeight = shelfHeight
        val iLimit = min(i + extraChecks * STRIDE, shelves.size - STRIDE)
        while (i < iLimit && bestHeight > rectHeight) {
            val shelfY = shelves[i + Y0]
            if (shelfY + rectHeight > height) break // impossible to find another spot

            val shelfHeight = shelves[i + Y1] - shelfY
            val shelfX = shelves[i + X0]
            if (shelfHeight in rectHeight until bestHeight &&
                shelfX + rectWidth <= width
            ) {
                bestI = i
                bestHeight = shelfHeight
            }
            i += STRIDE
        }

        val shelfX = shelves[bestI + X0]
        val shelfY = shelves[bestI + Y0]
        shelves[bestI + X0] = shelfX + rectWidth
        return pack64(shelfX, shelfY)
    }
}