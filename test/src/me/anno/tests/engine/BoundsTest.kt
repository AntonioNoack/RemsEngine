package me.anno.tests.engine

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.joml.AABBd
import org.junit.jupiter.api.Test

class BoundsTest {

    val mesh = flatCube.front
    val mesh2 = flatCube.scaled(2f).front
    val cubeBounds1 = AABBd(mesh.getBounds())
    val cubeBounds2 = AABBd(mesh2.getBounds())

    @Test
    fun testEmpty() {
        assertTrue(AABBd().isEmpty())
        assertEquals(AABBd(), Entity().getGlobalBounds())
    }

    @Test
    fun testSimple() {
        val entity = Entity().add(MeshComponent(mesh))
        assertFalse(cubeBounds1.isEmpty())
        assertEquals(cubeBounds1, entity.getGlobalBounds())
    }

    @Test
    fun testSimpleAddLater() {
        val entity = Entity()
        entity.add(MeshComponent(mesh))
        assertEquals(cubeBounds1, entity.getGlobalBounds())
        entity.add(MeshComponent(mesh2))
        assertEquals(cubeBounds2, entity.getGlobalBounds())
    }

    @Test
    fun testSimpleAddLaterReverse() {
        val entity = Entity()
        entity.add(MeshComponent(mesh2))
        assertEquals(cubeBounds2, entity.getGlobalBounds())
        entity.add(MeshComponent(mesh))
        assertEquals(cubeBounds2, entity.getGlobalBounds())
    }

    @Test
    fun testAddingChildFirst() {
        // first add entity, then mesh
        val parent = Entity("Parent")
        val child1 = Entity("Child1", parent)
        child1.add(MeshComponent(mesh))
        assertEquals(cubeBounds1, child1.getGlobalBounds())
        assertEquals(cubeBounds1, parent.getGlobalBounds())
        val child2 = Entity("Child2", parent)
        child2.add(MeshComponent(mesh2))
        assertEquals(cubeBounds2, child2.getGlobalBounds())
        assertEquals(cubeBounds2, parent.getGlobalBounds())
    }

    @Test
    fun testAddingMeshFirst() {
        // first add mesh, then entity
        val parent = Entity("Parent")
        val child1 = Entity("Child1")
            .add(MeshComponent(mesh))
        assertEquals(cubeBounds1, child1.getGlobalBounds())
        parent.add(child1)
        assertEquals(cubeBounds1, child1.getGlobalBounds())
        assertEquals(cubeBounds1, parent.getGlobalBounds())
        val child2 = Entity("Child2")
            .add(MeshComponent(mesh2))
        parent.add(child2)
        assertEquals(cubeBounds2, child2.getGlobalBounds())
        assertEquals(cubeBounds2, parent.getGlobalBounds())
    }

    @Test
    fun testRemovingComponent() {
        // first add mesh, then entity
        val parent = Entity("Parent")
        val child1 = Entity("Child1", parent)
            .add(MeshComponent(mesh))
        val child2 = Entity("Child2", parent)
            .add(MeshComponent(mesh2))
        assertEquals(cubeBounds1, child1.getGlobalBounds())
        assertEquals(cubeBounds2, child2.getGlobalBounds())
        assertEquals(cubeBounds2, parent.getGlobalBounds())
        assertTrue(child2.remove(child2.getComponent(MeshComponent::class)!!))
        assertEquals(cubeBounds1, parent.getGlobalBounds())
        assertTrue(child2.getGlobalBounds().isEmpty())
    }

    @Test
    fun testRemovingEntity() {
        // first add mesh, then entity
        val parent = Entity("Parent")
        val child1 = Entity("Child1", parent)
            .add(MeshComponent(mesh))
        val child2 = Entity("Child2", parent)
            .add(MeshComponent(mesh2))
        assertEquals(cubeBounds1, child1.getGlobalBounds())
        assertEquals(cubeBounds2, child2.getGlobalBounds())
        assertEquals(cubeBounds2, parent.getGlobalBounds())
        assertTrue(parent.remove(child2))
        assertEquals(cubeBounds1, parent.getGlobalBounds())
        assertTrue(parent.remove(child1))
        assertTrue(parent.getGlobalBounds().isEmpty())
        assertFalse(parent.remove(child1)) // already removed -> should return false
        assertFalse(parent.remove(child2)) // same
    }
}