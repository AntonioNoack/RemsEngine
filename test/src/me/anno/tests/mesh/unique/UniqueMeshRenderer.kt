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
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.input.Key
import me.anno.maths.Maths
import me.anno.maths.patterns.SpiralPattern.spiral2d
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.tests.utils.TestWorld
import me.anno.utils.hpc.ProcessingQueue
import org.joml.AABBf
import org.joml.Vector3i
import java.lang.Math.floorDiv
import kotlin.math.floor

/**
 * load/unload a big voxel world without much stutter;
 * glBufferData() unfortunately lags every once in a while, but that should be fine,
 * because it's a few times and then newer again
 *
 * (Minecraft like)
 *
 * todo dynamic chunk unloading
 * done block placing
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

    val world = TestWorld
    val csx = 64
    val csy = 32

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
                    (key.z * csx).toDouble(),
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
            val dz = key.z * csx
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
        val loadingPattern = spiral2d(5, 0, true).iterator()

        fun generate(key: Vector3i) {
            val x0 = key.x * csx
            val y0 = key.y * csy
            val z0 = key.z * csx
            val mesh = world.createTriangleMesh2(x0, y0, z0, csx, csy, csx)
            val data = chunkRenderer.getData(key, mesh)
            if (data != null) addEvent { // change back to GPU thread
                chunkRenderer.set(key, MeshEntry(mesh, data))
            }
        }

        override fun onUpdate(): Int {
            // load next mesh
            if (worker.remaining == 0) {
                if (loadingPattern.hasNext()) {
                    val idx = loadingPattern.next()
                    worker += { generate(idx) }
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

            fun invalidateChunkAt(coords: Vector3i) {
                coords.set(
                    floorDiv(coords.x, csx),
                    floorDiv(coords.y, csy),
                    floorDiv(coords.z, csx)
                )
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
                            world.setElementAt(coords.x, coords.y, coords.z, true, 0)
                            invalidateChunkAt(coords)
                        }
                        Key.BUTTON_RIGHT -> {
                            // add block
                            val coords = getCoords(query, -1e-3)
                            world.setElementAt(coords.x, coords.y, coords.z, true, inHandBlock)
                            invalidateChunkAt(coords)
                        }
                        Key.BUTTON_MIDDLE -> {
                            // get block
                            val coords = getCoords(query, +1e-3)
                            inHandBlock = world.getElementAt(coords.x, coords.y, coords.z, true) ?: 0
                            invalidateChunkAt(coords)
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}