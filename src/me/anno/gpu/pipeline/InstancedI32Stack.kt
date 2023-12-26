package me.anno.gpu.pipeline

import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshInstanceData
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.M4x3Delta.m4x3delta
import me.anno.utils.structures.arrays.ExpandingIntArray
import me.anno.utils.structures.maps.KeyPairMap
import me.anno.utils.structures.tuples.LongTriple
import org.joml.Matrix4x3d
import kotlin.math.min

/**
 * instanced stack, where attributes can be derived from an i32 value each
 * */
open class InstancedI32Stack(
    instanceData: MeshInstanceData,
    capacity: Int = 512
) : DrawableStack(instanceData) {

    val data = KeyPairMap<IMesh, Material, Data>(capacity)

    class Data {

        val size get() = data.size
        val data = ExpandingIntArray(256)
        val metadata = ExpandingIntArray(16)
        val matrices = ArrayList<Matrix4x3d>() // transform for a group of meshes

        fun clear() {
            data.clear()
            metadata.clear()
            matrices.clear()
        }

        fun start(gfxId: Int, matrix: Matrix4x3d): ExpandingIntArray {
            // we only need to mark a new section, when the matrix or gfx id changes
            if (metadata.isEmpty() || gfxId != metadata.last() || matrices.last() != matrix) {
                metadata.add(data.size)
                metadata.add(gfxId)
                matrices.add(matrix)
            }
            return data
        }
    }

    override fun draw1(
        pipeline: Pipeline,
        stage: PipelineStage,
        needsLightUpdateForEveryMesh: Boolean,
        time: Long, depth: Boolean
    ): LongTriple {
        var drawnPrimitives = 0L
        var drawnInstances = 0L
        var drawCalls = 0L
        for ((mesh, list) in data.values) {
            GFXState.vertexData.use(mesh.vertexData) {
                for ((material, values) in list) {
                    if (values.size > 0) {
                        drawCalls += draw(stage, mesh, material, pipeline, values, depth)
                        val numInstances = values.size.toLong()
                        drawnInstances += numInstances
                        drawnPrimitives += mesh.numPrimitives * numInstances
                    }
                }
            }
        }
        return LongTriple(drawnPrimitives, drawnInstances, drawCalls)
    }

    fun draw(
        stage: PipelineStage,
        mesh: IMesh,
        material: Material,
        pipeline: Pipeline,
        instances: Data,
        depth: Boolean
    ): Long {

        val aabb = PipelineStage.tmpAABBd

        mesh.ensureBuffer()

        val shader = stage.getShader(material)
        shader.use()
        stage.bindRandomness(shader)

        // update material and light properties
        val previousMaterial = PipelineStage.lastMaterial.put(shader, material)
        if (previousMaterial == null) {
            stage.initShader(shader, pipeline)
        }

        if (!depth && previousMaterial == null) {
            aabb.clear()
            // pipeline.frustum.union(aabb)
            stage.setupLights(pipeline, shader, aabb, true)
        }

        material.bind(shader)
        shader.v4f("tint", -1)
        shader.v1b("hasAnimation", false)
        shader.v1i("hasVertexColors", if (material.enableVertexColors) mesh.hasVertexColors else 0)
        shader.v2i("randomIdData", mesh.numPrimitives.toInt(), 0)
        GFX.check()

        var drawCalls = 0L
        GFXState.cullMode.use(mesh.cullMode * material.cullMode * stage.cullMode) {

            // creating a new buffer allows the gpu some time to sort things out; had no performance benefit on my RX 580
            val buffer = PipelineStage.instancedBufferI32
            // StaticBuffer(meshInstancedAttributes, instancedBatchSize, GL_STREAM_DRAW)
            val nioBytes = buffer.nioBuffer!!
            nioBytes.limit(nioBytes.capacity())
            val nioInt = nioBytes.asIntBuffer()
            nioInt.limit(nioInt.capacity())
            // fill the data
            val cameraPosition = RenderState.cameraPosition
            val worldScale = RenderState.worldScale

            var baseIndex = 0
            val batchSize = buffer.vertexCount
            val metadata = instances.metadata
            for (i in 0 until (metadata.size ushr 1)) {

                val offsetIdx = i * 2 + 2
                val totalEndIndex = if (offsetIdx < metadata.size)
                    metadata[offsetIdx] else instances.size

                shader.v4f("finalId", metadata[i * 2 + 1])
                shader.m4x3delta("localTransform", instances.matrices[i], cameraPosition, worldScale)

                // draw them in batches of size <= batchSize
                while (baseIndex < totalEndIndex) {

                    buffer.clear()

                    val data = instances.data
                    nioInt.position(0)
                    val length = min(totalEndIndex - baseIndex, batchSize)
                    nioInt.put(data.array, baseIndex, length)
                    nioBytes.position(length shl 2)
                    buffer.ensureBufferWithoutResize()
                    // slightly optimized over PSR ^^, ~ 8-fold throughput
                    mesh.drawInstanced(shader, 0, buffer)

                    drawCalls++
                    baseIndex += length
                }
            }
        }
        return drawCalls
    }

    override fun clear() {
        for ((_, l1) in data.values) {
            for ((_, stack) in l1) {
                stack.clear()
            }
        }
    }

    override fun size1(): Long {
        return data.values.values.sumOf { it.size.toLong() }
    }
}