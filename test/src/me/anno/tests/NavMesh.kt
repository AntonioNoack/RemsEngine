package me.anno.tests

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.maths.Maths.hasFlag
import me.anno.mesh.Shapes.smoothCube
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.OS.documents
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.types.Matrices.set2
import me.anno.utils.types.Triangles.rayTriangleIntersection
import org.joml.Matrix4x3f
import org.joml.Vector3d
import org.joml.Vector3f
import org.recast4j.detour.*
import org.recast4j.recast.*
import org.recast4j.recast.RecastConstants.PartitionType
import org.recast4j.recast.geom.InputGeomProvider
import org.recast4j.recast.geom.TriMesh

val sampleFile = documents.getChild("NavMeshTest.obj")

// test recast navmesh generation
fun main() {

    // done generate nav mesh
    // done display nav mesh

    // todo in the engine, mark recast-pathfinding-regions with bounding boxes
    // todo and then just have agent helpers or similar to do path finding in those areas :3

    testUI {

        // todo spawn active agent, and make it path-find

        ECSRegistry.init()
        val base = PrefabCache[sampleFile]!!.createInstance()
        val entity = Entity()
        entity.add(base as Entity)
        val data = build()!!

        val navMeshEntity = Entity("NavMesh")
        val mesh = Mesh()
        val material = Material()
        material.diffuseBase.set(0.2f, 1f, 0.2f, 0.5f)
        mesh.material = material.ref
        mesh.positions = dataToMesh(data)
        val mc = MeshComponent()
        mc.mesh = mesh.ref
        navMeshEntity.add(mc)
        navMeshEntity.position = Vector3d(0.0, agentHeight * 0.3, 0.0) // offset for better visibility
        entity.add(navMeshEntity)

        testScene(entity)
    }

}

fun dataToMesh(data: MeshData): FloatArray {
    val fal = FloatArrayList(256)
    val dv = data.verts
    val ddv = data.detailVerts
    for (i in 0 until data.header.polyCount) {
        val p = data.polys[i]
        if (p.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) continue
        val pv = p.verts
        val detailMesh = data.detailMeshes[i]
        if (detailMesh != null) {
            for (j in 0 until detailMesh.triCount) {
                val t = (detailMesh.triBase + j) * 4
                for (k in 0 until 3) {
                    val v = data.detailTris[t + k]
                    if (v < p.vertCount) {
                        fal.add(dv, pv[v] * 3, 3)
                    } else {
                        fal.add(ddv, (detailMesh.vertBase + v - p.vertCount) * 3, 3)
                    }
                }
            }
        } else {
            // FIXME: Use Poly if PolyDetail is unavailable
        }
    }
    return fal.toFloatArray()
}

object SampleAreaModifications {
    var SAMPLE_TYPE_MASK = 0x07
    var SAMPLE_TYPE_GROUND = 0x1
    var SAMPLE_TYPE_WATER = 0x2
    var SAMPLE_TYPE_ROAD = 0x3
    var SAMPLE_TYPE_DOOR = 0x4
    var SAMPLE_TYPE_GRASS = 0x5
    var SAMPLE_TYPE_JUMP = 0x6
    var SAMPLE_GROUND = AreaModification(
        SAMPLE_TYPE_GROUND,
        SAMPLE_TYPE_MASK
    )
    var SAMPLE_WATER = AreaModification(
        SAMPLE_TYPE_WATER,
        SAMPLE_TYPE_MASK
    )
    var SAMPLE_ROAD = AreaModification(
        SAMPLE_TYPE_ROAD,
        SAMPLE_TYPE_MASK
    )
    var SAMPLE_GRASS = AreaModification(
        SAMPLE_TYPE_GRASS,
        SAMPLE_TYPE_MASK
    )
    var SAMPLE_DOOR = AreaModification(
        SAMPLE_TYPE_DOOR,
        SAMPLE_TYPE_DOOR
    )
    var SAMPLE_JUMP = AreaModification(
        SAMPLE_TYPE_JUMP,
        SAMPLE_TYPE_JUMP
    )
    const val SAMPLE_FLAG_WALK = 0x01 // Ability to walk (ground, grass, road)
    const val SAMPLE_FLAG_SWIM = 0x02 // Ability to swim (water).
    const val SAMPLE_FLAG_DOOR = 0x04 // Ability to move through doors.
    const val SAMPLE_FLAG_JUMP = 0x08 // Ability to jump.
    const val SAMPLE_FLAG_DISABLED = 0x10 // Disabled polygon
    const val SAMPLE_FLAG_ALL = 0xffff // All abilities.
}

