package me.anno.gpu.pipeline

import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.utils.MeshInstanceData
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.bindRandomness
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.initShader
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.setupLights
import me.anno.maths.Maths
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.maps.KeyPairMap
import me.anno.utils.structures.tuples.LongTriple

/**
 * instanced stack, supporting position, uniform scale, and rotation
 * */
class InstancedTRSStack(capacity: Int = 64) :
    DrawableStack(MeshInstanceData.TRS) {

    val data = KeyPairMap<IMesh, Material, Data>(capacity)

    class Data {
        val size get() = posSizeRot.size ushr 3
        val posSizeRot = FloatArrayList(256)
        val gfxIds = IntArrayList(16)
        fun clear() {
            posSizeRot.clear()
            gfxIds.clear()
        }
    }

    override fun draw1(
        pipeline: Pipeline,
        stage: PipelineStageImpl,
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
        stage: PipelineStageImpl,
        mesh: IMesh,
        material: Material,
        pipeline: Pipeline,
        instances: Data,
        depth: Boolean
    ): Long {

        val aabb = PipelineStageImpl.tmpAABBd

        mesh.ensureBuffer()

        val shader = stage.getShader(material)
        shader.use()
        bindRandomness(shader)

        // update material and light properties
        val previousMaterial = PipelineStageImpl.lastMaterial.put(shader, material)
        if (previousMaterial == null) {
            initShader(shader, pipeline.applyToneMapping)
        }

        if (!depth && previousMaterial == null) {
            aabb.clear()
            // pipeline.frustum.union(aabb)
            setupLights(pipeline, shader, aabb, true)
        }

        material.bind(shader)
        shader.v4f("tint", 1f)
        shader.v1b("hasAnimation", false)
        shader.v1i("hasVertexColors", if (material.enableVertexColors) mesh.hasVertexColors else 0)
        shader.v2i("randomIdData", mesh.numPrimitives.toInt(), 0)
        GFX.check()

        // creating a new buffer allows the gpu some time to sort things out; had no performance benefit on my RX 580
        val buffer = PipelineStageImpl.instancedBufferSlim
        // StaticBuffer(meshInstancedAttributes, instancedBatchSize, GL_STREAM_DRAW)
        val nioBuffer = buffer.nioBuffer!!
        // fill the data
        val cameraPosition = RenderState.cameraPosition
        val worldScale = RenderState.worldScale

        var baseIndex = 0
        var drawCalls = 0L
        for (i in 0 until instances.gfxIds.size / 2) {

            val totalEndIndex = if (i * 2 + 2 < instances.gfxIds.size)
                instances.gfxIds[i * 2 + 2] else instances.size

            val gfxId = instances.gfxIds[i * 2 + 1]
            shader.v4f("finalId", gfxId)

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
                GFXState.cullMode.use(mesh.cullMode * material.cullMode * stage.cullMode) {
                    mesh.drawInstanced(pipeline, shader, 0, buffer, Mesh.drawDebugLines)
                }
                baseIndex = endIndex
                drawCalls++
            }
        }
        return drawCalls
    }

    override fun clear() {
        for ((_, values) in data.values) {
            for ((_, value) in values) {
                value.clear()
            }
        }
    }

    override fun isEmpty(): Boolean {
        return data.values.values.none { it.size > 0 }
    }
}