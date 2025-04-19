package me.anno.recast

import me.anno.ecs.Entity
import me.anno.ecs.annotations.Docs
import me.anno.engine.serialization.SerializedProperty
import org.recast4j.detour.DefaultQueryFilter
import org.recast4j.detour.MeshData
import org.recast4j.detour.NavMesh
import org.recast4j.detour.NavMeshBuilder
import org.recast4j.detour.NavMeshDataCreateParams
import org.recast4j.detour.NavMeshQuery
import org.recast4j.detour.crowd.Crowd
import org.recast4j.detour.crowd.CrowdConfig
import org.recast4j.recast.PartitionType
import org.recast4j.recast.RecastBuilder
import org.recast4j.recast.RecastBuilderConfig
import org.recast4j.recast.RecastConfig

class NavMeshBuilder {

    // todo probably should be bakeable for quicker navigation at runtime in static scenes

    // todo dynamic nav mesh
    // todo dynamic colliders, connected to Bullet
    // todo load/unload tiles

    val agentType = AgentType()

    var cellSize = 0.3f
    var cellHeight = 0.2f

    var regionMinSize = 8
    var regionMergeSize = 20

    /**
     * Longest allowed edge length on the border of the mesh;
     * If an edge would be longer, it will be subdivided.
     * */
    var edgeMaxLen = 12.0f

    // should be >= 1f in my testing
    var edgeMaxError = 1.3f
    var maxVerticesPerPoly = 3

    // for height data only
    var detailSampleDist = 6.0f
    var detailSampleMaxError = 1.0f

    var partitionType = PartitionType.LAYERS

    @Docs("Only meshes with this collision flag will be considered")
    @SerializedProperty
    var collisionMask: Int = 1

    fun buildMesh(world: Entity): MeshData? {

        val geometry = GeoProvider(world, collisionMask)

        val config = RecastConfig(
            partitionType,
            cellSize, cellHeight,
            agentType.height,
            agentType.radius,
            agentType.maxStepHeight,
            agentType.maxSlopeDegrees,
            regionMinSize,
            regionMergeSize,
            edgeMaxLen,
            edgeMaxError,
            maxVerticesPerPoly,
            detailSampleDist,
            detailSampleMaxError,
            DefaultAreaModifications.GROUND
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
        p.walkableHeight = agentType.height
        p.walkableRadius = agentType.radius
        p.walkableClimb = agentType.maxStepHeight
        p.cellSize = cellSize
        p.cellHeight = cellHeight
        p.buildBvTree = true
        return NavMeshBuilder.createNavMeshData(p)
    }

    fun buildData(scene: Entity): NavMeshData? {
        val meshData = buildMesh(scene) ?: return null
        val navMesh = NavMesh(meshData, 3, 0)
        val config = CrowdConfig()
        config.maxAgentRadius = agentType.radius
        val crowd = Crowd(config, navMesh)
        return NavMeshData(
            meshData, navMesh, NavMeshQuery(navMesh), DefaultQueryFilter(), agentType,
            crowd, collisionMask
        )
    }
}