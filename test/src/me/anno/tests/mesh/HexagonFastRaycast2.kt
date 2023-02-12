package me.anno.tests.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.chunks.spherical.Hexagon
import me.anno.ecs.components.chunks.spherical.HexagonSphere
import me.anno.ecs.components.collider.MeshCollider
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshJoiner
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.raycast.RayHit
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.bvh.*
import me.anno.studio.StudioBase
import org.joml.AABBf
import org.joml.Matrix4x3f
import org.joml.Vector3f

/**
 * finding the correct intersection is slow, when done linearly:
 *  -> create an acceleration structure using entities
 * */
fun main() {

    // large n to stress it
    val n = 20

    // create an acceleration structure for path-trace tests
    val hexagons = HexagonSphere.createHexSphere(n)
    val objects = ArrayList<TLASLeaf0>(hexagons.size)

    val meshes = ArrayList<Mesh>()

    val indices = IntArray((6 - 2) * 3)
    for (j in 2 until 6) {
        val j3 = (j - 2) * 3
        indices[j3 + 1] = j - 1
        indices[j3 + 2] = j
    }
    val indices5 = IntArray((5 - 2) * 3)
    System.arraycopy(indices, 0, indices5, 0, indices5.size)

    class HexLeaf(hex: Hexagon, val index: Int, bounds: AABBf) : TLASLeaf0(hex.center, bounds) {
        override fun collectMeshes(result: MutableCollection<BLASNode>) = throw NotImplementedError()
        override fun print(depth: Int) = throw NotImplementedError()
        override fun intersect(group: RayGroup) = throw NotImplementedError()
        override fun intersect(pos: Vector3f, dir: Vector3f, invDir: Vector3f, dirIsNeg: Int, hit: RayHit) =
            throw NotImplementedError()
    }

    for (i in hexagons.indices) {
        val hex = hexagons[i]
        val corners = hex.corners
        val bounds = AABBf()
        bounds.union(hex.center)

        val positions = FloatArray(corners.size * 3)
        for (j in corners.indices) {
            val c = corners[j]
            bounds.union(c)
            c.get(positions, j * 3)
        }

        objects.add(HexLeaf(hex, i, bounds))

        val mesh = Mesh()
        mesh.positions = positions
        mesh.indices = if (corners.size == 6) indices else indices5
        meshes.add(mesh)
    }

    val simpleMesh = object : MeshJoiner<Mesh>(false, false, false) {
        override fun getMesh(element: Mesh) = element
        override fun getTransform(element: Mesh, dst: Matrix4x3f) {
            dst.identity()
        }
    }.join(Mesh(), meshes)

    // middle is 2.78s instead of 12.64s for n=500
    val tlas = BVHBuilder.buildTLAS(objects, SplitMethod.MIDDLE)
    fun build(node: TLASNode): PrefabSaveable {
        return if (node is TLASBranch) {
            Entity().apply {
                addChild(build(node.n0))
                addChild(build(node.n1))
            }
        } else {
            node as HexLeaf
            // todo these are not reacting for large distances
            MeshCollider(meshes[node.index].ref)
        }
    }

    // todo drawGizmos() is slow when traversing the hierarchy...
    // because Frustum.isVisible()?

    val world = Entity()
    world.addChild(build(tlas))
    world.add(MeshComponent(simpleMesh.ref).apply {
        collisionMask = 0 // disable collisions
    })
    testSceneWithUI(world) {
        StudioBase.instance?.enableVSync = false
        it.renderer.renderMode = RenderMode.RAY_TEST
    }

}