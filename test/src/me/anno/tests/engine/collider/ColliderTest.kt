package me.anno.tests.engine.collider

import me.anno.ecs.components.collider.Axis
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.collider.UnionUtils.unionCube
import me.anno.ecs.components.collider.UnionUtils.unionPoint
import me.anno.ecs.components.collider.UnionUtils.unionRing
import me.anno.gpu.pipeline.Pipeline
import me.anno.utils.assertions.assertEquals
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.junit.jupiter.api.Test

class ColliderTest : Collider() {

    @Test
    fun testUnionRingSimple() {
        val m = Matrix4x3()
        val b = AABBd()
        b.clear()
        unionRing(m, b, Axis.X, 1.0, 3.0)
        assertEquals(AABBd(3.0, -1.0, -1.0, 3.0, 1.0, 1.0), b)

        b.clear()
        unionRing(m, b, Axis.Y, 1.0, 3.0)
        assertEquals(AABBd(-1.0, 3.0, -1.0, 1.0, 3.0, 1.0), b)

        b.clear()
        unionRing(m, b, Axis.Z, 1.0, 3.0)
        assertEquals(AABBd(-1.0, -1.0, 3.0, 1.0, 1.0, 3.0), b)
    }

    @Test
    fun testUnionCube() {
        val m = Matrix4x3()
        val b = AABBd()
        unionCube(m, b, 1.0, 2.0, 3.0)
        assertEquals(AABBd(-1.0, -2.0, -3.0, 1.0, 2.0, 3.0), b)
    }

    @Test
    fun testUnionTransform() {
        val m = Matrix4x3()
            .rotateYXZ(1f, 2f, 3f)
            .translate(1.0, 2.0, 3.0)
            .scale(1f, 2f, 3f)
        val b = AABBd()
        unionPoint(m, b, Vector3d(), 1.0, 2.0, 3.0)
        assertEquals(m.transformPosition(Vector3d(1.0, 2.0, 3.0)), b.getMin(Vector3d()))
        assertEquals(m.transformPosition(Vector3d(1.0, 2.0, 3.0)), b.getMax(Vector3d()))
    }

    override fun drawShape(pipeline: Pipeline) {}
}