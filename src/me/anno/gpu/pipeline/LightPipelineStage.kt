package me.anno.gpu.pipeline

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.light.*
import me.anno.engine.ui.render.Renderers
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.M4x3Delta.m4x3delta
import me.anno.gpu.M4x3Delta.m4x3x
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.pipeline.LightShaders.bindNullDepthTextures
import me.anno.gpu.pipeline.LightShaders.countPerPixel
import me.anno.gpu.pipeline.LightShaders.lightCountInstanceBuffer
import me.anno.gpu.pipeline.LightShaders.lightInstanceBuffer
import me.anno.gpu.pipeline.LightShaders.visualizeLightCountShader
import me.anno.gpu.pipeline.LightShaders.visualizeLightCountShaderInstanced
import me.anno.gpu.pipeline.PipelineStage.Companion.setupLocalTransform
import me.anno.gpu.shader.ReverseDepth.bindDepthToPosition
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.io.Saveable
import me.anno.maths.Maths.min
import me.anno.utils.structures.lists.SmallestKList
import org.joml.Matrix4f
import org.joml.Vector3d

class LightPipelineStage(var deferred: DeferredSettingsV2?) : Saveable() {

    // todo add optional iridescence parameter for shading ... it looks really nice on leather and metal :)
    // https://belcour.github.io/blog/research/publication/2017/05/01/brdf-thin-film.html
    // source code it at the top of the page
    // todo or even better, make this rendering part here modular, so you can use any parameters and materials, you want

    var visualizeLightCount = false

    var depthMode = DepthMode.ALWAYS
    var blendMode = BlendMode.ADD
    var writeDepth = false
    var cullMode = CullMode.FRONT

    // not yet optimized
    val environmentMaps = ArrayList<EnvironmentMap>()

    val size get() = instanced.size + nonInstanced.size

    private val instanced = LightData()
    private val nonInstanced = LightData()

    fun bindDraw(source: IFramebuffer, cameraMatrix: Matrix4f, cameraPosition: Vector3d, worldScale: Double) {
        bind {
            source.bindTrulyNearestMS(0)
            draw(
                cameraMatrix, cameraPosition, worldScale,
                ::getShader, source.depthTexture as Texture2D,
            )
        }
    }

    fun bind(run: () -> Unit) {
        if (instanced.isNotEmpty() || nonInstanced.isNotEmpty()) {
            GFXState.blendMode.use(blendMode) {
                GFXState.depthMode.use(depthMode) {
                    GFXState.depthMask.use(writeDepth) {
                        GFXState.cullMode.use(cullMode) {
                            run()
                        }
                    }
                }
            }
        }
    }

