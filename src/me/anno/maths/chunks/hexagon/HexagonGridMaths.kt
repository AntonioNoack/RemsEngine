package me.anno.maths.chunks.hexagon

import me.anno.maths.Maths.SQRT3
import me.anno.maths.Maths.TAU
import me.anno.maths.MinMax.max
import me.anno.maths.Maths.sq
import me.anno.utils.pooling.JomlPools
import org.joml.AABBd
import org.joml.Vector2d
import org.joml.Vector2i
import org.joml.Vector3i
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
 * Normal hexagons are so simple (compared to HexagonSphere 😆), that you shouldn't necessarily use Objects to save their corners/what you need.
 * Instead, you can save their index (i, j), maybe in a 2d-like array.
 *
 * Hexagons have a center, and six corners around them.
 * Each corner has three neighbor corners, and three neighbor hexagons (one of which is the original hexagon).
 * */
object HexagonGridMaths {

    val di = Vector2d(1.5, SQRT3 * 0.5)
    val dj = Vector2d(0.0, SQRT3)

    val dx = Vector2d(2.0 / 3.0, 0.0)
    val dy = Vector2d(-1.0 / 3.0, 1.0 / SQRT3)

    /**
     * neighbors are sorted by angle, starting at 0°
     * */
    val neighbors = listOf(
        Vector2i(+1, 0),
        Vector2i(0, +1),
        Vector2i(-1, +1),
        Vector2i(-1, 0),
        Vector2i(0, -1),
        Vector2i(+1, -1),
    )

    /**
     * corners are sorted by angle, starting at 0°
     * */
    val corners = listOf(
        Vector2d(1.0, 0.0),
        Vector2d(0.5, SQRT3 * 0.5),
        Vector2d(-0.5, SQRT3 * 0.5),
        Vector2d(-1.0, 0.0),
        Vector2d(-0.5, -SQRT3 * 0.5),
        Vector2d(0.5, -SQRT3 * 0.5)
    )

    // todo test this function
    @Suppress("unused")
    fun getCornerBounds(i0: Int, j0: Int, i1: Int, j1: Int, tmp: Vector2d, dst: AABBd): AABBd {
        getCenterBounds(i0, j0, i1, j1, tmp, dst)
        dst.minX -= 1.0
        dst.minY -= SQRT3 * 0.5
        dst.maxX += 1.0
        dst.maxY += SQRT3 * 0.5
        return dst
    }

    // todo test this function
    fun getCenterBounds(i0: Int, j0: Int, i1: Int, j1: Int, tmp: Vector2d, dst: AABBd): AABBd {
        getCenter(i0, j0, tmp)
        val minX = tmp.x
        val minY = tmp.y
        getCenter(i1, j1, tmp)
        return dst
            .setMin(minX, minY, 0.0)
            .setMax(tmp.x, tmp.y, 0.0)
    }

    @Suppress("unused")
    fun getCloseHexagon(center: Vector2d, dstIndex: Vector2i): Vector2i {
        val tmp = JomlPools.vec2d.borrow()
        return getCloseHexagon(center, dstIndex, tmp)
    }

    fun getCloseHexagon(center: Vector2d, index: Vector2i, remainder: Vector2d): Vector2i {
        val i = round(center.dot(dx))
        val j = round(center.dot(dy))

        index.set(i.toInt(), j.toInt())
        val center1 = getCenter(index, remainder)
        center.sub(center1, remainder)

        return index
    }

    @Suppress("unused")
    fun getClosestHexagon(center: Vector2d, dstIndex: Vector2i): Vector2i {
        val tmp = JomlPools.vec2d.borrow()
        return getClosestHexagon(center, dstIndex, tmp)
    }

