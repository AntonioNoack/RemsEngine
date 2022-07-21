package me.anno.tests.geometry

import me.anno.Engine
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.engine.ECSRegistry
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.input.MouseButton
import me.anno.maths.Maths.sq
import me.anno.maths.paths.PathFindingAccelerator
import me.anno.maths.paths.TestWorld
import me.anno.mesh.Shapes
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.LOGGER
import org.apache.logging.log4j.LogManager
import java.util.*
import kotlin.math.abs

/**
 * a test for accelerated path finding & example for a small and simple, Minecraft-like voxel world
 * */
fun main() {

    /**
     * (tested without secondary hops)
     * chunk size 8x8:
     *  - 20x-60x faster after warmup :3 (partial results)
     *  - 16-88x faster after warmup with full results...
     *          why wasn't 32x32 much better?
     *          because we still have the base overhead, and the only faster part is the rough estimation,
     *              and not needing to search in a large area for exact paths
     * chunk size 32x32:
     *  - 60x-1000x faster after warmup <3 😁 (partial results)
     *  - 20x-70x faster after warmup on full results
     * */

    // slower, just another rendering technique
    val rayTracing = false
    // when you just need the direction,
    // and the result may change over time,
    // use the partial procedure; it is way faster than a full result
    val partialResults = false

    // slightly better, ~2x more expensive
    val useSecondaryHops = false

    val world = TestWorld

    // if you use raytracing, make these smaller :D
    val sx = 512
    val sy = 32
    val sz = 512
    val x0 = 0
    val y0 = 0
    val z0 = 0

    data class Node(val x: Int, val y: Int, val z: Int, val isProxy: Boolean) {
        override fun toString() = if (isProxy) "Proxy[$x,$y,$z]" else "Node[$x,$y,$z]"
    }

    fun findPoint(x: Int, z: Int): Node? {
        for (y in 0 until 256) {
            if (TestWorld.canStand(x, y, z)) {
                return Node(x, y, z, false)
            }
        }
        return null
    }

    val accelerator = object : PathFindingAccelerator<ByteArray, Node>(useSecondaryHops) {

        override fun isProxy(node: Node) = node.isProxy

        override fun selectProxy(nodes: Set<Node>): Node {
            // find most central node
            val avgX = nodes.sumOf { it.x } / nodes.size
            val avgY = nodes.sumOf { it.y } / nodes.size
            val avgZ = nodes.sumOf { it.z } / nodes.size
            val mostCentralNode = nodes.minByOrNull { sq(it.x - avgX) + sq(it.y - avgY) + sq(it.z - avgZ) }!!
            // create a proxy from it
            return Node(mostCentralNode.x, mostCentralNode.y, mostCentralNode.z, true)
        }

        override fun getChunk(node: Node): ByteArray? =
            if (node.x in 0 until sx && node.y in 0 until sy && node.z in 0 until sz) world.getChunkAt(
                node.x,
                node.y,
                node.z,
                true
            ) else null

        override fun distance(start: Node, end: Node) =
            (abs(end.x - start.x) + abs(end.z - start.z) + abs(end.y - start.y)).toDouble()

        override fun listConnections(from: Node, callback: (Node) -> Unit) {
            if (!isProxy(from) && // not a proxy
                from.x in 0 until sx && from.y in 0 until sy && from.z in 0 until sz
            ) {
                // find whether we need to go up/down for each direction
                // could be optimized
                for (dy in -1..1) {
                    val y = from.y + dy
                    if (TestWorld.canStand(from.x, y, from.z + 1)) callback(Node(from.x, y, from.z + 1, false))
                    if (TestWorld.canStand(from.x, y, from.z - 1)) callback(Node(from.x, y, from.z - 1, false))
                    if (TestWorld.canStand(from.x + 1, y, from.z)) callback(Node(from.x + 1, y, from.z, false))
                    if (TestWorld.canStand(from.x - 1, y, from.z)) callback(Node(from.x - 1, y, from.z, false))
                }
            }// else throw IllegalArgumentException("Proxies neighbor or out-of-bounds requested, $from")
        }

    }

    val random = Random(1234L)

    var start = findPoint(random.nextInt(sx), random.nextInt(sz))
    var end = findPoint(random.nextInt(sx), random.nextInt(sz))

    fun randomizePoints() {
        start = findPoint(random.nextInt(sx), random.nextInt(sz))
        end = findPoint(random.nextInt(sx), random.nextInt(sz))
    }

    fun testPathfinding(): Pair<List<Node>?, List<Node>?>? {
        val p0 = start
        val p1 = end
        if (p0 != null && p1 != null && p0 != p1) {

            val maxDistance = accelerator.distance(p0, p1) * 1.4

            // draw path as debug cubes
            val cap = accelerator.distance(p0, p1).toInt()
            val includeStart = true
            val includeEnd = true
            val t0 = Engine.nanoTime
            val path0 = if (partialResults) {
                accelerator.find(
                    p0, p1, maxDistance, cap,
                    includeStart, includeEnd
                )
            } else {
                accelerator.findFull(
                    p0, p1, maxDistance, cap,
                    includeStart, includeEnd
                )
            }
            val t1 = Engine.nanoTime
            // val path1 = accelerator.findShortest(p0, p1, maxDistance, cap, includeStart, includeEnd)
            val t2 = Engine.nanoTime
            val path2 = accelerator.findWithoutAcceleration(p0, p1, maxDistance, cap, includeStart, includeEnd)
            val t3 = Engine.nanoTime
            LOGGER.debug("Solutions? ${path0?.size}, ${path2?.size}, ${(t2 - t1).toFloat() / (t1 - t0)}x, ${(t3 - t2).toFloat() / (t1 - t0)}x faster")
            // LOGGER.debug("$path0".replace("Node", ""))
            return path0 to path2

        } else return null
    }

    testPathfinding()

    LogManager.disableLogger("BlenderControlsAddon")

    // visualize results in 3d
    testUI {

        ECSRegistry.init()

        val mesh = if (rayTracing) {
            // slower, currently less color support, but avoids triangles ^^
            TestWorld.createRaytracingMeshV2(x0, y0, z0, sx, sy, sz)
        } else {
            TestWorld.createTriangleMesh(x0, y0, z0, sx, sy, sz)
        }

        val scene = Entity()
        scene.add(mesh)
        EditorState.prefabSource = scene.ref

        val dx = (sx - 1) * .5
        val dy = (sy - 1) * .5
        val dz = (sz - 1) * .5
        fun setCubePosition(cube: Transform, point: Node, s: Double) {
            cube.localPosition = cube.localPosition.set(point.x + s - dx, point.y + s - dy, point.z + s - dz)
            cube.validate() // compute global transform
            cube.teleportUpdate()
        }

        val mat0 = Material().apply { diffuseBase.set(1.0f, 0.0f, 0.0f, 1f) }
        val mat1 = Material().apply { diffuseBase.set(0.0f, 0.3f, 0.9f, 1f) }
        val mat2 = Material().apply { emissiveBase.set(50f, 50f, 20f) }
        var count0 = 0
        var count1 = 0
        var count2 = 0
        val cubeMesh = Shapes.flatCube.scaled(0.4f).front
        val debugCubeSpawner = object : MeshSpawner() {
            override fun clone() = throw NotImplementedError()
            override fun forEachMesh(run: (Mesh, Material?, Transform) -> Unit) {
                for (index in 0 until count0) {
                    run(cubeMesh, mat0, getTransform(index))
                }
                for (index in count0 until count1) {
                    run(cubeMesh, mat1, getTransform(index))
                }
                for (index in count1 until count2) {
                    run(cubeMesh, mat2, getTransform(index))
                }
            }
        }
        scene.add(debugCubeSpawner)

        fun updateCubes() {
            val pair = testPathfinding()
            if (pair != null) {
                val (path0, path1) = pair
                var i = 0
                debugCubeSpawner.ensureTransforms(
                    (path0?.size ?: 0) +
                            (path1?.size ?: 0) +
                            accelerator.proxyCache.size
                )
                for (point in path0 ?: emptyList()) {
                    setCubePosition(debugCubeSpawner.getTransform(i++), point, +0.1)
                }
                count0 = i
                for (point in path1 ?: emptyList()) {
                    setCubePosition(debugCubeSpawner.getTransform(i++), point, -0.1)
                }
                count1 = i
                for (point in accelerator.proxyCache.values.map { it.values.first().proxyNode }) {
                    setCubePosition(debugCubeSpawner.getTransform(i++), point, 0.0)
                }
                count2 = i
            }
        }

        updateCubes()

        val view = SceneView(EditorState, PlayMode.EDITING, style)
        view.setWeight(1f)

        fun raycastPoint(): Node? {
            val maxDistance = 1e3
            val hit = Raycast.raycast(
                scene,
                view.renderer.cameraPosition,
                view.renderer.mouseDirection,
                0.0, 0.0,
                maxDistance, -1
            )
            return if (hit != null) {
                // convert ws position to local space
                hit.normalWS.normalize()
                val x = (hit.positionWS.x + hit.normalWS.x + dx).toInt()
                val z = (hit.positionWS.z + hit.normalWS.z + dz).toInt()
                if (x in 0 until sx && z in 0 until sz) {
                    findPoint(x, z)
                } else null
            } else null
        }

        view.editControls = object : DraggingControls(view.renderer) {

            override fun onKeyTyped(x: Float, y: Float, key: Int) {
                if (key.toChar() in "rR") {
                    randomizePoints()
                    updateCubes()
                } else super.onKeyTyped(x, y, key)
            }

            override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
                if (!long) {
                    if (button.isLeft) {
                        start = raycastPoint() ?: start
                        updateCubes()
                        return
                    } else if (button.isRight) {
                        end = raycastPoint() ?: end
                        updateCubes()
                        return
                    }
                }
                super.onMouseClicked(x, y, button, long)
            }
        }

        // another way to receive input events
        /*EventBroadcasting.instance.registerListener(object : Any() {

            @EventHandler
            @Suppress("unused") // is found using reflection
            fun onPressR(e: UIEvent) {
                if (e.type == UIEventType.KEY_TYPED && view.isAnyChildInFocus) {
                    randomizePoints()
                    updateCubes()
                }
            }

            @EventHandler
            @Suppress("unused") // is found using reflection
            fun onClick(e: UIEvent) {
                if (e.type == UIEventType.MOUSE_CLICK && view.isAnyChildInFocus) {
                    when {
                        e.button.isLeft -> {
                            start = raycastPoint() ?: start
                            updateCubes()
                            e.cancel()
                        }
                        e.button.isRight -> {
                            end = raycastPoint() ?: end
                            updateCubes()
                            e.cancel()
                        }
                    }
                }
            }
        })*/

        view.setWeight(1f)

    }

}