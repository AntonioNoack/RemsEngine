package me.anno.tests.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.mesh.Shapes.flatCube
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class TestMeshComplete {
    // this fails when executing all tests :(, why???
    // todo is someone deleting flatCube.front?
    @Test
    fun testHasPositions() {
        val mesh = flatCube.front.clone() as Mesh
        assertNotNull(mesh.positions)
    }
}