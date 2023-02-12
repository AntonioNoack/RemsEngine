package me.anno.ecs.components.chunks.spherical

import me.anno.ecs.components.mesh.Mesh
import me.anno.maths.Maths
import me.anno.maths.Maths.TAUf
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Vectors.normalToQuaternion
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.min

/**
 * creates a sphere out of hexagons and 12 pentagons;
 * this could be used for planet-like simulations, e.g., fluids or civilisations
 *
 * the base shape is 20-triangle sides, which meet in pentagons. Each triangle is replaced by
 * a 2d-pyramid of hexagons.
 *
 * if you need to store data on hexagons, just extend the hexagon class,
 * and initialize the hexagon array yourself. The size of the array can be determined using the
 * countHexagons() function.
 * */
object HexagonSphere {

    // todo support for sub-chunks
    // (makes indexing more complicated)

    /**
     * calculates the number of hexagons; includes pentagons
     * */
    fun calculateHexagonCount(n: Int): Int {
        // smaller than zero is illegal,
        // larger than 14652 would result arrays with more than 2^31 elements (not possible in Java)
        if (n < 0 || n > 14652) throw IllegalArgumentException()
        return 20 * ((n * (n + 1)) shr 1) + lineCount * (n + 1) + pentagonCount
    }

    /**
     * if you want to split your world into chunks for culling, you can use this function
     * it will return the ranges of indices for 21 chunks;
     * the first chunk will be visible most times, because it contains the borders and edges
     * */
    fun calculateChunkEnd(i: Int, n: Int): Int {
        val special = lineCount * (n + 1) + pentagonCount
        return special + min(i, n) * ((n * (n + 1)) shr 1)
    }

    /**
     * creates a triangulated surface for a hexagon mesh;
     * each line is currently duplicated...
     * */
    fun createLineMesh(mesh: Mesh, hexagons: Array<Hexagon>) {
        var pi = 0
        var li = 0
        val positions = mesh.positions.resize(6 * 3 * hexagons.size)
        val indices = mesh.indices.resize(6 * 2 * hexagons.size)
        mesh.drawMode = GL11C.GL_LINES
        mesh.positions = positions
        mesh.indices = indices
        mesh.normals = positions
        for (hex in hexagons) {
            var p0 = pi / 3
            var p1 = p0 + hex.corners.size - 1
            for (c in hex.corners) {
                positions[pi++] = c.x
                positions[pi++] = c.y
                positions[pi++] = c.z
                indices[li++] = p0
                indices[li++] = p1
                p1 = p0
                p0++
            }
        }
        mesh.invalidateGeometry()
    }

    /**
     * creates a triangulated surface for a hexagon mesh
     * */
    fun createFaceMesh(
        mesh: Mesh,
        hexagons: Array<Hexagon>,
        i0: Int = 0,
        i1: Int = hexagons.size,
        pentagonCount: Int = 12
    ) {
        var pi = 0
        var li = 0
        val size = i1 - i0
        val positions = mesh.positions.resize(3 * (6 * size - pentagonCount))
        val indices = mesh.indices.resize(3 * (4 * size - pentagonCount))
        mesh.drawMode = GL11C.GL_TRIANGLES
        mesh.positions = positions
        mesh.indices = indices
        mesh.normals = positions
        for (i in i0 until i1) {
            val hex = hexagons[i]
            val p0 = pi / 3
            var p1 = p0 + 1
            for (c in hex.corners) {
                positions[pi++] = c.x
                positions[pi++] = c.y
                positions[pi++] = c.z
            }
            for (j in 2 until hex.corners.size) {
                indices[li++] = p0
                indices[li++] = p1++
                indices[li++] = p1
            }
        }
        mesh.invalidateGeometry()
    }

    val nullHex = Hexagon(-1, Vector3f(), emptyArray())

