package me.anno.tests.shader

import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.Texture3DBTv2Material
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.texture.Texture3D
import me.anno.maths.noise.PerlinNoise
import me.anno.mesh.Shapes.flatCube
import me.anno.engine.EngineBase
import org.joml.Vector3f
import org.joml.Vector4f

fun main() {

    val material = Texture3DBTv2Material()
    val size = material.size.set(16, 16, 16)
    val densities = ByteArray(size.x * size.y * size.z)
    var i = 0
    val noise = PerlinNoise(1234L, 5, 0.5f, 0f, 1f, Vector4f(0.2f))
    for (z in 0 until size.z) {
        for (y in 0 until size.y) {
            for (x in 0 until size.x) {
                densities[i++] = if (noise[x.toFloat(), y.toFloat(), z.toFloat()] > 0.5f) 0 else -1
            }
        }
    }

    val mesh = MeshComponent(flatCube.scaled(Vector3f(size).mul(0.5f)).back.ref)
    mesh.isInstanced = false // instancing isn't supported, because we don't have invLocalTransform there yet
    mesh.materials = listOf(material.ref)

    testSceneWithUI("3DRTTex", mesh) {
        EngineBase.instance?.enableVSync = true
        val data = Texture3D("density", size.x, size.y, size.z)
        data.createMonochrome(densities)
        data.swizzleAlpha()
        material.blocks = data
    }
}