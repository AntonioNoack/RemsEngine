package me.anno.tests.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.mesh.Shapes.flatCube
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class TestMeshComplete {
    @Test
    fun testHasPositions() {
        val mesh = flatCube.front.clone() as Mesh
        assertNotNull(mesh.positions)
    }
}