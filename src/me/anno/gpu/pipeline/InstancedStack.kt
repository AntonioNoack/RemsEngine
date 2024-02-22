package me.anno.gpu.pipeline

import me.anno.Time
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshInstanceData
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.animated
import me.anno.gpu.GFXState.cullMode
import me.anno.gpu.M4x3Delta
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.bindRandomness
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.initShader
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.instancedBuffer
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.instancedBufferA
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.instancedBufferM
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.instancedBufferMA
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.instancedBufferSlim
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.setupLights
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.maths.Maths
import me.anno.utils.Color.convertABGR2ARGB
import me.anno.utils.structures.maps.KeyTripleMap
import me.anno.utils.structures.tuples.LongTriple

/**
 * container for instanced transforms and their click ids
 * */
open class InstancedStack {

    var transforms = arrayOfNulls<Transform>(64)
    var gfxIds = IntArray(64)
    var size = 0

    fun clear() {
        size = 0
    }

    fun isNotEmpty() = size > 0
    fun isEmpty() = size == 0

    open fun add(transform: Transform, gfxId: Int) {
        if (size >= transforms.size) {
            // resize
            val newSize = transforms.size * 2
            val newTransforms = arrayOfNulls<Transform>(newSize)
            val newGfxIds = IntArray(newSize)
            transforms.copyInto(newTransforms)
            gfxIds.copyInto(newGfxIds)
            transforms = newTransforms
            gfxIds = newGfxIds
        }
        val index = size++
        transforms[index] = transform
        gfxIds[index] = gfxId
    }

    class Impl(capacity: Int = 512) : DrawableStack(MeshInstanceData.DEFAULT_INSTANCED) {

        val data = KeyTripleMap<IMesh, Material, Int, InstancedStack>(capacity)

        override fun isEmpty(): Boolean {
            return data.values.values.all { it.isEmpty() }
        }

        override fun draw1(
            pipeline: Pipeline,
            stage: PipelineStageImpl,
            needsLightUpdateForEveryMesh: Boolean,
            time: Long,
            depth: Boolean
        ): LongTriple {
            var drawnPrimitives = 0L
            var drawnInstances = 0L
            var drawCalls = 0L
            // draw instanced meshes
            for ((mesh, list) in data.values) {
                GFXState.vertexData.use(mesh.vertexData) {
                    for ((material, materialIndex, values) in list) {
                        if (values.isNotEmpty()) {
                            cullMode.use(mesh.cullMode * material.cullMode * stage.cullMode) {
                                drawCalls += drawInstances(
                                    mesh, material, materialIndex,
                                    pipeline, stage, needsLightUpdateForEveryMesh,
                                    time, values, depth
                                )
                            }
                            val numInstances = values.size.toLong()
                            drawnInstances += numInstances
                            drawnPrimitives += mesh.numPrimitives * numInstances
                        }
                    }
                }
            }
            return LongTriple(drawnPrimitives, drawnInstances, drawCalls)
        }

