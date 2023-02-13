package me.anno.ecs.components.chunks.spherical

import me.anno.ecs.components.chunks.spherical.HexagonSphere.indices
import me.anno.ecs.components.chunks.spherical.HexagonSphere.lineCount
import me.anno.ecs.components.chunks.spherical.HexagonSphere.lineIndices
import me.anno.ecs.components.chunks.spherical.HexagonSphere.pentagonCount
import me.anno.maths.Maths

object HexagonSpherePartitioner {

    val triangleLines = IntArray(20 * 2)
    val pentagonTris = IntArray(30)

    init {
        val triLiCounter = IntArray(20)
        triangleLines.fill(-1)
        lis@ for (li in 0 until 30) {
            val l0 = lineIndices[li * 2]
            val l1 = lineIndices[li * 2 + 1]
            for (ti in 0 until 20) {
                val ti3 = ti * 3
                val a = indices[ti3]
                val b = indices[ti3 + 1]
                val c = indices[ti3 + 2]
                if (
                    (a == l0 && b == l1) || (a == l1 && b == l0) ||
                    (a == l0 && c == l1) || (a == l1 && c == l0)
                ) {
                    // assign line to triangle
                    var di = ti * 2
                    if (triangleLines[di] >= 0) di++
                    if (triangleLines[di] >= 0) continue // there is already two lines
                    triangleLines[di] = li
                    triLiCounter[ti]++
                    continue@lis
                }
            }
            throw IllegalStateException("No triangle found for $li")
        }
        for (tri in 19 downTo 0) {
            // ab and ac shall be defined
            if (triLiCounter[tri] == 2) {
                val t3 = indices[tri * 3]
                pentagonTris[t3] = Maths.max(pentagonTris[t3], tri)
            }
        }
        var ctr = 0
        for (triIndex in 0 until 20) {
            if (pentagonTris[indices[triIndex * 3]] == triIndex)
                ctr++
        }
        if (ctr < pentagonCount) throw IllegalStateException()
    }

    /**
     * group lines onto triangle faces
     * then split each triangle face into s*(s+1)/2 sub-triangles
     * */
    fun partitionIntoSubChunks(n: Int, s: Int, hexagons: Array<Hexagon>): Array<Array<Hexagon>> {
        val special0 = lineCount * (n + 1)
        val special = special0 + pentagonCount
        val perSide = (n * (n + 1)) shr 1
        val t = n / s
        if (t * s != n) throw IllegalArgumentException()
        val groups = ArrayList<Array<Hexagon>>(20 * s * s)
        // the owner of each line is its first vertex
        val group = ArrayList<Hexagon>()
        for (triIndex in 0 until 20) {

            val idx0 = special + perSide * triIndex
            fun triSideIndex(i: Int, j: Int): Int {
                if (i !in 0 until n || j !in 0 until n - i)
                    throw IndexOutOfBoundsException("$i,$j is out of bounds for $n")
                return idx0 + j + n * i - (i * (i - 1)).shr(1)
            }

            var ab: IntProgression? = null
            var ac: IntProgression? = null
            val ti3 = triIndex * 3
            val a = indices[ti3]
            val b = indices[ti3 + 1]
            for (i in 0 until 2) {
                val li = triangleLines[triIndex * 2 + i]
                if (li < 0) continue
                val l0 = lineIndices[li * 2]
                val l1 = lineIndices[li * 2 + 1]
                val i0 = li * (n + 1)
                val i1 = i0 + n
                if (l0 == a) {
                    if (l1 == b) ab = i0..i1
                    else ac = i0..i1
                } else {
                    // reverse
                    if (l0 == b) ab = i1 downTo i0
                    else ac = i1 downTo i0
                }
            }

            // add a pentagon to the left corner
            if (pentagonTris[a] == triIndex) {
                group.add(hexagons[special0 + a])
            }

            // reorder all
            for (si in 0 until s) {
                if (ab != null) {
                    // add all bottom ones
                    for (i in si * t until (si + 1) * t) {
                        group.add(hexagons[ab[i]])
                    }
                    if (si == s - 1) { // last one
                        group.add(hexagons[ab[n]])
                    }
                }
                for (sj in 0 until s - si) {
                    if (si == 0 && sj == s - 1) continue // top
                    when (si) {
                        0 -> {
                            // left
                            val j0 = t * sj
                            for (tj in 0 until t) {
                                val tl = t - (tj + 1).shr(1)
                                for (ti in 0 until tl) {
                                    group.add(hexagons[triSideIndex(ti, tj + j0)])
                                }
                            }
                            if (ac != null) {
                                // add left part
                                for (j in j0 until j0 + t) {
                                    group.add(hexagons[ac[j]])
                                }
                            }
                        }
                        s - sj - 1 -> {
                            // right
                            val j0 = t * sj
                            for (tj in 0 until t) {
                                val tl = t - tj.shr(1)
                                val jj = tj + j0
                                for (ti in n - jj - tl until n - jj) {
                                    group.add(hexagons[triSideIndex(ti, jj)])
                                }
                            }
                        }
                        else -> {
                            // center
                            val j0 = t * sj
                            for (tj in 0 until t) {
                                val i0 = si * t - (tj + 1).shr(1)
                                val jj = tj + j0
                                for (ti in i0 until i0 + t) {
                                    group.add(hexagons[triSideIndex(ti, jj)])
                                }
                            }
                        }
                    }
                    groups.add(group.toTypedArray())
                    group.clear()
                }
            }
            // add top region
            val j0 = n - t
            if (ac != null) {
                // add left part
                // one extra for tip
                for (tj in j0..n) {
                    group.add(hexagons[ac[tj]])
                }
            }
            for (ti in 0 until t) {
                for (tj in j0 until n - ti) {
                    group.add(hexagons[triSideIndex(ti, tj)])
                }
            }
            groups.add(group.toTypedArray())
            group.clear()
        }

        return groups.toTypedArray()
    }

    operator fun IntProgression.get(index: Int): Int {
        val idx = first + index * step
        if ((step > 0 && idx > last)) throw IndexOutOfBoundsException()
        if ((step < 0 && idx < last)) throw IndexOutOfBoundsException()
        return idx
    }

}