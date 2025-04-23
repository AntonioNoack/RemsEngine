package me.anno.tests.gfx.textures

import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Texture3DBTMaterial
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.texture.Texture3D
import me.anno.maths.noise.PerlinNoise
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.Color.toVecRGB
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.max

fun main() {

    val material = Texture3DBTMaterial()
    0xff0000.toVecRGB(material.color0)
    0x000000.toVecRGB(material.color1)

    val blockTexture = Texture3D("bts", 64, 32, 80)
    val blockBytes = ByteArray(blockTexture.width * blockTexture.height * blockTexture.depth)

    var i = 0
    val terrain = PerlinNoise(1234L, 5, 0.5f, 0f, 255f, Vector4f(0.05f))
    for (z in 0 until blockTexture.depth) {
        for (y in 0 until blockTexture.height) {
            for (x in 0 until blockTexture.width) {
                val height = terrain[x.toFloat(), z.toFloat()] - y * 8f
                blockBytes[i++] = max(height.toInt(), 0).toByte()
            }
        }
    }

    addGPUTask("BlockUpload", blockBytes.size) {
        blockTexture.createMonochrome(blockBytes)
        material.blocks = blockTexture
    }

    val mesh = flatCube.scaled(
        Vector3f(
            blockTexture.width.toFloat(),
            blockTexture.height.toFloat(),
            blockTexture.depth.toFloat()
        ).mul(0.5f)
    ).back

    // check whether we can see everything properly -> yes :)
    // check whether we can be inside properly -> yes

    testSceneWithUI("BlockTracing Shader", MeshComponent(mesh, material))
}