    fun createHexSphere(
        n: Int, creator: HexagonCreator = HexagonCreator { a, b, c -> Hexagon(a, b, c) },
        hexagons: Array<Hexagon> = Array(calculateHexagonCount(n)) { nullHex }
    ): Array<Hexagon> {

        var hexagonCount = 0

        val ab = Vector3f()
        val ac = Vector3f()

        // could probably be calculated with some sqrt
        val len = findLength(n) / (n + 1) // +1 for extra lines at the edges

        fun connect(a: Hexagon, ai: Int, bi: Int) {
            val b = hexagons[bi]
            b.neighborIds[b.neighborIds.indexOf(-1)] = ai
            a.neighborIds[a.neighborIds.indexOf(-1)] = bi
            // replace the common points
            // use the vertex of the lower id
            val bc = b.center
            val distSq0 = a.center.distanceSquared(bc)
            var candidates = 0
            var completed = 0
            for (vai in a.corners.indices) {
                val va = a.corners[vai]
                val distSq = va.distanceSquared(bc)
                if (distSq < distSq0) {
                    candidates++
                    // it's a corner point, that is shared
                    var bestDist = distSq0
                    var bestOther: Vector3f? = null
                    for (vb in b.corners) {
                        val distSq1 = va.distanceSquared(vb)
                        if (distSq1 < bestDist) {
                            bestDist = distSq1
                            bestOther = vb
                        }
                    }
                    if (bestOther != null) {
                        // should ideally be weighted
                        bestOther.set(va)
                        a.corners[vai] = bestOther
                        completed++
                    } else throw IllegalStateException("close, but not completed")
                }
            }
        }

        fun addHex(center: Vector3f, c0: Float, c1: Float): Hexagon {
            val pos = Vector3f(center)
                .add(ab.x * c0, ab.y * c0, ab.z * c0)
                .add(ac.x * c1, ac.y * c1, ac.z * c1)
            val r = len * 0.5f
            val hi = hexagonCount++
            val hex = creator.create(hi, pos, Array(6) {
                val di = d[it]
                val d0 = di.x * r
                val d1 = di.y * r
                Vector3f(pos)
                    .add(ab.x * d0, ab.y * d0, ab.z * d0)
                    .add(ac.x * d1, ac.y * d1, ac.z * d1)
                    .normalize()
            })
            pos.normalize()
            hexagons[hi] = hex
            return hex
        }

        val lines = ArrayList<IntProgression>(lineIndices.size)
        val pointsToLines = Array(12) { ArrayList<Int>(5) }

        // create edges
        for (i in lineIndices.indices step 2) {

            val ai = lineIndices[i]
            val bi = lineIndices[i + 1]

            val a = vertices[ai]
            val b = vertices[bi]

            val i0 = hexagonCount
            val i1 = i0 + (n + 1)
            val factor = 1.070f
            val center = Vector3f(a).add(b).mul(0.5f).normalize(factor)
            ab.set(b).sub(a).normalize()
            val fx = 0.5f
            ac.set(center).cross(ab).add(ab.x * fx, ab.y * fx, ab.z * fx).normalize()
            val j0 = n * 0.5f
            val h0i = hexagonCount
            addHex(center, -j0 * len, 0f)
            pointsToLines[ai].add(h0i)
            for (j in 1 until n) {
                val hex = addHex(center, (j - j0) * len, 0f)
                connect(hex, hexagonCount - 1, hexagonCount - 2)
            }
            pointsToLines[bi].add(
                if (n > 0) {
                    val hex = addHex(center, (n - j0) * len, 0f)
                    connect(hex, hexagonCount - 1, hexagonCount - 2)
                    hexagonCount - 1
                } else h0i
            )
            lines.add(i0 until i1)
            lines.add((i1 - 1) downTo i0)

        }

        // create pentagons
        for (i in pointsToLines.indices) {
            // build coordinate system
            val point = vertices[i]
            val coords = point.normalToQuaternion(Quaternionf())
            val ax = coords.transform(Vector3f(1f, 0f, 0f))
            val az = coords.transform(Vector3f(0f, 0f, 1f))
            val hexagons1 = pointsToLines[i]
            val ax0 = ax.dot(point)
            val az0 = az.dot(point)
            // sort neighbors by their angle
            hexagons1.sortBy {
                val c = hexagons[it].center
                atan2(ax.dot(c) - ax0, az.dot(c) - az0)
            }
            // create a pentagon
            val pi = hexagonCount++
            val pentagon = creator.create(pi, point, Array(5) {
                val hex = hexagons[hexagons1[it]]
                Vector3f(point).lerp(hex.center, -0.5f)
            })
            hexagons[pi] = pentagon
            // add all connections
            var h0i = hexagons1.last()
            var h0 = hexagons[h0i]
            for (h1i in hexagons1) {
                connect(pentagon, pi, h1i)
                connect(h0, h0i, h1i)
                h0i = h1i
                h0 = hexagons[h1i]
            }
        }

        // fill triangles
        for (ix in indices.indices step 3) {

            val ai = indices[ix]
            val bi = indices[ix + 1]
            val ci = indices[ix + 2]

            val a = vertices[ai]
            val b = vertices[bi]
            val c = vertices[ci]

            val center = Vector3f(a).add(b).add(c).div(3f).normalize()
            ab.set(b).sub(a).normalize()
            ac.set(c).sub(a).normalize()

            // find all nearby lines
            fun findLine(aw: Int, bw: Int): IntProgression {
                for (li2 in lines.indices step 2) {
                    val ax = lineIndices[li2]
                    val bx = lineIndices[li2 + 1]
                    if (aw == ax && bw == bx) {
                        return lines[li2]
                    } else if (aw == bx && bw == ax) {
                        return lines[li2 + 1]
                    }
                }
                // must not happen
                throw IllegalStateException()
            }

            val abLine = findLine(ai, bi)
            val acLine = findLine(ai, ci)
            val bcLine = findLine(bi, ci)

            val i0 = (n - 1) / 3f
            var lastIdx = hexagonCount
            var prevIS = 0
            for (i in 0 until n) {
                val c0 = (i - i0) * len
                val j0 = (n - 1.5f) * 0.5f - n / 6f + 0.4f // why 0.4???
                val jn = n - i
                for (j in 0 until jn) {
                    val c1 = (j - j0) * len
                    val hi = hexagonCount
                    val hex = addHex(center, c0, c1)
                    // internal connections
                    if (j > 0) connect(hex, hi, hi - 1) // bottom left
                    if (i > 0) {
                        connect(hex, hi, lastIdx + j) // left
                        connect(hex, hi, lastIdx + j + 1) // top left
                    }
                    // external connections
                    if (j == 0) { // bottom
                        connect(hex, hi, abLine[i]) // bottom left
                        connect(hex, hi, abLine[i + 1]) // bottom right
                    }
                    if (i == 0) { // left
                        connect(hex, hi, acLine[j]) // left bottom
                        connect(hex, hi, acLine[j + 1]) // left top
                    }
                    if (j == jn - 1) { // right
                        connect(hex, hi, bcLine[j]) // right bottom
                        connect(hex, hi, bcLine[j + 1]) // right top
                    }
                }
                lastIdx += prevIS
                prevIS = jn
            }
        }

        // sort neighbors like corner points
        val a = Vector3f()
        val b = Vector3f()
        val tmp = IntArray(6)
        for (hex in hexagons) {
            val center = hex.center
            System.arraycopy(hex.neighborIds, 0, tmp, 0, 6)
            hex.neighborIds.fill(-1)
            a.set(hex.corners[0]).sub(center).normalize()
            b.set(center).cross(a).normalize()
            val ca = center.dot(a)
            val cb = center.dot(b)
            val size = hex.corners.size
            val factor = size / TAUf
            for (ni in tmp) {
                if (ni < 0) break
                val corner = hexagons[ni].center
                val angle = atan2(b.dot(corner) - cb, a.dot(corner) - ca)
                var idx0 = floor(angle * factor).toInt()
                if (idx0 < 0) idx0 += size
                if (hex.neighborIds[idx0] >= 0) throw IllegalStateException()
                hex.neighborIds[idx0] = ni
            }
        }

        return hexagons

    }

