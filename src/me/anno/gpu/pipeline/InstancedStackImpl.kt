package me.anno.gpu.pipeline

import me.anno.Time
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.utils.MeshInstanceData
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.animated
import me.anno.gpu.M4x3Delta
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.pipeline.InstancedBuffers.instancedBuffer
import me.anno.gpu.pipeline.InstancedBuffers.instancedBufferA
import me.anno.gpu.pipeline.InstancedBuffers.instancedBufferM
import me.anno.gpu.pipeline.InstancedBuffers.instancedBufferMA
import me.anno.gpu.pipeline.InstancedBuffers.instancedBufferSlim
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.bindCameraUniforms
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.bindJitterUniforms
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.bindLightUniforms
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.drawCallId
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.min
import me.anno.utils.Color.convertABGR2ARGB
import me.anno.utils.GFXFeatures
import me.anno.utils.algorithms.ForLoop.forLoop
import me.anno.utils.structures.maps.KeyTripleMap
import me.anno.utils.structures.tuples.LongTriple
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Vector3d
import java.nio.ByteBuffer

class InstancedStackImpl(capacity: Int = 512) : DrawableStack(MeshInstanceData.DEFAULT_INSTANCED) {

    val data = KeyTripleMap<IMesh, Material, Int, InstancedStack>(capacity)

    override fun isEmpty(): Boolean {
        return data.values.values.all { it.isEmpty() }
    }

    override fun draw1(
        pipeline: Pipeline, stage: PipelineStageImpl,
        needsLightUpdateForEveryMesh: Boolean, time: Long, depth: Boolean
    ): LongTriple {
        var drawnPrimitives = 0L
        var drawnInstances = 0L
        var drawCalls = 0L
        // draw instanced meshes
        for ((mesh, list) in data.values) {
            GFXState.vertexData.use(mesh.vertexData) {
                for ((material, materialIndex, values) in list) {
                    if (values.isNotEmpty()) {
                        GFXState.cullMode.use(mesh.cullMode * material.cullMode * stage.cullMode) {
                            drawCalls += drawInstances(
                                mesh, material, materialIndex,
                                pipeline, stage, needsLightUpdateForEveryMesh,
                                values, depth
                            )
                        }
                        val numInstances = values.size.toLong()
                        drawnInstances += numInstances
                        drawnPrimitives += mesh.numPrimitives * numInstances
                    }
                }
            }
        }
        GFX.check()
        return LongTriple(drawnPrimitives, drawnInstances, drawCalls)
    }

