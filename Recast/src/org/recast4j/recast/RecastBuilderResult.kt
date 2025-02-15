package org.recast4j.recast

class RecastBuilderResult(
    val tileX: Int,
    val tileZ: Int,
    val solidHeightField: Heightfield,
    val compactHeightField: CompactHeightfield,
    val contourSet: ContourSet,
    val mesh: PolyMesh,
    val meshDetail: PolyMeshDetail?,
    val telemetry: Telemetry?
)