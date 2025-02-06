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

import org.recast4j.recast.PartitionType

class DynamicNavMeshConfig internal constructor(
    val useTiles: Boolean,
    val tileSizeX: Int,
    val tileSizeZ: Int,
    val cellSize: Float
) {
    var partitionType = PartitionType.WATERSHED
    var walkableHeight = 0f
    var walkableSlopeAngle = 0f
    var walkableRadius = 0f
    var walkableClimb = 0f
    var minRegionArea = 0f
    var regionMergeArea = 0f
    var maxEdgeLen = 0f
    var maxSimplificationError = 0f
    var verticesPerPoly = 0
    var buildDetailMesh = false
    var detailSampleDistance = 0f
    var detailSampleMaxError = 0f
    var filterLowHangingObstacles = true
    var filterLedgeSpans = true
    var filterWalkableLowHeightSpans = true
    var enableCheckpoints = true
    var keepIntermediateResults = false
}