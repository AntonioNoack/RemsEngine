package me.anno.tests.maths.noise

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.noise.ErosionNoise
import me.anno.maths.noise.PerlinNoise
import me.anno.maths.noise.PhacelleHash
import me.anno.maths.noise.PhacelleNoise
import org.joml.Vector2f
import org.joml.Vector3f

/**
 * create terrain based on erosion noise,
 * as a baseline use fullnoise/perlin-noise
 * */
fun main() {

    // todo find good-looking parameters
    // todo color the terrain like in https://www.shadertoy.com/view/sf23W1
    val base = PerlinNoise(1234, 2, 0.5f, 0f, 1f)
    val noise = object : ErosionNoise(PhacelleNoise(PhacelleHash())) {
        val tmp1 = Vector2f()
        val tmp2 = Vector2f()
        override fun sampleTerrain(px: Float, py: Float, dst: Vector3f): Vector3f {
            val h = base.getSmoothGradient(px, py, tmp1, tmp2)
            return dst.set(h, tmp2.x, tmp2.y)
        }
    }
    // noise.cellSize *= 0.1f
    // noise.scale *= 0.1f

    val cellSize = 0.01f
    val mesh = RectangleTerrainModel.generateRegularQuadHeightMesh(
        512, 512,
        false, cellSize, Mesh(), { x, y ->
            noise[x * cellSize, y * cellSize]
        })
    testSceneWithUI("ErosionNoise", mesh)
}