    private fun drawInstances(
        mesh: IMesh, material: Material, materialIndex: Int,
        pipeline: Pipeline, stage: PipelineStageImpl, needsLightUpdateForEveryMesh: Boolean,
        instances: InstancedStack, depth: Boolean
    ): Int {

        val aabb = PipelineStageImpl.tmpAABBd

        mesh.ensureBuffer()

        val localAABB = mesh.getBounds()
        val useAnimations = instances is InstancedAnimStack && instances.animTexture != null

        val motionVectors = BaseShader.motionVectors
        var drawCalls = 0
        animated.use(useAnimations) {

            // val t0 = Time.nanoTime

            GFX.check()

            mesh.ensureBuffer()

            // todo useAttributeLayout in non-instanced meshes, too (?)
            // todo test with lots and lots of attributes
            //  (whether we still fail before this)
            // Android works :)
            val useAttributeLayout = mesh is Mesh && mesh.helperMeshes == null && mesh.buffer != null &&
                    GFXFeatures.supportsShaderStorageBuffers

            val tmpShader = stage.getShader(material)
            // to enable this mode, your vertex shader needs to be adjusted; motion vectors will only work with static meshes properly
            val highPerformanceMode = tmpShader.getAttributeLocation("instancePosSize") >= 0
            // creating a new buffer allows the gpu some time to sort things out; had no performance benefit on my RX 580
            val buffer = if (highPerformanceMode) instancedBufferSlim else {
                if (motionVectors) {
                    if (useAnimations) instancedBufferMA else instancedBufferM
                } else {
                    if (useAnimations) instancedBufferA else instancedBuffer
                }
            }

            fun draw(shader: Shader) {

                shader.use()
                GFX.check()
                bindJitterUniforms(shader)

                // update material and light properties
                val previousMaterial = PipelineStageImpl.lastMaterial.put(shader, material)
                if (previousMaterial == null) {
                    bindCameraUniforms(shader, pipeline.applyToneMapping)
                }

                if (!depth && previousMaterial == null && !needsLightUpdateForEveryMesh) {
                    aabb.clear()
                    // pipeline.frustum.union(aabb)
                    bindLightUniforms(pipeline, shader, aabb, true)
                }

                GFX.check()
                material.bind(shader)
                GFX.check()
                shader.v4f("tint", 1f)
                shader.v1b("hasAnimation", useAnimations)
                shader.v1i("hasVertexColors", if (material.enableVertexColors) mesh.hasVertexColors else 0)
                shader.v2i("randomIdData", mesh.numPrimitives.toInt(), 0)
                shader.v1i("frameId", Time.frameIndex)

                if (useAnimations) {
                    instances.animTexture!!
                        .bind(shader, "animTexture", Filtering.TRULY_LINEAR, Clamping.CLAMP)
                }
                GFX.check()

                drawCalls += drawInstances(
                    buffer, instances, highPerformanceMode,
                    useAnimations, motionVectors, needsLightUpdateForEveryMesh,
                    mesh, aabb, localAABB, pipeline, stage, shader, material, materialIndex,
                )
            }

            if (useAttributeLayout) {
                val bufferI = mesh.buffer!!
                GFXState.bakedMeshLayout.use(bufferI.attributes) {
                    GFXState.bakedInstLayout.use(buffer.attributes) {
                        draw(stage.getShader(material))
                    }
                }
            } else {
                draw(stage.getShader(material))
            }
        }
        return drawCalls
    }

