package me.anno.tests.mesh.hexagons

import me.anno.ecs.components.mesh.Mesh
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.clamp
import me.anno.maths.chunks.spherical.Hexagon
import me.anno.maths.noise.FullNoise
import me.anno.utils.hpc.threadLocal
import me.anno.utils.pooling.Pools
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.add
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Triangles
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

val positions = threadLocal { FloatArrayList(8192) }
val normals = threadLocal { FloatArrayList(8192) }
val colors = threadLocal { IntArrayList(8192) }

val uv6 = listOf(
    Vector2f(1f, 0.75f),
    Vector2f(0.5f, 1f),
    Vector2f(0f, 0.75f),
    Vector2f(0f, 0.25f),
    Vector2f(0.5f, 0f),
    Vector2f(1f, 0.25f)
).onEach {
    it.y += it.x * 0.5f
}

val uv5 = createArrayList(5) {
    val a = it * TAUf / 5f
    val x = cos(a) * .5f + .5f
    val y = sin(a) * .5f + .5f
    Vector2f(x, y + x * 0.5f)
}

fun needsFace(currType: Byte, otherType: Byte, transparent: Boolean?): Boolean {
    if (currType == otherType) return false
    return when (matType[currType.toInt().and(255)]) {
        full -> transparent != true && matType[otherType.toInt().and(255)] != full
        fluid, fluidLow -> transparent != false && matType[otherType.toInt().and(255)] != full
        else -> transparent != true
    }
}

