package me.anno.tests.mesh

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.chunks.spherical.HexagonSphere.findLength
import me.anno.ecs.components.chunks.spherical.LargeHexagonSphere
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.texture.Texture2D
import me.anno.image.raw.IntImage
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.sq
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.tests.cross
import me.anno.utils.OS.desktop
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.structures.maps.Maps.removeIf
import org.joml.AABBd
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.*
import kotlin.math.ceil
import kotlin.math.sqrt

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
                    val color1 = sijToColor.getOrPut(Triple(q.tri.index, q.i, q.j)) { rand.nextInt() }
                    if (q.tri.index != triIndex) w1++// println("wrong triangle for $triIndex/$si/$sj (${hex.center}, ${hex.index})")
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
                    val len = max(1, sqrt(sq(x0 - x1, y0 - y1)).toInt())
                    for (i in 0..len) {
                        val f = i.toFloat() / len
                        val lx = mix(x0, x1, f)
                        val ly = mix(y0, y1, f)
                        image.mixRGB(lx, ly, 0xff00ff, 1f)
                    }
                }
            }
        }
    }

    image.write(desktop.getChild("subchunk.png"))

    val total = sphere.total.toFloat()
    println("${ok / total}/${w1 / total}/${w2 / total} ok/wrong-tri/wrong-subchunk")

}

fun main() {

    ECSRegistry.initMeshes()

    val n = 1000
    val t = 25 // good chunk size
    val s = n / t
    val sphere = LargeHexagonSphere(n, s)
    // return testFindingSubChunks(sphere)

    val worker = ProcessingGroup("worldGen", 4)

    val len = findLength(n)

    val chunkLoader = object : Component() {
        override fun clone() = throw NotImplementedError()
        val aabb = AABBd()
        val chunks = HashMap<Triple<Int, Int, Int>, Entity>()
        override fun onUpdate(): Int {
            val scene = entity!!
            val pos = Vector3d(RenderState.cameraPosition).safeNormalize()
            if (pos.lengthSquared() < 0.5) pos.z = 1.0
            val maxDistance = len * 256
            chunks.removeIf { (_, child) ->
                val comp = child.getComponent(MeshComponent::class)
                if (comp != null) {
                    aabb.clear()
                    comp.fillSpace(scene.transform.globalTransform, aabb)
                    if (aabb.distance(pos) > maxDistance) {
                        scene.remove(child)
                        val mesh = comp.getMesh()
                        if (mesh != null) destroyMesh(mesh)
                        true
                    } else false
                } else false
            }
            // within a certain radius, request all chunks
            val minDistance = maxDistance * 0.5
            val step = len * max(t, 1) * 1.4f
            val steps = min(ceil(minDistance / step).toInt(), 3)
            val dir = Vector3f(pos)
            val dirX = dir.findSecondAxis()
            val dirZ = cross(dirX, dir)
            val dirI = Vector3f()

            fun ensure(tri: Int, si: Int, sj: Int) {
                if (chunks.size < 1000) chunks.getOrPut(Triple(tri, si, sj)) {
                    val entity = Entity()
                    worker += {
                        val helper = MeshBuildHelper(t)
                        val hexagons = sphere.querySubChunk(tri, si, sj)
                        sphere.ensureNeighbors(ArrayList(hexagons), HashMap(hexagons.associateBy { it.index }), 3)
                        val mesh = createMesh(hexagons, n, helper)
                        addEvent {
                            val comp = MeshComponent(mesh.ref)
                            entity.add(comp)
                            entity.invalidateAABBsCompletely()
                            entity.validateAABBs()
                            scene.add(entity)
                            scene.validateAABBs()
                        }
                    }
                    entity
                }
            }

            for (sz in -steps..steps) {
                val dz = sz * step
                for (sx in -steps..steps) {
                    val dx = sx * step
                    dirI.set(dir)
                        .add(dirX.x * dx, dirX.y * dx, dirX.z * dx)
                        .add(dirZ.x * dz, dirZ.y * dz, dirZ.z * dz)
                        .normalize()
                    val sub = sphere.findSubChunk(dirI)
                    val tri = sub.tri.index
                    val si = sub.i
                    val sj = sub.j
                    ensure(tri, si, sj)
                    if (si > 0) {
                        ensure(tri, si - 1, sj)
                        ensure(tri, si - 1, sj + 1)
                    }
                    if (sj > 0) {
                        ensure(tri, si, sj - 1)
                        ensure(tri, si + 1, sj - 1)
                    }
                    if (si + sj + 1 < s) {
                        ensure(tri, si + 1, sj)
                        ensure(tri, si, sj + 1)
                    }
                }
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