package me.anno.maths.chunks.triangles

import me.anno.maths.Maths.SQRT3
import me.anno.maths.Maths.TAU
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Vector2d
import org.joml.Vector2i
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.round

/**
 * This objects provides utility functions to make triangle grids easy to implement.
 * It contains all the maths required for them.
 * The center of each triangle is defined as the center of the AABB.
 * This makes it nicer to add textures to the triangles, and neighbor (180° rotated) triangles have the same positions.y.
 *
 * In this implementation, each side is 1.0 long.
 * If you need another scale, just scale the coordinates by a constant.
 *
 * Triangles are simple, so you shouldn't necessarily use Objects to save their vertices/what you need.
 * Instead, you can save their index (i, j), maybe in a 2d-like array.
 * */
object TriangleGridMaths {

    val di = Vector2d(1.0, 0.0)
    val dj = Vector2d(0.5, SQRT3 * 0.5)

    val dx = Vector2d(1.0, -1.0 / SQRT3)
    val dy = Vector2d(0.0, 2.0 / SQRT3)

    const val DELTA_Y_TO_CENTER = SQRT3 / 12

    // vertices are sorted by angle, starting at 0°
    val verticesUp = listOf(
        Vector2d(0.5, -SQRT3 * 0.25),
        Vector2d(0.0, SQRT3 * 0.25),
        Vector2d(-0.5, -SQRT3 * 0.25),
    )

    // vertices are sorted by angle, starting at >0°
    val verticesDown = listOf(
        Vector2d(0.5, SQRT3 * 0.25),
        Vector2d(-0.5, SQRT3 * 0.25),
        Vector2d(0.0, -SQRT3 * 0.25),
    )

    // neighbors are sorted by angle, starting at 0°
    val neighborsDirectUp = listOf(
        Vector2i(1, 0),
        Vector2i(-1, 0),
        Vector2i(1, -1),
    )

    val neighborsDirectDown = listOf(
        Vector2i(1, 0),
        Vector2i(-1, 1),
        Vector2i(-1, 0),
    )

    val neighborsIndirectUp = listOf(
        Vector2i(2, 0),
        Vector2i(1, 0),
        Vector2i(0, 1),
        Vector2i(-1, 1),
        Vector2i(-2, 1),
        Vector2i(-1, 0),
        Vector2i(-2, 0),
        Vector2i(-1, -1), // 7
        Vector2i(0, -1), // 8
        Vector2i(1, -1), // 9
        Vector2i(2, -1), // 10
        Vector2i(3, -1), // 11
    )

    val neighborsIndirectDown = listOf(
        Vector2i(1, 0),
        Vector2i(2, 0),
        Vector2i(1, 1), // 2
        Vector2i(0, 1), // 3
        Vector2i(-1, 1), // 4
        Vector2i(-2, 1), // 5
        Vector2i(-3, 1), // 6
        Vector2i(-2, 0),
        Vector2i(-1, 0),
        Vector2i(0, -1),
        Vector2i(1, -1),
        Vector2i(2, -1),
    )

    fun getVertices(down: Boolean): List<Vector2d> {
        return if (down) verticesDown else verticesUp
    }

    fun getVertex(i: Int, j: Int, vertexIndex: Int, dst: Vector2d): Vector2d {
        return getVertex(i shr 1, j, i.hasFlag(1), vertexIndex, dst)
    }

    fun getVertex(i: Int, j: Int, down: Boolean, vertexIndex: Int, dst: Vector2d): Vector2d {
        return indexToCoords(i, j, down, dst).add(getVertices(down)[vertexIndex])
    }

    @Suppress("unused")
    fun getVertex(index: Vector2i, vertexIndex: Int, dst: Vector2d): Vector2d {
        return getVertex(index.x, index.y, vertexIndex, dst)
    }

    fun getNeighborsDirect(down: Boolean): List<Vector2i> {
        return if (down) neighborsDirectDown
        else neighborsDirectUp
    }

    fun getNeighborsIndirect(down: Boolean): List<Vector2i> {
        return if (down) neighborsIndirectDown
        else neighborsIndirectUp
    }

    fun getNeighbors(direct: Boolean, down: Boolean): List<Vector2i> {
        return if (direct) getNeighborsDirect(down)
        else getNeighborsIndirect(down)
    }

    fun indexToCoords(index: Vector2i, dst: Vector2d): Vector2d {
        return indexToCoords(index.x, index.y, dst)
    }

