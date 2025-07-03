package me.anno.tests.maths.bvh

import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.raycast.RayHit
import me.anno.maths.bvh.BLASNode
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.HitType
import me.anno.maths.bvh.SplitMethod
import me.anno.utils.assertions.assertGreaterThan
import org.junit.jupiter.api.Test

class BLASRaycastTests {

    fun createBLAS(): BLASNode {
        val mesh = IcosahedronModel.createIcosphere(3)
        return BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, 16)!!
    }

    @Test
    fun testRaycastingSphereClosestHit() {
        var numCorrectHits = 0
        val hit = RayHit()
        val gen = RandomRayGenerator()
        val blas = createBLAS()
        repeat(1000) {
            hit.distance = 1e300
            val shouldHitSphere = gen.next()
            val hitsSphere = blas.raycast(gen.pos, gen.dir, hit)
            if (shouldHitSphere == hitsSphere) numCorrectHits++
        }
        assertGreaterThan(numCorrectHits, 990)
    }

    @Test
    fun testRaycastingSphereAnyHit() {
        var numCorrectHits = 0
        val hit = RayHit().apply { hitType = HitType.ANY }
        val blas = createBLAS()
        val gen = RandomRayGenerator()
        repeat(1000) {
            hit.distance = 1e300
            val shouldHitSphere = gen.next()
            val hitsSphere = blas.raycast(gen.pos, gen.dir, hit)
            if (shouldHitSphere == hitsSphere) numCorrectHits++
        }
        assertGreaterThan(numCorrectHits, 990)
    }
}