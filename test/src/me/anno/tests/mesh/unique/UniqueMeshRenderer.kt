package me.anno.tests.mesh.unique

import me.anno.Build
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Material.Companion.defaultMaterial
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshVertexData
import me.anno.ecs.components.mesh.unique.MeshEntry
import me.anno.ecs.components.mesh.unique.UniqueMeshRenderer
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.graph.hdb.ByteSlice
import me.anno.graph.hdb.HierarchicalDatabase
import me.anno.input.Key
import me.anno.maths.Maths
import me.anno.maths.patterns.SpiralPattern.spiral2d
import me.anno.mesh.vox.model.VoxelModel
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.tests.utils.TestWorld
import me.anno.utils.OS.documents
import me.anno.utils.hpc.ProcessingQueue
import org.joml.AABBf
import org.joml.Vector3i
import java.io.ByteArrayOutputStream
import java.lang.Math.floorDiv
import java.lang.Math.floorMod
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
 * */
fun main() {

    Build.isDebug = false

    val attributes = listOf(
        Attribute("coords", AttributeType.SINT32, 3, true),
        Attribute("colors0", AttributeType.UINT8_NORM, 4)
    )
    val vertexData = MeshVertexData(
        listOf(
            ShaderStage(
                "coords", listOf(
                    Variable(GLSLType.V3I, "coords", VariableMode.ATTR),
                    Variable(GLSLType.V3F, "localPosition", VariableMode.OUT)
                ), "" +
                        "localPosition = vec3(coords);\n" // stupidly simple xD
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

    class SaveLoadSystem {

        val db = HierarchicalDatabase(
            "blocks",
            documents.getChild("RemsEngine/Tests/UniqueMeshRenderer"),
            10_000_000,
            30_000L, 0L
        )

        val hash = 0L

        fun getPath(chunkId: Vector3i): List<String> {
            return listOf("${chunkId.x},${chunkId.y},${chunkId.z}")
        }

        fun get(chunkId: Vector3i, async: Boolean, callback: (HashMap<Vector3i, Byte>) -> Unit) {
            db.get(getPath(chunkId), hash, async) { slice ->
                if (slice != null) {
                    slice.stream().use { stream ->
                        val answer = HashMap<Vector3i, Byte>()
                        while (true) {
                            val x = stream.read()
                            val y = stream.read()
                            val z = stream.read()
                            val b = stream.read()
                            if (b < 0) break
                            answer[Vector3i(x, y, z)] = b.toByte()
                        }
                        callback(answer)
                    }
                } else callback(HashMap())
            }
        }

        fun put(chunkId: Vector3i, blocks: Map<Vector3i, Byte>) {
            val stream = ByteArrayOutputStream(blocks.size * 4)
            for ((k, v) in blocks) {
                stream.write(k.x)
                stream.write(k.y)
                stream.write(k.z)
                stream.write(v.toInt())
            }
            val bytes = stream.toByteArray()
            db.put(getPath(chunkId), hash, ByteSlice(bytes))
        }
    }

    val saveSystem = SaveLoadSystem()
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

    val csx = world.sizeX
    val csy = world.sizeY
    val csz = world.sizeZ

    val material = defaultMaterial

    class ChunkRenderer : UniqueMeshRenderer<Vector3i>(attributes, vertexData, material, DrawMode.TRIANGLES) {

        override val hasVertexColors: Int get() = 1

        override fun forEachMesh(run: (Mesh, Material?, Transform) -> Unit) {
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

        private val boundsF = AABBf().all()
        override fun getBounds(): AABBf {
            return boundsF.set(localAABB)
        }

        override fun getData(key: Vector3i, mesh: Mesh): StaticBuffer? {
            if (mesh.numPrimitives == 0L) return null
            val pos = mesh.positions!!
            val col = mesh.color0!!
            val buffer = StaticBuffer("cnk", attributes, pos.size / 3)
            val data = buffer.nioBuffer!!
            val dx = key.x * csx
            val dy = key.y * csy
            val dz = key.z * csz
            for (i in 0 until buffer.vertexCount) {
                data.putInt(dx + pos[i * 3].toInt())
                data.putInt(dy + pos[i * 3 + 1].toInt())
                data.putInt(dz + pos[i * 3 + 2].toInt())
                data.putInt(Maths.convertABGR2ARGB(col[i]))
            }
            buffer.isUpToDate = false
            return buffer
        }
    }

    class ChunkLoader(val chunkRenderer: ChunkRenderer) : Component() {

        val worker = ProcessingQueue("chunks")

        // load world in spiral pattern
        val loadingRadius = 10
        val loadingPattern = spiral2d(loadingRadius, 0, true).toList()
        val unloadingPattern = (2..4).map {
            spiral2d(loadingRadius + it, 0, false)
        }.flatMap { it }

        fun generate(key: Vector3i) {

            val x0 = key.x * csx
            val y0 = key.y * csy
            val z0 = key.z * csz

            val model = object : VoxelModel(csx, csy, csz) {
                override fun getBlock(x: Int, y: Int, z: Int): Int {
                    return world.getElementAt(x0 + x, y0 + y, z0 + z).toInt()
                }
            }

            model.centerX = 0f
            model.centerY = 0f
            model.centerZ = 0f

            val mesh = model.createMesh(world.palette, null, { x, y, z ->
                world.getElementAt(x0 + x, y0 + y, z0 + z).toInt() != 0
            })

            val data = chunkRenderer.getData(key, mesh)
            if (data != null) addEvent { // change back to GPU thread
                chunkRenderer.set(key, MeshEntry(mesh, data))
            }
        }

        val hasRequested = HashSet<Vector3i>()

        override fun onUpdate(): Int {
            // load next mesh
            if (worker.remaining == 0) {
                val delta = Vector3i()
                val ci = RenderView.currentInstance
                if (ci != null) {
                    val pos = ci.cameraPosition
                    delta.set((pos.x / csx).toInt(), 0, (pos.z / csz).toInt())
                }
                // load chunks
                for (idx in loadingPattern) {
                    val vec = Vector3i(idx).add(delta)
                    if (hasRequested.add(vec)) {
                        worker += { generate(vec) }
                        break
                    }
                }
                // unload chunks
                for (idx in unloadingPattern) {
                    val vec = Vector3i(idx).add(delta)
                    if (hasRequested.remove(vec)) {
                        chunkRenderer.remove(vec)
                    }
                }
            }
            return 1
        }
    }

    val chunkRenderer = ChunkRenderer()
    val chunkLoader = ChunkLoader(chunkRenderer)
    val scene = Entity("Scene")
    scene.add(chunkRenderer)
    scene.add(chunkLoader)
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
                    floorDiv(coords.x, csx),
                    floorDiv(coords.y, csy),
                    floorDiv(coords.z, csz)
                )
            }

            fun invalidateChunkAt(coords: Vector3i) {
                chunkLoader.worker += {
                    chunkLoader.generate(coords)
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