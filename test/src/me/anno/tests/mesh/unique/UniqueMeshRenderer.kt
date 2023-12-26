package me.anno.tests.mesh.unique

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Material.Companion.defaultMaterial
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshVertexData
import me.anno.ecs.components.mesh.unique.MeshEntry
import me.anno.ecs.components.mesh.unique.UniqueMeshRenderer
import me.anno.ecs.components.shaders.Skybox
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFX.addGPUTask
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.input.Key
import me.anno.maths.Maths.floorMod
import me.anno.maths.patterns.SpiralPattern.spiral2d
import me.anno.mesh.vox.model.VoxelModel
import me.anno.tests.utils.TestWorld
import me.anno.utils.Color.convertABGR2ARGB
import me.anno.utils.hpc.ProcessingQueue
import org.joml.AABBf
import org.joml.Vector3i
import kotlin.math.floor

/**
 * load/unload a big voxel world without much stutter;
 * glBufferData() unfortunately lags every once in a while, but that should be fine,
 * because it's a few times and then newer again
 *
 * (Minecraft like)
 *
 * done dynamic chunk unloading
 * done load/save system
 * done block placing
 *
 * todo first person player controller with simple physics
 * todo inventory system
 * */
fun main() {

    val attributes = listOf(
        Attribute("coords", 3),
        Attribute("colors0", AttributeType.UINT8_NORM, 4)
    )
    val blockVertexData = MeshVertexData(
        listOf(
            ShaderStage(
                "coords", listOf(
                    Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
                    Variable(GLSLType.V3F, "localPosition", VariableMode.OUT)
                ), "localPosition = coords;\n"
            )
        ),
        listOf(
            ShaderStage(
                "nor", listOf(
                    Variable(GLSLType.V3F, "normal", VariableMode.OUT),
                    Variable(GLSLType.V4F, "tangent", VariableMode.OUT)
                ), "normal = vec3(0.0); tangent = vec4(0.0);\n"
            )
        ),
        MeshVertexData.DEFAULT.loadColors,
        MeshVertexData.DEFAULT.loadMotionVec,
        listOf(
            // calculate normals using cross product
            ShaderStage(
                "px-nor", listOf(
                    Variable(GLSLType.V3F, "finalPosition"),
                    Variable(GLSLType.V3F, "normal", VariableMode.OUT)
                ), "normal = normalize(cross(dFdx(finalPosition), dFdy(finalPosition)));\n"
            )
        ),
    )

    val saveSystem = SaveLoadSystem("UniqueMeshRenderer")
    val world = object : TestWorld() {
        override fun generateChunk(chunkX: Int, chunkY: Int, chunkZ: Int, chunk: ByteArray) {
            super.generateChunk(chunkX, chunkY, chunkZ, chunk)
            saveSystem.get(Vector3i(chunkX, chunkY, chunkZ), false) { data ->
                for ((k, v) in data) {
                    chunk[getIndex(k.x, k.y, k.z)] = v
                }
            }
        }
    }

    world.timeoutMillis = 250

    val csx = world.sizeX
    val csy = world.sizeY
    val csz = world.sizeZ

    val material = defaultMaterial

    class ChunkRenderer : UniqueMeshRenderer<Vector3i>(attributes, blockVertexData, material, DrawMode.TRIANGLES) {

        override val hasVertexColors: Int get() = 1

        /**
         * defines what the world looks like for Raycasting,
         * and for AABBs
         * */
        override fun forEachMesh(run: (IMesh, Material?, Transform) -> Unit) {
            var i = 0
            for ((key, entry) in entryLookup) {
                val transform = getTransform(i++)
                transform.setLocalPosition(
                    (key.x * csx).toDouble(),
                    (key.y * csy).toDouble(),
                    (key.z * csz).toDouble(),
                )
                run(entry.mesh!!, material, transform)
            }
        }

        override fun getData(key: Vector3i, mesh: Mesh): StaticBuffer? {
            if (mesh.numPrimitives == 0L) return null
            val pos = mesh.positions!!
            val col = mesh.color0!!
            val buffer = StaticBuffer("chunk$key", attributes, pos.size / 3)
            val data = buffer.nioBuffer!!
            val dx = key.x * csx
            val dy = key.y * csy
            val dz = key.z * csz
            for (i in 0 until buffer.vertexCount) {
                data.putFloat(dx + pos[i * 3])
                data.putFloat(dy + pos[i * 3 + 1])
                data.putFloat(dz + pos[i * 3 + 2])
                data.putInt(convertABGR2ARGB(col[i]))
            }
            buffer.isUpToDate = false
            return buffer
        }
    }

    class ChunkLoader(val chunkRenderer: ChunkRenderer) : Component() {

        val worker = ProcessingQueue("chunks")

        // load world in spiral pattern
        val loadingRadius = 3
        val spiralPattern = spiral2d(loadingRadius + 5, 0, true).toList()
        val loadingPattern = spiralPattern.filter { it.length() < loadingRadius - 0.5f }
        val unloadingPattern = spiralPattern.filter { it.length() > loadingRadius + 1.5f }

        val loadedChunks = HashSet<Vector3i>()

        fun generateChunk(chunkId: Vector3i) {

            val x0 = chunkId.x * csx
            val y0 = chunkId.y * csy
            val z0 = chunkId.z * csz

            val model = object : VoxelModel(csx, csy, csz) {
                override fun getBlock(x: Int, y: Int, z: Int): Int {
                    return world.getElementAt(x0 + x, y0 + y, z0 + z).toInt()
                }
            }
            model.center0()
            val mesh = model.createMesh(world.palette, null, { x, y, z ->
                world.getElementAt(x0 + x, y0 + y, z0 + z).toInt() != 0
            })

            val data = chunkRenderer.getData(chunkId, mesh)
            if (data != null) {
                val bounds = mesh.getBounds()
                bounds.translate(x0.toFloat(), y0.toFloat(), z0.toFloat())
                addGPUTask("ChunkUpload", 1) { // change back to GPU thread
                    chunkRenderer.set(chunkId, MeshEntry(mesh, bounds, data))
                }
            }
        }

        fun AABBf.translate(dx: Float, dy: Float, dz: Float) {
            minX += dx
            minY += dy
            minZ += dz
            maxX += dx
            maxY += dy
            maxZ += dz
        }

        fun loadChunks(center: Vector3i) {
            for (idx in loadingPattern) {
                val vec = Vector3i(idx).add(center)
                if (loadedChunks.add(vec)) {
                    worker += { generateChunk(vec) }
                    break
                }
            }
        }

        fun unloadChunks(center: Vector3i) {
            for (idx in unloadingPattern) {
                val vec = Vector3i(idx).add(center)
                if (loadedChunks.remove(vec)) {
                    chunkRenderer.remove(vec)
                }
            }
        }

        fun getPlayerChunkId(): Vector3i {
            val delta = Vector3i()
            val ci = RenderView.currentInstance
            if (ci != null) {
                val pos = ci.orbitCenter // around where the camera orbits
                delta.set((pos.x / csx).toInt(), 0, (pos.z / csz).toInt())
            }
            return delta
        }

        override fun onUpdate(): Int {
            // load next mesh
            if (worker.remaining == 0) {
                val chunkId = getPlayerChunkId()
                loadChunks(chunkId)
                unloadChunks(chunkId)
            }
            return 1
        }
    }

    val chunkRenderer = ChunkRenderer()
    val chunkLoader = ChunkLoader(chunkRenderer)
    val scene = Entity("Scene")
    scene.add(chunkRenderer)
    scene.add(chunkLoader)

    val sun = DirectionalLight()
    sun.shadowMapCascades = 3
    val sunEntity = Entity("Sun")
        .setScale(100.0)
    sunEntity.add(object : Component() {
        // move shadows with player
        // todo only update every so often
        override fun onUpdate(): Int {
            val rv = RenderView.currentInstance
            if (rv != null) {
                sunEntity.transform.localPosition =
                    sunEntity.transform.localPosition
                        .set(rv.orbitCenter)
                        .apply { y = csy * 0.5 }
                        .round()
                sunEntity.validateTransform()
            }
            return 1
        }
    })
    sunEntity.add(sun)
    val sky = Skybox()
    sky.applyOntoSun(sunEntity, sun, 50f)
    scene.add(sky)
    scene.add(sunEntity)

    testSceneWithUI("Unique Mesh Renderer", scene) {
        it.editControls = object : DraggingControls(it.renderer) {

            var inHandBlock = 1.toByte()

            fun getCoords(query: RayQuery, delta: Double): Vector3i {
                val pos = query.result.positionWS
                val dir = query.direction
                dir.mulAdd(delta, pos, pos)
                return Vector3i(floor(pos.x).toInt(), floor(pos.y).toInt(), floor(pos.z).toInt())
            }

            fun setBlock(coords: Vector3i, block: Byte) {
                world.setElementAt(coords.x, coords.y, coords.z, true, block)
                val chunkId = coordsToChunkId(coords)
                invalidateChunkAt(chunkId)
                val localCoords = Vector3i(
                    floorMod(coords.x, csx),
                    floorMod(coords.y, csy),
                    floorMod(coords.z, csz),
                )
                // when we're on the edge, and we remove a block (set a transparent one), we need to invalidate our neighbors, too
                if (block == TestWorld.air) {
                    if (localCoords.x == 0) invalidateChunkAt(Vector3i(chunkId).sub(1, 0, 0))
                    if (localCoords.y == 0) invalidateChunkAt(Vector3i(chunkId).sub(0, 1, 0))
                    if (localCoords.z == 0) invalidateChunkAt(Vector3i(chunkId).sub(0, 0, 1))
                    if (localCoords.x == csx - 1) invalidateChunkAt(Vector3i(chunkId).add(1, 0, 0))
                    if (localCoords.y == csy - 1) invalidateChunkAt(Vector3i(chunkId).add(0, 1, 0))
                    if (localCoords.z == csz - 1) invalidateChunkAt(Vector3i(chunkId).add(0, 0, 1))
                }
                saveSystem.get(chunkId, true) { changesInChunk ->
                    changesInChunk[localCoords] = block
                    saveSystem.put(chunkId, changesInChunk)
                }
            }

            fun coordsToChunkId(coords: Vector3i): Vector3i {
                return Vector3i(
                    coords.x.floorDiv(csx),
                    coords.y.floorDiv(csy),
                    coords.z.floorDiv(csz)
                )
            }

            fun invalidateChunkAt(coords: Vector3i) {
                chunkLoader.worker += {
                    chunkLoader.generateChunk(coords)
                }
            }

            override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
                // find, which block was clicked
                // expensive way, using raycasting:
                val query = RayQuery(
                    renderView.cameraPosition,
                    renderView.getMouseRayDirection(),
                    1e3
                )
                // todo also implement cheaper raytracing (to show how) going block by block
                //  then we can throw away the meshes and save even more memory :3
                val hitSomething = Raycast.raycastClosestHit(scene, query)
                if (hitSomething) {
                    when (button) {
                        Key.BUTTON_LEFT -> {
                            // remove block
                            val coords = getCoords(query, +1e-3)
                            setBlock(coords, 0)
                        }
                        Key.BUTTON_RIGHT -> {
                            // add block
                            val coords = getCoords(query, -1e-3)
                            setBlock(coords, inHandBlock)
                        }
                        Key.BUTTON_MIDDLE -> {
                            // get block
                            val coords = getCoords(query, +1e-3)
                            inHandBlock = world.getElementAt(coords.x, coords.y, coords.z, true) ?: 0
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}