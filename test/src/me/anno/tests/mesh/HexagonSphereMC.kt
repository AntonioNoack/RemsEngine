package me.anno.tests.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.chunks.spherical.Hexagon
import me.anno.ecs.components.chunks.spherical.HexagonSphere
import me.anno.ecs.components.chunks.spherical.HexagonSphere.calculateChunkEnd
import me.anno.ecs.components.chunks.spherical.HexagonSphere.chunkCount
import me.anno.ecs.components.chunks.spherical.HexagonSphere.findLength
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.noise.FullNoise
import me.anno.maths.noise.PerlinNoise
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray

// create a Minecraft world on a hex sphere :3
// todo use chunks and a visibility system for them

val air = 0
val stone = 1
val dirt = 2
val grass = 3
val log = 4
val leaves = 5

val colors0 = intArrayOf(
    0,
    0x7a726c,
    0x6b4425,
    0x71d45f,
    0x452b18,
    0x3a692c,
)

val minHeight = -32
val maxHeight = 32
val sy = maxHeight - minHeight

fun interface IndexMap {
    fun map(index: Long): Int
}

fun generateWorld(hexagons: List<Hexagon>, mapping: IndexMap, ci0: Int, ci1: Int): IntArray {

    val world = IntArray((ci1-ci0) * sy)
    val rnd = FullNoise(2345L)

    val perlin = PerlinNoise(1234L, 8, 0.5f, -30f, 30f)
    for (i in ci0 until ci1) {
        val i0 = mapping.map(hexagons[i].index) * sy
        val hex = hexagons[i].center
        val hi = (perlin[hex.x, hex.y, hex.z] - minHeight).toInt()
        for (y in 0 until hi - 3) world[i0 + y] = stone
        for (y in hi - 3 until hi - 1) world[i0 + y] = dirt
        for (y in hi - 1 until hi) world[i0 + y] = grass
    }

    // generate random trees :3
    for (i in ci0 until ci1) {
        if (rnd[i.toFloat()] < 0.03f) {
            val hex0 = hexagons[i]
            val i0 = mapping.map(hex0.index) * sy
            val cen = hex0.center
            val hi = (perlin[cen.x, cen.y, cen.z] - minHeight).toInt()
            for (y in hi + 3 until hi + 6) world[i0 + y] = leaves
            for (neighbor0 in hex0.neighbors) {
                neighbor0 ?: continue
                val i1 = mapping.map(neighbor0.index) * sy
                if (i1 >= 0) for (y in hi + 2 until hi + 5) world[i1 + y] = leaves
                for (neighbor1 in neighbor0.neighbors) {
                    neighbor1 ?: continue
                    val i2 = mapping.map(neighbor1.index) * sy
                    if (i2 >= 0) for (y in hi + 2 until hi + 4) world[i2 + y] = leaves
                }
            }
            for (y in hi until hi + 3) world[i0 + y] = log
        }
    }

    return world

}

fun generateMesh(hexagons: List<Hexagon>, mapping: IndexMap, ci0: Int, ci1: Int, world: IntArray, len: Float): Mesh {

    val positions = ExpandingFloatArray(256)
    val colors = ExpandingIntArray(256)
    for (hexId in ci0 until ci1) {
        val hex = hexagons[hexId]
        val i0 = mapping.map(hex.index) * sy
        for (y in 0 until sy) {
            val here = world[i0 + y]
            if (here != air) {
                fun addLayer(fy: Float, di0: Int, di1: Int) {
                    val c0 = hex.corners[0]
                    val color = colors0[here]
                    for (i in 2 until hex.corners.size) {
                        positions.add(c0.x * fy, c0.y * fy, c0.z * fy)
                        val c2 = hex.corners[i + di0]
                        positions.add(c2.x * fy, c2.y * fy, c2.z * fy)
                        val c1 = hex.corners[i + di1]
                        positions.add(c1.x * fy, c1.y * fy, c1.z * fy)
                        colors.add(color)
                        colors.add(color)
                        colors.add(color)
                    }
                }
                // add top/bottom
                if (y > 0 && world[i0 + y - 1] == air) { // lowest floor is invisible
                    // add bottom
                    val fy = 1f + len * (y + minHeight)
                    addLayer(fy, 0, -1)
                }
                if (y + 1 >= sy || world[i0 + y + 1] == air) {
                    // add top
                    val fy = 1f + len * (1 + y + minHeight)
                    addLayer(fy, -1, 0)
                }
            }
        }
        for (k in hex.neighbors.indices) {
            val neighbor = hex.neighbors[k] ?: break
            val i1 = mapping.map(neighbor.index) * sy

            // sideways
            fun addSide(here: Int, y0: Int, y1: Int) {
                val c0 = hex.corners[k]
                val c1 = hex.corners[(k + 1) % hex.corners.size]
                val color = colors0[here]
                val h0 = 1f + len * (y0 + minHeight)
                val h1 = 1f + len * (y1 + minHeight)
                positions.add(c0.x * h0, c0.y * h0, c0.z * h0)
                positions.add(c1.x * h1, c1.y * h1, c1.z * h1)
                positions.add(c0.x * h1, c0.y * h1, c0.z * h1)
                colors.add(color)
                colors.add(color)
                colors.add(color)
                positions.add(c0.x * h0, c0.y * h0, c0.z * h0)
                positions.add(c1.x * h0, c1.y * h0, c1.z * h0)
                positions.add(c1.x * h1, c1.y * h1, c1.z * h1)
                colors.add(color)
                colors.add(color)
                colors.add(color)
            }

            if (i1 >= 0) {
                for (y in 0 until sy) {
                    val here = world[i0 + y]
                    if (here != air && world[i1 + y] == air) {
                        // add side
                        addSide(here, y, y + 1)
                    }
                }
            } else { // unknown (chunk border)
                var lastType = 0
                var lastY0 = 0
                for (y in 0 until sy) {
                    val currType = world[i0 + y]
                    if (currType != lastType) {
                        if (lastType > 0) addSide(lastType, lastY0, y)
                        lastY0 = y
                        lastType = currType
                    }
                }
                if (lastType > 0) {
                    addSide(lastType, lastY0, sy)
                }
            }
        }
    }

    val mesh = Mesh()
    mesh.positions = positions.toFloatArray()
    mesh.color0 = colors.toIntArray()
    mesh.invalidateGeometry()

    return mesh

}

fun main() {

    val n = 200
    val hexagons = HexagonSphere.createHexSphere(n)
    val hexagonsList = hexagons.toList()

    var ci0 = 0
    val scene = Entity()
    val len = findLength(n) / (n + 1)
    for (chunkId in 0 until chunkCount) {
        val ci1 = calculateChunkEnd(chunkId, n)
        val map = IndexMap { index ->
            val idx = index.toInt()
            if (idx in ci0 until ci1) idx - ci0
            else -1
        }
        val world = generateWorld(hexagonsList, map, ci0, ci1)
        val mesh = generateMesh(hexagonsList, map, ci0, ci1, world, len)
        scene.add(Entity().apply {
            add(MeshComponent(mesh.ref))
        })
        ci0 = ci1
    }

    // todo create planetary sky box

    testSceneWithUI(scene)

}