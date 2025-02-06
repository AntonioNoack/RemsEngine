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
package org.recast4j.dynamic.collider

import org.joml.Vector3f
import org.recast4j.dynamic.collider.CompositeCollider.Companion.emptyBounds
import org.recast4j.recast.Heightfield
import org.recast4j.recast.RecastFilledVolumeRasterization.rasterizeBox
import org.recast4j.recast.Telemetry
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class BoxCollider(
    private val center: Vector3f,
    private val halfEdges: Array<FloatArray>,
    area: Int,
    flagMergeThreshold: Float
) : AbstractCollider(area, flagMergeThreshold, bounds(center, halfEdges)) {
    override fun rasterize(hf: Heightfield, telemetry: Telemetry?) {
        rasterizeBox(
            hf,
            center,
            halfEdges,
            area,
            floor((flagMergeThreshold / hf.cellHeight)).toInt(),
            telemetry
        )
    }

    companion object {
        private fun bounds(center: Vector3f, halfEdges: Array<FloatArray>): FloatArray {
            val bounds = emptyBounds()
            for (i in 0..7) {
                val s0 = if (i and 1 != 0) 1f else -1f
                val s1 = if (i and 2 != 0) 1f else -1f
                val s2 = if (i and 4 != 0) 1f else -1f
                val vx = center.x + s0 * halfEdges[0][0] + s1 * halfEdges[1][0] + s2 * halfEdges[2][0]
                val vy = center.y + s0 * halfEdges[0][1] + s1 * halfEdges[1][1] + s2 * halfEdges[2][1]
                val vz = center.z + s0 * halfEdges[0][2] + s1 * halfEdges[1][2] + s2 * halfEdges[2][2]
                bounds[0] = min(bounds[0], vx)
                bounds[1] = min(bounds[1], vy)
                bounds[2] = min(bounds[2], vz)
                bounds[3] = max(bounds[3], vx)
                bounds[4] = max(bounds[4], vy)
                bounds[5] = max(bounds[5], vz)
            }
            return bounds
        }
    }
}