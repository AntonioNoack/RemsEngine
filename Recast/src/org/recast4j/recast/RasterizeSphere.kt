/*
+recast4j copyright (c) 2021 Piotr Piastucki piotr@jtilia.org

This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.
Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:
1. The origin of this software must not be misrepresented; you must not
 claim that you wrote the original software. If you use this software
 in a product, an acknowledgment in the product documentation would be
 appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be
 misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/
package org.recast4j.recast

import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.sq
import org.joml.AABBf
import org.joml.Vector3f
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object RasterizeSphere {

    fun rasterizeSphere(
        hf: Heightfield,
        center: Vector3f,
        radius: Float,
        area: Int,
        flagMergeThr: Int,
        ctx: Telemetry?
    ) {
        ctx?.startTimer(TelemetryType.RASTERIZE_SPHERE)
        val bounds = AABBf(center, center).addMargin(radius)
        rasterizationFilledShape(
            hf, bounds, area, flagMergeThr
        ) { rectangle, dst -> intersectSphere(rectangle, dst, center, radius * radius) }
        ctx?.stopTimer(TelemetryType.RASTERIZE_SPHERE)
    }

    fun rasterizationFilledShape(
        hf: Heightfield,
        bounds: AABBf,
        area: Int,
        flagMergeThr: Int,
        intersection: (Rectangle, HeightRange) -> Boolean
    ) {
        if (!hf.bounds.testAABB(bounds)) return

        bounds.minX = max(bounds.minX, hf.bounds.minX)
        bounds.minZ = max(bounds.minZ, hf.bounds.minZ)
        bounds.maxX = min(bounds.maxX, hf.bounds.maxX)
        bounds.maxZ = min(bounds.maxZ, hf.bounds.maxZ)
        if (bounds.isEmpty()) return

        val invCellSize = 1f / hf.cellSize
        val invCellHeight = 1f / hf.cellHeight
        val xMin = ((bounds.minX - hf.bounds.minX) * invCellSize).toInt()
        val zMin = ((bounds.minZ - hf.bounds.minZ) * invCellSize).toInt()
        val xMax = min(hf.width - 1, ((bounds.maxX - hf.bounds.minX) * invCellSize).toInt())
        val zMax = min(hf.height - 1, ((bounds.maxZ - hf.bounds.minZ) * invCellSize).toInt())
        val range = HeightRange(0f, 0f)
        val rectangle = Rectangle(0f, 0f, 0f, 0f, hf.bounds.minY)
        for (x in xMin..xMax) {
            for (z in zMin..zMax) {
                rectangle.minX = x * hf.cellSize + hf.bounds.minX
                rectangle.minZ = z * hf.cellSize + hf.bounds.minZ
                rectangle.maxX = rectangle.minX + hf.cellSize
                rectangle.maxZ = rectangle.minZ + hf.cellSize
                if (intersection(rectangle, range)) {
                    val sMin = floor(((range.minY - hf.bounds.minY) * invCellHeight)).toInt()
                    val sMax = ceil(((range.maxY - hf.bounds.minY) * invCellHeight)).toInt()
                    if (sMin != sMax) {
                        val isMin = clamp(sMin, 0, RecastConstants.SPAN_MAX_HEIGHT)
                        val isMax = clamp(sMax, isMin + 1, RecastConstants.SPAN_MAX_HEIGHT)
                        RecastRasterization.addSpan(hf, x, z, isMin, isMax, area, flagMergeThr)
                    }
                }
            }
        }
    }

    fun intersectSphere(rectangle: Rectangle, dst: HeightRange, center: Vector3f, radiusSqr: Float): Boolean {
        val mx = clamp(center.x, rectangle.minX, rectangle.maxX) - center.x
        val y = rectangle.minY
        val mz = clamp(center.z, rectangle.minZ, rectangle.maxZ) - center.z
        val my = y - center.y
        val c = sq(mx, my, mz) - radiusSqr
        if (c > 0f && my > 0f) return false
        val discr = my * my - c
        if (discr < 0f) return false
        val discrSqrt = sqrt(discr)
        var tmin = -my - discrSqrt
        val tmax = -my + discrSqrt
        if (tmin < 0f) {
            tmin = 0f
        }
        return dst.set(y + tmin, y + tmax)
    }
}