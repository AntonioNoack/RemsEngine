package me.anno.games.roadcraft

import me.anno.bullet.BulletPhysics
import me.anno.bullet.DynamicBody
import me.anno.bullet.StaticBody
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel
import me.anno.ecs.systems.Systems.registerSystem
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.clamp
import me.anno.maths.noise.PerlinNoise
import me.anno.utils.types.Floats.toIntOr
import org.joml.Vector4f
import kotlin.random.Random

/**
 * test terrain collider using a few spheres
 * */
fun main() {

    val minHeight = -25f
    val maxHeight = 25f

    val cellSize = 1f

    val perlin = PerlinNoise(
        1234L, 8, 0.5f,
        minHeight, maxHeight, Vector4f(0.025f)
    )

    val width = 100
    val length = 100

    val scene = Entity("Scene")

    val physics = BulletPhysics()
    registerSystem(physics)

    createTerrain(width, length, cellSize, perlin, scene)

    val balls = Entity("Balls", scene)
    val ballMesh = DefaultAssets.icoSphere
    val random = Random(1234)
    val dx = width * cellSize * 0.5
    val dz = length * cellSize * 0.5
    for (i in 0 until 25) {
        val x = random.nextDouble(-dx, dx)
        val z = random.nextDouble(-dz, dz)
        val y = perlin[(x + dx).toFloat(), (z + dz).toFloat()] + 1.5
        Entity("Ball[$i]", balls)
            .setPosition(x, y, z)
            .add(DynamicBody())
            .add(MeshComponent(ballMesh, DefaultAssets.goldenMaterial))
            .add(SphereCollider())
    }

    testSceneWithUI("RoadCraft", scene)
}

fun createTerrain(
    width: Int, length: Int, cellSize: Float,
    perlin: PerlinNoise, parent: Entity
): Mesh {

    val mesh = RectangleTerrainModel.generateRegularQuadHeightMesh(
        width, length,
        false, cellSize, Mesh(), { x, y ->
            perlin[x * cellSize, y * cellSize]
        })

    val heightData = ShortArray(width * length)
    val heightScale = 65535f / (perlin.max - perlin.min)
    for (y in 0 until length) {
        for (x in 0 until width) {
            val value = (perlin[x * cellSize, y * cellSize] - perlin.min) * heightScale
            heightData[x + y * width] = clamp(value.toIntOr(), 0, 65535).toShort()
        }
    }

    Entity("Terrain", parent)
        .add(MeshComponent(mesh))
        .add(StaticBody())
        .add(TerrainCollider(width, length, perlin.min, perlin.max, heightData))

    return mesh
}