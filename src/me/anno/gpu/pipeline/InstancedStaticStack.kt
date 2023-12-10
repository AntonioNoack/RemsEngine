package me.anno.gpu.pipeline

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshInstanceData
import me.anno.ecs.components.mesh.TypeValue
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.StaticBuffer
import me.anno.utils.structures.arrays.ExpandingIntArray
import me.anno.utils.structures.maps.KeyPairMap
import me.anno.utils.structures.tuples.LongTriple

/**
 * instanced stack of buffers of static data,
 * see MeshSpawner.forEachInstancedGroup()
 * */
class InstancedStaticStack(capacity: Int = 512) : DrawableStack(MeshInstanceData.DEFAULT_INSTANCED) {

    val data = KeyPairMap<Mesh, Material, Data>(capacity)

    class Data {

        val size get() = data.size
        val data = ArrayList<StaticBuffer>()
        val attr = ArrayList<Map<String, TypeValue>>()
        val clickIds = ExpandingIntArray(16)

        fun clear() {
            data.clear()
            attr.clear()
            clickIds.clear()
        }
    }

    override fun size1(): Long {
        return data.values.values.sumOf { it.size.toLong() }
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
        // draw instanced meshes
        for ((mesh, list) in data.values) {
            GFXState.vertexData.use(mesh.vertexData) {
                for ((material, stack) in list) {
                    for (i in 0 until stack.size) {
                        drawStack(pipeline, stage, mesh, material, stack, i)
                        val numInstances = stack.data[i].elementCount.toLong()
                        drawnPrimitives += mesh.numPrimitives * numInstances
                        drawnInstances += numInstances
                        drawCalls++
                    }
                }
            }
        }
        return LongTriple(drawnPrimitives, drawnInstances, drawCalls)
    }

    fun drawStack(
        pipeline: Pipeline, stage: PipelineStage,
        mesh: Mesh, material: Material,
        stack: Data, indexIntoStack: Int,
    ) {
        mesh.ensureBuffer()

        val shader = stage.getShader(material)
        shader.use()

        material.bind(shader)

        val attr = stack.attr[indexIntoStack]
        if (attr.isNotEmpty()) {
            for ((uniformName, valueType) in attr) {
                valueType.bind(shader, uniformName)
            }
        }

        stage.bindRandomness(shader)

        // update material and light properties
        val previousMaterial = PipelineStage.lastMaterial.put(shader, material)
        if (previousMaterial == null) {
            stage.initShader(shader, pipeline)
        }

        if (previousMaterial == null) {
            val aabb = PipelineStage.tmpAABBd
            aabb.clear()
            // pipeline.frustum.union(aabb)
            stage.setupLights(pipeline, shader, aabb, true)
        }

        shader.v4f("tint", -1)
        shader.v1b("hasAnimation", false)
        shader.v1i("hasVertexColors", if (material.enableVertexColors) mesh.hasVertexColors else 0)
        shader.v2i("randomIdData", mesh.numPrimitives.toInt(), 0)
        GFX.check()

        shader.v4f("finalId", stack.clickIds[indexIntoStack])
        GFXState.cullMode.use(mesh.cullMode * material.cullMode * stage.cullMode) {
            mesh.drawInstanced(shader, 0, stack.data[indexIntoStack])
        }
    }

    override fun clear() {
        for ((_, values) in data.values) {
            for ((_, value) in values) {
                value.clear()
            }
        }
    }
}