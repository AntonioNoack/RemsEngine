package me.anno.tests.engine.collider

import me.anno.ecs.components.collider.Axis
import me.anno.ecs.components.collider.Collider
import me.anno.gpu.pipeline.Pipeline
import me.anno.utils.assertions.assertEquals
import org.joml.AABBd
import org.joml.Matrix4x3m
import org.joml.Vector3d
import org.junit.jupiter.api.Test

class ColliderTest : Collider() {

    @Test
    fun testUnionRingSimple() {
        val m = Matrix4x3m()
        val b = AABBd()
        for (exact in listOf(true, false)) {
            b.clear()
            unionRing(m, b, Vector3d(), Axis.X, 1.0, 3.0, exact)
            assertEquals(AABBd(3.0, -1.0, -1.0, 3.0, 1.0, 1.0), b)
            b.clear()
            unionRing(m, b, Vector3d(), Axis.Y, 1.0, 3.0, exact)
            assertEquals(AABBd(-1.0, 3.0, -1.0, 1.0, 3.0, 1.0), b)
            b.clear()
            unionRing(m, b, Vector3d(), Axis.Z, 1.0, 3.0, exact)
            assertEquals(AABBd(-1.0, -1.0, 3.0, 1.0, 1.0, 3.0), b)
        }
    }

    @Test
    fun testUnionCube() {
        val m = Matrix4x3m()
        val b = AABBd()
        unionCube(m, b, Vector3d(), 1.0, 2.0, 3.0)
        assertEquals(AABBd(-1.0, -2.0, -3.0, 1.0, 2.0, 3.0), b)
    }

    @Test
    fun testUnionTransform() {
        val m = Matrix4x3m()
            .rotateYXZ(1f, 2f, 3f)
            .translate(1.0, 2.0, 3.0)
            .scale(1f, 2f, 3f)
        val b = AABBd()
        union(m, b, Vector3d(), 1.0, 2.0, 3.0)
        assertEquals(m.transformPosition(Vector3d(1.0, 2.0, 3.0)), b.getMin(Vector3d()))
        assertEquals(m.transformPosition(Vector3d(1.0, 2.0, 3.0)), b.getMax(Vector3d()))
    }

    override fun drawShape(pipeline: Pipeline) {}
}