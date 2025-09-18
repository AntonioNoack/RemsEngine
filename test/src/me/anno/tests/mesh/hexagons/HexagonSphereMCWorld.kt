package me.anno.tests.mesh.hexagons

import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import me.anno.maths.MinMax.max
import me.anno.maths.chunks.spherical.Hexagon
import me.anno.maths.chunks.spherical.HexagonSphere
import me.anno.maths.noise.FullNoise
import me.anno.maths.noise.PerlinNoise
import org.joml.Vector4f
import speiger.primitivecollections.LongToIntHashMap
import speiger.primitivecollections.LongToObjectHashMap
import kotlin.math.ln

class HexagonSphereMCWorld(val sphere: HexagonSphere, val save: HexMCWorldSave = HexMCWorldSave()) {

    // to do generate all easy chunks (si,sj > 0, < n-1), their data, and their mesh, on the GPU :3

    val minHeight = -16
    val maxHeight = 48
    val sy = maxHeight - minHeight
    val waterY = -minHeight

    val rnd = FullNoise(2345L)
    val perlin = PerlinNoise(1234L, 8, 0.5f, -63f, 56f, Vector4f(sphere.hexagonsPerSide / 100f))

    var depth = 3
    val base = 1f + 0.866f * sphere.len

    fun h(yi: Int): Float = h(yi.toFloat())
    fun h(yi: Float): Float {
        val y = yi + minHeight
        return Maths.pow(base, y)
    }

    fun yi(h: Float): Float {
        return ln(h) / ln(base) - minHeight
    }

    fun setBlock(
        hexagon: Hexagon, yi: Int, block: Byte,
        world: ByteArray = save[sphere, hexagon] ?: generateWorld(hexagon).first
    ) {
        if (world[yi] != block) {
            world[yi] = block
            save[sphere, hexagon] = if (world.size > sy) world.copyOfRange(0, sy) else world
        }
    }

    fun generateWorld(hex: Hexagon) = generateWorld(arrayListOf(hex), true)
    fun generateWorld(hexagons: ArrayList<Hexagon>, ensureNeighbors: Boolean): Pair<ByteArray, IndexMap> {
        if (ensureNeighbors) {
            val hexMap = LongToObjectHashMap<Hexagon>(hexagons.size)
            for (hex in hexagons) hexMap[hex.index] = hex
            sphere.ensureNeighbors(hexagons, hexMap, depth)
        }
        val idMap = LongToIntHashMap(-1, hexagons.size)
        for (index in hexagons.indices) {
            val hex = hexagons[index]
            idMap[hex.index] = index
        }
        val indexMap = IndexMap { idMap[it] }
        return generateWorld(hexagons, indexMap) to indexMap
    }

    fun generateWorld(hexagons: List<Hexagon>, mapping: IndexMap): ByteArray {

        val size = hexagons.size * sy
        val world = ByteArray(size)// Texture2D.byteArrayPool[size, false, false]
        // world.fill(air, 0, size)

        for (i in hexagons.indices) {
            // val wi = mapping.map(hexagons[i].index) * sy
            val wi = i * sy // the same, just faster
            val cen = hexagons[i].center
            val hi = clamp((perlin[cen.x, cen.y, cen.z] - minHeight).toInt(), 1, sy)
            if (hi <= waterY + 1) {
                for (y in 0 until hi - 2) world[wi + y] = stone
                for (y in max(hi - 2, 0) until hi - 1) world[wi + y] = gravel
                for (y in max(hi - 1, 0) until hi) world[wi + y] = sand
                for (y in max(hi, 0) until waterY) world[wi + y] = water
            } else {
                for (y in 0 until hi - 3) world[wi + y] = stone
                for (y in max(hi - 3, 0) until hi - 1) world[wi + y] = dirt
                for (y in max(hi - 1, 0) until hi) world[wi + y] = grass
            }
        }

        // generate random trees :3
        for (i in hexagons.indices) {
            val hex0 = hexagons[i]
            if (rnd[hex0.index.toInt()] < 0.03f) {
                // val wi = mapping.map(hex0.index) * sy
                val wi = i * sy // the same, just faster
                val cen = hex0.center
                val hi = (perlin[cen.x, cen.y, cen.z] - minHeight).toInt()
                if (hi >= 2 + waterY && hi <= sy - 6) {
                    for (y in hi + 3 until hi + 6) world[wi + y] = leaves
                    for (neighbor0 in hex0.neighbors) {
                        neighbor0 ?: continue
                        val i1 = mapping[neighbor0.index] * sy
                        if (i1 >= 0) for (y in hi + 2 until hi + 5) world[i1 + y] = leaves
                        for (neighbor1 in neighbor0.neighbors) {
                            neighbor1 ?: continue
                            val i2 = mapping[neighbor1.index] * sy
                            if (i2 >= 0) for (y in hi + 2 until hi + 4) world[i2 + y] = leaves
                        }
                    }
                    for (y in hi - 2 until hi + 3) world[wi + y] = log
                } // else too high or underwater
            }
        }

        // could be optimized
        for (i in hexagons.indices) {
            val hexagon = hexagons[i]
            val custom = save[sphere, hexagon]
            if (custom != null) {
                // val wi = mapping.map(hex0.index) * sy
                val wi = i * sy // the same, just faster
                custom.copyInto(world, wi, 0, sy)
            }
        }

        return world
    }
}