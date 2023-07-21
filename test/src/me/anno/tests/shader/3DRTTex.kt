package me.anno.tests.shader

import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.Texture3DBTv2Material
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.texture.Texture3D
import me.anno.maths.noise.PerlinNoise
import me.anno.mesh.Shapes.flatCube
import me.anno.studio.StudioBase
import org.joml.Vector3f

fun main() {

    val material = Texture3DBTv2Material()
    val size = material.size.set(16, 16, 16)
    val densities = ByteArray(size.x * size.y * size.z)
    var i = 0
    val noise = PerlinNoise(1234L, 5, 0.5f, 0f, 1f)
    val scale = 0.2f
    for (z in 0 until size.z) {
        val zs = z * scale
        for (y in 0 until size.y) {
            val ys = y * scale
            for (x in 0 until size.x) {
                val xs = x * scale
                densities[i++] = if (noise[xs, ys, zs] > 0.5f) 0 else -1
            }
        }
    }

    val mesh = MeshComponent(flatCube.scaled(Vector3f(size).mul(0.5f)).back.ref)
    mesh.materials = listOf(material.ref)

    testSceneWithUI("3DRTTex", mesh) {
        StudioBase.instance?.enableVSync = true
        val data = Texture3D("density", size.x, size.y, size.z)
        data.createMonochrome(densities)
        data.swizzleAlpha()
        material.blocks = data
    }
}