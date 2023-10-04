package me.anno.gpu.pipeline

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.M4x3Delta.m4x3delta
import me.anno.utils.structures.arrays.ExpandingIntArray
import me.anno.utils.structures.maps.KeyPairMap
import me.anno.utils.structures.tuples.LongPair
import org.joml.Matrix4x3d
import kotlin.math.min

/**
 * instanced stack, where attributes can be derived from an i32 value each
 * */
class InstancedI32Stack(capacity: Int = 512) :
    KeyPairMap<Mesh, Material, InstancedI32Stack.Data>(capacity), DrawableStack {

    class Data {

        val size get() = data.size
        val data = ExpandingIntArray(256)
        val gfxIds = ExpandingIntArray(16)
        val matrices = ArrayList<Matrix4x3d>() // transform for a group of meshes

        fun clear() {
            data.clear()
            gfxIds.clear()
            matrices.clear()
        }

    }

    override fun draw(
        pipeline: Pipeline,
        stage: PipelineStage,
        needsLightUpdateForEveryMesh: Boolean,
        time: Long, depth: Boolean
    ): LongPair {
        var drawnPrimitives = 0L
        var drawCalls = 0L
        for ((mesh, list) in values) {
            for ((material, values) in list) {
                if (values.size > 0) {
                    drawCalls += draw(stage, mesh, material, pipeline, values, depth)
                    drawnPrimitives += mesh.numPrimitives * values.size.toLong()
                }
            }
        }
        return LongPair(drawnPrimitives, drawCalls)
    }

    fun draw(
        stage: PipelineStage,
        mesh: Mesh,
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
        shader.v1i("hasVertexColors", if(material.enableVertexColors) mesh.hasVertexColors else 0)
        shader.v2i("randomIdData", mesh.numPrimitives.toInt(), 0)
        GFX.check()

        // creating a new buffer allows the gpu some time to sort things out; had no performance benefit on my RX 580
        val buffer = PipelineStage.instancedBufferI32
        // StaticBuffer(meshInstancedAttributes, instancedBatchSize, GL_STREAM_DRAW)
        val nioBuffer = buffer.nioBuffer!!
        nioBuffer.limit(nioBuffer.capacity())
        val nioInt = nioBuffer.asIntBuffer()
        nioInt.limit(nioInt.capacity())
        // fill the data
        val cameraPosition = RenderState.cameraPosition
        val worldScale = RenderState.worldScale

        var baseIndex = 0
        val batchSize = buffer.vertexCount
        var drawCalls = 0L
        for (i in 0 until instances.gfxIds.size / 2) {

            val totalEndIndex = if (i * 2 + 2 < instances.gfxIds.size)
                instances.gfxIds[i * 2 + 2] else instances.size

            val gfxId = instances.gfxIds[i * 2 + 1]
            shader.v4f("gfxId", gfxId)
            shader.m4x3delta("localTransform", instances.matrices[i], cameraPosition, worldScale)

            // draw them in batches of size <= batchSize
            while (baseIndex < totalEndIndex) {

                buffer.clear()

                val data = instances.data
                val endIndex = min(totalEndIndex, baseIndex + batchSize)
                nioBuffer.position(0)
                nioInt.position(0)
                nioInt.put(data.array, baseIndex, endIndex - baseIndex)
                nioBuffer.position(nioInt.position() shl 2)
                buffer.isUpToDate = false
                buffer.ensureBufferWithoutResize()
                // slightly optimized over PSR ^^, ~ 8-fold throughput
                GFXState.cullMode.use(mesh.cullMode * material.cullMode * stage.cullMode) {
                    mesh.drawInstanced(shader, 0, buffer)
                }
                drawCalls++
                baseIndex = endIndex

            }
        }
        return drawCalls
    }

    override fun clear() {
        for ((_, l1) in values) {
            for ((_, stack) in l1) {
                stack.clear()
            }
        }
    }

    override fun size1(): Long {
        return values.values.sumOf { it.size.toLong() }
    }

}