package me.anno.maths.geometry

import me.anno.maths.MinMax.max
import me.anno.maths.Packing.pack64
import me.anno.utils.structures.arrays.IntArrayList
import kotlin.math.abs

class SkylinePacking(
    val width: Int,
    val height: Int,
    // this allows growth :/
    val heightTolerance: Int = 0
) {

    // node i covers [x[i], x[i] + w[i]) at height y[i].
    private val xyw = IntArrayList()

    init {
        require(width > 0)
        require(height > 0)

        clear()
    }

    fun clear() {
        xyw.clear()
        // Initially the entire atlas is empty.
        xyw.add3(0, 0, width)
    }

    private fun IntArrayList.add3(x: Int, y: Int, w: Int) {
        ensureExtra(3)
        addUnsafe(x)
        addUnsafe(y)
        addUnsafe(w)
    }

    private fun IntArrayList.add3At(index: Int, x: Int, y: Int, w: Int) {
        insertRange(index, 3)
        xyw.values[index] = x
        xyw.values[index + 1] = y
        xyw.values[index + 2] = w
    }

    private fun IntArrayList.remove3At(index: Int) {
        removeRange(index, index + 3)
    }

    /**
     * Attempts to allocate a rectangle.
     *
     * Returns the top-left/bottom-left atlas position depending on
     * your coordinate convention, or null if it doesn't fit.
     */
    fun allocate(rectWidth: Int, rectHeight: Int): Long {
        require(rectWidth > 0)
        require(rectHeight > 0)

        if (rectWidth > width || rectHeight > height) return -1L

        var bestNode = -1
        var bestX = 0
        var bestY = Int.MAX_VALUE

        var i = 0
        while (i < xyw.size) {
            val candidateY = fitY(i, rectWidth)
            if (candidateY >= 0 &&
                candidateY + rectHeight <= height
            ) {
                // Bottom-left heuristic:
                // choose the lowest possible placement.
                //
                // Tie-breaking by x makes allocation deterministic.
                val candidateX = xyw[i]
                if (candidateY < bestY ||
                    (candidateY == bestY && candidateX < bestX)
                ) {
                    bestNode = i
                    bestX = candidateX
                    bestY = candidateY
                }
            }

            i += 3
        }

        if (bestNode < 0) {
            return -1
        }

        addSkylineLevel(
            bestNode,
            bestX,
            bestY,
            rectWidth,
            rectHeight
        )

        return pack64(bestX, bestY)
    }

    /**
     * Finds the height of the skyline under a rectangle starting
     * at nodeIndex.
     *
     * Returns -1 if the rectangle extends beyond the atlas width.
     */
    private fun fitY(
        nodeIndex: Int,
        rectWidth: Int
    ): Int {
        val startX = xyw[nodeIndex * 3]
        if (startX + rectWidth > width) {
            return -1
        }

        var remaining = rectWidth
        var i = nodeIndex
        var maxY = 0

        while (remaining > 0) {
            if (i >= xyw.size) return -1

            maxY = max(maxY, xyw[i + 1])

            val covered = minOf(remaining, xyw[i + 2])
            remaining -= covered
            i += 3
        }

        return maxY
    }

    /**
     * Inserts the top edge of the allocated rectangle into the skyline.
     */
    private fun addSkylineLevel(
        nodeIndex: Int,
        rectX: Int,
        rectY: Int,
        rectWidth: Int,
        rectHeight: Int
    ) {
        val rectEnd = rectX + rectWidth
        val rectTop = rectY + rectHeight

        var index = nodeIndex

        // Split the node at rectX if necessary.
        val nodeEnd = xyw[index] + xyw[index + 2]

        if (xyw[index] < rectX) {
            val oldEnd = nodeEnd
            val oldY = xyw[index + 1]

            xyw[index + 2] = rectX - xyw[index]
            xyw.add3At(index + 3, rectX, oldY, oldEnd - rectX)
            index += 3
        }

        // Remove skyline segments covered by the rectangle.
        while (index < xyw.size) {
            val nodeStart = xyw[index]
            val nodeEnd2 = nodeStart + xyw[index + 2]

            if (nodeStart >= rectEnd) {
                break
            }

            if (nodeEnd2 <= rectEnd) {
                xyw.remove3At(index)
            } else {
                // Preserve the uncovered right side.
                xyw[index] = rectEnd
                xyw[index + 2] = nodeEnd2 - rectEnd
                break
            }
        }

        // Insert rectangle's top edge.
        xyw.add3At(index, rectX, rectTop, rectWidth)
        mergeSimilarNeighbors(index)
    }

    /**
     * Merges adjacent skyline nodes with the same height.
     */
    private fun mergeSimilarNeighbors(index: Int) {
        var i = max(0, index - 3)

        while (i + 3 < xyw.size) {
            val thisEnd = xyw[i] + xyw[i + 2]
            if (thisEnd != xyw[i + 3]) {
                i += 3
                continue
            }

            val y0 = xyw[i + 1]
            val y1 = xyw[i + 4]

            if (abs(y0 - y1) > heightTolerance) {
                i += 3
                continue
            }

            // Raising the lower skyline is conservative:
            // it may waste space, but can never cause overlap.
            val mergedY = max(y0, y1)

            xyw[i + 1] = mergedY
            xyw[i + 2] += xyw[i + 5]

            xyw.remove3At(i + 3)

            // Stay at i, because the newly merged node may also
            // be mergeable with the node on its right.
        }
    }
}