class GeoProvider(world: Entity) : InputGeomProvider {

    init {
        world.validateAABBs()
    }

    fun transform(tr: Matrix4x3f, tmp: Vector3f, src: FloatArray, si: Int, dst: FloatArray, di: Int) {
        tmp.set(src[si], src[si + 1], src[si + 2])
        tr.transformPosition(tmp)
        dst[di] = tmp.x
        dst[di + 1] = tmp.y
        dst[di + 2] = tmp.z
    }

    val meshes = ArrayList<TriMesh>()
    val convexHulls = ArrayList<ConvexVolume>()

    init {
        for (it in world.getComponentsInChildren(MeshComponentBase::class)) {
            val mesh = it.getMesh()
            println("processing $it, mesh: $mesh")
            if (mesh != null) {

                val vertices = mesh.positions!!
                val faces = mesh.indices ?: IntArray(vertices.size / 3) { it }
                val vs = FloatArray(vertices.size)
                // apply transform onto mesh
                val tmp = Vector3f()
                val tr = Matrix4x3f()
                    .set2(it.transform!!.globalTransform)
                for (i in vs.indices step 3) {
                    transform(tr, tmp, vertices, i, vs, i)
                }
                meshes.add(TriMesh(vertices, faces))

                // generate convex hull
                val bounds = mesh.aabb
                // easiest convex hull: aabb
                val volume = ConvexVolume()
                val hull = FloatArray(8 * 3)
                for (i in 0 until 8) {
                    val j = i * 3
                    hull[j] = (if (i.hasFlag(1)) bounds.maxX else bounds.minX).toFloat()
                    hull[j + 1] = (if (i.hasFlag(2)) bounds.maxY else bounds.minY).toFloat()
                    hull[j + 2] = (if (i.hasFlag(4)) bounds.maxZ else bounds.minZ).toFloat()
                }
                volume.hmin = hull[1]
                volume.hmax = hull[2 * 3 + 1]
                val hullIndices = smoothCube.indices!!
                val hull2 = FloatArray(hullIndices.size * 3)
                for (i in hullIndices.indices) {
                    transform(tr, tmp, hull, hullIndices[i] * 3, hull2, 3 * i)
                }
                volume.verts = hull2
                // todo based on collider navmesh settings
                volume.areaMod = SampleAreaModifications.SAMPLE_GROUND
                convexHulls.add(volume)

            }
        }
    }

    override fun meshes() = meshes
    override fun convexVolumes() = convexHulls

    val boundsMin: FloatArray
    val boundsMax: FloatArray

    init {
        val aabb = world.aabb
        boundsMin = floatArrayOf(aabb.minX.toFloat(), aabb.minY.toFloat(), aabb.minZ.toFloat())
        boundsMax = floatArrayOf(aabb.maxX.toFloat(), aabb.maxY.toFloat(), aabb.maxZ.toFloat())
    }

    override fun getMeshBoundsMin() = boundsMin
    override fun getMeshBoundsMax() = boundsMax

}

private const val cellSize = 0.3f
private const val cellHeight = 0.2f
private const val agentHeight = 2.0f
private const val agentRadius = 0.6f
private const val agentMaxClimb = 0.9f
private const val agentMaxSlope = 45.0f
private const val regionMinSize = 8
private const val regionMergeSize = 20
private const val edgeMaxLen = 12.0f
private const val edgeMaxError = 1.3f
private const val maxVerticesPerPoly = 3
private const val detailSampleDist = 6.0f
private const val detailSampleMaxError = 1.0f

fun build() = build(
    GeoProvider(PrefabCache[sampleFile]!!.createInstance() as Entity),
    PartitionType.WATERSHED,
    cellSize,
    cellHeight,
    agentHeight,
    agentRadius,
    agentMaxClimb,
    agentMaxSlope,
    regionMinSize,
    regionMergeSize,
    edgeMaxLen,
    edgeMaxError,
    maxVerticesPerPoly,
    detailSampleDist,
    detailSampleMaxError
)

