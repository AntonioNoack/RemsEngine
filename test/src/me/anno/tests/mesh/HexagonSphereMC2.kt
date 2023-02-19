package me.anno.tests.mesh

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.chunks.spherical.HexagonSphere.findLength
import me.anno.ecs.components.chunks.spherical.LargeHexagonSphere
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFX
import me.anno.image.raw.IntImage
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.studio.StudioBase
import me.anno.utils.OS.desktop
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.structures.maps.Maps.removeIf
import org.joml.AABBd
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.*
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// create a Minecraft world on a hex sphere :3
// use chunks and a visibility system for them

@Suppress("unused")
fun testFindingSubChunks(sphere: LargeHexagonSphere) {

    // visualize how the engine decides subchunks, and how we do the inverse transform

    val sijToColor = HashMap<Triple<Int, Int, Int>, Int>()
    val rand = Random(1234L)

    val size = 2048
    val hs = size / 2
    val image = IntImage(size * 3, size, false)

    var ok = 0L
    var w1 = 0L
    var w2 = 0L
    for (triIndex in 0 until 20) {
        for (si in 0 until sphere.s) {
            for (sj in 0 until sphere.s - si) {
                val hexagons = sphere.querySubChunk(triIndex, si, sj)
                val color0 = sijToColor.getOrPut(Triple(triIndex, si, sj)) { rand.nextInt() }
                for (hex in hexagons) {
                    val q = sphere.findSubChunk(hex.center)
                    val color1 = sijToColor.getOrPut(Triple(q.tri, q.i, q.j)) { rand.nextInt() }
                    if (q.tri != triIndex) w1++// println("wrong triangle for $triIndex/$si/$sj (${hex.center}, ${hex.index})")
                    else if (q.i != si || q.j != sj) w2++ // println("wrong subchunk for $triIndex/$si/$sj (${hex.center}, ${hex.index})")
                    else ok++
                    if (hex.center.y > 0f) {
                        val x = ((hex.center.x + 1) * hs)
                        val y = ((hex.center.z + 1) * hs)
                        image.mixRGB(x, y, color0, 1f)
                        image.mixRGB(x + size, y, color1, 1f)
                        if (color0 != color1) image.mixRGB(x + size * 2, y, color1, 1f)
                    }
                }
                val cx = hexagons.map { it.center.x }.average().toFloat()
                val cy = hexagons.map { it.center.y }.average().toFloat()
                val cz = hexagons.map { it.center.z }.average().toFloat()
                val center = sphere.triangles[triIndex].getSubChunkCenter(si, sj)
                if (cy > 0f && center.y > 0f) {
                    val x0 = ((cx + 1) * hs)
                    val y0 = ((cz + 1) * hs)
                    val x1 = ((center.x + 1) * hs)
                    val y1 = ((center.z + 1) * hs)
                    image.drawLine(x0, y0, x1, y1, 0xff00ff, 1f)
                }
            }
        }
    }

    image.write(desktop.getChild("subchunk.png"))

    val total = sphere.total.toFloat()
    println("${ok / total}/${w1 / total}/${w2 / total} ok/wrong-tri/wrong-subchunk")

}

@Suppress("unused")
fun testFindingSubChunks2(sphere: LargeHexagonSphere) {

    // visualize how the engine decides subchunks, and how we do the inverse transform

    val maxDistance = sphere.n / 2 * sphere.len

    val size = 2048
    val hs = size / 2f
    val image = IntImage(size, size, false)

    val queried = HashSet<Triple<Int, Int, Int>>()
    sphere.querySubChunks(Vector3f(0f, 1f, 0f), maxDistance) { sc ->
        queried.add(Triple(sc.tri, sc.i, sc.j))
        false
    }

    val rand = Random(1234L)
    for (tri in 0 until 20) {
        for (si in 0 until sphere.s) {
            for (sj in 0 until sphere.s - si) {
                val hexagons = sphere.querySubChunk(tri, si, sj)
                val color0 = rand.nextInt().and(0x777777) or (if (Triple(tri, si, sj) in queried) 0x808080 else 0)
                for (hex in hexagons) {
                    if (hex.center.y > 0f) {
                        val x = ((hex.center.x + 1) * hs)
                        val y = ((hex.center.z + 1) * hs)
                        image.mixRGB(x, y, color0, 1f)
                    }
                }
            }
        }
    }

    // draw circle :)
    val r = sin(maxDistance) * hs
    val rs = (r * TAUf).roundToInt()
    var x0 = hs + r
    var y0 = hs
    for (i in 1..rs) {
        val a = i * TAUf / rs
        val x1 = hs + cos(a) * r
        val y1 = hs + sin(a) * r
        image.drawLine(x0, y0, x1, y1, -1)
        x0 = x1
        y0 = y1
    }

    image.write(desktop.getChild("subchunk.png"))

}

