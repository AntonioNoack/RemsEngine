package me.anno.tests

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.navigation.NavMesh
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.OS.documents
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.types.Matrices.set2
import me.anno.utils.types.Vectors.print
import org.joml.Matrix4x3f
import org.joml.Vector3d
import org.joml.Vector3f
import org.recast4j.LongArrayList
import org.recast4j.detour.*
import org.recast4j.recast.*
import org.recast4j.recast.RecastConstants.PartitionType
import org.recast4j.recast.geom.InputGeomProvider
import org.recast4j.recast.geom.TriMesh
import java.util.*

val sampleFile = documents.getChild("NavMeshTest.obj")

// test recast navmesh generation
fun main() {

    // done generate nav mesh
    // done display nav mesh

    // todo in the engine, mark recast-pathfinding-regions with bounding boxes
    // todo and then just have agent helpers or similar to do path finding in those areas :3

    testUI {

        // todo spawn active agent, and make it path-find to mouse position / click

        ECSRegistry.init()
        val base = PrefabCache[sampleFile]!!.createInstance()
        val entity = Entity()
        entity.add(base as Entity)
        val nmm = NavMesh()
        nmm.sampleFile = sampleFile
        val data = nmm.build()!!

        val navMeshEntity = Entity("NavMesh")
        val mesh = Mesh()
        val material = Material()
        material.diffuseBase.set(0.2f, 1f, 0.2f, 0.5f)
        mesh.material = material.ref
        mesh.positions = dataToMesh(data)
        val mc = MeshComponent()
        mc.mesh = mesh.ref
        navMeshEntity.add(mc)
        navMeshEntity.position = Vector3d(0.0, nmm.agentHeight * 0.3, 0.0) // offset for better visibility
        entity.add(navMeshEntity)

        // todo spawn agent somewhere...
        // todo click to set target point

        val navMesh = NavMesh(data, nmm.maxVerticesPerPoly, 0)
        val header = data.header!!
        val tileRef = navMesh.getTileRefAt(header.x, header.y, header.layer)

        val query = NavMeshQuery(navMesh)
        val filter = DefaultQueryFilter()
        val random = Random()
        val p0 = Vector3f()
        val p1 = Vector3f()

        val ref0 = query.findRandomPointWithinCircle(tileRef, p0, 200f, filter, random)
        val ref1 = query.findRandomPointWithinCircle(tileRef, p1, 200f, filter, random)

        println("refs: $ref0, $ref1")

        val path = query.findPath(ref0.result!!.randomRef, ref1.result!!.randomRef, p0, p1, filter)
        println("path: ${path.status}, ${path.message}, ${path.result}")
        for (v in path.result ?: LongArrayList.empty) {
            // convert ref to position
            val r = navMesh.getTileAndPolyByRef(v).result ?: continue
            val tile = r.first!!
            val poly = r.second
            val pos = Vector3f()
            val vs = tile.data!!.vertices
            for (idx in poly.vertices) {
                val i3 = idx * 3
                pos.add(vs[i3], vs[i3 + 1], vs[i3 + 2])
            }
            pos.div(poly.vertices.size.toFloat())
            println(pos.print())
        }

        testScene(entity)

    }

}

private operator fun LongArrayList.iterator(): Iterator<Long> {
    return object : Iterator<Long> {
        var index = 0
        override fun hasNext() = index < size
        override fun next() = get(index++)
    }
}

fun Vector3f.f() = floatArrayOf(x, y, z)

fun dataToMesh(data: MeshData): FloatArray {
    val fal = FloatArrayList(256)
    val dv = data.vertices
    val ddv = data.detailVertices
    for (i in 0 until data.header!!.polyCount) {
        val p = data.polygons[i]
        if (p.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) continue
        val pv = p.vertices
        val detailMesh = data.detailMeshes?.getOrNull(i)
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
            // FIXME: Use Poly if PolyDetail is unavailable
        }
    }
    return fal.toFloatArray()
}
