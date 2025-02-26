package me.anno.tests.maths.bvh

import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.raycast.RayHit
import me.anno.maths.bvh.BLASNode
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.HitType
import me.anno.maths.bvh.SplitMethod
import me.anno.utils.assertions.assertGreaterThan
import me.anno.utils.types.Booleans.toInt
import org.junit.jupiter.api.Test

class BLASRaycastTests {

    fun createBLAS(): BLASNode {
        val mesh = IcosahedronModel.createIcosphere(3)
        return BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, 16)!!
    }

    @Test
    fun testRaycastingSphereClosestHit() {
        var ctr = 0
        val hit = RayHit()
        val gen = RandomRayGenerator()
        val blas = createBLAS()
        for (i in 0 until 1000) {
            hit.distance = 1e300
            val shouldHitSphere = gen.next()
            val hitsSphere = blas.raycast(gen.pos, gen.dir, hit)
            ctr += (shouldHitSphere == hitsSphere).toInt()
        }
        assertGreaterThan(ctr, 990)
    }

    @Test
    fun testRaycastingSphereAnyHit() {
        var ctr = 0
        val hit = RayHit().apply { hitType = HitType.ANY }
        val blas = createBLAS()
        val gen = RandomRayGenerator()
        for (i in 0 until 1000) {
            hit.distance = 1e300
            val shouldHitSphere = gen.next()
            val hitsSphere = blas.raycast(gen.pos, gen.dir, hit)
            ctr += (shouldHitSphere == hitsSphere).toInt()
        }
        assertGreaterThan(ctr, 990)
    }
}