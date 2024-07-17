package me.anno.tests.gfx

import me.anno.utils.callbacks.I3U
import org.joml.AABBf
import org.joml.Vector2f
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object Rasterizer {
    // callback: (xMin,xMax,y)
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
}