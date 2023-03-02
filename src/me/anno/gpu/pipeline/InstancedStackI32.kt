package me.anno.gpu.pipeline

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.maths.Maths
import me.anno.utils.structures.arrays.ExpandingIntArray
import me.anno.utils.structures.maps.KeyPairMap
import me.anno.utils.structures.tuples.LongPair
import org.joml.Matrix4x3f

class InstancedStackI32(capacity: Int = 512) :
    KeyPairMap<Mesh, Material, InstancedStackI32.Data>(capacity), DrawableStack {

    class Data {

        val size get() = data.size
        val data = ExpandingIntArray(256)
        val clickIds = ExpandingIntArray(16)
        val matrices = ArrayList<Matrix4x3f>() // transform for a group of meshes

        fun clear() {
            data.clear()
            clickIds.clear()
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
        GFXState.limitedTransform.use(true) {
            for ((mesh, list) in values) {
                for ((material, values) in list) {
                    if (values.size > 0) {
                        drawCalls += draw(stage, mesh, material, pipeline, values, depth)
                        drawnPrimitives += mesh.numPrimitives * values.size.toLong()
                    }
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
            pipeline.frustum.union(aabb)
            stage.setupLights(pipeline, shader, aabb, true)
        }

        material.bind(shader)
        GFX.shaderColor(shader, "tint", -1)
        shader.v1b("hasAnimation", false)
        shader.v1i("hasVertexColors", mesh.hasVertexColors)
        shader.v2i("randomIdData", mesh.numPrimitives.toInt(), 0)
        GFX.check()

        // creating a new buffer allows the gpu some time to sort things out; had no performance benefit on my RX 580
        val buffer = PipelineStage.instancedBufferI32
        // StaticBuffer(meshInstancedAttributes, instancedBatchSize, GL_STREAM_DRAW)
        val nioBuffer = buffer.nioBuffer!!
        // fill the data
        val cameraPosition = RenderState.cameraPosition
        val worldScale = RenderState.worldScale

        var baseIndex = 0
        val batchSize = buffer.vertexCount
        var drawCalls = 0L
        for (i in 0 until instances.clickIds.size / 2) {

            val totalEndIndex = if (i * 2 + 2 < instances.clickIds.size)
                instances.clickIds[i * 2 + 2] else instances.size

            val clickId = instances.clickIds[i * 2 + 1]
            shader.v4f("clickId", clickId)
            shader.v3f(
                "cameraPosition",
                cameraPosition.x.toFloat(),
                cameraPosition.y.toFloat(),
                cameraPosition.z.toFloat()
            )
            shader.v1f("worldScale", worldScale.toFloat())
            shader.m4x3("instTransform", instances.matrices[i])

            // draw them in batches of size <= batchSize
            while (baseIndex < totalEndIndex) {

                buffer.clear()

                val endIndex = Maths.min(totalEndIndex, baseIndex + batchSize)
                val data = instances.data
                for (index in baseIndex until endIndex) {
                    nioBuffer.putInt(data[index])
                }
                // slightly optimized over PSR ^^, ~ 8-fold throughput
                mesh.drawInstanced(shader, 0, buffer)
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

}