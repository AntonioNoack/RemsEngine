/*
recast4j copyright (c) 2021 Piotr Piastucki piotr@jtilia.org

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
package org.recast4j.dynamic

import org.joml.Vector3f
import org.recast4j.recast.Heightfield
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Voxel raycast based on the algorithm described in "A Fast Voxel Traversal Algorithm for Ray Tracing" by John Amanatides and Andrew Woo
 */
class VoxelQuery(
    private val origin: Vector3f, private val tileWidth: Float, private val tileDepth: Float,
    private val heightfieldProvider: (Int, Int) -> Heightfield?
) {
    /**
     * Perform raycast using voxels heightfields.
     * @return Hit parameter (t) or NaN if no hit found
     */
    fun raycast(start: Vector3f, end: Vector3f): Float {
        return traverseTiles(start, end)
    }

    private fun traverseTiles(start: Vector3f, end: Vector3f): Float {
        val relStartX = start.x - origin.x
        val relStartZ = start.z - origin.z
        var sx = floor((relStartX / tileWidth)).toInt()
        var sz = floor((relStartZ / tileDepth)).toInt()
        val ex = floor(((end.x - origin.x) / tileWidth)).toInt()
        val ez = floor(((end.z - origin.z) / tileDepth)).toInt()
        val dx = ex - sx
        val dz = ez - sz
        val stepX = if (dx < 0) -1 else 1
        val stepZ = if (dz < 0) -1 else 1
        val xRem = tileWidth + relStartX % tileWidth % tileWidth
        val zRem = tileDepth + relStartZ % tileDepth % tileDepth
        var tx = end.x - start.x
        var tz = end.z - start.z
        val xOffest = abs(if (tx < 0) xRem else tileWidth - xRem)
        val zOffest = abs(if (tz < 0) zRem else tileDepth - zRem)
        tx = abs(tx)
        tz = abs(tz)
        var tMaxX = xOffest / tx
        var tMaxZ = zOffest / tz
        val tDeltaX = tileWidth / tx
        val tDeltaZ = tileDepth / tz
        var t = 0f
        while (true) {
            val hit = traversHeightfield(sx, sz, start, end, t, min(1f, min(tMaxX, tMaxZ)))
            if (hit.isFinite()) return hit
            if ((if (dx > 0) sx >= ex else sx <= ex) && (if (dz > 0) sz >= ez else sz <= ez)) {
                break
            }
            if (tMaxX < tMaxZ) {
                t = tMaxX
                tMaxX += tDeltaX
                sx += stepX
            } else {
                t = tMaxZ
                tMaxZ += tDeltaZ
                sz += stepZ
            }
        }
        return Float.NaN
    }

    private fun traversHeightfield(
        x: Int, z: Int,
        start: Vector3f, end: Vector3f,
        tMin: Float, tMax: Float
    ): Float {
        val ohf = heightfieldProvider(x, z)
        if (ohf != null) {
            var tx = end.x - start.x
            val ty = end.y - start.y
            var tz = end.z - start.z
            val entry = Vector3f(start.x + tMin * tx, start.y + tMin * ty, start.z + tMin * tz)
            val exit = Vector3f(start.x + tMax * tx, start.y + tMax * ty, start.z + tMax * tz)
            val relStartX = entry.x - ohf.bmin.x
            val relStartZ = entry.z - ohf.bmin.z
            var sx = floor((relStartX / ohf.cellSize)).toInt()
            var sz = floor((relStartZ / ohf.cellSize)).toInt()
            val ex = floor(((exit.x - ohf.bmin.x) / ohf.cellSize)).toInt()
            val ez = floor(((exit.z - ohf.bmin.z) / ohf.cellSize)).toInt()
            val dx = ex - sx
            val dz = ez - sz
            val stepX = if (dx < 0) -1 else 1
            val stepZ = if (dz < 0) -1 else 1
            val xRem = ohf.cellSize + relStartX % ohf.cellSize % ohf.cellSize
            val zRem = ohf.cellSize + relStartZ % ohf.cellSize % ohf.cellSize
            val xOffest = abs(if (tx < 0) xRem else ohf.cellSize - xRem)
            val zOffest = abs(if (tz < 0) zRem else ohf.cellSize - zRem)
            val atx = 1f / abs(tx)
            val atz = 1f / abs(tz)
            var tMaxX = xOffest * atx
            var tMaxZ = zOffest * atz
            val tDeltaX = ohf.cellSize * atx
            val tDeltaZ = ohf.cellSize * atz
            var t = 0f
            while (true) {
                if (sx >= 0 && sx < ohf.width && sz >= 0 && sz < ohf.height) {
                    val bySpan = traversHeightfieldSpan(start, ohf, sx, sz, t, ty, tMin, tMaxX, tMaxZ)
                    if (bySpan.isFinite()) {
                        return bySpan
                    }
                }
                if ((if (dx > 0) sx >= ex else sx <= ex) && (if (dz > 0) sz >= ez else sz <= ez)) {
                    break
                }
                if (tMaxX < tMaxZ) {
                    t = tMaxX
                    tMaxX += tDeltaX
                    sx += stepX
                } else {
                    t = tMaxZ
                    tMaxZ += tDeltaZ
                    sz += stepZ
                }
            }
        }
        return Float.NaN
    }

    private fun traversHeightfieldSpan(
        start: Vector3f, ohf: Heightfield,
        sx: Int, sz: Int,
        t: Float, ty: Float, tMin: Float, tMaxX: Float, tMaxZ: Float
    ): Float {
        val y1 = start.y + ty * (tMin + t) - ohf.bmin.y
        val y2 = start.y + ty * (tMin + min(tMaxX, tMaxZ)) - ohf.bmin.y
        val minY = min(y1, y2) / ohf.cellHeight
        val maxY = max(y1, y2) / ohf.cellHeight
        var span = ohf.spans[sx + sz * ohf.width]
        while (span != null) {
            if (span.min <= minY && span.max >= maxY) {
                return min(1f, tMin + t)
            }
            span = span.next
        }
        return Float.NaN
    }
}