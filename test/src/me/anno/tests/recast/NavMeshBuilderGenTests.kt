package me.anno.tests.recast

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.CylinderModel
import me.anno.recast.NavMeshBuilder
import me.anno.recast.NavMeshDebug.toMesh
import me.anno.utils.assertions.assertEquals
import org.joml.AABBf
import org.junit.jupiter.api.Test

class NavMeshBuilderGenTests {
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
        val builder = NavMeshBuilder()
        builder.agentType.height = 1f
        builder.cellSize = 0.05f
        builder.cellHeight = 0.5f
        builder.agentType.radius = 0.01f
        builder.agentType.maxStepHeight = 0f
        builder.collisionMask = -1
        builder.edgeMaxError = 1f

        val meshData = builder.buildMesh(scene)!!
        return toMesh(meshData)!!
    }

    // todo test with tiles, check their links and such
}