    fun getClosestHexagon(center: Vector2d, index: Vector2i, remainder: Vector2d): Vector2i {
        getCloseHexagon(center, index, remainder)
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

    fun getCenter(i: Int, j: Int, dst: Vector2d): Vector2d {
        return dst.set(di.x * i + dj.x * j, di.y * i + dj.y * j)
    }

    fun getCenter(index: Vector2i, dst: Vector2d): Vector2d {
        return getCenter(index.x, index.y, dst)
    }

    /**
     * get coordinates of that corner;
     * cornerIndex: [0,5]
     * */
    fun getCorner(i: Int, j: Int, cornerIndex: Int, dst: Vector2d): Vector2d {
        return getCenter(i, j, dst).add(corners[cornerIndex])
    }

    fun getCorner(index: Vector3i, dst: Vector2d): Vector2d {
        return getCorner(index.x, index.y, index.z, dst)
    }

    // calculated in HexagonGridMathsTest
    val oppositeCorners = listOf(
        Vector3i(1, -1, 1),
        Vector3i(0, 1, 0),
        Vector3i(-1, 1, 1),
        Vector3i(-1, 0, 2),
        Vector3i(0, -1, 3),
        Vector3i(0, -1, 0),
    )

    fun getNeighborCorner0(i: Int, j: Int, cornerIndex: Int, dst: Vector3i): Vector3i {
        return dst.set(i, j, if (cornerIndex == 0) 5 else cornerIndex - 1)
    }

    fun getNeighborCorner1(i: Int, j: Int, cornerIndex: Int, dst: Vector3i): Vector3i {
        return dst.set(i, j, if (cornerIndex == 5) 0 else cornerIndex + 1)
    }

    fun getNeighborCorner2(i: Int, j: Int, cornerIndex: Int, dst: Vector3i): Vector3i {
        val oppo = oppositeCorners[cornerIndex]
        return dst.set(oppo.x + i, oppo.y + j, oppo.z)
    }

    /**
     * Get coordinate-indices of that corner's neighbor corners.
     * Each corner has exactly three neighbor corners.
     * The first and second neighbors are trivial: just walk along the outside of the current hexagon.
     * The last neighbor however is tricky, and has to be hardcoded.
     * cornerIndex: [0,5], neighborIndex: [0,2]
     *
     * Result: Vector3i(i,j,cornerIndex)
     * */
    fun getNeighborCorner(i: Int, j: Int, cornerIndex: Int, neighborIndex: Int, dst: Vector3i): Vector3i {
        return when (neighborIndex) {
            0 -> getNeighborCorner0(i, j, cornerIndex, dst)
            1 -> getNeighborCorner1(i, j, cornerIndex, dst)
            else -> getNeighborCorner2(i, j, cornerIndex, dst)
        }
    }

    fun getNeighborCorner(corner: Vector3i, neighborIndex: Int, dst: Vector3i): Vector3i {
        return getNeighborCorner(corner.x, corner.y, corner.z, neighborIndex, dst)
    }

    val neighborHexagons = listOf(
        Vector2i(1, 0), // 0a
        Vector2i(1, -1), // 0b
        Vector2i(1, 0), // 1a
        Vector2i(0, 1), // 1b
        Vector2i(0, 1), // 2a
        Vector2i(-1, 1), // 2b
        Vector2i(-1, 1), // 3a
        Vector2i(-1, 0), // 3b
        Vector2i(-1, 0), // 4a
        Vector2i(0, -1), // 4b
        Vector2i(0, -1), // 5a
        Vector2i(1, -1), // 5b
    )

    fun getNeighborHexagon0(i: Int, j: Int, cornerIndex: Int, dst: Vector2i): Vector2i {
        return neighborHexagons[cornerIndex * 2].add(i, j, dst)
    }

    fun getNeighborHexagon1(i: Int, j: Int, cornerIndex: Int, dst: Vector2i): Vector2i {
        return neighborHexagons[cornerIndex * 2 + 1].add(i, j, dst)
    }

    fun getNeighborHexagon(i: Int, j: Int, cornerIndex: Int, neighborIndex: Int, dst: Vector2i): Vector2i {
        return when (neighborIndex) {
            0 -> getNeighborHexagon0(i, j, cornerIndex, dst)
            1 -> getNeighborHexagon1(i, j, cornerIndex, dst)
            else -> dst.set(i, j)
        }
    }

    fun getNeighborHexagon(corner: Vector3i, neighborIndex: Int, dst: Vector2i): Vector2i {
        return getNeighborHexagon(corner.x, corner.y, corner.z, neighborIndex, dst)
    }

    fun getGridDistance(delta: Vector2i): Int {
        return getGridDistance(delta.x, delta.y)
    }

    fun getGridDistance(di: Int, dj: Int): Int {
        val distI = when {
            dj > 0 && di < 0 -> max(-dj, di) - di // = -(min(dj, -di) + di)
            dj < 0 && di > 0 -> max(dj, -di) + di // = +(max(dj, -di) + di)
            else -> abs(di) // no shortcut possible
        }
        return distI + abs(dj)
    }

    /**
     * return value: i,j,cornerIndex
     * */
    fun getClosestCorner(
        center: Vector2d, allowCenter: Boolean,
        tmpCenter: Vector2d, tmpRemainder: Vector2d,
        dstIndex: Vector2i,
    ): Int {
        if (center === tmpCenter) throw IllegalArgumentException()
        getClosestHexagon(center, dstIndex, tmpRemainder)
        getCenter(dstIndex, tmpCenter)
        var idx = round(atan2(tmpRemainder.y, tmpRemainder.x) * 6 / TAU + 6).toInt()
        if (idx >= 6) idx -= 6
        // if allow center, also check its distance vs towards the center
        val corner = corners[idx]
        val useCenter = allowCenter && tmpRemainder.lengthSquared() < corner.distanceSquared(tmpRemainder)
        return if (useCenter) 6 else idx
    }

    /**
     * return value: i,j,lineIndex (index of first corner of line, the second one is the next one)
     * */
    fun getClosestLine(
        center: Vector2d,
        tmpCenter: Vector2d, tmpRemainder: Vector2d,
        dstIndex: Vector2i,
    ): Int {
        if (center === tmpCenter) throw IllegalArgumentException()
        getClosestHexagon(center, dstIndex, tmpRemainder)
        getCenter(dstIndex, tmpCenter)
        var idx = floor(atan2(tmpRemainder.y, tmpRemainder.x) * 6 / TAU + 6).toInt()
        if (idx >= 6) idx -= 6
        return idx
    }
}