package me.anno.tests.mesh

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.chunks.spherical.HexagonSphere.findLength
import me.anno.ecs.components.chunks.spherical.LargeHexagonSphere
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.image.raw.IntImage
import me.anno.tests.cross
import me.anno.utils.OS.desktop
import me.anno.utils.structures.maps.Maps.removeIf
import org.joml.AABBd
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil

// create a Minecraft world on a hex sphere :3
// todo use chunks and a visibility system for them

fun testFindingSubChunks(sphere: LargeHexagonSphere, s: Int) {

    // visualize how the engine decides subchunks, and how we do the inverse transform

    val sijToColor = HashMap<Triple<Int, Int, Int>, Int>()
    val rand = Random(1234L)

    val size = 1024
    val hs = size / 2
    val image = IntImage(size * 2, size, false)

    var ok = 0
    var w1 = 0
    var w2 = 0
    for (ti in 0 until 20) {
        println("[[[ ----------- $ti ----------- ]]]")
        for (si in 0 until s) {
            for (sj in 0 until s - si) {
                println("/// $si,$sj")
                val hexagons = sphere.querySubChunk(ti, si, sj)
                val color0 = sijToColor.getOrPut(Triple(ti, si, sj)) { rand.nextInt() }
                // .shuffled().subList(0, min(50, hexagons.size))
                for (hex in hexagons) {
                    if (hex.center.y < 0f) continue
                    val x = ((hex.center.x + 1) * hs).toInt()
                    val y = ((hex.center.z + 1) * hs).toInt()
                    image.setRGBSafely(x, y, color0)
                    val q = sphere.findSubChunk(hex.center)
                    val color1 = sijToColor.getOrPut(Triple(q.tri.index, q.i, q.j)) { rand.nextInt() }
                    image.setRGBSafely(x + size, y, color1)
                    if (q.tri.index != ti) w1++// println("wrong triangle for $triIndex/$si/$sj (${hex.center}, ${hex.index})")
                    else if (q.i != si || q.j != sj) w2++ // println("wrong subchunk for $triIndex/$si/$sj (${hex.center}, ${hex.index})")
                    else ok++
                }
            }
        }
    }

    image.write(desktop.getChild("subchunk.png"))

    println("$ok/$w1/$w2 ok/wrong-tri/wrong-subchunk")

}

fun main() {

    val n = 2000
    val s = n / 50
    val sphere = LargeHexagonSphere(n, s)
    val len = findLength(n) / (n + 1)


    // todo test all hexagons for ray checks :)

    val chunkLoader = object : Component() {
        override fun clone() = throw NotImplementedError()
        val aabb = AABBd()
        val chunks = HashMap<Triple<Int, Int, Int>, Entity>()
        override fun onUpdate(): Int {
            val scene = entity!!
            val pos = Vector3d(RenderState.cameraPosition).normalize()
            val maxDistance = len * 256
            chunks.removeIf { (sub, child) ->
                val comp = child.getComponent(MeshComponent::class)
                if (comp != null) {
                    aabb.clear()
                    comp.fillSpace(scene.transform.globalTransform, aabb)
                    if (aabb.distance(pos) > maxDistance) {
                        println("destroying $sub")
                        scene.remove(child)
                        true
                    } else false
                } else false
            }
            // within a certain radius, request all chunks
            val minDistance = maxDistance * 0.5
            val step = len * (n / s) * 0.7f // a little smaller for safety
            val steps = ceil(minDistance / step).toInt()
            val dir = Vector3f(pos)
            val dirX = dir.findSecondAxis()
            val dirZ = cross(dirX, dir)
            val dirI = Vector3f()
            for (sz in -steps..steps) {
                val dz = sz * step
                for (sx in -steps..steps) {
                    val dx = sx * step
                    dirI.set(dir)
                        .add(dirX.x * dx, dirX.y * dx, dirX.z * dx)
                        .add(dirZ.x * dz, dirZ.y * dz, dirZ.z * dz)
                        .normalize()
                    val sub = sphere.findSubChunk(dirI)
                    chunks.getOrPut(Triple(sub.tri.index, sub.i, sub.j)) {
                        val sc = sphere.querySubChunk(sub.tri.index, sub.i, sub.j)
                        sphere.ensureNeighbors(ArrayList(sc), HashMap(sc.associateBy { it.index }), 3)
                        val map = HashMap<Long, Int>(sc.size)
                        for (hex in sc) map[hex.index] = map.size
                        val map1 = IndexMap { index -> map[index] ?: -1 }
                        val world = generateWorld(sc, map1, 0, sc.size)
                        val mesh = generateMesh(sc, map1, 0, sc.size, world, len)
                        val entity = Entity().apply {
                            add(MeshComponent(mesh.ref))
                        }
                        scene.add(entity)
                        scene.invalidateAABBsCompletely()
                        println("creating ${sub.tri.index}/${sub.i}/${sub.j}")
                        entity
                    }
                    chunks
                }
            }
            return 1
        }
    }

    val scene = Entity()
    scene.add(chunkLoader)

    // todo create planetary sky box

    testSceneWithUI(scene)

}