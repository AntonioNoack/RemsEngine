package me.anno.tests.mesh

import com.bulletphysics.collision.shapes.CapsuleShape
import com.bulletphysics.collision.shapes.SphereShape
import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.chunks.spherical.HSPhysics
import me.anno.ecs.components.chunks.spherical.Hexagon
import me.anno.ecs.components.chunks.spherical.LargeHexagonSphere
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.graph.ui.GraphEditor.Companion.yellow
import me.anno.input.Input
import me.anno.maths.Maths.dtTo01
import me.anno.utils.Color.black
import org.joml.Vector3f
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

// test player physics on a hexagon sphere
fun main() {

    var touchesFloor = false

    val world = LargeHexagonSphere(20, 5)
    val len = world.len

    val worldCache = HashMap<Hexagon, ByteArray>()
    fun getWorld(hex: Hexagon): ByteArray {
        return worldCache.getOrPut(hex) {
            val list = arrayListOf(hex)
            world.ensureNeighbors(list, hashMapOf(hex.index to hex), 3)
            generateWorld(list, world.n)
        }
    }

    // todo using a capsule, the collisions often are neighbor triangles... why???
    val shape = SphereShape(0.25)
    val physics = HSPhysics(world, shape,
        object : HSPhysics.TriangleQuery {

            val a = Vector3f()
            val b = Vector3f()
            val c = Vector3f()

            fun y0(minY: Float) = max(floor(yi(minY, len)).toInt(), 0)
            fun y1(maxY: Float) = min(ceil(yi(maxY, len)).toInt(), maxHeight - minHeight)

            override fun run(
                hex1: Hexagon, minY: Float, maxY: Float,
                callback: (Vector3f, Vector3f, Vector3f) -> Boolean
            ) {
                val y0 = y0(minY)
                val y1 = y1(maxY)
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
                                addLayer(h(y, len), 0, -1)
                            }
                            if (y + 1 < w0.size && w0[y + 1] != air) {
                                // top
                                addLayer(h(y + 1, len), -1, 0)
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
                run(hex2, minY, maxY, callback)
                // add sides
                val y0 = y0(minY)
                val y1 = y1(maxY)
                if (y1 > y0) {
                    val w0 = getWorld(hex1)
                    val w1 = getWorld(hex2)
                    // could be made more efficient by joining sides
                    for (y in y0 until y1) {
                        if (w0[y] == air && w1[y] != air) {
                            // side
                            val c0 = hex1.corners[i]
                            val c1 = hex1.corners[(i + 1) % hex1.corners.size]
                            val h0 = h(y, len)
                            val h1 = h(y + 1, len)
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
        })

    physics.debugMeshInactiveColor = 0x777777 or black
    physics.debugMeshActiveColor = yellow or black

    // add visuals
    val scene = Entity()
    for (tri in 0 until world.triangles.size) {
        for (si in 0 until world.s) {
            for (sj in 0 until world.s - si) {
                val helper = MeshBuildHelper(world.t)
                val hexagons = world.querySubChunk(tri, si, sj)
                world.ensureNeighbors(ArrayList(hexagons), java.util.HashMap(hexagons.associateBy { it.index }), 3)
                val mesh = createMesh(hexagons, world.n, helper)
                val comp = MeshComponent(mesh.ref)
                val entity = Entity()
                entity.add(comp)
                scene.add(entity)
            }
        }
    }

    physics.gravity /= world.n

    testSceneWithUI(scene) {
        // override controller
        it.renderer.playMode = PlayMode.PLAYING // remove grid
        it.renderer.enableOrbiting = false
        it.renderer.radius = 0.1
        physics.init(Vector3f(0f, 1f, 0f))
        it.playControls = object : ControllerOnSphere(it.renderer, null) {
            override fun onUpdate() {
                super.onUpdate()
                // jumping
                if (touchesFloor && Input.wasKeyPressed(' ')) {
                    physics.addForce(up.x.toFloat(), up.y.toFloat(), up.z.toFloat(), 3f * len)
                }
                // friction
                val dtx = Engine.deltaTime * (if (touchesFloor) 5f else 1f)
                physics.velocity.mul(1f - dtTo01(dtx))
                // reset floor touching state
                touchesFloor = false
                // execute physics
                physics.update(Engine.deltaTime)
                // update visuals & control transform
                position.set(physics.currPosition)//.mul(1.0 + len * shape.halfHeight) // eye height
                onChangePosition()
            }

            override fun moveCamera(dx: Double, dy: Double, dz: Double) {
                physics.addForce(
                    (dx * right.x + dy * up.x - dz * forward.x).toFloat(),
                    (dx * right.y + dy * up.y - dz * forward.y).toFloat(),
                    (dx * right.z + dy * up.z - dz * forward.z).toFloat(),
                    200f * Engine.deltaTime
                )
            }
        }
    }

}