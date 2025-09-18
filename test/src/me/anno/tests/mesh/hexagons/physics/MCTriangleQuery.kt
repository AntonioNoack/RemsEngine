package me.anno.tests.mesh.hexagons.physics

import me.anno.maths.MinMax.max
import me.anno.maths.MinMax.min
import me.anno.maths.chunks.spherical.Hexagon
import me.anno.maths.chunks.spherical.HexagonTriangleQuery
import me.anno.tests.mesh.hexagons.HexagonSphereMCWorld
import me.anno.tests.mesh.hexagons.air
import org.joml.Vector3f
import kotlin.math.ceil
import kotlin.math.floor

class MCTriangleQuery(val world: HexagonSphereMCWorld) : HexagonTriangleQuery {

    val worldCache = HashMap<Hexagon, ByteArray>()
    fun getWorld(hex: Hexagon): ByteArray {
        return worldCache.getOrPut(hex) {
            world.generateWorld(hex).first
        }
    }

    var touchesFloor = false
    var considerNeighborGrounds = true

    private val a = Vector3f()
    private val b = Vector3f()
    private val c = Vector3f()

    private fun yi0(minY: Float): Int = max(floor(world.yi(minY)).toInt(), 0)
    private fun yi1(maxY: Float): Int = min(ceil(world.yi(maxY)).toInt(), world.sy)

    override fun query(
        hexagon: Hexagon, minY: Float, maxY: Float,
        callback: (Vector3f, Vector3f, Vector3f) -> Boolean
    ) {
        val y0 = yi0(minY)
        val y1 = yi1(maxY)
        if (y1 > y0) {
            val w0 = getWorld(hexagon)
            fun addLayer(fy: Float, di0: Int, di1: Int) {
                val c0 = hexagon.corners[0]
                for (j in 2 until hexagon.corners.size) {
                    c0.mul(fy, a)
                    hexagon.corners[j + di0].mul(fy, b)
                    hexagon.corners[j + di1].mul(fy, c)
                    if (callback(a, b, c) && di0 == 0) {
                        touchesFloor = true
                    }
                }
            }
            for (y in y0 until y1) {
                if (w0[y] == air) {
                    if (y == 0 || w0[y - 1] != air) {
                        // bottom
                        addLayer(world.h(y), 0, -1)
                    }
                    if (y + 1 < w0.size && w0[y + 1] != air) {
                        // top
                        addLayer(world.h(y + 1), -1, 0)
                    }
                }
            }
        }
    }

    override fun query(
        hex1: Hexagon, hex2: Hexagon, i: Int, minY: Float, maxY: Float,
        callback: (Vector3f, Vector3f, Vector3f) -> Boolean
    ) {
        // add floor for neighbors as well
        if (considerNeighborGrounds) query(hex2, minY, maxY, callback)
        // add sides
        val y0 = yi0(minY)
        val y1 = yi1(maxY)
        if (y1 > y0) {
            val w0 = getWorld(hex1)
            val w1 = getWorld(hex2)
            // could be made more efficient by joining sides
            for (y in y0 until y1) {
                if (w0[y] == air && w1[y] != air) {
                    // side
                    val c0 = hex1.corners[i]
                    val c1 = hex1.corners[(i + 1) % hex1.corners.size]
                    val h0 = world.h(y)
                    val h1 = world.h(y + 1)
                    c0.mul(h0, a)
                    c1.mul(h1, b)
                    c0.mul(h1, c)
                    callback(a, b, c)
                    c0.mul(h0, a)
                    c1.mul(h0, b)
                    c1.mul(h1, c)
                    callback(a, b, c)
                }
            }
        }
    }
}
