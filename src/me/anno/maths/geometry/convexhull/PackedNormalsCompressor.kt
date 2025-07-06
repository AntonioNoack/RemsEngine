package me.anno.maths.geometry.convexhull

import me.anno.maths.Maths.max
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.types.Floats.toIntOr
import org.joml.AABBd
import org.joml.Vector3d
import org.joml.Vector3d.Companion.lengthSquared
import org.joml.Vector3f
import kotlin.math.abs

/**
 * Independently discovered approximate convex hull algorithm for large N, where the algorithm from Bullet becomes very slow.
 * Just a preprocessing step thinning out the vertices.
 *
 * This resulted in an up to 1000x performance improvement on a sphere of random points with N = 128k (37700ms -> 24ms).
 *
 * The algorithm works by putting the vertices into buckets based on their direction, and only keeping those with the largest distance from the center.
 * The corners get a few more vertices than average, but that's fine.
 *
 * For large meshes (128k vertices), and small output sizes (e.g., ideal for physics), convex hull generation now runs in ~18ns/vertex on my Ryzen 9 7950X3D.
 * */
object PackedNormalsCompressor {

    /**
     * Maps an input direction and grid size into a valid grid index.
     * */
    private fun calculateGridIndex(
        nx0: Double, ny0: Double, nz0: Double,
        gsx: Double, gridSize: Int
    ): Int {
        // pack normal
        val scale = 1.0 / max(abs(nx0) + abs(ny0) + abs(nz0), 1e-308)
        var nx = nx0 * scale
        var ny = ny0 * scale
        // nz *= scale // only sign is used, so scaling it can be skipped

        if (nz0 < 0.0) { // backside -> move to outside
            val anx = abs(nx)
            val any = abs(ny)
            val mx = (1.0 - any) * if (nx >= 0.0) 1.0 - any else any - 1.0
            val my = (1.0 - anx) * if (ny >= 0.0) 1.0 - anx else anx - 1.0
            nx = mx
            ny = my
        }

        val gx = ((nx + 1.0) * gsx).toIntOr()
        val gy = ((ny + 1.0) * gsx).toIntOr()
        return gx + gy * gridSize
    }

    private fun calculateGSX(gridSize: Int): Double {
        return gridSize * 0.5 * (1.0 - 1e-9)
    }

    private fun getBounds(vertices: List<Vector3d>): AABBd {
        val bounds = AABBd()
        bounds.union(vertices)
        bounds.addMargin(1e-6) // add a small margin to avoid division by zero
        return bounds
    }

    private fun getBounds(vertices: FloatArray): AABBd {
        val bounds = AABBd()
        val tmp = Vector3f()
        forLoopSafely(vertices.size, 3) { i ->
            bounds.union(tmp.set(vertices, i))
        }
        bounds.addMargin(1e-6) // add a small margin to avoid division by zero
        return bounds
    }

    /**
     * Thins out the vertices to be used for convex hull generation.
     * */
    fun compressVertices(vertices: List<Vector3d>, gridSize: Int = 16): List<Vector3d> {
        if (vertices.size * 4 <= gridSize * gridSize) {
            // not worth compressing
            return vertices
        }

        val bounds = getBounds(vertices)
        val cx = bounds.centerX
        val cy = bounds.centerY
        val cz = bounds.centerZ

        val dx = 2.0 / bounds.deltaX
        val dy = 2.0 / bounds.deltaY
        val dz = 2.0 / bounds.deltaZ

        val numGridEntries = gridSize * gridSize
        val grid = arrayOfNulls<Vector3d>(numGridEntries)
        val scores = DoubleArray(numGridEntries)

        val gsx = calculateGSX(gridSize)
        for (i in vertices.indices) {

            // normalize into [-1,1]³
            val vertex = vertices[i]
            val nx0 = (vertex.x - cx) * dx
            val ny0 = (vertex.y - cy) * dy
            val nz0 = (vertex.z - cz) * dz

            val gridIndex = calculateGridIndex(nx0, ny0, nz0, gsx, gridSize)
            val score = lengthSquared(nx0, ny0, nz0)
            if (score > scores[gridIndex]) {
                grid[gridIndex] = vertex
                scores[gridIndex] = score
            }
        }

        return grid.filterNotNull()
    }

    /**
     * Thins out the vertices to be used for convex hull generation.
     *
     * Only generates Vector3d-s for output vertices to safe dynamic allocations.
     * Solely by that, this algorithm is about 2x as fast as the base algorithm 🤩.
     * */
    fun compressVertices(vertices: FloatArray, gridSize: Int = 16): List<Vector3d> {
        if (vertices.size / 3 * 4 <= gridSize * gridSize) {
            // not worth compressing
            return List(vertices.size / 3) { i ->
                Vector3d(vertices, i * 3)
            }
        }

        val bounds = getBounds(vertices)
        val cx = bounds.centerX
        val cy = bounds.centerY
        val cz = bounds.centerZ

        val dx = 2.0 / bounds.deltaX
        val dy = 2.0 / bounds.deltaY
        val dz = 2.0 / bounds.deltaZ

        val numGridEntries = gridSize * gridSize
        val grid = arrayOfNulls<Vector3d>(numGridEntries)
        val scores = DoubleArray(numGridEntries)

        val gsx = calculateGSX(gridSize)
        forLoopSafely(vertices.size, 3) { i ->

            // normalize into [-1,1]³
            val nx0 = (vertices[i] - cx) * dx
            val ny0 = (vertices[i + 1] - cy) * dy
            val nz0 = (vertices[i + 2] - cz) * dz

            val gridIndex = calculateGridIndex(nx0, ny0, nz0, gsx, gridSize)
            val score = lengthSquared(nx0, ny0, nz0)
            if (score > scores[gridIndex]) {
                val prev = grid[gridIndex]
                if (prev == null) grid[gridIndex] = Vector3d(vertices, i)
                else prev.set(vertices, i)
                scores[gridIndex] = score
            }
        }

        return grid.filterNotNull()
    }
}