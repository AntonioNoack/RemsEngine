package me.anno.ecs.components.mesh.terrain

import me.anno.utils.types.Vectors.print
import org.joml.Matrix2f
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.roundToInt
import kotlin.math.sqrt

abstract class TerrainInit {

    abstract fun ensure(position: Vector3f, radius: Float, terrain: TriTerrain)

    class RegularTerrainInit(
        val density: Int,
        var size: Float
    ) : TerrainInit() {

        fun forwardX(x: Float, y: Float) = x + y * 0.5f
        fun forwardY(x: Float, y: Float) = y * k0

        fun backwardX(x: Float, y: Float) = x - k2 * y
        fun backwardY(x: Float, y: Float) = y * k1

        val generated = HashSet<ULong>()

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
                    val hash = x.toULong().shl(32) or z.toULong()
                    if (hash !in generated) {
                        generate(terrain, x, z)
                    }
                }
            }
        }

    }

    companion object {

        val k0 = sqrt(3f) * 0.5f
        val k1 = sqrt(3f) / 3f
        const val k2 = 1.1547005f

        @JvmStatic
        fun main(args: Array<String>) {

            val bx = 1f
            val by = sqrt(3f) * 0.5f
            val bxy = 0.5f
            val bxy2 = -0.57735026f
            val by22 = 1.1547005

            fun forwardX(x: Float, y: Float) = bx * x + bxy * y
            fun forwardY(x: Float, y: Float) = by * y

            // correct?
            fun backwardX(x: Float, y: Float) = bx * x + bxy2 * y
            fun backwardY(x: Float, y: Float) = by22 * y

            val x = 1f
            val y = 1f
            val fx = forwardX(x, y)
            val fy = forwardY(x, y)
            val bx2 = backwardX(fx, fy)
            val by2 = backwardY(fx, fy)

            val m = Matrix2f()
            m.m10(bxy)
            m.m11(by)

            println(m)

            println(m.transform(Vector2f(x, y)).print())

            m.invert()

            println("$x $y -> $fx $fy -> $bx2 $by2")

            println(m)
            println(m.m10)
            println(m.m11)

        }
    }

}