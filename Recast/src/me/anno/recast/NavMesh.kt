package me.anno.recast

import me.anno.ecs.Component
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.gpu.pipeline.Pipeline
import me.anno.recast.NavMeshDebug.drawNavMesh
import me.anno.utils.Color.black
import org.apache.logging.log4j.LogManager
import org.recast4j.detour.MeshData
import org.recast4j.detour.NavMeshBuilder
import org.recast4j.detour.NavMeshDataCreateParams
import org.recast4j.recast.AreaModification
import org.recast4j.recast.PartitionType
import org.recast4j.recast.RecastBuilder
import org.recast4j.recast.RecastBuilderConfig
import org.recast4j.recast.RecastConfig

class NavMesh : Component(), OnDrawGUI {

    companion object {
        private val LOGGER = LogManager.getLogger(NavMesh::class)
        var debugColor = 0xfff973 or black
    }

    // todo probably should be bakeable for quicker navigation at runtime in static scenes

    object DefaultAreaModifications {
        const val TYPE_MASK = 0x07
        const val TYPE_GROUND = 0x1
        const val TYPE_WATER = 0x2
        const val TYPE_ROAD = 0x3
        const val TYPE_DOOR = 0x4
        const val TYPE_GRASS = 0x5
        const val TYPE_JUMP = 0x6

        // todo how does this work, and what would be good defaults, that would be usable (nearly) everywhere?
        val GROUND = AreaModification(TYPE_GROUND, TYPE_MASK)
        val WATER = AreaModification(TYPE_WATER, TYPE_MASK)
        val ROAD = AreaModification(TYPE_ROAD, TYPE_MASK)
        val GRASS = AreaModification(TYPE_GRASS, TYPE_MASK)
        val DOOR = AreaModification(TYPE_DOOR, TYPE_DOOR)
        val JUMP = AreaModification(TYPE_JUMP, TYPE_JUMP)
        const val FLAG_WALK = 0x01 // Ability to walk (ground, grass, road)
        const val FLAG_SWIM = 0x02 // Ability to swim (water).
        const val FLAG_DOOR = 0x04 // Ability to move through doors.
        const val FLAG_JUMP = 0x08 // Ability to jump.
        const val FLAG_DISABLED = 0x10 // Disabled polygon
        const val FLAG_ALL = 0xffff // All abilities.
    }

    // todo dynamic nav mesh
    // todo dynamic colliders, connected to Bullet
    // todo load/unload tiles

    var cellSize = 0.3f
    var cellHeight = 0.2f
    var agentHeight = 2.0f
    var agentRadius = 0.6f
    var agentMaxClimb = 1.9f
    var agentMaxSlope = 45.0f
    var regionMinSize = 8
    var regionMergeSize = 20
    var edgeMaxLen = 12.0f

    // should be >= 1f in my testing
    var edgeMaxError = 1.3f
    var maxVerticesPerPoly = 3

    // for height data only
    var detailSampleDist = 6.0f
    var detailSampleMaxError = 1.0f

    var partitionType = PartitionType.LAYERS

    @NotSerializedProperty
    var data: MeshData? = null

    @Docs("Only meshes with this collision flag will be considered")
    @SerializedProperty
    var collisionMask: Int = 1

    fun build(): MeshData? {

        val world = entity
        if (world == null) {
            LOGGER.warn("World missing")
            return null
        }

        val geometry = GeoProvider(world, collisionMask)

        val config = RecastConfig(
            partitionType, cellSize, cellHeight, agentHeight, agentRadius,
            agentMaxClimb, agentMaxSlope, regionMinSize, regionMergeSize, edgeMaxLen, edgeMaxError,
            maxVerticesPerPoly, detailSampleDist, detailSampleMaxError, DefaultAreaModifications.GROUND
        )

        val builderConfig = RecastBuilderConfig(config, geometry.bounds)
        val built = RecastBuilder().build(geometry, builderConfig)

        val mesh = built.mesh
        for (i in 0 until mesh.numPolygons) {
            mesh.flags[i] = 1
        }
        val md = built.meshDetail
        val p = NavMeshDataCreateParams()
        p.vertices = mesh.vertices
        p.vertCount = mesh.numVertices
        p.polys = mesh.polygons
        p.polyFlags = mesh.flags
        p.polyAreas = mesh.areaIds
        p.polyCount = mesh.numPolygons
        p.maxVerticesPerPolygon = mesh.maxVerticesPerPolygon
        if (md != null) {
            p.detailVertices = md.vertices
            p.detailVerticesCount = md.numVertices
            p.detailTris = md.triangles
            p.detailTriCount = md.numTriangles
        }
        // todo properly set these values, these are just bogus values
        p.offMeshConVertices = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f)
        p.offMeshConRad = floatArrayOf(0.1f)
        p.offMeshConFlags = intArrayOf(12)
        p.offMeshConAreas = intArrayOf(2)
        p.offMeshConDir = intArrayOf(1)
        p.offMeshConUserID = intArrayOf(0x4567)
        p.offMeshConCount = 1
        p.bounds = mesh.bounds
        p.walkableHeight = agentHeight
        p.walkableRadius = agentRadius
        p.walkableClimb = agentMaxClimb
        p.cellSize = cellSize
        p.cellHeight = cellHeight
        p.buildBvTree = true
        return NavMeshBuilder.createNavMeshData(p)
    }

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        if (all) drawNavMesh(entity, data)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is NavMesh) return
        dst.agentHeight = agentHeight
        dst.agentRadius = agentRadius
        dst.agentMaxClimb = agentMaxClimb
        dst.agentMaxSlope = agentMaxSlope
        dst.partitionType = partitionType
        dst.maxVerticesPerPoly = maxVerticesPerPoly
        dst.cellSize = cellSize
        dst.cellHeight = cellHeight
        dst.regionMergeSize = regionMergeSize
        dst.regionMinSize = regionMinSize
        dst.edgeMaxLen = edgeMaxLen
        dst.edgeMaxError = edgeMaxError
        dst.detailSampleDist = detailSampleDist
        dst.detailSampleMaxError = detailSampleMaxError
    }
}