val rnd = FullNoise(1234L)
fun generateMesh(
    hexagons: List<Hexagon>, size: Int,
    mapping: IndexMap, blocks: ByteArray,
    world: HexagonSphereMCWorld, mesh: Mesh,
    transparent: Boolean?
): Mesh {

    for (hex in hexagons) {
        if (hex.center.length() !in 0.99f..1.01f)
            throw IllegalStateException()
    }

    val sy = world.sy

    val positions = positions.get()
    val normals = normals.get()
    val colors = colors.get()

    val uv6 = IntArray(12)
    val uv5 = IntArray(12)

    for (i in uv6.indices) uv6[i] = (i % 6).shl(8)
    for (i in uv5.indices) uv5[i] = ((i % 5) + 6).shl(8)

    val normal = Vector3f()
    val c2v = Vector3f()
    for (i in 0 until size) {
        val hex = hexagons[i]

        // sideways
        fun addSide(k: Int, block: Byte, h0: Float, h1: Float, dy: Int) {
            val c0 = hex.corners[k]
            val c1 = hex.corners[(k + 1) % hex.corners.size]
            val color = texIdsXZ[block.toInt().and(255)]
            val v1 = (dy * 2 + 11).shl(8)
            c2v.set(c0).mul(1.1f * sign(h1 - h0))
            Triangles.subCross(c0, c1, c2v, normal).normalize()
            positions.add(c0.x * h0, c0.y * h0, c0.z * h0)
            positions.add(c1.x * h1, c1.y * h1, c1.z * h1)
            positions.add(c0.x * h1, c0.y * h1, c0.z * h1)
            colors.add(color or (11.shl(8)))
            colors.add(color or (v1 + 256))
            colors.add(color or v1)
            positions.add(c0.x * h0, c0.y * h0, c0.z * h0)
            positions.add(c1.x * h0, c1.y * h0, c1.z * h0)
            positions.add(c1.x * h1, c1.y * h1, c1.z * h1)
            colors.add(color or (11.shl(8)))
            colors.add(color or (12.shl(8)))
            colors.add(color or (v1 + 256))
            for (j in 0 until 6) {
                normals.add(normal)
            }
        }

        // val i0 = mapping[hex.index] * sy
        val i0 = i * sy // the same, just faster
        for (y in 0 until sy) {
            val here = blocks[i0 + y]
            if (here != air) {
                fun addLayer(fy: Float, di0: Int, di1: Int, color: Int) {
                    val uvi = if (hex.corners.size == 6) uv6 else uv5
                    val rotation = clamp((rnd[hex.index.toInt()] * 6).toInt(), 0, 5)
                    val c0 = hex.corners[0]
                    val uv0 = uvi[rotation]
                    for (j in 2 until hex.corners.size) {
                        positions.add(c0.x * fy, c0.y * fy, c0.z * fy)
                        normals.add(c0)
                        val c2 = hex.corners[j + di0]
                        positions.add(c2.x * fy, c2.y * fy, c2.z * fy)
                        normals.add(c2)
                        val c1 = hex.corners[j + di1]
                        positions.add(c1.x * fy, c1.y * fy, c1.z * fy)
                        normals.add(c1)
                        colors.add(color or uv0)
                        colors.add(color or uvi[j + di0 + rotation])
                        colors.add(color or uvi[j + di1 + rotation])
                    }
                }
                // add top/bottom
                if (y > 0 && needsFace(here, blocks[i0 + y - 1], transparent)) { // lowest floor is invisible
                    // add bottom
                    addLayer(world.h(y), 0, -1, texIdsNY[here.toInt().and(255)])
                    // flip normals
                    val len1 = (hex.corners.size - 2) * 3 * 3
                    for (j in normals.size - len1 until normals.size) {
                        normals[j] = -normals[j]
                    }
                }
                if (y + 1 >= sy || needsFace(here, blocks[i0 + y + 1], transparent)) {
                    // add top
                    val lower = matType[here.toInt().and(255)] == fluidLow
                    addLayer(world.h(y + if (lower) fluidLowY else 1f), -1, 0, texIdsPY[here.toInt().and(255)])
                    if (lower) {
                        // scan all directions
                        for (k in hex.neighbors.indices) {
                            val neighbor = hex.neighbors[k]!!
                            val i1 = mapping[neighbor.index] * sy + y
                            if (blocks[i1] == here && blocks[i1 + 1] == here) {
                                addSide(k, here, world.h(y + 1f), world.h(y + fluidLowY), 1)
                            }
                        }
                    }
                }
            }
        }
        for (k in hex.neighbors.indices) {
            val neighbor = hex.neighbors[k]!!
            val i1 = mapping[neighbor.index] * sy
            if (i1 < 0) throw IllegalStateException("Missing neighbor[$k]=${neighbor.index} of list[$i]=${hex.index}")

            // sideways
            fun addSide(block: Byte, y0: Int, y1: Int, atop: Byte) {
                val lower = matType[block.toInt().and(255)] == fluidLow && block != atop
                val h0 = world.h(y0)
                val h1 = world.h(y1 + (if (lower) fluidLowY - 1f else 0f))
                addSide(k, block, h0, h1, (y1 - y0) * 8 - lower.toInt())
            }

            var lastType: Byte = 0
            var lastAir = true
            var lastY0 = 0
            for (y in 0 until sy) {
                val currType = blocks[i0 + y]
                val needsFace = needsFace(currType, blocks[i1 + y], transparent)
                if (currType != lastType || needsFace != lastAir) {
                    if (lastType > 0 && lastAir) {
                        addSide(lastType, lastY0, y, currType)
                    }
                    lastY0 = y
                    lastType = currType
                    lastAir = needsFace
                }
            }
            if (lastType > 0 && lastAir) {
                addSide(lastType, lastY0, sy, air)
            }
        }
    }

    mesh.positions = positions.toFloatArray(canReturnSelf = false)
    mesh.normals = normals.toFloatArray(canReturnSelf = false)
    mesh.color0 = colors.toIntArray(canReturnSelf = false)
    mesh.materials = listOf(diffuseHexMaterial.ref)
    mesh.invalidateGeometry()

    positions.clear()
    normals.clear()
    colors.clear()

    return mesh
}

fun createMesh(
    visualList: ArrayList<Hexagon>,
    world: HexagonSphereMCWorld,
    transparent: Boolean?,
    mesh: Mesh = Mesh()
): Mesh {
    val size = visualList.size
    val (world1, indexMap) = world.generateWorld(visualList, true)
    generateMesh(visualList, size, indexMap, world1, world, mesh, transparent)
    Pools.byteArrayPool.returnBuffer(world1)
    return mesh
}
