package me.anno.gpu.pipeline

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.TypeValue
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.StaticBuffer
import me.anno.utils.structures.arrays.ExpandingIntArray
import me.anno.utils.structures.maps.KeyPairMap

class InstancedStackStatic(capacity: Int = 512) : KeyPairMap<Mesh, Material, InstancedStackStatic.Data>(capacity), DrawableStack {

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

    override fun draw(
        pipeline: Pipeline,
        stage: PipelineStage,
        needsLightUpdateForEveryMesh: Boolean,
        time: Long, depth: Boolean
    ): Long {
        var sum = 0L
        // draw instanced meshes
        GFXState.instanced.use(true) {
            for ((mesh, list) in values) {
                for ((material, stack) in list) {
                    for (i in 0 until stack.size) {

                        mesh.ensureBuffer()

                        val shader = stage.getShader(material)
                        shader.use()

                        material.bind(shader)

                        val attr = stack.attr[i]
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
                            pipeline.frustum.union(aabb)
                            stage.setupLights(pipeline, shader, aabb, true)
                        }

                        GFX.shaderColor(shader, "tint", -1)
                        shader.v1b("hasAnimation", false)
                        shader.v1i("hasVertexColors", mesh.hasVertexColors)
                        shader.v2i("randomIdData", mesh.numPrimitives.toInt(), 0)
                        GFX.check()

                        shader.v4f("clickId", stack.clickIds[i])
                        mesh.drawInstanced(shader, 0, stack.data[i])
                        sum += mesh.numPrimitives * stack.data[i].elementCount.toLong()

                    }
                }
            }
        }
        return sum
    }

    override fun clear() {
        for ((_, values) in values) {
            for ((_, value) in values) {
                value.clear()
            }
        }
    }

}