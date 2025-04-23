package me.anno.tests.gfx.textures

import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Texture3DBTv2Material
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.texture.Texture3D
import me.anno.maths.noise.PerlinNoise
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.Color.black
import me.anno.utils.structures.lists.Lists.any2
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4f
import kotlin.random.Random

fun main() {

    val material = Texture3DBTv2Material()

    val blockTexture = Texture3D("bts", 64, 32, 80)
    val blockColors = IntArray(blockTexture.width * blockTexture.height * blockTexture.depth)

    var i = 0
    val random = Random(32145)
    val terrain = PerlinNoise(1234L, 5, 0.5f, 0f, 32f, Vector4f(0.05f))
    val trees = (0 until 10).map {
        val x = random.nextInt(5, blockTexture.width - 5)
        val z = random.nextInt(5, blockTexture.depth - 5)
        val y = terrain[x.toFloat(), z.toFloat()].toInt() + 6
        Vector3i(x, y, z)
    }

    for (z in 0 until blockTexture.depth) {
        for (y in 0 until blockTexture.height) {
            for (x in 0 until blockTexture.width) {
                val height = y - terrain[x.toFloat(), z.toFloat()].toInt()
                blockColors[i++] = when {
                    height > 0 -> {
                        if (trees.any2 { tree -> tree.distanceSquared(x, y, z) < 16 }) {
                            0x59B22D or black
                        } else if (trees.any2 { tree -> y < tree.y && x == tree.x && z == tree.z }) {
                            0x532C18 or black
                        } else 0
                    }
                    height == 0 -> 0x6CD060 or black
                    height >= -3 -> 0x66513A or black
                    else -> 0xaaaaaa or black
                }
            }
        }
    }

    addGPUTask("BlockUpload", blockColors.size) {
        blockTexture.createRGBA8(blockColors)
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
    // also check v2 of that shader

    testSceneWithUI("BlockTracing Shader", MeshComponent(mesh, material))
}