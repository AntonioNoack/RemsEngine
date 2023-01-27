package me.anno.ecs.components.navigation

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.types.Arrays.resize
import org.joml.Vector3f
import org.recast4j.detour.MeshData
import org.recast4j.detour.NavMeshBuilder
import org.recast4j.detour.NavMeshDataCreateParams
import org.recast4j.detour.Poly
import org.recast4j.recast.*

class NavMesh : Component() {

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


    // todo draw mesh for debugging

    // todo dynamic nav mesh
    // todo crowd navigation
    // todo tiled nav mesh

    var cellSize = 0.3f
    var cellHeight = 0.2f
    var agentHeight = 2.0f
    var agentRadius = 0.6f
    var agentMaxClimb = 1.9f
    var agentMaxSlope = 45.0f
    var regionMinSize = 8
    var regionMergeSize = 20
    var edgeMaxLen = 12.0f
    var edgeMaxError = 1.3f
    var maxVerticesPerPoly = 3
    var detailSampleDist = 6.0f
    var detailSampleMaxError = 1.0f

    var partitionType = RecastConstants.PartitionType.WATERSHED

    private var data: MeshData? = null
    private var mesh: org.recast4j.detour.NavMesh? = null
    var sampleFile: FileReference = InvalidRef

    fun build2() {
        data = data ?: build() ?: return
        mesh = mesh ?: org.recast4j.detour.NavMesh(data!!, maxVerticesPerPoly, 0)
    }

    fun build(): MeshData? {

        // todo for the geometry, collect all colliders from the scene

        val world = PrefabCache[sampleFile]?.createInstance() as? Entity ?: return null
        val geometry = GeoProvider(world)

        val config = RecastConfig(
            partitionType, cellSize, cellHeight, agentHeight, agentRadius,
            agentMaxClimb, agentMaxSlope, regionMinSize, regionMergeSize, edgeMaxLen, edgeMaxError,
            maxVerticesPerPoly, detailSampleDist, detailSampleMaxError, DefaultAreaModifications.GROUND
        )

        val builderConfig = RecastBuilderConfig(config, geometry.meshBoundsMin, geometry.meshBoundsMax)

        val built = RecastBuilder().build(geometry, builderConfig)
        val mesh = built.mesh
        for (i in 0 until mesh.numPolygons) {
            mesh.flags[i] = 1
        }
        val p = NavMeshDataCreateParams()
        p.vertices = mesh.vertices
        p.vertCount = mesh.numVertices
        p.polys = mesh.polygons
        p.polyAreas = mesh.areaIds
        p.polyFlags = mesh.flags
        p.polyCount = mesh.numPolygons
        p.maxVerticesPerPolygon = mesh.maxVerticesPerPolygon
        val meshDetail = built.meshDetail
        if (meshDetail != null) {// can happen, if there is no valid surface
            p.detailMeshes = meshDetail.subMeshes
            p.detailVertices = meshDetail.vertices
            p.detailVerticesCount = meshDetail.numVertices
            p.detailTris = meshDetail.triangles
            p.detailTriCount = meshDetail.numTriangles
        } else println("warn: mesh detail is null")
        p.walkableHeight = agentHeight
        p.walkableRadius = agentRadius
        p.walkableClimb = agentMaxClimb
        p.bmin = mesh.bmin
        p.bmax = mesh.bmax
        p.cellSize = cellSize
        p.cellHeight = cellHeight
        p.buildBvTree = true
        p.offMeshConVertices = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f)
        p.offMeshConRad = floatArrayOf(0.1f)
        p.offMeshConDir = intArrayOf(1)
        p.offMeshConAreas = intArrayOf(2)
        p.offMeshConFlags = intArrayOf(12)
        p.offMeshConUserID = intArrayOf(0x4567)
        p.offMeshConCount = 1

        return NavMeshBuilder.createNavMeshData(p)

    }

    /**
     * create a mesh from the nav mesh data
     * */
    fun toMesh(mesh: Mesh = Mesh()): Mesh? {
        val data = data ?: return null
        val fal = FloatArrayList(256)
        val dv = data.vertices
        val ddv = data.detailVertices
        val header = data.header!!
        for (i in 0 until header.polyCount) {
            val p = data.polygons[i]
            if (p.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) continue
            val pv = p.vertices
            val detailMesh = data.detailMeshes?.get(i)
            if (detailMesh != null) {
                for (j in 0 until detailMesh.triCount) {
                    val t = (detailMesh.triBase + j) * 4
                    for (k in 0 until 3) {
                        val v = data.detailTriangles[t + k]
                        if (v < p.vertCount) {
                            fal.add(dv, pv[v] * 3, 3)
                        } else {
                            fal.add(ddv, (detailMesh.vertBase + v - p.vertCount) * 3, 3)
                        }
                    }
                }
            } else {
                // todo Use Poly if PolyDetail is unavailable
            }
        }
        mesh.positions = fal.toFloatArray()
        mesh.normals = mesh.normals.resize(fal.size)
        return mesh
    }

    override fun onDrawGUI(all: Boolean) {
        if (all) {
            val data = data ?: return
            val dv = data.vertices
            val ddv = data.detailVertices
            val a = Vector3f()
            val b = Vector3f()
            val c = Vector3f()
            val header = data.header!!
            for (i in 0 until header.polyCount) {
                val p = data.polygons[i]
                if (p.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) continue
                val pv = p.vertices
                val detailMesh = data.detailMeshes?.get(i)
                if (detailMesh != null) {
                    for (j in 0 until detailMesh.triCount) {
                        val t = (detailMesh.triBase + j) * 4

                        val v0 = data.detailTriangles[t]
                        if (v0 < p.vertCount) a.set(dv, pv[v0] * 3)
                        else a.set(ddv, (detailMesh.vertBase + v0 - p.vertCount) * 3)

                        val v1 = data.detailTriangles[t + 1]
                        if (v1 < p.vertCount) b.set(dv, pv[v1] * 3)
                        else b.set(ddv, (detailMesh.vertBase + v1 - p.vertCount) * 3)

                        val v2 = data.detailTriangles[t + 2]
                        if (v2 < p.vertCount) c.set(dv, pv[v2] * 3)
                        else c.set(ddv, (detailMesh.vertBase + v2 - p.vertCount) * 3)

                        LineShapes.drawLine(entity, a, b)
                        LineShapes.drawLine(entity, b, c)
                        LineShapes.drawLine(entity, c, a)

                    }
                } else {
                    // todo Use Poly if PolyDetail is unavailable
                }
            }
        }
    }

    override fun clone(): Component {
        val clone = NavMesh()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as NavMesh
        clone.agentHeight = agentHeight
        clone.agentRadius = agentRadius
        clone.agentMaxClimb = agentMaxClimb
        clone.agentMaxSlope = agentMaxSlope
        clone.partitionType = partitionType
        clone.maxVerticesPerPoly = maxVerticesPerPoly
        clone.cellSize = cellSize
        clone.cellHeight = cellHeight
        clone.regionMergeSize = regionMergeSize
        clone.regionMinSize = regionMinSize
        clone.edgeMaxLen = edgeMaxLen
        clone.edgeMaxError = edgeMaxError
        clone.detailSampleDist = detailSampleDist
        clone.detailSampleMaxError = detailSampleMaxError
    }

}