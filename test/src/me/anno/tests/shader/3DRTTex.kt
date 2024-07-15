package me.anno.tests.shader

import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Texture3DBTv2Material
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.texture.Texture3D
import me.anno.maths.noise.PerlinNoise
import me.anno.mesh.Shapes.flatCube
import org.joml.Vector3f

fun main() {
    val material = Texture3DBTv2Material()
    val size = material.size.set(16, 16, 16)
    val mesh = MeshComponent(flatCube.scaled(Vector3f(size).mul(0.5f)).back, material)
    testSceneWithUI("3DRTTex", mesh) {
        val data = Texture3D("density", size.x, size.y, size.z)
        val s = 0.2f
        val noise = PerlinNoise(1234L, 5, 0.5f, 0f, 1f)
        data.createRGBA8 { x, y, z ->
            if (noise[x * s, y * s, z * s] > 0.5f) 0 else -1
        }
        material.blocks = data
    }
}