package me.anno.maths.paths

import me.anno.Engine
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.chunks.cartesian.ByteArrayChunkSystem
import me.anno.ecs.components.mesh.ManualProceduralMesh
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.shaders.CuboidMesh
import me.anno.ecs.components.shaders.Texture3DBTMaterial
import me.anno.engine.ECSRegistry
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.texture.Texture3D
import me.anno.input.Input
import me.anno.maths.Maths.sq
import me.anno.maths.noise.FullNoise
import me.anno.maths.noise.PerlinNoise
import me.anno.mesh.Shapes
import me.anno.mesh.vox.model.VoxelModel
import me.anno.ui.base.SpyPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.Color.toVecRGB
import me.anno.utils.LOGGER
import me.anno.utils.structures.maps.Maps.flatten
import org.apache.logging.log4j.LogManager
import java.util.*
import kotlin.math.abs

/**
 * a test for accelerated path finding & example for a small and simple, Minecraft-like voxel world
 * */
fun main() {

    /**
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

    // slower, just another technique
    val rayTracing = false
    // when you just need the direction,
    // and the result may change over time,
    // use the partial procedure; it is way faster than a full result
    val partialResults = false

    // generate a voxel world
    val air = 0.toByte()
    val dirt = 1.toByte()
    val grass = 2.toByte()
    // raytracing currently only supports two colors with my default shader
    val log = if (rayTracing) dirt else 3.toByte()
    val leaves = if (rayTracing) grass else 4.toByte()

    val dirtColor = 0x684530
    val grassColor = 0x2f8d59
    val colors = mapOf(
        dirt to dirtColor, grass to grassColor, log to 0x463125, leaves to 0x067e3c
    )

    val sx = 256 * 4
    val sy = 32
    val sz = 256 * 4

    data class Node(val x: Int, val y: Int, val z: Int, val isProxy: Boolean) {
        override fun toString() = if (isProxy) "Proxy[$x,$y,$z]" else "Node[$x,$y,$z]"
    }

    // this world surely could be useful in a few other instances as well 😄
    val treeRandom = FullNoise(1234L)
    val noise = PerlinNoise(1234L, 3, 0.5f, 0f, 1f)
    val world = object : ByteArrayChunkSystem(5, 5, 5, defaultElement = 0) {

        val scale = 0.05f
        val scaleY = scale * 0.5f
        val treeChance = 0.013f

        fun isSolid(x: Int, y: Int, z: Int) = (y == 0) || noise[x * scale, y * scaleY, z * scale] - y * scaleY > 0.1f

        fun plantTree(chunk: ByteArray, lx: Int, ly: Int, lz: Int) {
            // tree crone
            for (j in -2..2) {
                for (k in -2..2) {
                    val sq = j * j + k * k
                    if (sq < 8) {
                        chunk[getIndex(lx + j, ly + 3, lz + k)] = leaves
                        chunk[getIndex(lx + j, ly + 4, lz + k)] = leaves
                        if (sq < 4) {
                            chunk[getIndex(lx + j, ly + 5, lz + k)] = leaves
                            if (sq < 2) chunk[getIndex(lx + j, ly + 6, lz + k)] = leaves
                        }
                    }
                }
            }
            // stem
            for (i in 0 until 5) chunk[getIndex(lx, ly + i, lz)] = log
        }

        override fun generateChunk(chunkX: Int, chunkY: Int, chunkZ: Int, chunk: ByteArray) {
            val x0 = chunkX shl bitsX
            val y0 = chunkY shl bitsY
            val z0 = chunkZ shl bitsZ
            for (x in x0 until x0 + sizeX) {
                for (z in z0 until z0 + sizeZ) {
                    var index = getIndex(x - x0, sizeY - 1, z - z0)
                    var aboveIsSolid = isSolid(x, y0 + sizeY, z)
                    for (y in y0 + sizeY - 1 downTo y0) {
                        val isSolid = isSolid(x, y, z)
                        val block = if (isSolid) if (aboveIsSolid) dirt else grass else air
                        if (block != air) chunk[index] = block
                        aboveIsSolid = isSolid
                        if (block == grass) {
                            // with a chance, place a tree here
                            // our cheap method only works distanced from chunk borders
                            if (x - x0 in 2 until sizeX - 2 && y - y0 in 1 until sizeY - 7 && z - z0 in 2 until sizeZ - 2 && treeRandom.getValue(
                                    x.toFloat(),
                                    y.toFloat(),
                                    z.toFloat()
                                ) < treeChance
                            ) plantTree(chunk, x - x0, y - y0, z - z0)
                        }
                        index -= dy
                    }
                }
            }
        }

    }

    fun isAir(x: Int, y: Int, z: Int) = world.getElementAt(x, y, z) == air
    fun isSolid(x: Int, y: Int, z: Int) = world.getElementAt(x, y, z) != air
    fun canStand(x: Int, y: Int, z: Int) = isAir(x, y, z) && isAir(x, y + 1, z) && isSolid(x, y - 1, z)

    fun findPoint(x: Int, z: Int): Node? {
        for (y in 0 until 256) {
            if (canStand(x, y, z)) {
                return Node(x, y, z, false)
            }
        }
        return null
    }

    val accelerator = object : PathFindingAccelerator<ByteArray, Node>() {

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
                    if (canStand(from.x, y, from.z + 1)) callback(Node(from.x, y, from.z + 1, false))
                    if (canStand(from.x, y, from.z - 1)) callback(Node(from.x, y, from.z - 1, false))
                    if (canStand(from.x + 1, y, from.z)) callback(Node(from.x + 1, y, from.z, false))
                    if (canStand(from.x - 1, y, from.z)) callback(Node(from.x - 1, y, from.z, false))
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

            // slower, but avoids triangles ^^

            val texture = Texture3D("blocks", sx, sy, sz)
            texture.createMonochrome { x, y, z -> world.getElementAt(x, y, z) }
            texture.clamping(false)

            val material = Texture3DBTMaterial()
            material.color0 = dirtColor.toVecRGB()
            material.color1 = grassColor.toVecRGB()
            material.limitColors(2)
            material.blocks = texture

            val mesh = CuboidMesh()
            mesh.size.set(-sx * 1f, -sy * 1f, -sz * 1f)
            mesh.materials = listOf(material.ref)
            mesh

        } else {

            val mesh = ManualProceduralMesh()
            val palette = colors.flatten(0) { blockType -> blockType.toInt() }
            object : VoxelModel(sx, sy, sz) {
                override fun getBlock(x: Int, y: Int, z: Int) = world.getElementAt(x, y, z).toInt()
            }.createMesh(palette, { _, _, _ -> false }, mesh.mesh2)
            mesh

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

        val debugCubes = ArrayList<Transform>()
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
                    run(cubeMesh, mat0, debugCubes[index])
                }
                for (index in count0 until count1) {
                    run(cubeMesh, mat1, debugCubes[index])
                }
                for (index in count1 until count2) {
                    run(cubeMesh, mat2, debugCubes[index])
                }
            }
        }
        scene.add(debugCubeSpawner)

        fun updateCubes() {
            val pair = testPathfinding()
            if (pair != null) {
                val (path0, path1) = pair
                var i = 0
                for (point in path0 ?: emptyList()) {
                    if (i >= debugCubes.size) debugCubes.add(Transform())
                    setCubePosition(debugCubes[i++], point, +0.1)
                }
                count0 = i
                for (point in path1 ?: emptyList()) {
                    if (i >= debugCubes.size) debugCubes.add(Transform())
                    setCubePosition(debugCubes[i++], point, -0.1)
                }
                count1 = i
                for (point in accelerator.proxyCache.values.map { it.values.first().proxyNode }) {
                    if (i >= debugCubes.size) debugCubes.add(Transform())
                    setCubePosition(debugCubes[i++], point, 0.0)
                }
                count2 = i
            }
        }

        updateCubes()

        val list = PanelListY(style)
        list.add(SpyPanel {
            fun raycastPoint(): Node? {
                val maxDistance = 1e3
                val hit = Raycast.raycast(
                    scene, RenderView.camPosition, RenderView.camDirection, 0.0, 0.0, maxDistance, -1
                )
                return if (hit != null) {
                    // convert ws position to local space
                    val x = (hit.positionWS.x + dx).toInt()
                    val z = (hit.positionWS.z + dz).toInt()
                    if (x in 0 until sx && z in 0 until sz) {
                        findPoint(x, z)
                    } else null
                } else null
            }
            // todo intercept left and right click somehow
            /*if (Input.isLeftDown) {
                start = raycastPoint() ?: start
                updateCubes()
            }
            if (Input.isRightDown) {
                end = raycastPoint() ?: end
                updateCubes()
            }*/
            if (Input.wasKeyPressed('r')) {
                randomizePoints()
                updateCubes()
            }
        })
        list.add(SceneView(EditorState, PlayMode.EDITING, style).setWeight(1f))
        list.setWeight(1f)
    }

}