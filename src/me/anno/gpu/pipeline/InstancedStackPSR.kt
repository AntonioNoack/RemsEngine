package me.anno.gpu.pipeline

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.maths.Maths
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray
import me.anno.utils.structures.maps.KeyPairMap

class InstancedStackPSR(capacity: Int = 64) :
    KeyPairMap<Mesh, Material, InstancedStackPSR.Data>(capacity), DrawableStack {

    class Data {
        val size get() = posSizeRot.size ushr 3
        val posSizeRot = ExpandingFloatArray(256)
        val clickIds = ExpandingIntArray(16)
        fun clear() {
            posSizeRot.clear()
            clickIds.clear()
        }
    }

    override fun draw(
        pipeline: Pipeline,
        stage: PipelineStage,
        needsLightUpdateForEveryMesh: Boolean,
        time: Long, depth: Boolean
    ): Long {
        var sum = 0L
        GFXState.limitedTransform.use(true) {
            for ((mesh, list) in values) {
                for ((material, values) in list) {
                    if (values.size > 0) {
                        draw(stage, mesh, material, pipeline, values, depth)
                        sum += mesh.numTriangles * values.size.toLong()
                    }
                }
            }
        }
        return sum
    }

    fun draw(
        stage: PipelineStage,
        mesh: Mesh,
        material: Material,
        pipeline: Pipeline,
        instances: Data,
        depth: Boolean
    ) {

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
        shader.v1b("hasVertexColors", mesh.hasVertexColors)
        shader.v2i("randomIdData", mesh.numTriangles, 0)
        GFX.check()

        // creating a new buffer allows the gpu some time to sort things out; had no performance benefit on my RX 580
        val buffer = PipelineStage.instancedBufferSlim
        // StaticBuffer(meshInstancedAttributes, instancedBatchSize, GL_STREAM_DRAW)
        val nioBuffer = buffer.nioBuffer!!
        // fill the data
        val cameraPosition = RenderState.cameraPosition
        val worldScale = RenderState.worldScale

        var baseIndex = 0
        for (i in 0 until instances.clickIds.size / 2) {

            val totalEndIndex = if (i * 2 + 2 < instances.clickIds.size)
                instances.clickIds[i * 2 + 2] else instances.size

            val clickId = instances.clickIds[i * 2 + 1]
            shader.v4f("clickId", clickId)

            // draw them in batches of size <= batchSize
            val batchSize = buffer.vertexCount
            while (baseIndex < totalEndIndex) {

                buffer.clear()

                val endIndex = Maths.min(totalEndIndex, baseIndex + batchSize)
                val data = instances.posSizeRot
                if (worldScale == 1.0) {
                    val cx = cameraPosition.x.toFloat()
                    val cy = cameraPosition.y.toFloat()
                    val cz = cameraPosition.z.toFloat()
                    for (index in baseIndex until endIndex) {
                        val i8 = index * 8
                        nioBuffer.putFloat(data[i8] - cx)
                        nioBuffer.putFloat(data[i8 + 1] - cy)
                        nioBuffer.putFloat(data[i8 + 2] - cz)
                        nioBuffer.putFloat(data[i8 + 3])
                        nioBuffer.putFloat(data[i8 + 4])
                        nioBuffer.putFloat(data[i8 + 5])
                        nioBuffer.putFloat(data[i8 + 6])
                        nioBuffer.putFloat(data[i8 + 7])
                    }
                } else {
                    val cx = cameraPosition.x
                    val cy = cameraPosition.y
                    val cz = cameraPosition.z
                    for (index in baseIndex until endIndex) {
                        val i8 = index * 8
                        nioBuffer.putFloat(((data[i8] - cx) * worldScale).toFloat())
                        nioBuffer.putFloat(((data[i8 + 1] - cy) * worldScale).toFloat())
                        nioBuffer.putFloat(((data[i8 + 2] - cz) * worldScale).toFloat())
                        nioBuffer.putFloat((data[i8 + 3] * worldScale).toFloat())
                        nioBuffer.putFloat(data[i8 + 4])
                        nioBuffer.putFloat(data[i8 + 5])
                        nioBuffer.putFloat(data[i8 + 6])
                        nioBuffer.putFloat(data[i8 + 7])
                    }
                }

                mesh.drawInstanced(shader, 0, buffer)
                baseIndex = endIndex

            }
        }
    }

    override fun clear() {
        for ((_, values) in values) {
            for ((_, value) in values) {
                value.clear()
            }
        }
    }

}