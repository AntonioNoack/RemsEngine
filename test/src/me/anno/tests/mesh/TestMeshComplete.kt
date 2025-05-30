package me.anno.tests.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.assertions.assertNotNull
import org.junit.jupiter.api.Test

class TestMeshComplete {
    @Test
    fun testHasPositions() {
        val mesh = flatCube.front.shallowClone()
        assertNotNull(mesh.positions)
    }
}