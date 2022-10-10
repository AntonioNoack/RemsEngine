package me.anno.tests

import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.Texture3DBTv2Material
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.gpu.texture.Texture3D
import me.anno.maths.noise.PerlinNoise
import me.anno.mesh.Shapes.flatCube
import me.anno.ui.debug.TestStudio.Companion.testUI

fun main() {
    testUI {
        val material = Texture3DBTv2Material()
        val size = material.size.set(16, 16, 16)
        val densities = ByteArray(size.x * size.y * size.z)
        var i = 0
        val noise = PerlinNoise(1234L, 5, 0.5f, 0f, 1f)
        val scale = 0.2f
        for (z in 0 until size.z) {
            for (y in 0 until size.y) {
                for (x in 0 until size.x) {
                    densities[i++] = if (noise[x * scale, y * scale, z * scale] > 0.5f) 0 else -1
                }
            }
        }
        val data = Texture3D("density", size.x, size.y, size.z)
        data.createMonochrome(densities)
        material.blocks = data
        val mesh = MeshComponent(flatCube.front.ref)
        mesh.materials = listOf(material.ref)
        testScene(mesh)
    }
}