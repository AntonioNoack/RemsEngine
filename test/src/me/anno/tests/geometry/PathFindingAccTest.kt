package me.anno.tests.geometry

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.LightSpawner
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.systems.Systems
import me.anno.engine.ECSRegistry
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.pipeline.LightData
import me.anno.gpu.pipeline.Pipeline
import me.anno.input.Key
import me.anno.maths.Maths.length
import me.anno.maths.Maths.sq
import me.anno.maths.paths.PathFindingAccelerator
import me.anno.mesh.Shapes
import me.anno.tests.LOGGER
import me.anno.tests.engine.light.setTranslateScaleInverse
import me.anno.tests.utils.TestWorld
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.Clock
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.random.Random

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

    val world = TestWorld()

    // if you use raytracing, make these smaller :D
    val sx = if (rayTracing) 128 else 256
    val sz = if (rayTracing) 128 else 256
    val sy = 32
    var x0 = 0
    var y0 = 0
    var z0 = 0

    data class AccNode(val x: Int, val y: Int, val z: Int, val isProxy: Boolean) {
        override fun toString() = if (isProxy) "Proxy[$x,$y,$z]" else "Node[$x,$y,$z]"
        fun distance(other: AccNode): Double {
            return length((x - other.x).toDouble(), (y - other.y).toDouble(), (z - other.z).toDouble())
        }
    }

    fun findPoint(x: Int, z: Int): AccNode? {
        for (y in 1..sy) {
            if (world.canStand(x, y, z)) {
                return AccNode(x, y, z, false)
            }
        }
        return null
    }

    val accelerator = object : PathFindingAccelerator<ByteArray, AccNode>(useSecondaryHops) {

        override fun isProxy(node: AccNode) = node.isProxy

        override fun selectProxy(nodes: Set<AccNode>): AccNode {
            // find most central node
            val avgX = nodes.sumOf { it.x } / nodes.size
            val avgY = nodes.sumOf { it.y } / nodes.size
            val avgZ = nodes.sumOf { it.z } / nodes.size
            val mostCentralNode = nodes.minByOrNull { sq(it.x - avgX) + sq(it.y - avgY) + sq(it.z - avgZ) }!!
            // create a proxy from it
            return AccNode(mostCentralNode.x, mostCentralNode.y, mostCentralNode.z, true)
        }

        override fun getChunk(node: AccNode): ByteArray? =
            if (node.x in 0 until sx && node.y in 0 until sy && node.z in 0 until sz)
                world.getChunkAt(node.x, node.y, node.z, true)
                    ?.waitFor()
            else null

        override fun distance(start: AccNode, end: AccNode): Double {
            val diff = abs(end.x - start.x) + abs(end.z - start.z) + abs(end.y - start.y)
            return diff.toDouble()
        }

        override fun listConnections(from: AccNode, callback: (AccNode) -> Unit) {
            if (!isProxy(from) && // not a proxy
                from.x in 0 until sx && from.y in 0 until sy && from.z in 0 until sz
            ) {
                // find whether we need to go up/down for each direction
                // could be optimized
                fun check(x: Int, y: Int, z: Int) {
                    if (world.canStand(x, y, z)) {
                        callback(AccNode(x, y, z, false))
                    }
                }
                for (dy in -1..1) {
                    val x = from.x
                    val y = from.y + dy
                    val z = from.z
                    check(x, y, z + 1)
                    check(x, y, z - 1)
                    check(x + 1, y, z)
                    check(x - 1, y, z)
                }
            }
        }
    }

    val random = Random(1234L)

    var start: AccNode? = null
    var end: AccNode? = null

    fun randomizePoints() {
        start = findPoint(random.nextInt(sx), random.nextInt(sz))
        end = findPoint(random.nextInt(sx), random.nextInt(sz))
    }

    fun testPathfinding(): Pair<List<AccNode>?, List<AccNode>?>? {
        val p0 = start
        val p1 = end
        if (p0 != null && p1 != null && p0 != p1) {

            val maxDistance = accelerator.distance(p0, p1) * 1.4

            // draw path as debug cubes
            val cap = accelerator.distance(p0, p1).toInt()
            val includeStart = true
            val includeEnd = true
            val t0 = Time.nanoTime
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
            val t1 = Time.nanoTime
            // val path1 = accelerator.findShortest(p0, p1, maxDistance, cap, includeStart, includeEnd)
            val t2 = Time.nanoTime
            val path2 = accelerator.findWithoutAcceleration(p0, p1, maxDistance, cap, includeStart, includeEnd)
            val t3 = Time.nanoTime
            LOGGER.debug("Solutions? ${path0?.size}, ${path2?.size}, ${(t2 - t1).toFloat() / (t1 - t0)}x, ${(t3 - t2).toFloat() / (t1 - t0)}x faster")
            // LOGGER.debug("$path0".replace("Node", ""))
            return path0 to path2
        } else return null
    }

    while (start == null || end == null || start == end ||
        start!!.distance(end!!) < (abs(sx) + abs(sz)) / 2
    ) {
        randomizePoints()
    }
    println("finding path from $start to $end")
    testPathfinding()

    LogManager.disableLogger("BlenderControlsAddon")

    // visualize results in 3d
    testUI3("PathFindingAccTest") {

        ECSRegistry.init()

        val clock = Clock("PathFindingAccTest")
        val mesh: MeshComponentBase = if (rayTracing) {
            // slower, currently less color support, but avoids triangles ^^
            world.createRaytracingMeshV2(x0, y0, z0, sx, sy, sz)
        } else {
            world.createTriangleMesh(x0, y0, z0, sx, sy, sz)
        }
        clock.stop("Creating World Mesh")

        val scene = Entity()
        scene.add(mesh)
        Systems.world = scene

        val dx = (sx - 1) * .5
        val dy = (sy - 1) * .5
        val dz = (sz - 1) * .5
        fun setCubePosition(cube: Transform, point: AccNode, s: Double) {
            cube.localPosition = cube.localPosition.set(point.x + s - dx, point.y + s - dy, point.z + s - dz)
            cube.validate() // compute global transform
            cube.teleportUpdate()
        }

        val mat0 = Material().apply { diffuseBase.set(1.0f, 0.0f, 0.0f, 1f) }
        val mat1 = Material().apply { diffuseBase.set(0.0f, 0.3f, 0.9f, 1f) }
        val mat2 = Material().apply { emissiveBase.set(50f, 50f, 20f).mul(0.5f) }
        var count0 = 0
        var count1 = 0
        var count2 = 0
        val cubeMesh = Shapes.flatCube.scaled(0.4f).front
        val debugCubeSpawner = object : MeshSpawner() {
            override fun forEachMesh(pipeline: Pipeline?, callback: (IMesh, Material?, Transform) -> Boolean) {
                for (index in 0 until count0) {
                    if (callback(cubeMesh, mat0, getTransform(index))) return
                }
                for (index in count0 until count1) {
                    if (callback(cubeMesh, mat1, getTransform(index))) return
                }
                for (index in count1 until count2) {
                    if (callback(cubeMesh, mat2, getTransform(index))) return
                }
            }
        }
        scene.add(debugCubeSpawner)
        if (true) {
            val debugLightSpawner = object : LightSpawner() {
                private val light = PointLight().apply {
                    color.set(mat2.emissiveBase).mul(0.2f)
                }

                override fun fill(pipeline: Pipeline, instancedLights: LightData, transform: Transform) {
                    val dst = instancedLights[light]
                    val scale = 10f
                    for (index in count1 until count2) {
                        val (drawMatrix, invMatrix) = getTransform(index - count1)
                        val src = debugCubeSpawner.getTransform(index)
                        drawMatrix.set(src.getDrawMatrix()).scale(scale)
                        invMatrix.setTranslateScaleInverse(drawMatrix.m30, drawMatrix.m31, drawMatrix.m32, scale.toDouble())
                        dst.add(light, drawMatrix, invMatrix)
                    }
                }
            }
            scene.add(debugLightSpawner)
            val sun = DirectionalLight()
            val sunE = Entity(scene).add(sun)
            val sky = Skybox()
            sky.applyOntoSun(sunE, sun, 10f)
            scene.add(sky)
        }

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

        val view = SceneView(PlayMode.EDITING, style)

        fun raycastPoint(): AccNode? {
            val query = view.renderView.rayQuery()
            val hit = Raycast.raycast(scene, query)
            return if (hit) {
                val result = query.result
                // convert ws position to local space
                result.geometryNormalWS.normalize()
                val x = (result.positionWS.x + result.geometryNormalWS.x + dx).toInt()
                val z = (result.positionWS.z + result.geometryNormalWS.z + dz).toInt()
                if (x in 0 until sx && z in 0 until sz) {
                    findPoint(x, z)
                } else null
            } else null
        }

        view.editControls = object : DraggingControls(view.renderView) {

            override fun onKeyTyped(x: Float, y: Float, key: Key) {
                if (key == Key.KEY_R) {
                    randomizePoints()
                    updateCubes()
                } else super.onKeyTyped(x, y, key)
            }

            override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
                if (!long) {
                    when (button) {
                        Key.BUTTON_LEFT -> {
                            start = raycastPoint() ?: start
                            updateCubes()
                            return
                        }
                        Key.BUTTON_RIGHT -> {
                            end = raycastPoint() ?: end
                            updateCubes()
                            return
                        }
                        else -> {}
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
                        e.button == Key.BUTTON_LEFT -> {
                            start = raycastPoint() ?: start
                            updateCubes()
                            e.cancel()
                        }
                        e.button == Key.BUTTON_RIGHT -> {
                            end = raycastPoint() ?: end
                            updateCubes()
                            e.cancel()
                        }
                    }
                }
            }
        })*/

        view

    }
}