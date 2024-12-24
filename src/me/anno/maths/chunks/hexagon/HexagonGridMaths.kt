package me.anno.maths.chunks.hexagon

import me.anno.maths.Maths.SQRT3
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.max
import me.anno.maths.Maths.sq
import me.anno.utils.pooling.JomlPools
import org.joml.AABBd
import org.joml.Vector2d
import org.joml.Vector2i
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.round

/**
 * This objects provides utility functions to make hexagon grids easy to implement.
 * It contains all the maths required for them.
 *
 * In this implementation, the distance from the center to each point is 1.0 long, and each side is, too.
 * If you need another scale, just scale the coordinates by a constant.
 *
 * Normal hexagons are so simple (compared to HexagonSphere 😆), that you shouldn't necessarily use Objects to save their vertices/what you need.
 * Instead, you can save their index (i, j), maybe in a 2d-like array.
 * */
object HexagonGridMaths {

    val di = Vector2d(1.5, SQRT3 * 0.5)
    val dj = Vector2d(0.0, SQRT3)

    val dx = Vector2d(2.0 / 3.0, 0.0)
    val dy = Vector2d(-1.0 / 3.0, 1.0 / SQRT3)

    // neighbors are sorted by angle, starting at 0°
    val neighbors = listOf(
        Vector2i(+1, 0),
        Vector2i(0, +1),
        Vector2i(-1, +1),
        Vector2i(-1, 0),
        Vector2i(0, -1),
        Vector2i(+1, -1),
    )

    // vertices are sorted by angle, starting at 0°
    val vertices = listOf(
        Vector2d(1.0, 0.0),
        Vector2d(0.5, SQRT3 * 0.5),
        Vector2d(-0.5, SQRT3 * 0.5),
        Vector2d(-1.0, 0.0),
        Vector2d(-0.5, -SQRT3 * 0.5),
        Vector2d(0.5, -SQRT3 * 0.5)
    )

    // todo test this function
    @Suppress("unused")
    fun getVertexBounds(i0: Int, j0: Int, i1: Int, j1: Int, tmp: Vector2d, dst: AABBd): AABBd {
        getCenterBounds(i0, j0, i1, j1, tmp, dst)
        dst.minX -= 1.0
        dst.minY -= SQRT3 * 0.5
        dst.maxX += 1.0
        dst.maxY += SQRT3 * 0.5
        return dst
    }

    // todo test this function
    fun getCenterBounds(i0: Int, j0: Int, i1: Int, j1: Int, tmp: Vector2d, dst: AABBd): AABBd {
        indexToCoords(i0, j0, tmp)
        val minX = tmp.x
        val minY = tmp.y
        indexToCoords(i1, j1, tmp)
        return dst
            .setMin(minX, minY, 0.0)
            .setMax(tmp.x, tmp.y, 0.0)
    }

    @Suppress("unused")
    fun coordsToIndexFast(coords: Vector2d, dstIndex: Vector2i): Vector2i {
        val tmp = JomlPools.vec2d.borrow()
        return coordsToIndexFast(coords, dstIndex, tmp)
    }

    fun coordsToIndexFast(coords: Vector2d, index: Vector2i, remainder: Vector2d): Vector2i {
        val i = round(coords.dot(dx))
        val j = round(coords.dot(dy))

        index.set(i.toInt(), j.toInt())
        val center = indexToCoords(index, remainder)
        coords.sub(center, remainder)

        return index
    }

    @Suppress("unused")
    fun coordsToIndex(coords: Vector2d, dstIndex: Vector2i): Vector2i {
        val tmp = JomlPools.vec2d.borrow()
        return coordsToIndex(coords, dstIndex, tmp)
    }

    fun coordsToIndex(coords: Vector2d, index: Vector2i, remainder: Vector2d): Vector2i {
        coordsToIndexFast(coords, index, remainder)
        val lenSq = remainder.lengthSquared()
        if (lenSq < 0.75) {
            // ideal, because there cannot be any closer hexagon
            return index
        }
        var idx = floor(atan2(remainder.y, remainder.x) * 6 / TAU + 6).toInt()
        if (idx >= 6) idx -= 6
        val neighbor = neighbors[idx]
        val dx = neighbor.x * di.x + neighbor.y * dj.x
        val dy = neighbor.x * di.y + neighbor.y * dj.y
        if (sq(remainder.x - dx, remainder.y - dy) < lenSq) {
            index.add(neighbor)
            remainder.sub(dx, dy)
        }
        return index
    }

    fun indexToCoords(i: Int, j: Int, dst: Vector2d): Vector2d {
        return dst.set(di.x * i + dj.x * j, di.y * i + dj.y * j)
    }

    fun indexToCoords(index: Vector2i, dst: Vector2d): Vector2d {
        return indexToCoords(index.x, index.y, dst)
    }

    @Suppress("unused")
    fun getVertex(i: Int, j: Int, vertexIndex: Int, dst: Vector2d): Vector2d {
        return indexToCoords(i, j, dst).add(vertices[vertexIndex])
    }

    fun getGridDistance(delta: Vector2i): Int {
        return getGridDistance(delta.x, delta.y)
    }

    fun getGridDistance(di: Int, dj: Int): Int {
        val distI = when {
            dj > 0 && di < 0 -> max(-dj, di) - di // = -(min(dj, -di) + di)
            dj < 0 && di > 0 -> max(dj, -di) + di // = +(max(dj, -di) + di)
            else -> abs(di) // not shortcut possible
        }
        return distI + abs(dj)
    }

    /**
     * return value: i,j,vertexIndex
     * */
    fun getClosestVertex(
        coords: Vector2d, allowCenter: Boolean,
        tmpCoords: Vector2d, tmpRemainder: Vector2d,
        dstIndex: Vector2i,
    ): Int {
        if (coords === tmpCoords) throw IllegalArgumentException()
        coordsToIndex(coords, dstIndex, tmpRemainder)
        indexToCoords(dstIndex, tmpCoords)
        var idx = round(atan2(tmpRemainder.y, tmpRemainder.x) * 6 / TAU + 6).toInt()
        if (idx >= 6) idx -= 6
        // if allow center, also check its distance vs towards the center
        val vertex = vertices[idx]
        val useCenter = allowCenter && tmpRemainder.lengthSquared() < vertex.distanceSquared(tmpRemainder)
        return if (useCenter) 6 else idx
    }

    /**
     * return value: i,j,lineIndex (index of first vertex of line, the second one is the next one)
     * */
    fun getClosestLine(
        coords: Vector2d,
        tmpCoords: Vector2d, tmpRemainder: Vector2d,
        dstIndex: Vector2i,
    ): Int {
        if (coords === tmpCoords) throw IllegalArgumentException()
        coordsToIndex(coords, dstIndex, tmpRemainder)
        indexToCoords(dstIndex, tmpCoords)
        var idx = floor(atan2(tmpRemainder.y, tmpRemainder.x) * 6 / TAU + 6).toInt()
        if (idx >= 6) idx -= 6
        return idx
    }
}