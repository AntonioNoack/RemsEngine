package me.anno.tests.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.LODMeshComponent
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.terrain.HeightMap
import me.anno.ecs.components.mesh.terrain.DefaultNormalMap
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel.fillInYAndNormals
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel.generateQuadIndices
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel.generateQuadVertices
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel.generateSparseQuadIndices
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.noise.PerlinNoise
import org.joml.Vector4f

/**
 * test the function "generateSparseQuadIndices" to create simple LODs without much computational overhead,
 * and reusing the same vertex data (at least on the CPU)
 *
 * todo we could allow helper/sub-meshes to be controlled by a LOD-manager...
 * */
fun main() {

    val sx = 320
    val sy = 512

    val lods = ArrayList<Mesh>()
    val lod0 = Mesh()

    val noise = PerlinNoise(1324, 8, 0.5f, -50f, +50f, Vector4f(0.01f))
    val cellSize = 1f
    val flip = false

    generateQuadVertices(sx, sy, cellSize, lod0, true)
    val heightMap = HeightMap { x, y -> noise[x.toFloat(), y.toFloat()] }
    fillInYAndNormals(sx, sy, heightMap, DefaultNormalMap(heightMap, cellSize, flip, 0, 0), lod0)
    generateQuadIndices(sx, sy, flip, lod0)
    lods.add(lod0)

    for (i in 1 until 5) {
        val lodI = Mesh()
        lodI.positions = lod0.positions
        lodI.normals = lod0.normals

        generateSparseQuadIndices(sx, sy, sx shr i, sy shr i, flip, lodI)

        lods.add(lodI)
    }

    val scene = Entity()
    val spacing = 1.2
    val posZ = sy * cellSize * spacing * 0.5
    for ((i, lod) in lods.withIndex()) {
        val posX = (i - (lods.lastIndex * 0.5)) * sx * cellSize * spacing
        Entity("LOD$i", scene)
            .add(MeshComponent(lod))
            .setPosition(posX, 0.0, posZ)
    }

    val lodComponent = LODMeshComponent(lods.map { it.ref })
    lodComponent.lod1Dist = 100.0
    Entity("LODs", scene)
        .add(lodComponent)
        .setPosition(0.0, 0.0, -posZ)

    testSceneWithUI("Sparse Terrain LODs", scene)
}