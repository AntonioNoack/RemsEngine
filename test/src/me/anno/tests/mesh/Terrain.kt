package me.anno.tests.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.terrain.TerrainUtils.generateRegularQuadHeightMesh
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.noise.PerlinNoise

fun main() {
    val width = 256
    val height = 256
    val s = 4f / width
    val cellSize = 1f
    val heightMap = PerlinNoise(1234L, 8, 0.5f, 0f, cellSize / s)
    val mesh = Mesh()
    generateRegularQuadHeightMesh(width, height, false, cellSize, mesh, { xi, zi ->
        heightMap[xi * s, zi * s]
    })
    testSceneWithUI("Terrain", mesh)
}