    private fun initShader(
        shader: Shader, cameraMatrix: Matrix4f,
        type: LightType, depthTexture: Texture2D
    ) {
        shader.use()
        // information for the shader, which is material agnostic
        // add all things, the shader needs to know, e.g., light direction, strength, ...
        // (for the cheap shaders, which are not deferred)
        shader.v1b("isDirectional", type == LightType.DIRECTIONAL)
        shader.v1b("receiveShadows", true)
        shader.v1f("countPerPixel", countPerPixel)
        depthTexture.bindTrulyNearest(shader, "depthTex")
        shader.m4x4("transform", cameraMatrix)
        bindNullDepthTextures(shader)
        bindDepthToPosition(shader)
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
        cameraMatrix: Matrix4f,
        cameraPosition: Vector3d,
        worldScale: Double,
        getShader: (LightType, Boolean) -> Shader,
        depthTexture: Texture2D,
    ) {

        val time = Engine.gameTime

        // todo detect, where MSAA is applicable
        // todo and then only do computations with MSAA on those pixels
        //  - render the remaining pixels on a new FB without MSAA
        // todo we can also separate the case of 2 different fragments, just with some ratio (4x less compute/light lookups)
        // (twice as many draw calls, but hopefully less work altogether)

        // if (destination is multi-sampled &&) settings is multisampled, bind the multi-sampled textures

        nonInstanced.forEachType { lights, type, size ->

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

                val transform = request.transform

                shader.v1b("fullscreen", light is DirectionalLight && light.cutoff <= 0.0)

                setupLocalTransform(shader, transform, time)

                val m = transform.getDrawMatrix(time)

                // define the light data
                // data0: color, type;
                // type is ignored by the shader -> just use 1
                shader.v4f("data0", light.color, 1f)

                // data1: camera position, shader specific value (cone angle / size)
                shader.v4f(
                    "data1",
                    ((m.m30 - cameraPosition.x) * worldScale).toFloat(),
                    ((m.m31 - cameraPosition.y) * worldScale).toFloat(),
                    ((m.m32 - cameraPosition.z) * worldScale).toFloat(),
                    light.getShaderV0(m, worldScale)
                )

                shader.v1f("cutoff", if (light is DirectionalLight) light.cutoff else 1f)

                shader.m4x3("camSpaceToLightSpace", light.invCamSpaceMatrix)

                var shadowIdx1 = 0
                if (light is PointLight) {
                    if (supportsCubicShadows) {
                        val cascades = light.shadowTextures
                        if (cascades != null) {
                            val texture = cascades[0].depthTexture!!
                            // bind the texture, and don't you dare to use mipmapping ^^
                            // (at least without variance shadow maps)
                            texture.bind(cubicIndex0, GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
                            shadowIdx1 = 1 // end index
                        }
                    }
                } else {
                    if (supportsPlanarShadows) {
                        var planarSlot = 0
                        val cascades = light.shadowTextures
                        if (cascades != null) for (j in cascades.indices) {
                            val slot = planarIndex0 + planarSlot
                            if (slot > maxTextureIndex) break
                            val texture = cascades[j].depthTexture!!
                            // bind the texture, and don't you dare to use mipmapping ^^
                            // (at least without variance shadow maps)
                            texture.bind(slot, GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
                            if (++planarSlot >= Renderers.MAX_PLANAR_LIGHTS) break
                        }
                        shadowIdx1 = planarSlot // end index
                    }
                }

                val shadowIdx0 = 0f
                shader.v4f("data2", shadowIdx0, shadowIdx1.toFloat(), light.getShaderV1(), light.getShaderV2())

                mesh.draw(shader, 0)

            }
        }

        // draw instanced meshes
        if (instanced.isNotEmpty()) {
            this.cameraMatrix = cameraMatrix
            this.cameraPosition = cameraPosition
            this.worldScale = worldScale
            GFXState.instanced.use(true) {
                instanced.forEachType { lights, type, _ ->
                    val shader = getShader(type, true)
                    if (type == LightType.DIRECTIONAL) {
                        GFXState.depthMode.use(DepthMode.ALWAYS) {
                            drawBatches(depthTexture, lights, type, size, shader)
                        }
                    } else drawBatches(depthTexture, lights, type, size, shader)
                }
            }
        }

    }

    private var cameraMatrix: Matrix4f? = null
    private var cameraPosition: Vector3d? = null
    private var worldScale: Double = 1.0

    fun drawBatches(
        depthTexture: Texture2D,
        lights: List<LightRequest<*>>,
        type: LightType, size: Int,
        shader: Shader
    ) {

        val visualizeLightCount = visualizeLightCount

        val sample = lights[0].light
        val mesh = sample.getLightPrimitive()
        mesh.ensureBuffer()

        val cameraMatrix = cameraMatrix!!
        val cameraPosition = cameraPosition!!
        val worldScale = worldScale

        initShader(shader, cameraMatrix, type, depthTexture)

        val time = Engine.gameTime

        val buffer =
            if (visualizeLightCount) lightCountInstanceBuffer
            else lightInstanceBuffer
        val nioBuffer = buffer.nioBuffer!!
        val stride = buffer.attributes[0].stride

        // draw them in batches of size <= batchSize
        // converted from for(.. step ..) to while to avoid allocation
        var baseIndex = 0
        while (baseIndex < size) {

            buffer.clear()
            nioBuffer.limit(nioBuffer.capacity())
            // fill the data
            val batchSize = buffer.vertexCount
            for (index in baseIndex until min(size, baseIndex + batchSize)) {
                nioBuffer.position((index - baseIndex) * stride)
                val lightI = lights[index]
                val light = lightI.light
                val m = lightI.transform.getDrawMatrix(time)
                m4x3delta(m, cameraPosition, worldScale, nioBuffer)
                if (!visualizeLightCount) {
                    m4x3x(light.invCamSpaceMatrix, nioBuffer)
                    // put all light data: lightData0, lightData1
                    // put data0:
                    val color = light.color
                    nioBuffer.putFloat(color.x)
                    nioBuffer.putFloat(color.y)
                    nioBuffer.putFloat(color.z)
                    nioBuffer.putFloat(0f) // type, not used
                    // put data1/xyz: world position
                    nioBuffer.putFloat(((m.m30 - cameraPosition.x) * worldScale).toFloat())
                    nioBuffer.putFloat(((m.m31 - cameraPosition.y) * worldScale).toFloat())
                    nioBuffer.putFloat(((m.m32 - cameraPosition.z) * worldScale).toFloat())
                    // put data1/a: custom property
                    nioBuffer.putFloat(light.getShaderV0(m, worldScale))
                    // put data2:
                    nioBuffer.putFloat(0f)
                    nioBuffer.putFloat(0f)
                    nioBuffer.putFloat(light.getShaderV1())
                }
                nioBuffer.putFloat(light.getShaderV2())
            }
            buffer.ensureBufferWithoutResize()
            mesh.drawInstanced(shader, 0, buffer)

            baseIndex += batchSize

        }
    }

    operator fun get(index: Int): LightRequest<*> {
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
        val group = if (light.hasShadow && light.shadowTextures != null) nonInstanced else instanced
        group.add(light, entity.transform)
    }

    fun add(environmentMap: EnvironmentMap) {
        environmentMaps.add(environmentMap)
    }

    fun listOfAll(): List<LightRequest<*>> {
        return instanced.listOfAll() + nonInstanced.listOfAll()
    }

    fun listOfAll(dst: SmallestKList<LightRequest<*>>): Int {
        instanced.listOfAll(dst)
        nonInstanced.listOfAll(dst)
        return dst.size
    }

    override val className: String get() = "LightPipelineStage"
    override val approxSize get() = 5

}