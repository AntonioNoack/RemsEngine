package me.anno.tests.mesh.hexagons

import me.anno.ecs.components.chunks.spherical.HexagonSphere

fun main() {

    // everything seems to be correct :)

    for (s in 3..4) {
        for (t in 3..4) {
            val n = s * t
            val world = HexagonSphere(n, s)
            val total = HashSet<Long>()
            for (tri in 0 until 20) {
                for (si in 0 until s) {
                    for (sj in 0 until s - si) {
                        val hexagons = world.querySubChunk(tri, si, sj)
                        total.addAll(hexagons.map { it.index })
                    }
                }
            }
            if (total.size != world.total.toInt()) throw IllegalStateException("Incorrect total!")
        }
    }

    for (s in 3..4) {
        for (t in 3..4) {
            val n = s * t
            val world = HexagonSphere(n, s)
            println("$s*$t = $n, ${world.special0},${world.special}")
            for (tri in 0 until 20) {
                for (si in 0 until s) {
                    for (sj in 0 until s - si) {
                        println("checking $s/$t/$tri/$si/$sj")
                        val hexagons = world.querySubChunk(tri, si, sj)
                        for (hex in hexagons) {
                            val test = world.findSubChunk(hex)
                            println("  #${hex.index}: ${test.tri}/${test.si}/${test.sj}")
                            if (test.tri != tri) throw IllegalStateException("Wrong tri")
                            if (test.si != si) throw IllegalStateException("Wrong si")
                            if (test.sj != sj) throw IllegalStateException("Wrong sj")
                        }
                    }
                }
            }
        }
    }
}