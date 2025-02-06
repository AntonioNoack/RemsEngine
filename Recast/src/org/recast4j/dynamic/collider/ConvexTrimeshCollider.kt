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

import org.recast4j.recast.Heightfield
import org.recast4j.recast.RecastFilledVolumeRasterization.rasterizeConvex
import org.recast4j.recast.Telemetry
import kotlin.math.floor

class ConvexTrimeshCollider : AbstractCollider {
    private val vertices: FloatArray
    private val triangles: IntArray

    constructor(vertices: FloatArray, triangles: IntArray, area: Int, flagMergeThreshold: Float) : super(
        area,
        flagMergeThreshold,
        TrimeshCollider.computeBounds(vertices)
    ) {
        this.vertices = vertices
        this.triangles = triangles
    }

    constructor(
        vertices: FloatArray,
        triangles: IntArray,
        bounds: FloatArray?,
        area: Int,
        flagMergeThreshold: Float
    ) : super(area, flagMergeThreshold, bounds!!) {
        this.vertices = vertices
        this.triangles = triangles
    }

    override fun rasterize(hf: Heightfield, telemetry: Telemetry?) {
        rasterizeConvex(
            hf,
            vertices,
            triangles,
            area,
            floor((flagMergeThreshold / hf.cellHeight)).toInt(),
            telemetry
        )
    }
}