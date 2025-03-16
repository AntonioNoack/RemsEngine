package me.anno.gpu.pipeline

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.EnvironmentMap
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.light.LightType
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.utils.MeshInstanceData
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.CullMode
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.alwaysDepthMode
import me.anno.gpu.M4x3Delta.m4x3delta
import me.anno.gpu.M4x3Delta.m4x3x
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.pipeline.LightShaders.bindNullDepthTextures
import me.anno.gpu.pipeline.LightShaders.countPerPixel
import me.anno.gpu.pipeline.LightShaders.lightInstanceBuffer
import me.anno.gpu.pipeline.LightShaders.visualizeLightCountShader
import me.anno.gpu.pipeline.LightShaders.visualizeLightCountShaderInstanced
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.bindTransformUniforms
import me.anno.gpu.shader.DepthTransforms.bindDepthUniforms
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.TextureLib
import me.anno.maths.Maths.min
import me.anno.utils.structures.lists.SmallestKList
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector4f

class LightPipelineStage(var deferred: DeferredSettings?) {

    var visualizeLightCount = false

    var depthMode = alwaysDepthMode
    var blendMode = BlendMode.ADD
    var writeDepth = false
    var cullMode = CullMode.BACK

    // not yet optimized
    val environmentMaps = ArrayList<EnvironmentMap>()

    val size: Long get() = instanced.size + nonInstanced.size

    val instanced = LightData()
    val nonInstanced = LightData()

    fun bindDraw(
        pipeline: Pipeline, source: IFramebuffer, depthTexture: Texture2D, depthMask: Vector4f,
        cameraMatrix: Matrix4f, cameraPosition: Vector3d,
    ) {
        bind {
            source.bindTrulyNearestMS(0)
            draw(
                pipeline, cameraMatrix, cameraPosition,
                ::getShader, depthTexture, depthMask
            )
        }
    }

    fun bind(callback: () -> Unit) {
        if (size > 0) {
            GFXState.blendMode.use(blendMode) {
                GFXState.depthMode.use(depthMode) {
                    GFXState.depthMask.use(writeDepth) {
                        GFXState.cullMode.use(cullMode, callback)
                    }
                }
            }
        }
    }

    private fun initShader(
        shader: Shader, cameraMatrix: Matrix4f,
        type: LightType, depthTexture: ITexture2D
    ) {
        shader.use()
        // information for the shader, which is material agnostic
        // add all things, the shader needs to know, e.g., light direction, strength, ...
        // (for the cheap shaders, which are not deferred)
        shader.v1b("isDirectional", type == LightType.DIRECTIONAL)
        shader.v1b("isSpotLight", type == LightType.SPOT)
        shader.v1b("receiveShadows", true)
        shader.v1f("countPerPixel", countPerPixel)
        depthTexture.bindTrulyNearest(shader, "depthTex")
        shader.m4x4("transform", cameraMatrix)
        shader.v3f("cameraPosition", RenderState.cameraPosition)
        shader.v4f("cameraRotation", RenderState.cameraRotation)
        val target = GFXState.currentBuffer
        shader.v2f("invScreenSize", 1f / target.width, 1f / target.height)
        bindNullDepthTextures(shader)
        bindDepthUniforms(shader)
    }

    fun getShader(type: LightType, isInstanced: Boolean): Shader {
        return if (visualizeLightCount) {
            if (isInstanced) visualizeLightCountShaderInstanced
            else visualizeLightCountShader
        } else {
            val deferred = deferred
                ?: throw IllegalStateException("Cannot draw lights directly without deferred buffers")
            LightShaders.getShader(deferred, type)
        }
    }

