package me.anno.tests.mesh

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.chunks.spherical.Hexagon
import me.anno.ecs.components.chunks.spherical.HexagonSphere.findLength
import me.anno.ecs.components.chunks.spherical.LargeHexagonSphere
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.texture.Texture2D
import me.anno.image.raw.IntImage
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.tests.cross
import me.anno.utils.OS.desktop
import me.anno.utils.hpc.ProcessingQueue
import me.anno.utils.structures.maps.Maps.removeIf
import org.joml.AABBd
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.*
import kotlin.math.ceil

// create a Minecraft world on a hex sphere :3
// use chunks and a visibility system for them

@Suppress("unused")
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
        for (si in 0 until s) {
            for (sj in 0 until s - si) {
                val hexagons = sphere.querySubChunk(ti, si, sj)
                val color0 = sijToColor.getOrPut(Triple(ti, si, sj)) { rand.nextInt() }
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
    val len = findLength(n)

    val worker = ProcessingQueue("WorldGen", 4)

    val chunkLoader = object : Component() {
        override fun clone() = throw NotImplementedError()
        val aabb = AABBd()
        val chunks = HashMap<Triple<Int, Int, Int>, Entity>()
        override fun onUpdate(): Int {
            val scene = entity!!
            val pos = Vector3d(RenderState.cameraPosition).normalize()
            val maxDistance = len * 256
            chunks.removeIf { (_, child) ->
                val comp = child.getComponent(MeshComponent::class)
                if (comp != null) {
                    aabb.clear()
                    comp.fillSpace(scene.transform.globalTransform, aabb)
                    if (aabb.distance(pos) > maxDistance) {
                        scene.remove(child)
                        comp.getMesh()?.destroy()
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
                        val entity = Entity()
                        worker += {
                            val sc = sphere.querySubChunk(sub.tri.index, sub.i, sub.j)
                            val chunkSize = sc.size
                            val map2 = HashMap<Long, Hexagon>(chunkSize)
                            for (hex in sc) map2[hex.index] = hex
                            sphere.ensureNeighbors(sc, map2, 1)
                            val indexMap = HashMap<Long, Int>(sc.size)
                            for (hex in sc) indexMap[hex.index] = indexMap.size
                            val map1i = IndexMap { index -> indexMap[index] ?: -1 }
                            val world = generateWorld(sc, map1i, 0, sc.size, n)
                            val map2i = IndexMap { index ->
                                val idx = indexMap[index]
                                if (idx == null || idx >= chunkSize) -1
                                else idx
                            }
                            val mesh = generateMesh(sc, map2i, 0, chunkSize, world, len, false)
                            Texture2D.byteArrayPool.returnBuffer(world)
                            for (hex in sc) {
                                if (hex.index >= sphere.special) {
                                    Hexagon.hexagonPool.ret(hex)
                                }
                            }
                            addEvent {
                                entity.add(MeshComponent(mesh.ref))
                                entity.invalidateAABBsCompletely()
                                scene.add(entity)
                            }
                        }
                        entity
                    }
                    chunks
                }
            }
            (scene.children as MutableList).sortBy { it.aabb.distanceSquared(pos) }
            return 1
        }
    }

    val scene = Entity()
    scene.add(chunkLoader)

    // todo create planetary sky box

    testSceneWithUI(scene)

}