    operator fun IntProgression.get(index: Int): Int {
        val idx = first + index * step
        if ((step > 0 && idx > last)) throw IndexOutOfBoundsException()
        if ((step < 0 && idx < last)) throw IndexOutOfBoundsException()
        return idx
    }

    // scale factor, that is needed, why ever
    // values measured by eye; will be kind-of fixed
    private val lengthI = intArrayOf(
        0, 1, 2, 3, 4, 5, 6, 7, 8,
        10, 15, 20, 25, 40, 60, 90, 150, 200, 500
    )
    private val lengthF = floatArrayOf(
        2f / 3f, 0.87f, 0.98f, 1.05f, 1.097f, 1.151f, 1.158f, 1.165f, 1.185f,
        1.20f, 1.24f, 1.253f, 1.268f, 1.288f, 1.302f, 1.308f, 1.314f, 1.316f, 1.320f
    )

    fun findLength(n: Int): Float {
        if (n < 0) throw IllegalArgumentException()
        val bi = lengthI.binarySearch(n)
        if (bi >= 0) return lengthF[bi]
        val i0 = min(-bi - 1, lengthI.size - 2)
        val i1 = i0 + 1
        return Maths.mix(
            lengthF[i0],
            lengthF[i1],
            Maths.unmix(ln(lengthI[i0].toFloat()), ln(lengthI[i1].toFloat()), ln(n.toFloat()))
        )
    }

