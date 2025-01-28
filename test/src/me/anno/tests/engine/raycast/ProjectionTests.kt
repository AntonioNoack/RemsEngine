package me.anno.tests.engine.raycast

import me.anno.engine.raycast.Projection.projectRayToAABBBack
import me.anno.engine.raycast.Projection.projectRayToAABBFront
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class ProjectionTests {

    @Test
    fun testFrontAABBCollisionSomeSpecificCases() {
        val bounds1 = AABBf(-1f, -2f, -3f, 4f, 5f, 6f)
        val bounds2 = AABBd(bounds1)
        fun test(dist: Float, pos: Vector3f, dir: Vector3f) {
            dir.normalize()
            val hit1 = projectRayToAABBFront(pos, dir, bounds1, Vector3f())
            assertEquals(dist, hit1, 1e-6f)
            val hit2 = projectRayToAABBFront(Vector3d(pos), Vector3d(dir), bounds2, Vector3d())
            assertEquals(dist.toDouble(), hit2, 1e-7)
        }

        // test direct collision with division by zero
        test(19f, Vector3f(-20f, 0f, 0f), Vector3f(1f, 0f, 0f))
        test(18f, Vector3f(0f, -20f, 0f), Vector3f(0f, 1f, 0f))
        test(17f, Vector3f(0f, 0f, -20f), Vector3f(0f, 0f, 1f))
        test(16f, Vector3f(20f, 0f, 0f), Vector3f(-1f, 0f, 0f))
        test(15f, Vector3f(0f, 20f, 0f), Vector3f(0f, -1f, 0f))
        test(14f, Vector3f(0f, 0f, 20f), Vector3f(0f, 0f, -1f))

        // corner
        test(sqrt(2f), Vector3f(-2f, -3f, 0f), Vector3f(1f, 1f, 0f))
        test(sqrt(2f), Vector3f(-1.9f, -3f, 0f), Vector3f(1f, 1f, 0f))
        test(sqrt(2f), Vector3f(-2f, -2.9f, 0f), Vector3f(1f, 1f, 0f))
    }

    @Test
    fun testBackAABBCollisionSomeSpecificCases() {
        val bounds1 = AABBf(-1f, -2f, -3f, 4f, 5f, 6f)
        val bounds2 = AABBd(bounds1)
        fun test(dist: Float, pos: Vector3f, dir: Vector3f) {
            dir.normalize()
            val hit1 = projectRayToAABBBack(pos, dir, bounds1, Vector3f())
            assertEquals(dist, hit1, 1e-6f)
            val hit2 = projectRayToAABBBack(Vector3d(pos), Vector3d(dir), bounds2, Vector3d())
            assertEquals(dist.toDouble(), hit2, 1e-7)
        }

        // test direct collision with division by zero
        test(24f, Vector3f(-20f, 0f, 0f), Vector3f(1f, 0f, 0f))
        test(25f, Vector3f(0f, -20f, 0f), Vector3f(0f, 1f, 0f))
        test(26f, Vector3f(0f, 0f, -20f), Vector3f(0f, 0f, 1f))
        test(21f, Vector3f(20f, 0f, 0f), Vector3f(-1f, 0f, 0f))
        test(22f, Vector3f(0f, 20f, 0f), Vector3f(0f, -1f, 0f))
        test(23f, Vector3f(0f, 0f, 20f), Vector3f(0f, 0f, -1f))

        // corner
        test(sqrt(2f), Vector3f(3f, 4f, 0f), Vector3f(1f, 1f, 0f))
        test(sqrt(2f), Vector3f(3.1f, 4f, 0f), Vector3f(1f, 1f, 0f))
        test(sqrt(2f), Vector3f(3f, 4.1f, 0f), Vector3f(1f, 1f, 0f))
    }

    @Test
    fun testFrontAABBCollisionGenerally() {
        val rnd = Random(1656)
        val pos1 = Vector3f()
        val pos2 = Vector3d()
        val dir1 = Vector3f()
        val dir2 = Vector3d()
        val dst1 = Vector3f()
        val dst2 = Vector3d()
        val bounds1 = AABBf()
        val bounds2 = AABBd()
        var hits = 0
        for (i in 0 until 100) {
            // test float method
            dir1.set(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()).sub(0.5f).normalize()
            pos1.set(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()).sub(0.5f).mul(3f).sub(dir1)
            bounds1.setMin(-rnd.nextFloat(), -rnd.nextFloat(), -rnd.nextFloat())
            bounds1.setMax(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat())
            val hit1 = projectRayToAABBFront(pos1, dir1, bounds1, dst1)
            if (hit1.isFinite()) {
                // check that a correct side was hit
                val hx = if (dir1.x > 0f) bounds1.minX else bounds1.maxX
                val hy = if (dir1.y > 0f) bounds1.minY else bounds1.maxY
                val hz = if (dir1.z > 0f) bounds1.minZ else bounds1.maxZ
                assertTrue(eq(dst1.x, hx) || eq(dst1.y, hy) || eq(dst1.z, hz))
                hits++
            }
            // test double method on same data
            dir2.set(dir1)
            pos2.set(pos1)
            bounds2.set(bounds1)
            val hit2 = projectRayToAABBFront(pos2, dir2, bounds2, dst2)
            assertEquals(min(hit1.toDouble(), 1e38), min(hit2, 1e38), 1e-5)
            assertEquals(dst1, dst2, 1e-5)
        }
        assertTrue(hits > 50)
        assertTrue(hits < 100)
    }

    @Test
    fun testBackAABBCollisionGenerally() {
        val rnd = Random(1656)
        val pos1 = Vector3f()
        val pos2 = Vector3d()
        val dir1 = Vector3f()
        val dir2 = Vector3d()
        val dst1 = Vector3f()
        val dst2 = Vector3d()
        val bounds1 = AABBf()
        val bounds2 = AABBd()
        var hits = 0
        for (i in 0 until 100) {
            // test float method
            dir1.set(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()).sub(0.5f).normalize()
            pos1.set(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()).sub(0.5f).mul(3f).sub(dir1)
            bounds1.setMin(-rnd.nextFloat(), -rnd.nextFloat(), -rnd.nextFloat())
            bounds1.setMax(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat())
            val hit1 = projectRayToAABBBack(pos1, dir1, bounds1, dst1)
            if (hit1.isFinite()) {
                // check that a correct side was hit
                val hx = if (dir1.x < 0f) bounds1.minX else bounds1.maxX
                val hy = if (dir1.y < 0f) bounds1.minY else bounds1.maxY
                val hz = if (dir1.z < 0f) bounds1.minZ else bounds1.maxZ
                assertTrue(eq(dst1.x, hx) || eq(dst1.y, hy) || eq(dst1.z, hz))
                hits++
            }
            // test double method on same data
            dir2.set(dir1)
            pos2.set(pos1)
            bounds2.set(bounds1)
            val hit2 = projectRayToAABBBack(pos2, dir2, bounds2, dst2)
            assertEquals(min(hit1.toDouble(), 1e38), min(hit2, 1e38), 1e-5)
            assertEquals(dst1, dst2, 1e-5)
        }
        assertTrue(hits > 50)
        assertTrue(hits < 100)
    }

    fun eq(x: Float, y: Float, e: Float = 1e-6f): Boolean {
        return abs(x - y) <= e
    }
}