    private fun drawInstances(
        buffer: StaticBuffer, instances: InstancedStack,
        highPerformanceMode: Boolean, useAnimations: Boolean,
        motionVectors: Boolean, needsLightUpdateForEveryMesh: Boolean,
        mesh: IMesh, aabb: AABBd, localAABB: AABBf,
        pipeline: Pipeline, stage: PipelineStageImpl, shader: Shader,
        material: Material, materialIndex: Int,
    ): Int {
        // StaticBuffer(meshInstancedAttributes, instancedBatchSize, GL_STREAM_DRAW)
        val nioBuffer = buffer.getOrCreateNioBuffer()
        // fill the data
        val transforms = instances.transforms
        val gfxIds = instances.gfxIds

        val anim = (instances as? InstancedAnimStack)?.animData
        val cameraPosition = RenderState.cameraPosition
        val prevCameraPosition = RenderState.prevCameraPosition

        // val t1 = Time.nanoTime
        var st23 = 0L
        var st34 = 0L
        var st45 = 0L
        var st56 = 0L
        // var st78 = 0L
        // var st89 = 0L

        val batchSize = buffer.vertexCount
        val overrideGfxId = RenderView.currentInstance?.renderMode == RenderMode.DRAW_CALL_ID
        val drawDebugLines = Mesh.drawDebugLines
        val instanceCount = instances.size
        val bindBuffersDirectly = GFXState.bakedMeshLayout.currentValue != null
        forLoop(0, instanceCount, batchSize) { baseIndex ->

            val t2 = Time.nanoTime

            buffer.clear()

            val t3 = Time.nanoTime
            st23 += t3 - t2

            val drawCallId = drawCallId++
            val endIndex = min(instanceCount, baseIndex + batchSize)
            if (highPerformanceMode) {
                val cx = cameraPosition.x
                val cy = cameraPosition.y
                val cz = cameraPosition.z
                put(nioBuffer, transforms, baseIndex, endIndex, cx, cy, cz)
            } else {
                putAdvanced(
                    nioBuffer, buffer, transforms, baseIndex, endIndex,
                    prevCameraPosition, cameraPosition, motionVectors,
                    useAnimations, anim, overrideGfxId, gfxIds, drawCallId
                )
            }

            val t4 = Time.nanoTime
            st34 += t4 - t3

            if (needsLightUpdateForEveryMesh) {
                updateLights(pipeline, shader, aabb, localAABB, transforms, baseIndex, endIndex)
            }
            GFX.check()

            val t5 = Time.nanoTime
            st45 += t5 - t4

            buffer.ensureBufferWithoutResize()

            if (bindBuffersDirectly) {
                shader.bindBuffer(0, (mesh as Mesh).buffer!!)
                shader.bindBuffer(1, buffer)
            }

            val cullMode = mesh.cullMode * material.cullMode * stage.cullMode
            GFXState.cullMode.use(cullMode) {
                mesh.drawInstanced(pipeline, shader, materialIndex, buffer, drawDebugLines)
            }

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
        return ceilDiv(instanceCount, batchSize)
    }

    private fun updateLights(
        pipeline: Pipeline, shader: Shader, aabb: AABBd, localAABB: AABBf,
        transforms: Array<Any?>, baseIndex: Int, endIndex: Int
    ) {
        // would need to be voted by MeshComponents...
        // could be encoded into a float's lowest bit somewhere...
        val receiveShadows = true
        // calculate the lights for each group
        // todo cluster them cheaply?
        aabb.clear()
        for (index in baseIndex until endIndex) {
            val transform = transforms[index] as Transform
            localAABB.transformUnion(transform.getDrawMatrix(), aabb)
        }
        bindLightUniforms(pipeline, shader, aabb, receiveShadows)
        shader.checkIsUsed()
    }

    private fun put(
        nioBuffer: ByteBuffer, transforms: Array<Any?>,
        baseIndex: Int, endIndex: Int,
        cx: Double, cy: Double, cz: Double
    ) {
        for (index in baseIndex until endIndex) {
            val tr = transforms[index] as Transform
            val tri = tr.localPosition
            nioBuffer.putFloat((tri.x - cx).toFloat())
            nioBuffer.putFloat((tri.y - cy).toFloat())
            nioBuffer.putFloat((tri.z - cz).toFloat())
            val sc = tr.localScale
            nioBuffer.putFloat(sc.x * 0.33333334f)
            val rt = tr.localRotation
            nioBuffer.putFloat(rt.x)
            nioBuffer.putFloat(rt.y)
            nioBuffer.putFloat(rt.z)
            nioBuffer.putFloat(rt.w)
        }
    }

    private fun putAdvanced(
        nioBuffer: ByteBuffer, buffer: StaticBuffer,
        transforms: Array<Any?>,
        baseIndex: Int, endIndex: Int,
        prevCameraPosition: Vector3d, cameraPosition: Vector3d,
        motionVectors: Boolean, useAnimations: Boolean,
        anim: FloatArray?, overrideGfxId: Boolean,
        gfxIds: IntArray, newGfxId: Int,
    ) {
        for (index in baseIndex until endIndex) {
            val transform = transforms[index] as Transform
            val tri = transform.getDrawMatrix()
            M4x3Delta.m4x3delta(tri, cameraPosition, nioBuffer)
            if (motionVectors) {
                // put previous matrix
                val tri2 = transform.getDrawnMatrix()
                M4x3Delta.m4x3delta(tri2, prevCameraPosition, nioBuffer)
                // put animation data
                if (useAnimations) {
                    // anim and previous anim data
                    buffer.put(anim!!, index * 16, 16)
                }
            } else {
                // put current animation data
                if (useAnimations) buffer.put(anim!!, index * 16, 8)
            }
            nioBuffer.putInt(
                convertABGR2ARGB(
                    if (overrideGfxId) newGfxId
                    else gfxIds[index]
                )
            )
        }
    }

    override fun clear() {
        for (stack in data.values.values) {
            for (i in 0 until stack.size) {
                val instancedStack = stack.getThird(i)
                instancedStack.clear()
                InstancedStack.returnStack(instancedStack)
            }
            stack.clear()
        }
        // must be cleared to avoid Meshes staying as keys indefinitely
        data.clear()
    }
}