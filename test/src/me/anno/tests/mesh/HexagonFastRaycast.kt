package me.anno.tests.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.chunks.spherical.HexagonSphere
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.MeshJoiner
import me.anno.engine.raycast.RayHit
import me.anno.engine.ui.render.DrawAABB
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.input.Input
import me.anno.maths.bvh.*
import me.anno.studio.StudioBase
import org.joml.*

/**
 * finding the correct intersection is slow, when done linearly:
 *  -> create an acceleration structure
 * */
fun main() {

    // large n to stress it
    val n = 50

    // create an acceleration structure for path-trace tests
    val hexagons = HexagonSphere.createHexSphere(n)
    val objects = ArrayList<TLASLeaf>(hexagons.size)

    // except for pentagons, all hexagons look approx. the same,
    //  so we could find their transform, and use the same BLAS for all

    // this also means we could draw a hexagon sphere using MeshSpawner :)

    val meshes = ArrayList<Mesh>()

    val unit = Matrix4x3f()

    val indices = IntArray((6 - 2) * 3)
    for (j in 2 until 6) {
        val j3 = (j - 2) * 3
        indices[j3 + 1] = j - 1
        indices[j3 + 2] = j
    }
    val indices5 = IntArray((5 - 2) * 3)
    System.arraycopy(indices, 0, indices5, 0, indices5.size)

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

        val geometry = GeometryData(positions, positions, indices, null)
        val blas = BLASLeaf(0, corners.size - 2, geometry, bounds)
        objects.add(TLASLeaf(hex.center, unit, unit, blas, bounds))

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

    val spawner = object : MeshComponentBase() {
        override fun getMesh() = simpleMesh
        override fun clone() = throw NotImplementedError()
        override fun onDrawGUI(all: Boolean) {
            if (Input.isShiftDown) {
                val tmp = AABBd()
                tlas.forEach {
                    val worldScale = RenderState.worldScale
                    DrawAABB.drawAABB(tmp.set(it.bounds), worldScale, -1)
                }
            }
        }

        override fun raycast(
            entity: Entity, start: Vector3d, direction: Vector3d, end: Vector3d,
            radiusAtOrigin: Double, radiusPerUnit: Double,
            typeMask: Int, includeDisabled: Boolean, result: RayHit
        ): Boolean {
            val start1 = Vector3f(start)
            val dir1 = Vector3f(direction)
            return if (tlas.intersect(start1, dir1, result)) {
                // Raycast.raycastTriangleMesh() does this calculation
                result.positionWS.set(direction).mul(result.distance).add(start)
                result.mesh = simpleMesh
                result.component = this
                true
            } else false
        }
    }

    testSceneWithUI(spawner) {
        StudioBase.instance?.enableVSync = false
        it.renderer.renderMode = RenderMode.RAY_TEST
    }

}