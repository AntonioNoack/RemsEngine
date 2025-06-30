package me.anno.tests.mesh.hexagons

import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView
import me.anno.language.translation.NameDesc
import me.anno.maths.chunks.spherical.Hexagon
import me.anno.maths.chunks.spherical.HexagonSphere
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.debug.TestEngine.Companion.testUI2
import me.anno.ui.input.IntInput
import me.anno.ui.input.NumberType
import speiger.primitivecollections.LongToObjectHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

fun main() {

    // todo bug: this has become invisible... why??

    var dj = 0
    var dk = 20
    var n = 10

    val scene = Entity()
    fun recalculate() {

        scene.removeAllComponents()

        val t = if (n % 25 == 0 && n > 25) 25
        else if (n % 10 == 0 && n > 10) 10
        else if (n % 5 == 0 && n > 5) 5
        else if (n % 3 == 0 && n > 3) 3
        else 1
        val s = max(1, n / t)
        if (n > 150) { // if you're testing a large world, only generate a single triangle
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
                val hexMap = LongToObjectHashMap<Hexagon>(chunk.size)
                for (hexagon in chunk) {
                    hexMap[hexagon.index] = hexagon
                }
                hexagons.ensureNeighbors(ArrayList(chunk), hexMap, 0)
                scene.add(MeshComponent(chunkToFaceMesh(chunk, random.nextInt(16_777_216))))
                // .add(MeshComponent(chunkToMesh2(chunk, len, random.nextInt(16_777_216))))
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
            SceneView.createSceneUI(scene),
            PanelListX(style).apply {
                for (it in listOf(
                    TextButton(NameDesc("j+"), true, DefaultConfig.style)
                        .addLeftClickListener {
                            dj++
                            recalculate()
                        },
                    TextButton(NameDesc("j-"), true, DefaultConfig.style)
                        .addLeftClickListener {
                            dj--
                            recalculate()
                        },
                    TextButton(NameDesc("k+"), true, DefaultConfig.style)
                        .addLeftClickListener {
                            dk++
                            recalculate()
                        },
                    TextButton(NameDesc("k-"), true, DefaultConfig.style)
                        .addLeftClickListener {
                            dk--
                            recalculate()
                        },
                    IntInput(NameDesc("n"), "", n, NumberType.LONG_PLUS, style)
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