package me.anno.ecs.components.mesh.terrain

import me.anno.maths.Packing.pack64
import org.joml.Vector3f
import kotlin.math.roundToInt

class RegularTerrainInit(
    val density: Int,
    var size: Float
) : TerrainInit() {

    fun forwardX(x: Float, y: Float) = x + y * 0.5f
    fun forwardY(x: Float, y: Float) = y * k0

    fun backwardX(x: Float, y: Float) = x - k2 * y
    fun backwardY(x: Float, y: Float) = y * k1

    val generated = HashSet<Long>()

    fun getCoordinates(x: Float): Int = (x / size).roundToInt()

    fun generate(terrain: TriTerrain, x: Int, z: Int) {
        // todo reuse existing vertices?
        val data = TriangleOctTree(terrain, density * density * 2)
        val pos = terrain.positions
        val idx0 = pos.size / 3
        val x0 = x * size
        val z0 = z * size
        for (j in 0..density) {
            for (i in 0..density) {
                val nx = x0 + i * size / (density - 1)
                val ny = z0 + j * size / (density - 1)
                pos.add(backwardX(nx, ny))
                pos.add(0f)
                pos.add(backwardY(nx, ny))
            }
        }
        val dst = data.indices
        var k = 0
        var l = idx0
        for (j in 0 until density) {
            for (i in 0 until density) {

                dst[k++] = l
                dst[k++] = l + 1
                dst[k++] = l + density

                dst[k++] = l + density
                dst[k++] = l + 1 + density
                dst[k++] = l

                l++
            }
        }
    }

    override fun ensure(position: Vector3f, radius: Float, terrain: TriTerrain) {
        val minX = getCoordinates(forwardX(position.x - radius, position.y - radius))
        val minZ = getCoordinates(forwardY(position.x - radius, position.y - radius))
        val maxX = getCoordinates(forwardX(position.x + radius, position.y + radius))
        val maxZ = getCoordinates(forwardY(position.x + radius, position.y + radius))
        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                val hash = pack64(x, z)
                if (hash !in generated) {
                    generate(terrain, x, z)
                }
            }
        }
    }
}