fun build(
    geometry: InputGeomProvider, partitionType: PartitionType?, cellSize: Float,
    cellHeight: Float, agentHeight: Float, agentRadius: Float, agentMaxClimb: Float, agentMaxSlope: Float,
    regionMinSize: Int, regionMergeSize: Int, edgeMaxLen: Float, edgeMaxError: Float, maxVerticesPerPoly: Int,
    detailSampleDist: Float, detailSampleMaxError: Float
): MeshData? {

    val config = RecastConfig(
        partitionType, cellSize, cellHeight, agentHeight, agentRadius,
        agentMaxClimb, agentMaxSlope, regionMinSize, regionMergeSize, edgeMaxLen, edgeMaxError,
        maxVerticesPerPoly, detailSampleDist, detailSampleMaxError, SampleAreaModifications.SAMPLE_GROUND
    )

    val builderConfig = RecastBuilderConfig(config, geometry.meshBoundsMin, geometry.meshBoundsMax)

    val built = RecastBuilder { a, b -> println("building progress: $a/$b") }
        .build(geometry, builderConfig)
    val mesh = built.mesh
    for (i in 0 until mesh.npolys) {
        mesh.flags[i] = 1
    }
    val p = NavMeshDataCreateParams()
    p.verts = mesh.verts
    p.vertCount = mesh.nverts
    p.polys = mesh.polys
    p.polyAreas = mesh.areas
    p.polyFlags = mesh.flags
    p.polyCount = mesh.npolys
    p.nvp = mesh.nvp
    val meshDetail = built.meshDetail
    if (meshDetail != null) {// can happen, if there is no valid surface
        p.detailMeshes = meshDetail.meshes
        p.detailVerts = meshDetail.verts
        p.detailVertsCount = meshDetail.nverts
        p.detailTris = meshDetail.tris
        p.detailTriCount = meshDetail.ntris
    } else println("warn: mesh detail is null")
    p.walkableHeight = agentHeight
    p.walkableRadius = agentRadius
    p.walkableClimb = agentMaxClimb
    p.bmin = mesh.bmin
    p.bmax = mesh.bmax
    p.cs = cellSize
    p.ch = cellHeight
    p.buildBvTree = true
    p.offMeshConVerts = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f)
    p.offMeshConRad = floatArrayOf(0.1f)
    p.offMeshConDir = intArrayOf(1)
    p.offMeshConAreas = intArrayOf(2)
    p.offMeshConFlags = intArrayOf(12)
    p.offMeshConUserID = intArrayOf(0x4567)
    p.offMeshConCount = 1
    return NavMeshBuilder.createNavMeshData(p)
}

fun raycast(mesh: NavMesh, start: Vector3f, dir: Vector3f, dist: Float): Float {
    for (t in 0 until mesh.maxTiles) {
        val tile = mesh.getTile(t)
        if (tile?.data != null) {
            val intersection = raycast(tile.data, start, dir, dist)
            if (intersection < dist) return intersection
        }
    }
    return Float.POSITIVE_INFINITY
}

private fun raycast(data: MeshData, start: Vector3f, dir: Vector3f, dist: Float): Float {
    val dv = data.detailVerts
    val dt = data.detailTris
    val vs2 = data.verts
    val vs = Array(3) { Vector3f() }
    for (i in 0 until data.header.polyCount) {
        val p = data.polys[i]
        if (p.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) continue
        val pv = p.verts
        val detailMesh = data.detailMeshes[i]
        if (detailMesh != null) {
            for (j in 0 until detailMesh.triCount) {
                val t = (detailMesh.triBase + j) * 4
                for (k in 0 until 3) {
                    val v = dt[t + k]
                    val vk = vs[k]
                    if (v < p.vertCount) {
                        val bi = pv[v] * 3
                        vk.set(vs2[bi], vs2[bi + 1], vs2[bi + 2])
                    } else {
                        val bi = (detailMesh.vertBase + v - p.vertCount) * 3
                        vk.set(dv[bi], dv[bi + 1], dv[bi + 2])
                    }
                }
                val intersection = rayTriangleIntersection(start, dir, vs[0], vs[1], vs[2], dist, true)
                if (intersection != null && intersection.second < dist) return intersection.second
            }
        } else {
            // FIXME: Use Poly if PolyDetail is unavailable
        }
    }
    return Float.POSITIVE_INFINITY
}