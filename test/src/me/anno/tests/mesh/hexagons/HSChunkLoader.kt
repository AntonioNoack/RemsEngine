package me.anno.tests.mesh.hexagons

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.unique.MeshEntry
import me.anno.ecs.components.mesh.unique.UniqueMeshRenderer
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.ecs.components.mesh.utils.MeshVertexData.Companion.flatNormalsFragment
import me.anno.ecs.components.mesh.utils.MeshVertexData.Companion.flatNormalsNorTan
import me.anno.ecs.components.mesh.utils.MeshVertexData.Companion.noColors
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFX
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.GPUTasks.gpuTasks
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.io.files.FileReference
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.chunks.spherical.HexagonSphere
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.structures.maps.Maps.removeIf
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Assertions.assertEquals

// todo why is this still lagging soo much? shouldn't it be smooth???
class HSChunkLoader(
    val sphere: HexagonSphere, val world: HexagonSphereMCWorld,
    val transparent: Boolean?, val material: Material
) : UniqueMeshRenderer<Mesh, HexagonSphere.Chunk>(attributes, hexVertexData, DrawMode.TRIANGLES), OnUpdate {

    companion object {
        val attributes = listOf(
            Attribute("coords", 3),
            Attribute("colors0", AttributeType.UINT8_NORM, 4)
        )

        val hexVertexData = MeshVertexData(
            listOf(
                ShaderStage(
                    "hex-coords", listOf(
                        Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
                        Variable(GLSLType.V3F, "localPosition", VariableMode.OUT)
                    ), "localPosition = coords;\n"
                )
            ),
            listOf(flatNormalsNorTan),
            listOf(noColors),
            MeshVertexData.DEFAULT.loadMotionVec,
            listOf(flatNormalsFragment),
        )

        val worker = ProcessingGroup("worldGen", 1f)
    }

    override val materials: List<FileReference> = listOf(material.ref)

    override fun fillSpace(globalTransform: Matrix4x3d, dstUnion: AABBd): Boolean {
        globalAABB.all()
        localAABB.all()
        dstUnion.all()
        return true
    }

    override fun getData(key: HexagonSphere.Chunk, mesh: Mesh): StaticBuffer? {
        val positions = mesh.positions ?: return null
        val colors = mesh.color0 ?: return null
        if (positions.isEmpty()) return null
        assertEquals(null, mesh.indices) // indices aren't supported here
        assertEquals(positions.size, colors.size * 3)
        val buffer = StaticBuffer("hexagons", attributes, positions.size / 3)
        for (i in colors.indices) {
            buffer.put(positions, i * 3, 3)
            buffer.putRGBA(colors[i])
        }
        return buffer
    }

    override fun getTransformAndMaterial(key: HexagonSphere.Chunk, transform: Transform): Material {
        // transform can stay identity
        return material
    }

    val dir = Vector3f()
    val pos = Vector3d()
    val chunks = HashMap<HexagonSphere.Chunk, AABBf>()
    val requests = ArrayList<HexagonSphere.Chunk>()
    var maxAngleDifference = sphere.len * 512

    override fun onUpdate() {
        val renderView = RenderView.currentInstance ?: return
        val pos = pos.set(renderView.orbitCenter).safeNormalize()
        if (pos.lengthSquared() < 0.5) pos.y = 1.0
        dir.set(pos)
        val pos3 = Vector3f(pos)
        chunks.removeIf { (key, bounds) ->
            // DebugShapes.debugTexts.add(DebugText(Vector3d(key.center), "${key.tri} ${key.si} ${key.sj}", -1, 0f))
            if (!bounds.isEmpty() && bounds.distance(pos3) > 1.5f * maxAngleDifference) {
                remove(key, true)
                true
            } else false
        }
        // within a certain radius, request all chunks
        sphere.queryChunks(dir, maxAngleDifference) { sc ->
            if (sc !in chunks) requests.add(sc)
            false
        }
        // sort requests by distance
        requests.sortByDescending { it.center.angleCos(dir) }
        for (i in 0 until min(
            5000 - chunks.size,
            min(requests.size, 16 - max(worker.remaining, gpuTasks.size))
        )) {
            val key = requests[i]
            worker += {
                // check if the request is still valid
                val mesh = createMesh(sphere.queryChunk(key), world, transparent)
                val buffer = getData(key, mesh)
                if (buffer != null) {
                    addGPUTask("chunk", sphere.chunkCount) {
                        buffer.ensureBufferAsync {
                            GFX.check()
                            add(key, MeshEntry(mesh, mesh.getBounds(), buffer))
                            GFX.check()
                            chunks[key] = mesh.getBounds()
                            GFX.check()
                        }
                    }
                }
            }
            chunks[key] = AABBf()
        }
        requests.clear()
    }
}
