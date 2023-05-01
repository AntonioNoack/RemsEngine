package me.anno.tests.mesh.hexagons

import com.bulletphysics.collision.shapes.SphereShape
import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.chunks.spherical.Hexagon
import me.anno.ecs.components.chunks.spherical.HexagonSphere
import me.anno.ecs.components.bullet.HexagonSpherePhysics
import me.anno.ecs.components.chunks.spherical.HexagonTriangleQuery
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.graph.ui.GraphEditor.Companion.yellow
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.maths.Maths.dtTo01
import me.anno.utils.Color.black
import me.anno.utils.OS.documents
import me.anno.utils.files.Files.formatFileSize
import org.joml.Vector3f
import java.io.IOException
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

fun testIncompletePentagons() {
    val sphere = HexagonSphere(10, 1)
    val world = HexagonSphereMCWorld(sphere)
    val sc = sphere.findClosestChunk(Vector3f(0f, 0f, 1f)).center
    val hex = sphere.findClosestHexagon(sc)
    val (_, map) = world.generateWorld(hex)
    var pentagonsIncluded = 0
    for (pentagonId in sphere.special0 until sphere.special) {
        if (map[pentagonId] >= 0) pentagonsIncluded++
    }
}

class MCTriangleQuery(val world: HexagonSphereMCWorld) : HexagonTriangleQuery {

    val worldCache = HashMap<Hexagon, ByteArray>()
    fun getWorld(hex: Hexagon): ByteArray {
        return worldCache.getOrPut(hex) {
            world.generateWorld(hex).first
        }
    }

    var touchesFloor = false
    var considerNeighborGrounds = true

    private val a = Vector3f()
    private val b = Vector3f()
    private val c = Vector3f()

    private fun yi0(minY: Float): Int = max(floor(world.yi(minY)).toInt(), 0)
    private fun yi1(maxY: Float): Int = min(ceil(world.yi(maxY)).toInt(), world.sy)

    override fun run(
        hex1: Hexagon, minY: Float, maxY: Float,
        callback: (Vector3f, Vector3f, Vector3f) -> Boolean
    ) {
        val y0 = yi0(minY)
        val y1 = yi1(maxY)
        if (y1 > y0) {
            val w0 = getWorld(hex1)
            fun addLayer(fy: Float, di0: Int, di1: Int) {
                val c0 = hex1.corners[0]
                for (j in 2 until hex1.corners.size) {
                    c0.mul(fy, a)
                    hex1.corners[j + di0].mul(fy, b)
                    hex1.corners[j + di1].mul(fy, c)
                    if (callback(a, b, c) && di0 == 0) {
                        touchesFloor = true
                    }
                }
            }
            for (y in y0 until y1) {
                if (w0[y] == air) {
                    if (y == 0 || w0[y - 1] != air) {
                        // bottom
                        addLayer(world.h(y), 0, -1)
                    }
                    if (y + 1 < w0.size && w0[y + 1] != air) {
                        // top
                        addLayer(world.h(y + 1), -1, 0)
                    }
                }
            }
        }
    }

    override fun run(
        hex1: Hexagon, hex2: Hexagon, i: Int, minY: Float, maxY: Float,
        callback: (Vector3f, Vector3f, Vector3f) -> Boolean
    ) {
        // add floor for neighbors as well
        if (considerNeighborGrounds) run(hex2, minY, maxY, callback)
        // add sides
        val y0 = yi0(minY)
        val y1 = yi1(maxY)
        if (y1 > y0) {
            val w0 = getWorld(hex1)
            val w1 = getWorld(hex2)
            // could be made more efficient by joining sides
            for (y in y0 until y1) {
                if (w0[y] == air && w1[y] != air) {
                    // side
                    val c0 = hex1.corners[i]
                    val c1 = hex1.corners[(i + 1) % hex1.corners.size]
                    val h0 = world.h(y)
                    val h1 = world.h(y + 1)
                    c0.mul(h0, a)
                    c1.mul(h1, b)
                    c0.mul(h1, c)
                    callback(a, b, c)
                    c0.mul(h0, a)
                    c1.mul(h0, b)
                    c1.mul(h1, c)
                    callback(a, b, c)
                }
            }
        }
    }
}

