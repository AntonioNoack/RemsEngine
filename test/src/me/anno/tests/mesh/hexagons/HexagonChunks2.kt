package me.anno.tests.mesh.hexagons

import me.anno.animation.Type
import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.chunks.spherical.HexagonSphere
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.debug.TestStudio.Companion.testUI2
import me.anno.ui.input.IntInput
import java.util.*
import kotlin.math.max
import kotlin.math.min

fun main() {

    var dj = 0
    var dk = 20
    var n = 10

    val scene = Entity()
    fun recalculate() {

        scene.removeAllChildren()

        val t = if (n % 25 == 0 && n > 25) 25
        else if (n % 10 == 0 && n > 10) 10
        else if (n % 5 == 0 && n > 5) 5
        else if (n % 3 == 0 && n > 3) 3
        else 1
        val s = max(1, n / t)
        if (n > 150) { // if you're testing a large world, only generate a single chunk
            dj = 16
            dk = 17
        }
        val outlineOnly = n > 5000
        val hexagons = HexagonSphere(n, s)

        for (ti in max(0, dj) until min(20, dk)) {
            val tri = hexagons.triangles[ti]
            val random = Random(1234L * ti)
            fun add(si: Int, sj: Int) {
                val chunk = hexagons.queryChunk(tri.index, si, sj)
                hexagons.ensureNeighbors(ArrayList(chunk), HashMap(chunk.associateBy { it.index }), 0)
                scene.add(Entity().apply {
                    add(MeshComponent(chunkToMesh(chunk, random.nextInt(16_777_216))))
                    // add(MeshComponent(chunkToMesh2(chunk, len, random.nextInt(16_777_216))))
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
    testUI2("Hexagon Chunks/2") {
        listOf(
            SceneView.testScene(scene),
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
                    IntInput("n", "", n, Type.LONG_PLUS, style)
                        .setChangeListener {
                            n = it.toInt()
                            recalculate()
                        },
                )) {
                    add(it)
                }
            },
        )
    }
}