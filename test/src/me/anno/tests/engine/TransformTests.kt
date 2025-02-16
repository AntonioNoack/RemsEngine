package me.anno.tests.engine

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.assertions.assertEquals
import org.joml.Matrix4x3
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.junit.jupiter.api.Test

class TransformTests {

    // todo bug: children move smoothly, when their parent is teleported
    @Test
    fun testTeleport() {
        val step = 16 * MILLIS_TO_NANOS
        val (parent, child, grandchild) = scene()
        child.setPosition(Vector3d(1.0, 0.0, 0.0))
        grandchild.setPosition(Vector3d(2.0, 0.0, 0.0))
        Time.updateTime(step, 0L)
        parent.teleportToGlobal(Vector3d(3.0, 0.0, 0.0))
        checkM(parent, child, grandchild, 3.0, 1.0, 2.0)
        Time.updateTime(2 * step, step)
        Time.updateTime(3 * step, 2 * step)
        parent.teleportToGlobal(Vector3d(20.0, 0.0, 0.0))
        checkM(parent, child, grandchild, 20.0, 1.0, 2.0)
        Time.updateTime(4 * step, 3 * step)
        checkM(parent, child, grandchild, 20.0, 1.0, 2.0)
    }

    @Test
    fun testSetPosition() {
        val (_, parent, child) = scene()
        // update child
        child.setPosition(5.0, 0.0, 0.0)
        check(parent, child, 0.0, 5.0)
        // then parent, and check that both moved
        parent.setPosition(1.0, 0.0, 0.0)
        check(parent, child, 1.0, 5.0)
        // then parent again, check that both moved
        parent.setPosition(2.0, 0.0, 0.0)
        check(parent, child, 2.0, 5.0)
        // finally move child, and check that it was moved
        child.setPosition(6.0, 0.0, 0.0)
        check(parent, child, 2.0, 6.0)
    }

    @Test
    fun testTeleportToGlobal() {
        val (_, parent, child) = scene()
        child.setPosition(5.0, 0.0, 0.0)
        parent.setPosition(1.0, 0.0, 0.0)
        check(parent, child, 1.0, 5.0)

        // move parent globally
        parent.teleportToGlobal(Vector3d(-3.0, 0.0, 0.0))
        check(parent, child, -3.0, 5.0)
    }

    @Test
    fun testMoveToGlobal() {
        val (_, parent, child) = scene()
        child.setPosition(5.0, 0.0, 0.0)
        parent.setPosition(1.0, 0.0, 0.0)
        check(parent, child, 1.0, 5.0)

        // move parent globally
        parent.moveToGlobal(Vector3d(-3.0, 0.0, 0.0))
        check(parent, child, -3.0, 5.0)
    }

    fun check(parent: Entity, child: Entity, px: Double, cx: Double) {
        check(parent.transform, child.transform, px, cx)
    }

    fun check(parent: Transform, child: Transform, px: Double, cx: Double) {
        assertEquals(Vector3d(px, 0.0, 0.0), parent.globalPosition)
        assertEquals(Vector3d(px, 0.0, 0.0), parent.localPosition)
        assertEquals(Vector3d(px + cx, 0.0, 0.0), child.globalPosition)
        assertEquals(Vector3d(cx, 0.0, 0.0), child.localPosition)
    }

    fun checkM(parent: Entity, child: Entity, grandchild: Entity, px: Double, cx: Double, gx: Double) {
        checkM(parent.transform, child.transform, grandchild.transform, px, cx, gx)
    }

    fun checkM(parent: Transform, child: Transform, grandchild: Transform, px: Double, cx: Double, gx: Double) {
        assertEquals(Matrix4x3().translate(px, 0.0, 0.0), parent.getDrawMatrix())
        assertEquals(Matrix4x3().translate(px + cx, 0.0, 0.0), child.getDrawMatrix())
        assertEquals(Matrix4x3().translate(px + cx + gx, 0.0, 0.0), grandchild.getDrawMatrix())
    }

    fun scene(): List<Entity> {
        val root = Entity("root")
        val parent = Entity("parent", root)
        val child = Entity("child", parent)
        return listOf(root, parent, child)
    }
}