package me.anno.tests.physics.shapes

import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.collision.shapes.TriangleMeshShape
import me.anno.bullet.createBulletSphereShape
import me.anno.bullet.createBulletMeshShape
import me.anno.ecs.components.collider.MeshCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.tests.physics.shapes.SDFSphereTest.Companion.nextPos
import me.anno.utils.assertions.assertEquals
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import kotlin.random.Random

class MeshSphereTest {
    @Test
    fun testMeshSphereConvexWithoutSimplifications() {
        val baseline = SphereCollider().createBulletSphereShape(Vector3d(1.0))
        assertEquals(1.0, baseline.margin)
        val tested = MeshCollider(IcosahedronModel.createIcosphere(2))
            .apply {
                margin = 0f
                enableSimplifications = false
                isConvex = true
            }
            .createBulletMeshShape(Vector3d(1.0)) as ConvexShape
        assertEquals(0.0, tested.margin)
        val random = Random(1234)
        for (i in 0 until 100) {
            val pos = random.nextPos()
            val expected = baseline.localGetSupportingVertex(pos, Vector3d())
            val actual = tested.localGetSupportingVertex(pos, Vector3d())
            assertEquals(expected, actual, 0.17) // error comes from low poly approximation
        }
    }

    @Test
    fun testMeshSphereConvexWithSimplifications() {
        val baseline = SphereCollider().createBulletSphereShape(Vector3d(1.0))
        assertEquals(1.0, baseline.margin)
        val tested = MeshCollider(IcosahedronModel.createIcosphere(3))
            .apply {
                margin = 0f
                enableSimplifications = true
                isConvex = true
            }
            .createBulletMeshShape(Vector3d(1.0)) as ConvexShape
        assertEquals(0.0, tested.margin)
        val random = Random(1234)
        for (i in 0 until 100) {
            val pos = random.nextPos()
            val expected = baseline.localGetSupportingVertex(pos, Vector3d())
            val actual = tested.localGetSupportingVertex(pos, Vector3d())
            assertEquals(expected, actual, 0.30) // error comes from low poly approximation
        }
    }

    @Test
    fun testMeshSphereConcave() {
        val baseline = SphereCollider().createBulletSphereShape(Vector3d(1.0))
        assertEquals(1.0, baseline.margin)
        val tested = MeshCollider(IcosahedronModel.createIcosphere(2))
            .apply {
                margin = 0f
                isConvex = false
            }
            .createBulletMeshShape(Vector3d(1.0)) as TriangleMeshShape
        assertEquals(0.0, tested.margin)
        val random = Random(1234)
        for (i in 0 until 100) {
            val pos = random.nextPos()
            val expected = baseline.localGetSupportingVertex(pos, Vector3d())
            val actual = tested.localGetSupportingVertex(pos, Vector3d())
            assertEquals(expected, actual, 0.17) // error comes from low poly approximation
        }
    }
}