// test player physics on a hexagon sphere
fun main() {

    testIncompletePentagons()

    val n = 100
    val t = 25

    val sphere = HexagonSphere(n, n / t)
    val world = HexagonSphereMCWorld(sphere)
    val len = sphere.len

    val save = world.save
    val file = documents.getChild("hexSphere.bin")
    if (file.exists) try {
        save.read(file)
    } catch (e: IOException) {
        e.printStackTrace()
    }

    // todo using a capsule, the collisions often are neighbor triangles... why???
    val shape = SphereShape(0.25)
    val triQ = MCTriangleQuery(world)
    val physics = HexagonSpherePhysics(sphere, shape, triQ)

    physics.debugMeshInactiveColor = 0x777777 or black
    physics.debugMeshActiveColor = yellow or black

    // add visuals
    val chunks = HashMap<HexagonSphere.Chunk, Mesh>()
    val scene = Entity()
    for (tri in 0 until sphere.triangles.size) {
        val triEntity = Entity()
        scene.add(triEntity)
        for (si in 0 until sphere.s) {
            for (sj in 0 until sphere.s - si) {
                val hexagons = sphere.queryChunk(tri, si, sj)
                val mesh = createMesh(hexagons, world)
                val comp = MeshComponent(mesh.ref)
                val entity = Entity()
                entity.add(comp)
                triEntity.add(entity)
                chunks[sphere.chunk(tri, si, sj)] = mesh
            }
        }
    }

    physics.gravity /= sphere.n

    testSceneWithUI(scene) {
        // override controller
        it.renderer.playMode = PlayMode.PLAYING // remove grid
        it.renderer.enableOrbiting = false
        it.renderer.radius = 0.1
        val hex0 = sphere.findClosestHexagon(Vector3f(0f, 1f, 0f))
        var yi = triQ.getWorld(hex0).indexOfFirst { block -> block == air }
        if (yi < 0) yi = world.sy
        physics.init(Vector3f(0f, world.h(yi + 1), 0f))
        it.playControls = object : ControllerOnSphere(it.renderer, null) {

            override fun onUpdate() {
                // super.onUpdate()
                // jumping
                if (triQ.touchesFloor && Input.wasKeyPressed(' ')) {
                    physics.addForce(up.x.toFloat(), up.y.toFloat(), up.z.toFloat(), 3f * len)
                }
                // friction
                val dtx = Engine.deltaTime * (if (triQ.touchesFloor) 5f else 1f)
                physics.velocity.mul(1f - dtTo01(dtx))
                // reset floor touching state
                triQ.touchesFloor = false
                // execute physics
                physics.update(Engine.deltaTime)
                // update visuals & control transform
                position.set(physics.currPosition)//.mul(1.0 + len * shape.halfHeight) // eye height
                onChangePosition()
                updateViewRotation()

                if (Input.wasKeyPressed('s') && Input.isControlDown) {
                    println("Saving")
                    file.getParent()?.mkdirs()
                    save.write(world, file)
                    println("Saved, ${file.length().formatFileSize()}")
                }

            }

            // todo show inventory
            // todo serialization
            var inventory = grass
            override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
                // resolve click
                val start = it.renderer.cameraPosition
                val dir = it.renderer.getMouseRayDirection()
                val hit = Raycast.raycast(scene, start, dir, 0.0, 0.0, 10.0, -1)
                if (hit != null) {
                    val setBlock = button.isRight
                    val testBlock = button.isMiddle
                    if (setBlock) {
                        // move hit back slightly
                        dir.mulAdd(-sphere.len * 0.05, hit.positionWS, hit.positionWS)
                    } else {
                        // move hit more into the block
                        hit.geometryNormalWS.mulAdd(-sphere.len * 0.25, hit.positionWS, hit.positionWS)
                    }
                    val hexagon = sphere.findClosestHexagon(Vector3f(hit.positionWS))
                    val h = hit.positionWS.length().toFloat()
                    val yj = world.yi(h).toInt()
                    if (yj !in 0 until world.sy) return
                    if (testBlock) {
                        inventory = triQ.getWorld(hexagon)[yj]
                    } else {
                        // set block
                        world.setBlock(hexagon, yj, if (setBlock) inventory else air)
                        // physics need to be updated as well
                        triQ.worldCache.remove(hexagon)
                        // invalidate chunk
                        val invalidChunks = HashSet<HexagonSphere.Chunk>()
                        invalidChunks.add(sphere.findChunk(hexagon))
                        // invalidate neighbor chunks
                        sphere.ensureNeighbors(hexagon)
                        for (neighbor in hexagon.neighbors) {
                            invalidChunks.add(sphere.findChunk(neighbor!!))
                        }
                        for (key in invalidChunks) {
                            val mesh = chunks[key]!!
                            val (_, tri, si, sj) = key
                            createMesh(sphere.queryChunk(tri, si, sj), world, mesh)
                        }
                    }
                }
            }

            override fun moveCamera(dx: Double, dy: Double, dz: Double) {
                val dy2 = dy * 5f
                physics.addForce(
                    (dx * right.x + dy2 * up.x - dz * forward.x).toFloat(),
                    (dx * right.y + dy2 * up.y - dz * forward.y).toFloat(),
                    (dx * right.z + dy2 * up.z - dz * forward.z).toFloat(),
                    200f * Engine.deltaTime
                )
            }
        }
    }

}