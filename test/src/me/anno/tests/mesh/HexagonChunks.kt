package me.anno.tests.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.chunks.spherical.Hexagon
import me.anno.ecs.components.chunks.spherical.HexagonSphere
import me.anno.ecs.components.chunks.spherical.HexagonSphere.createFaceMesh
import me.anno.ecs.components.chunks.spherical.HexagonSpherePartitioner
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.Color.toVecRGB

private fun createMesh(color: Int): Mesh {
    val mesh = Mesh()
    val material = Material()
    material.diffuseBase.set(0f, 0f, 0f, 1f)
    material.emissiveBase.set(color.toVecRGB().mul(3f))
    material.isDoubleSided = true
    mesh.material = material.ref
    return mesh
}

fun chunkToMesh(chunk: Array<Hexagon>, color: Int = (Math.random() * 1e9).toInt()): Mesh {
    val mesh = createMesh(color)
    createFaceMesh(mesh, chunk, 0, chunk.size, 0)
    return mesh
}

fun chunkToMesh2(chunk: Array<Hexagon>, color: Int = (Math.random() * 1e9).toInt()): Mesh {
    val mesh = createMesh(color)
    createConnectionMesh(mesh, chunk)
    return mesh
}

fun main() {
    val n = 10
    val s = 2
    val hexagons = HexagonSphere.createHexSphere(n)
    val chunks = HexagonSpherePartitioner.partitionIntoSubChunks(n, s, hexagons)
    val entity = Entity()
    for (chunk in chunks) {
        entity.add(Entity().apply {
            add(MeshComponent(chunkToMesh(chunk)))
        })
    }
    testSceneWithUI(entity)
}