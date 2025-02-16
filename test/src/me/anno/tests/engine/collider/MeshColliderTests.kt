package me.anno.tests.engine.collider

import me.anno.ecs.components.collider.MeshCollider
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.raycast.RayQueryLocal
import me.anno.maths.Maths.length
import me.anno.maths.Maths.min
import me.anno.maths.bvh.HitType
import me.anno.utils.assertions.assertEquals
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.random.Random

class MeshColliderTests {

    companion object {
        private val sphere3 = IcosahedronModel.createIcosphere(3)
    }

    @Test
    fun testSignedDistanceWithoutNormal() {
        val collider = MeshCollider(sphere3)
        collider.enableSimplifications = false
        val random = Random(1324)
        for (i in 0 until 100) {
            val x = (random.nextFloat() - 0.5f) * 3f
            val y = (random.nextFloat() - 0.5f) * 3f
            val z = (random.nextFloat() - 0.5f) * 3f
            val expectedDistance = length(x, y, z) - 1f
            val actualDistance = collider.getSignedDistance(Vector3f(x, y, z))
            assertEquals(expectedDistance, actualDistance, 0.005f)
        }
    }

    @Test
    fun testSignedDistanceWithNormal() {
        val collider = MeshCollider(sphere3)
        val random = Random(1324)
        for (i in 0 until 100) {
            val x = (random.nextFloat() - 0.5f) * 3f
            val y = (random.nextFloat() - 0.5f) * 3f
            val z = (random.nextFloat() - 0.5f) * 3f
            val expectedDistance = length(x, y, z) - 1f
            val expectedNormal = Vector3f(x, y, z).normalize()
            val actualNormal = Vector3f()
            val actualDistance = collider.getSignedDistance(Vector3f(x, y, z), actualNormal)
            actualNormal.normalize()
            assertEquals(expectedDistance, actualDistance, 0.005f)
            assertEquals(expectedNormal, actualNormal, 0.07)
        }
    }

    @Test
    fun testUnion() {
        val collider = MeshCollider(sphere3)
        val bounds = AABBd()
        collider.union(Matrix4x3(), bounds, Vector3d(), false)
        assertEquals(sphere3.getBounds(), AABBf(bounds))
    }

    @Test
    fun testRaycast() {
        val collider = MeshCollider(sphere3)
        val random = Random(1324)
        for (i in 0 until 100) {
            val x = (random.nextFloat() - 0.5f) * 3f
            val y = (random.nextFloat() - 0.5f) * 3f
            val normal = Vector3f()
            val pos = Vector3f(x, y, -2f)
            val dir = Vector3f(0f, 0f, 1f)
            val dist1 = collider.raycast(RayQueryLocal(pos, dir, 1e3f, HitType.CLOSEST), normal)
            val expectedDist = 2f - sqrt(1f - (x * x + y * y))
            assertEquals(min(expectedDist, 1e38f), min(dist1, 1e38f), 0.05f)
        }
    }
}