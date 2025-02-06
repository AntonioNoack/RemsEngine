package me.anno.tests.recast

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.CylinderModel
import me.anno.recast.NavMesh
import me.anno.recast.NavMeshDebug.toMesh
import me.anno.utils.assertions.assertEquals
import org.joml.AABBf
import org.junit.jupiter.api.Test

class NavMeshGenTests {
    @Test
    fun testGenerateComplexCircle() {
        testGenerateNGon(512)
    }

    @Test
    fun testGenerateCircle() {
        testGenerateNGon(32)
    }

    @Test
    fun testGenerateSquare() {
        testGenerateNGon(4)
    }

    fun testGenerateNGon(n: Int) {
        val geometryMesh = CylinderModel.createCylinder(
            n, 2, true, false,
            null, 1f, Mesh()
        )
        val navMesh = generateNavMesh(geometryMesh)
        // validate there is polygons and all polygons are within bounds
        assertEquals(
            AABBf(
                -0.9f, 1.5f, -0.9f,
                0.9f, 1.5f, 0.9f
            ), navMesh.getBounds(), 0.1
        )
    }

    fun generateNavMesh(mesh: Mesh): Mesh {
        // scene is needed as a mesh-lookup
        val scene = Entity()
        scene.add(MeshComponent(mesh).apply { collisionMask = -1 })

        // setup recast
        val navMesh1 = NavMesh()
        navMesh1.agentHeight = 1f
        navMesh1.cellSize = 0.05f
        navMesh1.cellHeight = 0.5f
        navMesh1.agentRadius = 0.01f
        navMesh1.agentMaxClimb = 0f
        navMesh1.collisionMask = -1
        navMesh1.edgeMaxError = 1f
        scene.add(navMesh1)

        val meshData = navMesh1.build()!!
        return toMesh(meshData)!!
    }

    // todo test with tiles, check their links and such
}