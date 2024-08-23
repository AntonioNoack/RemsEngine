package me.anno.maths.geometry

import me.anno.utils.callbacks.I3U
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Triangles.subCross
import org.joml.AABBf
import org.joml.Planef
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

object Rasterizer {

    /**
     * callback: (xMin,xMax,y)
     * */
    fun rasterize(ua: Vector2f, ub: Vector2f, uc: Vector2f, bounds: AABBf?, callback: I3U) {
        var minX = min(ua.x, min(ub.x, uc.x))
        var maxX = max(ua.x, max(ub.x, uc.x))
        var minY = min(ua.y, min(ub.y, uc.y))
        var maxY = max(ua.y, max(ub.y, uc.y))
        if (bounds != null) {
            minX = max(minX, bounds.minX)
            maxX = min(maxX, bounds.maxX)
            minY = max(minY, bounds.minY)
            maxY = min(maxY, bounds.maxY)
        }
        if (minX > maxX || minY > maxY) return // easy, empty
        for (y in ceil(minY).toInt()..floor(maxY).toInt()) {
            var minX1 = maxX
            var maxX1 = minX
            fun union(a: Vector2f, b: Vector2f) {
                if (y <= max(a.y, b.y) && y >= min(a.y, b.y)) {
                    if (a.y == b.y) {
                        minX1 = min(minX1, min(a.x, b.x))
                        maxX1 = max(maxX1, max(a.x, b.x))
                    } else {
                        val x = a.x + (b.x - a.x) * (y - a.y) / (b.y - a.y)
                        minX1 = min(minX1, x)
                        maxX1 = max(maxX1, x)
                    }
                }
            }
            union(ua, ub)
            union(ub, uc)
            union(uc, ua)
            minX1 = max(minX, minX1)
            maxX1 = min(maxX, maxX1)
            val minXI = ceil(minX1).toInt()
            val maxXI = floor(maxX1).toInt()
            callback.call(minXI, maxXI, y)
        }
    }

    /**
     * callback: (x,y,z)
     * */
    fun rasterize(ua: Vector3f, ub: Vector3f, uc: Vector3f, bounds: AABBf?, callback: I3U) {
        // 0.5 is to ensure that we have enough tolerance for rounding
        if (bounds != null && bounds.isEmpty()) return
        val boundsI = bounds ?: AABBf().all()
        val normal = subCross(ua, ub, uc, Vector3f()).normalize()
        val plane = Planef(ua, normal)
        normal.absolute()
        when (normal.max()) {
            normal.x -> {
                val minX = boundsI.minX
                val maxX = boundsI.maxX
                rasterize(ua.yz, ub.yz, uc.yz, bounds.yz) { yMin, yMax, z ->
                    for (y in yMin..yMax) {
                        val x = round(plane.findX(y.toFloat(), z.toFloat()))
                        if (x in minX..maxX) {
                            callback.call(x.toInt(), y, z)
                        }
                    }
                }
            }
            normal.y -> {
                val minY = boundsI.minY
                val maxY = boundsI.maxY
                rasterize(ua.xz, ub.xz, uc.xz, bounds.xz) { xMin, xMax, z ->
                    for (x in xMin..xMax) {
                        val y = round(plane.findY(x.toFloat(), z.toFloat()))
                        if (y in minY..maxY) {
                            callback.call(x, y.toInt(), z)
                        }
                    }
                }
            }
            else -> {
                val minZ = boundsI.minZ
                val maxZ = boundsI.maxZ
                rasterize(ua.xy, ub.xy, uc.xy, bounds.xy) { xMin, xMax, y ->
                    for (x in xMin..xMax) {
                        val z = round(plane.findZ(x.toFloat(), y.toFloat()))
                        if (z in minZ..maxZ) {
                            callback.call(x, y, z.toInt())
                        }
                    }
                }
            }
        }
    }

    private val Vector3f.yz: Vector2f
        get() = Vector2f(y, z)

    private val AABBf?.yz: AABBf?
        get() = if (this != null) AABBf()
            .setMin(minY, minZ, 0f)
            .setMax(maxY, maxZ, 0f)
        else null

    private val Vector3f.xz: Vector2f
        get() = Vector2f(x, z)

    private val AABBf?.xz: AABBf?
        get() = if (this != null) AABBf()
            .setMin(minX, minZ, 0f)
            .setMax(maxX, maxZ, 0f)
        else null

    private val Vector3f.xy: Vector2f
        get() = Vector2f(x, y)

    private val AABBf?.xy: AABBf?
        get() = if (this != null) AABBf()
            .setMin(minX, minY, 0f)
            .setMax(maxX, maxY, 0f)
        else null
}