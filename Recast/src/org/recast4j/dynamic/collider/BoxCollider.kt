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

import org.joml.AABBf
import org.joml.Vector3f
import org.recast4j.recast.Heightfield
import org.recast4j.recast.RecastFilledVolumeRasterization.rasterizeBox
import org.recast4j.recast.Telemetry
import kotlin.math.floor

class BoxCollider(
    private val center: Vector3f,
    private val halfEdges: Array<FloatArray>,
    area: Int, flagMergeThreshold: Float
) : AbstractCollider(area, flagMergeThreshold, bounds(center, halfEdges)) {

    override fun rasterize(hf: Heightfield, telemetry: Telemetry?) {
        rasterizeBox(
            hf, center, halfEdges, area,
            floor(flagMergeThreshold / hf.cellHeight).toInt(),
            telemetry
        )
    }

    companion object {
        private fun bounds(center: Vector3f, halfEdges: Array<FloatArray>): AABBf {
            val (hex, hey, hez) = halfEdges
            val bounds = AABBf()
            for (i in 0..7) {
                val s0 = if (i and 1 != 0) 1f else -1f
                val s1 = if (i and 2 != 0) 1f else -1f
                val s2 = if (i and 4 != 0) 1f else -1f
                val vx = center.x + s0 * hex[0] + s1 * hey[0] + s2 * hez[0]
                val vy = center.y + s0 * hex[1] + s1 * hey[1] + s2 * hez[1]
                val vz = center.z + s0 * hex[2] + s1 * hey[2] + s2 * hez[2]
                bounds.union(vx, vy, vz)
            }
            return bounds
        }
    }
}