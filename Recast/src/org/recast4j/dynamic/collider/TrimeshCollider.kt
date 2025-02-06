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

import org.recast4j.dynamic.collider.CompositeCollider.Companion.emptyBounds
import org.recast4j.recast.Heightfield
import org.recast4j.recast.RecastRasterization.rasterizeTriangle
import org.recast4j.recast.Telemetry
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class TrimeshCollider : AbstractCollider {
    private val vertices: FloatArray
    private val triangles: IntArray

    constructor(vertices: FloatArray, triangles: IntArray, area: Int, flagMergeThreshold: Float) :
            super(area, flagMergeThreshold, computeBounds(vertices)) {
        this.vertices = vertices
        this.triangles = triangles
    }

    constructor(
        vertices: FloatArray,
        triangles: IntArray,
        bounds: FloatArray,
        area: Int,
        flagMergeThreshold: Float
    ) : super(area, flagMergeThreshold, bounds) {
        this.vertices = vertices
        this.triangles = triangles
    }

    override fun rasterize(hf: Heightfield, telemetry: Telemetry?) {
        var i = 2
        while (i < triangles.size) {
            rasterizeTriangle(
                hf, vertices,
                triangles[i - 2], triangles[i - 1], triangles[i],
                area, floor((flagMergeThreshold / hf.cellHeight)).toInt(),
                telemetry
            )
            i += 3
        }
    }

    companion object {
        fun computeBounds(vertices: FloatArray): FloatArray {
            var i = 2
            val bounds = emptyBounds()
            while (i < vertices.size) {
                val x = vertices[i - 2]
                val y = vertices[i - 1]
                val z = vertices[i]
                bounds[0] = min(bounds[0], x)
                bounds[1] = min(bounds[1], y)
                bounds[2] = min(bounds[2], z)
                bounds[3] = max(bounds[3], x)
                bounds[4] = max(bounds[4], y)
                bounds[5] = max(bounds[5], z)
                i += 3
            }
            return bounds
        }
    }
}