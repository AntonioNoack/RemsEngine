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
import org.recast4j.recast.RecastFilledVolumeRasterization.rasterizeSphere
import org.recast4j.recast.Telemetry
import kotlin.math.floor

class SphereCollider(private val center: Vector3f, private val radius: Float, area: Int, flagMergeThreshold: Float) :
    AbstractCollider(area, flagMergeThreshold, bounds(center, radius)) {
    override fun rasterize(hf: Heightfield, telemetry: Telemetry?) {
        rasterizeSphere(
            hf, center, radius, area, floor((flagMergeThreshold / hf.cellHeight)).toInt(),
            telemetry
        )
    }

    companion object {
        private fun bounds(center: Vector3f, radius: Float): FloatArray {
            return floatArrayOf(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
            )
        }
    }
}