    fun indexToCoords(i: Int, j: Int, down: Boolean, dst: Vector2d): Vector2d {
        return dst.set(di.x * i + dj.x * j + if (down) 0.5 else 0.0, di.y * i + dj.y * j)
    }

    fun indexToCoords(i: Int, j: Int, dst: Vector2d): Vector2d {
        return indexToCoords(i shr 1, j, i.hasFlag(1), dst)
    }

    fun getCenter(i: Int, j: Int, down: Boolean, dst: Vector2d): Vector2d {
        return dst.set(
            di.x * i + dj.x * j + if (down) 0.5 else 0.0,
            di.y * i + dj.y * j + if (down) +DELTA_Y_TO_CENTER else -DELTA_Y_TO_CENTER
        )
    }

    fun getCenter(i: Int, j: Int, dst: Vector2d): Vector2d {
        return getCenter(i shr 1, j, i.hasFlag(1), dst)
    }

    fun coordsToIndex(
        positions: Vector2d,
        dstIndex: Vector2i,
        dstRemainder: Vector2d,
        uvSpaceRemainder: Boolean
    ): Vector2i {
        val i = positions.dot(dx)
        val j = positions.dot(dy)
        val fi = floor(i + 0.25)
        val fj = round(j)
        dstIndex.set(fi.toInt() * 2, fj.toInt())
        dstRemainder.set(i - fi, j - fj)
        if (dstRemainder.x + dstRemainder.y > 0.25) {
            dstIndex.x++
            // hopefully correct...
            if (uvSpaceRemainder) {
                dstRemainder.x = 0.5 - dstRemainder.x
                dstRemainder.y = -dstRemainder.y
            } else {
                dstRemainder.x -= 0.5
            }
        }
        if (!uvSpaceRemainder) {
            dstRemainder.set( // we could use a matrix here xD
                dstRemainder.dot(di.x, dj.x),
                dstRemainder.dot(di.y, dj.y)
            )
        }
        return dstIndex
    }

    fun getClosestTriangle(
        positions: Vector2d,
        dstIndex: Vector2i,
    ): Vector2i {
        val i = positions.dot(dx)
        val j = positions.dot(dy)
        val fi = floor(i + 0.25)
        val fj = round(j)
        dstIndex.set(fi.toInt() * 2, fj.toInt())
        val dx = i - fi
        val dy = j - fj
        if (dx + dy > 0.25) dstIndex.x++
        return dstIndex
    }

    fun getClosestVertex(
        positions: Vector2d, allowCenter: Boolean,
        tmpCoords: Vector2d, tmpDiffCenter: Vector2d,
        dstIndex: Vector2i,
    ): Int {
        if (positions === tmpCoords) throw IllegalArgumentException()
        coordsToIndex(positions, dstIndex, tmpDiffCenter, false)
        indexToCoords(dstIndex, tmpCoords)
        val down = dstIndex.x.hasFlag(1)
        tmpDiffCenter.y += (if (down) -DELTA_Y_TO_CENTER else +DELTA_Y_TO_CENTER)
        // offset remainder such that all vertices have the same distance
        var idx = round(atan2(tmpDiffCenter.y, tmpDiffCenter.x) * 3 / TAU + 3).toInt()
        if (idx >= 3) idx -= 3
        // if allow center, also check its distance vs towards the center
        val vertex = getVertices(down)[idx]
        val useCenter = allowCenter && tmpDiffCenter.lengthSquared() < vertex.distanceSquared(tmpDiffCenter)
        return if (useCenter) 3 else idx
    }

    fun getClosestLine(
        positions: Vector2d,
        tmpCoords: Vector2d, tmpRemainder: Vector2d,
        dstIndex: Vector2i,
    ): Int {
        if (positions === tmpCoords) throw IllegalArgumentException()
        coordsToIndex(positions, dstIndex, tmpRemainder, false)
        indexToCoords(dstIndex, tmpCoords)
        val down = dstIndex.x.hasFlag(1)
        val remY = tmpRemainder.y + (if (down) -DELTA_Y_TO_CENTER else +DELTA_Y_TO_CENTER)
        // offset remainder such that all vertices have the same distance
        var idx = floor(atan2(remY, tmpRemainder.x) * 3 / TAU + 3).toInt()
        if (idx >= 3) idx -= 3
        return idx
    }
}