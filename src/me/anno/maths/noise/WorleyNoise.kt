package me.anno.maths.noise

import me.anno.maths.Maths.sq
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Voronoi-style noise with cellSize ~ 1
 * todo iterate corners?
 * todo iterate edges?
 * */
class WorleyNoise(seed: Long, val randomness: Float = 1f) {

    companion object {
        private const val SEED_Y = 8106025934069731
        private const val SEED_Z = 3637989868390793
    }

    val rndX = FullNoise(seed)
    val rndY = FullNoise(seed xor SEED_Y)
    val rndZ = FullNoise(seed xor SEED_Z)

    private val offset = 0.5f * (1f - randomness)

    fun getCellI(x: Int, rnd: FullNoise): Float = rnd[x] * randomness + offset
    fun getCellI(x: Int, y: Int, rnd: FullNoise): Float = rnd[x, y] * randomness + offset
    fun getCellI(x: Int, y: Int, z: Int, rnd: FullNoise): Float = rnd[x, y, z] * randomness + offset

    inline fun process(x: Float, callback: (xi: Int, xfi: Float, distance: Float) -> Unit) {
        val xf = floor(x)
        val xi = xf.toInt()
        val xr = x - xf
        for (dxi in -1..1) {
            val xi = xi + dxi
            val xfi = getCellI(xi, rndX)
            val cellPosX = xfi + dxi
            val distance = abs(xr - cellPosX)
            callback(xi, xfi, distance)
        }
    }

    fun getDistanceSq(x: Float, dst: WorleyCell? = null): Float = sq(getDistance(x, dst))
    fun getDistance(x: Float, dst: WorleyCell? = null): Float {
        var minDistance = Float.POSITIVE_INFINITY
        process(x) { xi, xfi, distance ->
            if (distance < minDistance) {
                minDistance = distance
                dst?.set(xi, xfi, distance)
            }
        }
        return minDistance
    }

    fun getDistanceToEdge(
        x: Float,
        dstClosest: WorleyCell? = null,
        dst2ndClosest: WorleyCell? = null
    ): Float {
        var minDistance1 = Float.POSITIVE_INFINITY
        var minDistance2 = Float.POSITIVE_INFINITY
        process(x) { xi, xfi, distance ->
            if (distance <= minDistance1) {
                minDistance2 = minDistance1
                minDistance1 = distance
                dst2ndClosest?.set(dstClosest)
                dstClosest?.set(xi, xfi, distance)
            } else if (distance <= minDistance2) {
                minDistance2 = distance
                dst2ndClosest?.set(xi, xfi, distance)
            }
        }
        return (minDistance2 - minDistance1) * 0.5f
    }

    inline fun process(
        x: Float, y: Float,
        callback: (
            xi: Int, xfi: Float,
            yi: Int, yfi: Float,
            distanceSq: Float
        ) -> Unit
    ) {
        val xf = floor(x)
        val yf = floor(y)

        val xi = xf.toInt()
        val yi = yf.toInt()

        val xr = x - xf
        val yr = y - yf

        for (dyi in -1..1) {
            for (dxi in -1..1) {
                val xi = xi + dxi
                val yi = yi + dyi

                val xfi = getCellI(xi, yi, rndX)
                val yfi = getCellI(xi, yi, rndY)

                val cellPosX = xfi + dxi
                val cellPosY = yfi + dyi

                val distance = sq(xr - cellPosX, yr - cellPosY)
                callback(xi, xfi, yi, yfi, distance)
            }
        }
    }


    fun getDistance(x: Float, y: Float, dst: WorleyCell? = null): Float {
        val dist = getDistanceSq(x, y, dst)
        return dst?.sqrt() ?: sqrt(dist)
    }

    fun getDistanceSq(x: Float, y: Float, dst: WorleyCell? = null): Float {
        var minDistance = Float.POSITIVE_INFINITY
        process(x, y) { xi, xfi, yi, yfi, distance ->
            if (distance < minDistance) {
                minDistance = distance
                dst?.set(xi, xfi, yi, yfi, distance)
            }
        }
        return minDistance
    }