    fun draw(
        pipeline: Pipeline,
        cameraMatrix: Matrix4f,
        cameraPosition: Vector3d,
        getShader: (LightType, Boolean) -> Shader,
        depthTexture: ITexture2D,
        depthMask: Vector4f,
    ) {
        nonInstanced.forEachType { lights, size, type ->

            val sample = lights[0].light
            val mesh = sample.getLightPrimitive()

            val shader = getShader(type, false)
            initShader(shader, cameraMatrix, type, depthTexture)

            mesh.ensureBuffer()

            val maxTextureIndex = 31
            val planarIndex0 = shader.getTextureIndex("shadowMapPlanar0")
            val cubicIndex0 = shader.getTextureIndex("shadowMapCubic0")
            val supportsPlanarShadows = planarIndex0 in 0..maxTextureIndex
            val supportsCubicShadows = cubicIndex0 in 0..maxTextureIndex

            for (index in 0 until size) {

                val request = lights[index]
                val light = request.light
                val transform = request.drawMatrix

                shader.v1b("fullscreen", light is DirectionalLight && light.cutoff == 0f)

                bindTransformUniforms(shader, transform)

                // define the light data
                // data0: color, type;
                // type is ignored by the shader -> just use 1
                shader.v4f("data0", light.color, 1f)

                // data1: shader specific value (cone angle / size)
                shader.v1f("data1", light.getShaderV0())
                shader.v4f("depthMask", depthMask)

                shader.v1f("cutoff", if (light is DirectionalLight) light.cutoff else 1f)
                shader.m4x3("camSpaceToLightSpace", light.invCamSpaceMatrix)

                var i1 = 0f
                val textures = light.shadowTextures
                if (textures != null) {
                    var texture = textures.depthTexture ?: textures.getTexture0()
                    if (!texture.isCreated()) {
                        texture = TextureLib.depthTexture
                    }
                    if (light is PointLight) {
                        if (supportsCubicShadows) {
                            // bind the texture, and don't you dare to use mipmapping ^^
                            // (at least without variance shadow maps)
                            texture.bind(cubicIndex0, Filtering.TRULY_LINEAR, Clamping.CLAMP)
                            i1 = 1f // end index
                        }
                    } else {
                        if (supportsPlanarShadows) {
                            // bind the texture, and don't you dare to use mipmapping ^^
                            // (at least without variance shadow maps)
                            texture.bind(planarIndex0, Filtering.TRULY_LINEAR, Clamping.CLAMP)
                            i1 = (texture as Texture2DArray).layers.toFloat() // end index
                        }
                    }
                }

                shader.v4f("data2", 0f, i1, light.getShaderV1(), light.getShaderV2())

                mesh.draw(pipeline, shader, 0)

                PipelineStageImpl.drawnPrimitives += mesh.numPrimitives
                PipelineStageImpl.drawnInstances++
                PipelineStageImpl.drawCalls++
            }
        }

        // draw instanced meshes
        this.cameraMatrix = cameraMatrix
        this.cameraPosition = cameraPosition
        GFXState.instanceData.use(MeshInstanceData.DEFAULT_INSTANCED) {
            instanced.forEachType { lights, size, type ->
                val shader = getShader(type, true)
                if (type == LightType.DIRECTIONAL) {
                    GFXState.depthMode.use(alwaysDepthMode) {
                        drawBatches(pipeline, depthTexture, lights, size, type, shader)
                    }
                } else drawBatches(pipeline, depthTexture, lights, size, type, shader)
            }
        }
    }

    private var cameraMatrix: Matrix4f? = null
    private var cameraPosition: Vector3d? = null

    fun drawBatches(
        pipeline: Pipeline, depthTexture: ITexture2D,
        lights: List<LightRequest>, size: Int,
        type: LightType, shader: Shader,
    ) {

        val sample = lights[0].light
        val mesh = sample.getLightPrimitive()

        val cameraMatrix = cameraMatrix!!
        val cameraPosition = cameraPosition!!

        initShader(shader, cameraMatrix, type, depthTexture)

        val buffer = lightInstanceBuffer
        val nioBuffer = buffer.getOrCreateNioBuffer()
        val stride = buffer.stride

        // draw them in batches of size <= batchSize
        // converted from for(.. step ..) to while to avoid allocation
        var baseIndex = 0
        var callCount = 0L
        while (baseIndex < size) {

            buffer.clear()
            nioBuffer.limit(nioBuffer.capacity())
            // fill the data
            val batchSize = buffer.vertexCount
            for (index in baseIndex until min(size, baseIndex + batchSize)) {
                nioBuffer.position((index - baseIndex) * stride)
                val lightI = lights[index]
                m4x3delta(lightI.drawMatrix, cameraPosition, nioBuffer)
                m4x3x(lightI.invCamSpaceMatrix, nioBuffer)
                // put all light data: lightData0, lightData1
                // put data0:
                val light = lightI.light
                val color = light.color
                nioBuffer.putFloat(color.x)
                nioBuffer.putFloat(color.y)
                nioBuffer.putFloat(color.z)
                nioBuffer.putInt(light.lightType.id)
                // put data1: type-dependant property
                nioBuffer.putFloat(light.getShaderV0())
                // put data2:
                nioBuffer.putFloat(0f)
                nioBuffer.putFloat(0f)
                nioBuffer.putFloat(light.getShaderV1())
                nioBuffer.putFloat(light.getShaderV2())
            }
            buffer.ensureBufferWithoutResize()
            mesh.drawInstanced(pipeline, shader, 0, buffer, Mesh.drawDebugLines)

            baseIndex += batchSize
            callCount++
        }

        PipelineStageImpl.drawnPrimitives += size * mesh.numPrimitives
        PipelineStageImpl.drawnInstances += size
        PipelineStageImpl.drawCalls += callCount
    }

    operator fun get(index: Long): LightRequest {
        val nSize = nonInstanced.size
        return if (index < nSize) {
            nonInstanced[index]
        } else {
            instanced[index - nSize]
        }
    }

    fun clear() {
        instanced.clear()
        nonInstanced.clear()
        environmentMaps.clear()
    }

    fun add(light: LightComponent, entity: Entity) {
        add(light, entity.transform)
    }

    fun add(light: LightComponent, transform: Transform) {
        val group = if (light.hasShadow && light.shadowTextures != null) nonInstanced else instanced
        group.add(light, transform)
    }

    fun add(environmentMap: EnvironmentMap) {
        environmentMaps.add(environmentMap)
    }

    fun listOfAll(dst: SmallestKList<LightRequest>): Int {
        instanced.listOfAll(dst)
        nonInstanced.listOfAll(dst)
        return dst.size
    }
}