fun main() {

    // todo sizes like 10k no longer work properly, and I suspect findClosestSubChunk() is the culprit
    val n = 1000
    val t = 25 // good chunk size
    val s = n / t
    val sphere = LargeHexagonSphere(n, s)
    // return testFindingSubChunks(sphere)
    // return testFindingSubChunks2(sphere)

    ECSRegistry.initMeshes()

    val worker = ProcessingGroup("worldGen", 4)

    val len = findLength(n)
    val dir = Vector3f()

    val chunkLoader = object : Component() {
        override fun clone() = throw NotImplementedError()
        val aabb = AABBd()
        val chunks = HashMap<LargeHexagonSphere.SubChunk, Entity>()
        val requests = ArrayList<LargeHexagonSphere.SubChunk>()
        override fun onUpdate(): Int {

            val scene = entity!!
            val pos = Vector3d(RenderState.cameraPosition).safeNormalize()
            if (pos.lengthSquared() < 0.5) pos.z = 1.0
            val maxAngleDifference = len * 256
            chunks.removeIf { (_, child) ->
                val comp = child.getComponent(MeshComponent::class)
                if (comp != null) {
                    aabb.clear()
                    comp.fillSpace(scene.transform.globalTransform, aabb)
                    if (aabb.distance(pos) > maxAngleDifference) {
                        scene.remove(child)
                        val mesh = comp.getMesh()
                        if (mesh != null) destroyMesh(mesh)
                        true
                    } else false
                } else false
            }

            // within a certain radius, request all chunks

            dir.set(pos)

            // todo sort requests by distance
            if (chunks.size < 5000) {
                sphere.querySubChunks(dir, maxAngleDifference) { sc ->
                    if (sc !in chunks) requests.add(sc)
                    false
                }
                requests.sortByDescending { it.center.angleCos(dir) }
                for (i in 0 until min(5000 - chunks.size, min(requests.size, 16 - max(worker.remaining, GFX.gpuTasks.size)))) {
                    val sc = requests[i]
                    val entity = Entity()
                    worker += {
                        // check if the request is still valid
                        val helper = MeshBuildHelper(t)
                        val hexagons = sphere.querySubChunk(sc)
                        sphere.ensureNeighbors(ArrayList(hexagons), HashMap(hexagons.associateBy { it.index }), 3)
                        val mesh = createMesh(hexagons, n, helper)
                        GFX.addGPUTask("chunk", s) {
                            mesh.ensureBuffer()
                            StudioBase.addEvent {
                                val comp = MeshComponent(mesh.ref)
                                entity.add(comp)
                                entity.invalidateAABBsCompletely()
                                entity.validateAABBs()
                                scene.add(entity)
                                scene.validateAABBs()
                            }
                        }
                    }
                    chunks[sc] = entity
                }
                requests.clear()
            }

            (scene.children as MutableList).sortBy { it.aabb.distanceSquared(pos) }
            return 1
        }
    }

    val scene = Entity()
    scene.add(chunkLoader)

    // todo create planetary sky box

    testSceneWithUI(scene) {
        it.renderer.position.set(0.0, 1.0, 0.0)
        it.renderer.radius = 10.0 * len
        it.renderer.near = it.renderer.radius * 0.01
        it.renderer.far = it.renderer.radius * 1e5
        it.renderer.updateEditorCameraTransform()
    }

}