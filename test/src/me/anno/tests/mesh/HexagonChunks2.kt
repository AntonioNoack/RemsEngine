package me.anno.tests.mesh

import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.chunks.spherical.HexagonSphere.findLength
import me.anno.ecs.components.chunks.spherical.LargeHexagonSphere
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.debug.TestStudio
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.max
import kotlin.math.min

fun main() {

    var dj = 0
    var dk = 20
    val n = 100
    // todo crashes at 100k because of indices :/

    val len = findLength(n)
    val s = 1
    val entity = Entity()
    fun recalculate() {
        if (n > 150) { // if you're testing a large world, only generate a single chunk
            dj = 16
            dk = 17
        }
        val outlineOnly = n > 5000
        entity.removeAllComponents()
        val hexagons = LargeHexagonSphere(n, s)

        println("stats:")
        println(hexagons.special0)
        println(hexagons.special)
        println(hexagons.perSide)
        println(hexagons.total)

        for (ti in max(0, dj) until min(20, dk)) {
            val tri = hexagons.triangles[ti]
            val random = Random(1234L * ti)
            fun add(si: Int, sj: Int) {
                val chunk = hexagons.querySubChunk(tri.index, si, sj)
                hexagons.ensureNeighbors(ArrayList(chunk), HashMap(chunk.associateBy { it.index }), 0)
                for(hex in chunk){
                    for(ni in hex.neighborIds.indices){
                        val nei = hex.neighborIds[ni]
                        if(nei < 0) throw IllegalStateException("${hex.index} is missing neighbor #$ni, available: ${hex.neighborIds.joinToString()}")
                    }
                }
                entity.add(Entity().apply {
                    // add(MeshComponent(chunkToMesh(chunk.toTypedArray(), random.nextInt(16_777_216))))
                    add(MeshComponent(chunkToMesh2(chunk.toTypedArray(), len, random.nextInt(16_777_216))))
                })
            }
            if (outlineOnly) {
                for (si in 0 until s) {
                    add(si, 0)
                    if (si > 0) add(0, si)
                    if (si > 0 && si + 1 < s) add(si, s - 1 - si)
                }
            } else {
                for (si in 0 until s) {
                    for (sj in 0 until s - si) {
                        add(si, sj)
                    }
                }
            }
        }
    }
    recalculate()
    TestStudio.testUI2 {
        listOf(
            SceneView.testScene(entity),
            PanelListX(style).apply {
                for (it in listOf(
                    TextButton("j+", true, DefaultConfig.style)
                        .addLeftClickListener {
                            dj++
                            recalculate()
                        },
                    TextButton("j-", true, DefaultConfig.style)
                        .addLeftClickListener {
                            dj--
                            recalculate()
                        },
                    TextButton("k+", true, DefaultConfig.style)
                        .addLeftClickListener {
                            dk++
                            recalculate()
                        },
                    TextButton("k-", true, DefaultConfig.style)
                        .addLeftClickListener {
                            dk--
                            recalculate()
                        },
                )) {
                    add(it)
                }
            },
        )
    }
}