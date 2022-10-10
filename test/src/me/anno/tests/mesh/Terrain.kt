package me.anno.tests.mesh

import me.anno.ecs.components.mesh.ProceduralMesh.Companion.testProceduralMesh
import me.anno.ecs.components.mesh.terrain.TerrainUtils
import me.anno.ecs.components.mesh.terrain.TerrainUtils.generateRegularQuadHeightMesh
import me.anno.maths.noise.PerlinNoise

fun main() {
    testProceduralMesh { mesh ->
        val width = 256
        val height = 256
        val s = 4f / width
        val cellSize = 1f
        val heightMap = PerlinNoise(1234L, 8, 0.5f, 0f, cellSize / s)
        generateRegularQuadHeightMesh(width, height, 0, width, false, cellSize, mesh,
            TerrainUtils.HeightMap {
                heightMap[(it % width) * s, (it / width) * s]
            }) { -1 }
    }
}