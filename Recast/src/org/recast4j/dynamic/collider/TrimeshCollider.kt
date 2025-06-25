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

import me.anno.utils.algorithms.ForLoop.forLoopSafely
import org.joml.AABBf
import org.recast4j.recast.Heightfield
import org.recast4j.recast.RecastRasterization.rasterizeTriangle
import org.recast4j.recast.Telemetry
import kotlin.math.floor

class TrimeshCollider(
    private val vertices: FloatArray,
    private val triangles: IntArray,
    bounds: AABBf, area: Int,
    flagMergeThreshold: Float
) : AbstractCollider(area, flagMergeThreshold, bounds) {

    constructor(vertices: FloatArray, triangles: IntArray, area: Int, flagMergeThreshold: Float) :
            this(vertices, triangles, computeBounds(vertices), area, flagMergeThreshold)

    override fun rasterize(hf: Heightfield, telemetry: Telemetry?) {
        forLoopSafely(triangles.size, 3) { i ->
            rasterizeTriangle(
                hf, vertices,
                triangles[i], triangles[i + 1], triangles[i + 2],
                area, floor(flagMergeThreshold / hf.cellHeight).toInt(),
                telemetry
            )
        }
    }

    companion object {
        fun computeBounds(vertices: FloatArray): AABBf {
            val bounds = AABBf()
            forLoopSafely(vertices.size, 3) { i ->
                bounds.union(vertices, i)
            }
            return bounds
        }
    }
}