    fun getDistanceToEdge(
        x: Float, y: Float,
        dstClosest: WorleyCell? = null,
        dst2ndClosest: WorleyCell? = null
    ): Float {
        var minDistance1 = Float.POSITIVE_INFINITY
        var minDistance2 = Float.POSITIVE_INFINITY
        process(x, y) { xi, xfi, yi, yfi, distance ->
            if (distance <= minDistance1) {
                minDistance2 = minDistance1
                minDistance1 = distance
                dst2ndClosest?.set(dstClosest)
                dstClosest?.set(xi, xfi, yi, yfi, distance)
            } else if (distance <= minDistance2) {
                minDistance2 = distance
                dst2ndClosest?.set(xi, xfi, yi, yfi, distance)
            }
        }
        dstClosest?.sqrt()
        dst2ndClosest?.sqrt()
        return (sqrt(minDistance2) - sqrt(minDistance1)) * 0.5f
    }

    inline fun process(
        x: Float, y: Float, z: Float,
        callback: (xi: Int, xfi: Float, yi: Int, yfi: Float, zi: Int, zfi: Float, distanceSq: Float) -> Unit
    ) {
        val xf = floor(x)
        val yf = floor(y)
        val zf = floor(z)

        val xi = xf.toInt()
        val yi = yf.toInt()
        val zi = zf.toInt()

        val xr = x - xf
        val yr = y - yf
        val zr = z - zf

        for (dzi in -1..1) {
            for (dyi in -1..1) {
                for (dxi in -1..1) {
                    val xi = xi + dxi
                    val yi = yi + dyi
                    val zi = zi + dzi

                    val xfi = getCellI(xi, yi, zi, rndX)
                    val yfi = getCellI(xi, yi, zi, rndY)
                    val zfi = getCellI(xi, yi, zi, rndZ)

                    val cellPosX = xfi + dxi
                    val cellPosY = yfi + dyi
                    val cellPosZ = zfi + dzi

                    val distanceSq = sq(xr - cellPosX, yr - cellPosY, zr - cellPosZ)
                    callback(xi, xfi, yi, yfi, zi, zfi, distanceSq)
                }
            }
        }
    }


    fun getDistance(x: Float, y: Float, z: Float, dst: WorleyCell? = null): Float {
        val dist = getDistanceSq(x, y, z, dst)
        return dst?.sqrt() ?: sqrt(dist)
    }

    fun getDistanceSq(x: Float, y: Float, z: Float, dst: WorleyCell? = null): Float {
        var minDistance = Float.POSITIVE_INFINITY
        process(x, y, z) { xi, xfi, yi, yfi, zi, zfi, distance ->
            if (distance < minDistance) {
                minDistance = distance
                dst?.set(xi, xfi, yi, yfi, zi, zfi, distance)
            }
        }
        return minDistance
    }

    fun getDistanceToEdge(
        x: Float, y: Float, z: Float,
        dstClosest: WorleyCell? = null,
        dst2ndClosest: WorleyCell? = null
    ): Float {
        var minDistance1 = Float.POSITIVE_INFINITY
        var minDistance2 = Float.POSITIVE_INFINITY
        process(x, y, z) { xi, xfi, yi, yfi, zi, zfi, distance ->
            if (distance <= minDistance1) {
                minDistance2 = minDistance1
                minDistance1 = distance
                dst2ndClosest?.set(dstClosest)
                dstClosest?.set(xi, xfi, yi, yfi, zi, zfi, distance)
            } else if (distance <= minDistance2) {
                minDistance2 = distance
                dst2ndClosest?.set(xi, xfi, yi, yfi, zi, zfi, distance)
            }
        }
        dstClosest?.sqrt()
        dst2ndClosest?.sqrt()
        return (sqrt(minDistance2) - sqrt(minDistance1)) * 0.5f
    }
}