package me.anno.tests.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.PlanarMaterial
import me.anno.ecs.components.mesh.terrain.TerrainUtils.generateRegularQuadHeightMesh
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.noise.PerlinNoise
import me.anno.utils.OS.pictures

fun main() {
    val width = 256
    val height = 256
    val s = 4f / width
    val cellSize = 1f
    val heightMap = PerlinNoise(1234L, 8, 0.5f, 0f, cellSize / s)
    val mesh = generateRegularQuadHeightMesh(width, height, false, cellSize, Mesh(), { xi, zi ->
        heightMap[xi * s, zi * s]
    })
    val material = PlanarMaterial()
    material.diffuseMap = pictures.getChild("textures/grass.jpg")
    material.scale.set(10.0)
    mesh.material = material.ref
    testSceneWithUI("Terrain", mesh)
}