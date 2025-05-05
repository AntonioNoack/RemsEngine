package me.anno.tests.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.terrain.HexagonTerrainModel.createHexagonTerrain
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.clamp
import me.anno.maths.chunks.hexagon.HexagonGridMaths
import me.anno.maths.noise.PerlinNoise
import me.anno.utils.Color.mixARGB
import me.anno.utils.types.Arrays.resize
import org.joml.Vector2d
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.max

val waterLevel = 0.4f
val snowLevel = 0.7f
fun getColor(y: Float): Int {
    return if (y < waterLevel) mixARGB(0x88aaff, 0x3F537E, clamp(-5f * (y - waterLevel)))
    else if (y < snowLevel) mixARGB(0x335537, 0xaaffaa, (y - waterLevel) / (snowLevel - waterLevel))
    else 0xffffff
}

fun getHexagonTilesInRadius(radius: Int): List<Vector2i> {
    val result = ArrayList<Vector2i>()
    for (z in -radius..radius) {
        for (x in -radius..radius) {
            if (HexagonGridMaths.getGridDistance(x, z) <= radius) {
                result.add(Vector2i(x, z))
            }
        }
    }
    return result
}

fun main() {
    //  add sample y-level and colors
    val n = 75
    val scene = Entity()
    val baseMesh = createHexagonTerrain(n, 0.05f, HexagonGridMaths.corners, Mesh())
    for (positions in getHexagonTilesInRadius(3)) {
        val offset = HexagonGridMaths.getCenter(positions, Vector2d())
        val mesh = baseMesh.deepClone() // indices could be copied instead of deep-copied
        val elevation = PerlinNoise(1234, 8, 0.4f, 0f, 1f, Vector4f(1f))
        val positions = mesh.positions!!
        val normals = mesh.normals!!
        val normal = Vector3f()
        val e = 1f / n
        val colors = mesh.color0.resize(normals.size / 3)
        mesh.color0 = colors
        for (i in positions.indices step 3) {
            val x = positions[i] + offset.x.toFloat()
            val z = positions[i + 2] + offset.y.toFloat()
            val h0 = elevation.getSmooth(x, z)
            val h1 = max(h0, waterLevel)
            positions[i + 1] += h1
            normal.set(
                max(elevation.getSmooth(x - e, z), waterLevel) - h1, e,
                max(elevation.getSmooth(x, z - e), waterLevel) - h1
            ).normalize()
            normal.get(normals, i)
            colors[i / 3] = getColor(h0)
        }
        Entity("Tile$positions", scene)
            .add(MeshComponent(mesh))
            .setPosition(offset.x, 0.0, offset.y)
            .setScale(0.9f)
    }

    testSceneWithUI("HexagonTerrain", scene)
}