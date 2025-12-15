package me.anno.maths.noise

import me.anno.maths.Maths.clamp
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector
import java.util.Random
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Generates as many points as possible randomly distributed within a cube from (0,0,0) to (size),
 * with at least minDist as the minimum distance between points.
 *
 * A Euclidean distance function is expected.
 * Otherwise, invCellSize may be too big or small.
 * */
abstract class BlueNoiseSampler<Point : Vector>(
    private val size: Point,
    private val minDist: Float,
    private val maxAttempts: Int,
    seed: Long,
) {

    companion object {
        private const val EMPTY_GRID_CELL = -1
    }

    private val rnd = Random(seed)
    private val invCellSize = sqrt(size.numComponents.toFloat()) / minDist
    private val gridSize = IntArray(size.numComponents) {
        max(ceil(size.getComp(it) * invCellSize).toInt(), 1)
    }

    /**
     * Fast neighbor lookup
     * */
    private val grid = IntArray(gridSize.reduce { a, b -> Math.multiplyExact(a, b) })

    /**
     * All points
     * */
    private val points = ArrayList<Point>()

    /**
     * Indices, which may not be too dense
     * */
    private val active = IntArrayList()

    fun generatePoints(): List<Point> {
        grid.fill(EMPTY_GRID_CELL)
        points.clear()
        addInitialPoint()

        var pt = generatePoint()
        loop@ while (active.isNotEmpty()) {
            val index = rnd.nextInt(active.size)
            val activeIndex = active[index]
            val center = points[activeIndex]

            for (i in 0 until maxAttempts) {
                val candidate = generateCandidate(center, pt)
                if (candidate != null && canAddToGrid(candidate)) {
                    addPoint(candidate)
                    pt = generatePoint() // create new point instance
                    continue@loop
                }
            }

            // too many attempts ->
            //  the checked point was already very dense
            active.swapRemoveAt(index)
        }
        return points
    }

    private fun addInitialPoint() {
        val pt = generatePoint()
        for (i in gridSize.indices) {
            pt.setComp(i, rnd.nextFloat() * size.getComp(i))
        }
        addPoint(pt)
    }

    abstract fun generatePoint(): Point
    abstract fun distanceSquared(a: Point, b: Point): Float

    private fun canAddToGrid(p: Point): Boolean {
        return grid[getGridIndex(p)] == EMPTY_GRID_CELL &&
                canAddToGrid(p, 0, gridSize.lastIndex)
    }

    /**
     * Recursive function to check a hypercube around p for neighbors, which may be too close
     * */
    private fun canAddToGrid(p: Point, index: Int, dim: Int): Boolean {
        if (dim < 0) {
            val index = grid[index]
            return index == EMPTY_GRID_CELL ||
                    distanceSquared(p, points[index]) >= minDist * minDist
        } else {
            val centerI = (p.getComp(dim) * invCellSize).toInt()
            val gridSizeI = gridSize[dim]
            val newIndex = index * gridSizeI
            for (xi in max(0, centerI - 2)..min(gridSizeI - 1, centerI + 2)) {
                if (!canAddToGrid(p, newIndex + xi, dim - 1)) return false
            }
            return true
        }
    }

    private fun generateCandidate(center: Point, dst: Point): Point? {
        var sumSq = 0f
        // first fill with a random, not-normalized direction
        for (i in gridSize.indices) {
            val xi = rnd.nextFloat() * 2f - 1f
            dst.setComp(i, xi.toDouble())
            sumSq += xi * xi
        }
        val radius = minDist * (1f + rnd.nextFloat())
        val factor = radius / (sqrt(sumSq) + 1e-9f)
        for (i in gridSize.indices) {
            val newValue = center.getComp(i) + factor * dst.getComp(i)
            if (newValue < 0f || newValue >= size.getComp(i)) return null
            dst.setComp(i, newValue)
        }
        return dst
    }

    /**
     * this calculation must match canAddToGrid()
     * */
    private fun getGridIndex(p: Point): Int {
        var index = 0
        for (i in gridSize.lastIndex downTo 0) {
            val sizeI = gridSize[i]
            val gx = clamp((p.getComp(i) * invCellSize).toInt(), 0, sizeI)
            index = sizeI * index + gx
        }
        return index
    }

    private fun addPoint(p: Point) {
        val index = points.size
        points.add(p)
        active.add(index)
        grid[getGridIndex(p)] = index
    }
}