        private fun drawInstances(
            mesh: IMesh, material: Material, materialIndex: Int,
            pipeline: Pipeline, stage: PipelineStageImpl, needsLightUpdateForEveryMesh: Boolean,
            time: Long, instances: InstancedStack, depth: Boolean
        ): Int {

            val receiveShadows = true
            val aabb = PipelineStageImpl.tmpAABBd

            mesh.ensureBuffer()

            val localAABB = mesh.getBounds()
            val useAnimations = instances is InstancedAnimStack && instances.animTexture != null

            val motionVectors = BaseShader.motionVectors
            var drawCalls = 0
            animated.use(useAnimations) {

                // val t0 = Time.nanoTime

                val shader = stage.getShader(material)
                shader.use()
                GFX.check()
                bindRandomness(shader)
                GFX.check()

                // update material and light properties
                val previousMaterial = PipelineStageImpl.lastMaterial.put(shader, material)
                if (previousMaterial == null) {
                    initShader(shader, pipeline.applyToneMapping)
                }

                if (!depth && previousMaterial == null && !needsLightUpdateForEveryMesh) {
                    aabb.clear()
                    // pipeline.frustum.union(aabb)
                    setupLights(pipeline, shader, aabb, true)
                }

                GFX.check()
                material.bind(shader)
                GFX.check()
                shader.v4f("tint", 1f)
                shader.v1b("hasAnimation", useAnimations)
                shader.v1i("hasVertexColors", if (material.enableVertexColors) mesh.hasVertexColors else 0)
                shader.v2i("randomIdData", mesh.numPrimitives.toInt(), 0)
                if (useAnimations) {
                    (instances as InstancedAnimStack).animTexture!!
                        .bind(shader, "animTexture", Filtering.TRULY_LINEAR, Clamping.CLAMP)
                }
                GFX.check()

                // draw them in batches of size <= batchSize
                val instanceCount = instances.size
                // to enable this mode, your vertex shader needs to be adjusted; motion vectors will only work with static meshes properly
                val highPerformanceMode = shader.getAttributeLocation("instancePosSize") >= 0
                // creating a new buffer allows the gpu some time to sort things out; had no performance benefit on my RX 580
                val buffer = if (highPerformanceMode) instancedBufferSlim else {
                    if (motionVectors) {
                        if (useAnimations) instancedBufferMA else instancedBufferM
                    } else {
                        if (useAnimations) instancedBufferA else instancedBuffer
                    }
                }
                // StaticBuffer(meshInstancedAttributes, instancedBatchSize, GL_STREAM_DRAW)
                val nioBuffer = buffer.nioBuffer!!
                // fill the data
                val transforms = instances.transforms
                val gfxIds = instances.gfxIds

                val anim = (instances as? InstancedAnimStack)?.animData
                val cameraPosition = RenderState.cameraPosition
                val worldScale = RenderState.worldScale

                // val t1 = Time.nanoTime
                var st23 = 0L
                var st34 = 0L
                var st45 = 0L
                var st56 = 0L
                // var st78 = 0L
                // var st89 = 0L

                val prevWorldScale = RenderState.prevWorldScale
                // worth ~15%; to use it, ensure that RenderView.worldScale is 1.0
                val noWorldScale = worldScale == 1.0 && (prevWorldScale == 1.0 || !motionVectors)

                val batchSize = buffer.vertexCount
                for (baseIndex in 0 until instanceCount step batchSize) {

                    val t2 = Time.nanoTime

                    buffer.clear()

                    val t3 = Time.nanoTime
                    st23 += t3 - t2

                    val endIndex = Maths.min(instanceCount, baseIndex + batchSize)
                    if (highPerformanceMode) {
                        val cx = cameraPosition.x
                        val cy = cameraPosition.y
                        val cz = cameraPosition.z
                        if (noWorldScale) {
                            for (index in baseIndex until endIndex) {
                                val tr = transforms[index]!!
                                val tri = tr.localPosition
                                nioBuffer.putFloat((tri.x - cx).toFloat())
                                nioBuffer.putFloat((tri.y - cy).toFloat())
                                nioBuffer.putFloat((tri.z - cz).toFloat())
                                val sc = tr.localScale
                                nioBuffer.putFloat(sc.x.toFloat() * 0.33333334f)
                                val rt = tr.localRotation
                                nioBuffer.putFloat(rt.x.toFloat())
                                nioBuffer.putFloat(rt.y.toFloat())
                                nioBuffer.putFloat(rt.z.toFloat())
                                nioBuffer.putFloat(rt.w.toFloat())
                            }
                        } else {
                            for (index in baseIndex until endIndex) {
                                val tr = transforms[index]!!
                                val tri = tr.localPosition
                                nioBuffer.putFloat(((tri.x - cx) * worldScale).toFloat())
                                nioBuffer.putFloat(((tri.y - cy) * worldScale).toFloat())
                                nioBuffer.putFloat(((tri.z - cz) * worldScale).toFloat())
                                val sc = tr.localScale
                                nioBuffer.putFloat((sc.x * worldScale).toFloat() * 0.33333334f)
                                val rt = tr.localRotation
                                nioBuffer.putFloat(rt.x.toFloat())
                                nioBuffer.putFloat(rt.y.toFloat())
                                nioBuffer.putFloat(rt.z.toFloat())
                                nioBuffer.putFloat(rt.w.toFloat())
                            }
                        }
                    } else {
                        for (index in baseIndex until endIndex) {
                            val tri = transforms[index]!!.getDrawMatrix(time)
                            if (noWorldScale) M4x3Delta.m4x3delta(tri, cameraPosition, nioBuffer)
                            else M4x3Delta.m4x3delta(tri, cameraPosition, worldScale, nioBuffer)
                            if (motionVectors) {
                                // put previous matrix
                                val tri2 = transforms[index]!!.getDrawnMatrix(time)
                                if (noWorldScale) M4x3Delta.m4x3delta(tri2, cameraPosition, nioBuffer)
                                else M4x3Delta.m4x3delta(tri2, cameraPosition, prevWorldScale, nioBuffer)
                                // put animation data
                                if (useAnimations) {
                                    // anim and previous anim data
                                    buffer.put(anim!!, index * 16, 16)
                                }
                            } else {
                                // put current animation data
                                if (useAnimations) buffer.put(anim!!, index * 16, 8)
                            }
                            nioBuffer.putInt(convertABGR2ARGB(gfxIds[index]))
                        }
                    }

                    val t4 = Time.nanoTime
                    st34 += t4 - t3

                    if (needsLightUpdateForEveryMesh) {
                        // calculate the lights for each group
                        // todo cluster them cheaply?
                        aabb.clear()
                        for (index in baseIndex until endIndex) {
                            localAABB.transformUnion(transforms[index]!!.getDrawMatrix(), aabb)
                        }
                        setupLights(pipeline, shader, aabb, receiveShadows)
                    }
                    GFX.check()

                    val t5 = Time.nanoTime
                    st45 += t5 - t4

                    cullMode.use(mesh.cullMode * material.cullMode * stage.cullMode) {
                        mesh.drawInstanced(shader, materialIndex, buffer, Mesh.drawDebugLines)
                    }
                    drawCalls++

                    val t6 = Time.nanoTime
                    st56 += t6 - t5

                    // if (buffer !== meshInstanceBuffer) addGPUTask("PipelineStage.drawColor", 1) { buffer.destroy() }
                }

                // has been optimized from ~150ns/e to ~64ns/e on 1M bricks test, with worldScale=1.0 (or ~75 with worldScale != 1.0)
                // mainly optimizing transforms to stop updating with lerp(), when they were no longer being changed
                /*val t6 = Time.nanoTime
                val dt = t6 - t1
                println(
                    "base: ${(t1 - t0)} + $instanceCount meshes with [$st23, $st34, $st45, $st56] -> " +
                            "[${st23 * 100 / dt}, ${st34 * 100 / dt}, " +
                            "${st45 * 100 / dt}, ${st56 * 100 / dt}]"
                )*/

            }
            return drawCalls
        }

        override fun clear() {
            for ((_, values) in data.values) {
                for ((_, _, value) in values) {
                    value.clear()
                }
            }
        }
    }
}