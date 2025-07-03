package me.anno.tests.maths.bvh

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangle
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.OfficialExtensions
import me.anno.engine.raycast.RayHit
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.HitType
import me.anno.maths.bvh.SplitMethod
import me.anno.maths.bvh.TLASNode
import me.anno.tests.FlakyTest
import me.anno.utils.assertions.assertGreaterThan
import org.apache.logging.log4j.LogManager
import org.joml.Quaternionf
import org.joml.Vector3d
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TLASRaycastTests {

    companion object {
        private val LOGGER = LogManager.getLogger(TLASRaycastTests::class)
    }

    @Test
    fun runFlakyTest() {
        testRaycastingSphereAnyHit()
    }

    @BeforeEach
    fun init() {
        OfficialExtensions.initForTests()
    }

    fun createTLAS(): TLASNode {

        val scene = Entity()
        val sphereMesh = IcosahedronModel.createIcosphere(3)
        sphereMesh.forEachTriangle { a, b, c ->
            val subMesh = Mesh()
            val pos = FloatArray(9)
            a.get(pos, 0)
            b.get(pos, 3)
            c.get(pos, 6)
            subMesh.positions = pos
            scene.add(Entity().add(MeshComponent(subMesh)))
            false
        }

        // add them to PipelineStage
        val pipeline = Pipeline(null)
        pipeline.frustum.setToEverything(Vector3d(), Quaternionf())
        pipeline.fill(scene)

        val stage = pipeline.defaultStage
        // build TLAS
        return BVHBuilder.buildTLAS(stage, Vector3d(), SplitMethod.MEDIAN_APPROX, 8)!!
    }

    @Test
    @FlakyTest
    fun testRaycastingSphereClosestHit() {
        var numCorrectHits = 0
        val hit = RayHit()
        val gen = RandomRayGenerator()
        val blas = createTLAS()
        repeat(1000) {
            hit.distance = 1e300
            val shouldHitSphere = gen.next()
            val hitsSphere = blas.raycast(gen.pos, gen.dir, hit)
            if (shouldHitSphere == hitsSphere) numCorrectHits++
        }
        LOGGER.info("Sphere-Closest: $numCorrectHits/1000")
        assertGreaterThan(numCorrectHits, 990)
    }

    @Test
    @FlakyTest
    fun testRaycastingSphereAnyHit() {
        var numCorrectHits = 0
        val hit = RayHit().apply { hitType = HitType.ANY }
        val blas = createTLAS()
        val gen = RandomRayGenerator()
        repeat(1000) {
            hit.distance = 1e300
            val shouldHitSphere = gen.next()
            val hitsSphere = blas.raycast(gen.pos, gen.dir, hit)
            if (shouldHitSphere == hitsSphere) numCorrectHits++
        }
        LOGGER.info("Sphere-Any: $numCorrectHits/1000")
        assertGreaterThan(numCorrectHits, 990)
    }
}