    // Icosphere without subdivisions from Blender = dodecahedron (20 triangle faces, each corner is a pentagon)
    val indices = intArrayOf(
        0, 1, 2, 1, 0, 5, 0, 2, 3, 0, 3, 4, 0, 4, 5, 1, 5, 10, 2, 1, 6, 3, 2, 7, 4, 3, 8, 5, 4, 9, 1, 10,
        6, 2, 6, 7, 3, 7, 8, 4, 8, 9, 5, 9, 10, 6, 10, 11, 7, 6, 11, 8, 7, 11, 9, 8, 11, 10, 9, 11
    )

    val vertices = run {
        val s = 0.276385f
        val t = 0.723600f
        val u = 0.447215f
        val v = 0.850640f
        val w = 0.525720f
        val x = 0.894425f
        arrayOf(
            Vector3f(0f, -1f, 0f), Vector3f(t, -u, w),
            Vector3f(-s, -u, v), Vector3f(-x, -u, 0f),
            Vector3f(-s, -u, -v), Vector3f(t, -u, -w),
            Vector3f(s, u, v), Vector3f(-t, u, w),
            Vector3f(-t, u, -w), Vector3f(s, u, -v),
            Vector3f(x, u, 0f), Vector3f(0f, 1f, 0f)
        )
    }

    val lineIndices = intArrayOf(
        0, 1, 1, 2, 0, 5, 0, 2, 2, 3, 0, 3, 3, 4, 0, 4, 4, 5, 1, 5, 5, 10, 1, 6, 2, 7, 3, 8, 4, 9, 1, 10,
        2, 6, 6, 7, 3, 7, 7, 8, 4, 8, 8, 9, 5, 9, 9, 10, 6, 10, 10, 11, 6, 11, 7, 11, 8, 11, 9, 11
    )

    val d = run {
        val s = 2f / 3f
        val f = 4f / 3f
        arrayOf(
            Vector2f(s, s),
            Vector2f(-s, f),
            Vector2f(-f, s),
            Vector2f(-s, -s),
            Vector2f(s, -f),
            Vector2f(f, -s)
        )
    }

    val pentagonCount = 12
    val lineCount = 30
    val chunkCount = 21

}