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
import org.recast4j.recast.Heightfield
import org.recast4j.recast.RecastFilledVolumeRasterization.rasterizeCylinder
import org.recast4j.recast.Telemetry
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class CylinderCollider(
    private val start: Vector3f,
    private val end: Vector3f,
    private val radius: Float,
    area: Int,
    flagMergeThreshold: Float
) : AbstractCollider(area, flagMergeThreshold, bounds(start, end, radius)) {
    override fun rasterize(hf: Heightfield, telemetry: Telemetry?) {
        rasterizeCylinder(
            hf, start, end, radius, area, floor((flagMergeThreshold / hf.cellHeight)).toInt(),
            telemetry
        )
    }

    companion object {
        private fun bounds(start: Vector3f, end: Vector3f, radius: Float): FloatArray {
            return floatArrayOf(
                min(start.x, end.x) - radius, min(start.y, end.y) - radius, min(start.z, end.z) - radius,
                max(start.x, end.x) + radius, max(start.y, end.y) + radius, max(start.z, end.z) + radius
            )
        }
    }
}