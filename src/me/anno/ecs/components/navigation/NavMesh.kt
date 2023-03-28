package me.anno.ecs.components.navigation

import me.anno.ecs.Component
import me.anno.ecs.annotations.Docs
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes
import me.anno.image.raw.IntImage
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.utils.OS.desktop
import me.anno.utils.structures.arrays.ExpandingFloatArray
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


    // done draw mesh for debugging

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

    var partitionType = RecastConstants.PartitionType.LAYERS

    @NotSerializedProperty
    var data: MeshData? = null

    @NotSerializedProperty
    private var mesh: org.recast4j.detour.NavMesh? = null

    @Docs("Only meshes with this collision flag will be considered")
    @SerializedProperty
    var collisionMask: Int = 1

    fun build2() {
        val data = data ?: build() ?: return
        this.data = data
        mesh = mesh ?: org.recast4j.detour.NavMesh(data, maxVerticesPerPoly, 0)
    }

    fun build(): MeshData? {

        // todo for the geometry, collect all colliders from the scene

        val world = entity ?: return null
        val geometry = GeoProvider(world, collisionMask)
        println("bounds: ${geometry.bounds}")
        println(
            "meshes: ${
                geometry.meshes.joinToString {
                    "${it.triangles.size} tris x ${it.vertices.size} vertices"
                }
            }"
        )

        val config = RecastConfig(
            partitionType, cellSize, cellHeight, agentHeight, agentRadius,
            agentMaxClimb, agentMaxSlope, regionMinSize, regionMergeSize, edgeMaxLen, edgeMaxError,
            maxVerticesPerPoly, detailSampleDist, detailSampleMaxError, DefaultAreaModifications.GROUND
        )

        println("agent: $agentRadius x $agentHeight")

        println("bounds2: ${geometry.meshBoundsMin}, ${geometry.meshBoundsMax}")
        val builderConfig = RecastBuilderConfig(config, geometry.meshBoundsMin, geometry.meshBoundsMax)

        val built = RecastBuilder().build(geometry, builderConfig)
        println("got results: ${built.tileX},${built.tileZ}")
        println(built.mesh.run {
            "${this.numVertices} verts, ${this.numPolygons} polys, ${this.bmin}..${this.bmax}, " +
                    "${this.flags.size} flags, verts-values: ${this.vertices.size}"
        })
        println(built.meshDetail)
        println(built.contourSet.run {
            "contours: $width x $height, cs: $cellSize, ch: $cellHeight, ${contours.size} contours"
        })
        built.telemetry?.print()
        println(built.compactHeightField.run {
            "compactHeightField: cells[${index.size}]: ${index.indices.count { index[it] + 1 == endIndex[it] }} + ..." +
                    "${this.width} x ${this.height}, ${this.borderSize} border, ${this.areas.size} areas, " +
                    "${this.spans.size} spans"
        })

        built.compactHeightField.apply {
            IntImage(width, height, IntArray(width * height) {
                if (endIndex[it] > index[it]) spans[index[it]].y * 0x10101 else 0
            }, false)
                .write(desktop.getChild("compactHeight.png"))
        }

        val mesh = built.mesh
        for (i in 0 until mesh.numPolygons) {
            mesh.flags[i] = 1
        }
        // val i0 = IntArray(0)
        // val f0 = FloatArray(0)
        val md = built.meshDetail
        /*val p = NavMeshDataCreateParams(
            mesh.vertices,
            mesh.numVertices,
            mesh.polygons,
            mesh.flags,
            mesh.areaIds,
            mesh.numPolygons,
            mesh.maxVerticesPerPolygon,
            md?.subMeshes ?: i0,
            md?.vertices ?: f0,
            md?.numVertices ?: 0,
            md?.triangles ?: i0,
            md?.numTriangles ?: 0,
            floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f),
            floatArrayOf(0.1f),
            intArrayOf(12),
            intArrayOf(2),
            intArrayOf(1),
            intArrayOf(0x4567), 1,
            0, 0, 0, 0,
            mesh.bmin, mesh.bmax,
            agentHeight, agentRadius, agentMaxClimb,
            cellSize, cellHeight, true
        ) // */
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
            p.detailTris = md.subMeshes
            p.detailVerticesCount = md.numVertices
            p.detailTris = md.triangles
            p.detailTriCount = md.numTriangles
            println("det-vertices: ${p.detailVerticesCount}")
            println("det-tris: ${p.detailTriCount}")
        } else println("md is null")
        p.offMeshConVertices = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f)
        p.offMeshConRad = floatArrayOf(0.1f)
        p.offMeshConFlags = intArrayOf(12)
        p.offMeshConAreas = intArrayOf(2)
        p.offMeshConDir = intArrayOf(1)
        p.offMeshConUserID = intArrayOf(0x4567)
        p.offMeshConCount = 1
        // 0, 0, 0, 0,
        p.bmin = mesh.bmin
        p.bmax = mesh.bmax
        p.walkableHeight = agentHeight
        p.walkableRadius = agentRadius
        p.walkableClimb = agentMaxClimb
        p.cellSize = cellSize
        p.cellHeight = cellHeight
        p.buildBvTree = true // */

        println("vertices: ${mesh.numVertices}")
        println("polys: ${mesh.numPolygons}")

        println("building...")
        return NavMeshBuilder.createNavMeshData(p)

    }

    /**
     * create a mesh from the nav mesh data
     * */
    fun toMesh(mesh: Mesh = Mesh()): Mesh? {
        val data = data ?: return null
        val dv = data.vertices
        val ddv = data.detailVertices
        val header = data.header!!
        var triCount = 0
        for (i in 0 until header.polyCount) {
            val p = data.polygons[i]
            if (p.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) continue
            val detailMesh = data.detailMeshes?.get(i)
            if (detailMesh != null) {
                triCount += detailMesh.triCount
            } else {
                // todo Use Poly if PolyDetail is unavailable
            }
        }
        val fal = ExpandingFloatArray(triCount * 3)
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

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as NavMesh
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