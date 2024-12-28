package me.anno.tests.engine

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.utils.assertions.assertEquals
import org.joml.Vector3d
import org.junit.jupiter.api.Test

class TransformTests {

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

    fun scene(): List<Entity> {
        val root = Entity("root")
        val parent = Entity("parent", root)
        val child = Entity("child", parent)
        return listOf(root, parent, child)
    }
}