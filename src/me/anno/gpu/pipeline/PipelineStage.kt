package me.anno.gpu.pipeline

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.anim.AnimRenderer
import me.anno.ecs.components.light.*
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MaterialCache
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.Mesh.Companion.defaultMaterial
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.Renderers
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFX.shaderColor
import me.anno.gpu.GFXState
import me.anno.gpu.M4x3Delta.buffer16x256
import me.anno.gpu.M4x3Delta.m4x3delta
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib
import me.anno.gpu.texture.TextureLib.blackCube
import me.anno.input.Input
import me.anno.io.Saveable
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Matrices.set4x3Delta
import org.joml.AABBd
import org.joml.Matrix4x3f
import org.joml.Vector3d
import org.lwjgl.opengl.GL30C.*
import java.util.*
import kotlin.math.max
import kotlin.math.min

class PipelineStage(
    var name: String,
    var sorting: Sorting,
    var maxNumberOfLights: Int,
    var blendMode: BlendMode?,
    var depthMode: DepthMode,
    var writeDepth: Boolean,
    var cullMode: CullMode,
    var defaultShader: BaseShader
) : Saveable() {

    companion object {

        const val OPAQUE_PASS = 0
        const val TRANSPARENT_PASS = 1
        const val DECAL_PASS = 2

        var drawnPrimitives = 0L
        var drawCalls = 0L

        val lastMaterial = HashMap<Shader, Material>(64)
        private val tmp4x3 = Matrix4x3f()

        // 16k is ~ 20% better than 1024: 9 fps instead of 7 fps with 150k instanced lights on my RX 580
        const val instancedBatchSize = 1024 * 16

        val instancedBuffer = StaticBuffer(
            listOf(
                Attribute("instanceTrans0", 3),
                Attribute("instanceTrans1", 3),
                Attribute("instanceTrans2", 3),
                Attribute("instanceTrans3", 3),
                Attribute("instanceTint", AttributeType.UINT8_NORM, 4)
            ), instancedBatchSize, GL_DYNAMIC_DRAW
        )

        val instancedBufferA = StaticBuffer(
            listOf(
                Attribute("instanceTrans0", 3),
                Attribute("instanceTrans1", 3),
                Attribute("instanceTrans2", 3),
                Attribute("instanceTrans3", 3),
                Attribute("animWeights", 4),
                Attribute("animIndices", 4),
                Attribute("instanceTint", AttributeType.UINT8_NORM, 4)
            ), instancedBatchSize, GL_DYNAMIC_DRAW
        )

        val instancedBufferM = StaticBuffer(
            listOf(
                Attribute("instanceTrans0", 3),
                Attribute("instanceTrans1", 3),
                Attribute("instanceTrans2", 3),
                Attribute("instanceTrans3", 3),
                Attribute("instancePrevTrans0", 3),
                Attribute("instancePrevTrans1", 3),
                Attribute("instancePrevTrans2", 3),
                Attribute("instancePrevTrans3", 3),
            ), instancedBatchSize, GL_DYNAMIC_DRAW
        )

        val instancedBufferMA = StaticBuffer(
            listOf(
                Attribute("instanceTrans0", 3),
                Attribute("instanceTrans1", 3),
                Attribute("instanceTrans2", 3),
                Attribute("instanceTrans3", 3),
                Attribute("instancePrevTrans0", 3),
                Attribute("instancePrevTrans1", 3),
                Attribute("instancePrevTrans2", 3),
                Attribute("instancePrevTrans3", 3),
                // todo somehow add instance-prefix
                Attribute("animWeights", 4),
                Attribute("animIndices", 4),
                Attribute("prevAnimWeights", 4),
                Attribute("prevAnimIndices", 4),
            ), instancedBatchSize, GL_DYNAMIC_DRAW
        )

        val instancedBufferSlim = StaticBuffer(
            listOf(
                Attribute("instancePosSize", 4),
                Attribute("instanceRot", 4),
            ),
            instancedBatchSize * 2, GL_DYNAMIC_DRAW
        )

        val instancedBufferI32 = StaticBuffer(
            listOf(
                Attribute("instanceI32", AttributeType.SINT32, 1, true)
            ), instancedBatchSize * 16, GL_DYNAMIC_DRAW
        )

        val tmpAABBd = AABBd()

        fun setupLocalTransform(
            shader: Shader,
            transform: Transform,
            time: Long
        ) {

            val localTransform = transform.getDrawMatrix(time)
            tmp4x3.set4x3Delta(localTransform)
            shader.m4x3("localTransform", tmp4x3)

            val ilt = shader["invLocalTransform"]
            if (ilt >= 0) {
                shader.m4x3(ilt, tmp4x3.invert())
            }

            shader.v1f("worldScale", RenderState.worldScale)

            val oldTransform = shader["prevLocalTransform"]
            if (oldTransform >= 0) {
                val prevWorldScale = RenderState.prevWorldScale
                shader.m4x3delta(
                    oldTransform, transform.getDrawnMatrix(time),
                    RenderState.prevCameraPosition, prevWorldScale
                )
                shader.v1f("prevWorldScale", prevWorldScale)
            }

        }

    }

    var nextInsertIndex = 0
    val drawRequests = ArrayList2<DrawRequest>()

    class ArrayList2<V>(cap: Int = 16) {

        var size = 0
        private var content = arrayOfNulls<Any?>(cap)

        fun resize(size: Int) {
            val newSize = max(16, size)
            val content = content
            if (content.size != newSize) {
                val new = arrayOfNulls<Any?>(newSize)
                System.arraycopy(content, 0, new, 0, min(newSize, content.size))
                this.content = new
                this.size = min(this.size, new.size)
            }
        }

        fun add(element: V) {
            if (size >= content.size) resize(content.size * 2)
            content[size++] = element
        }

        @Suppress("unchecked_cast")
        operator fun get(index: Int): V = content[index] as V
        fun sortWith(comp: Comparator<V>) {
            @Suppress("unchecked_cast")
            Arrays.sort(content, 0, size, comp as Comparator<Any?>)
        }

    }

    // doesn't work yet, why ever
    var occlusionQueryPrepass = false

    val size: Long
        get() {
            var sum = nextInsertIndex.toLong()
            for (i in instances.indices) sum += instances[i].size()
            return sum
        }

    val instanced = InstancedStack.Impl()
    val instancedWithIdx = InstancedStack.ImplIdx()
    val instancedPSR = InstancedStackPSR()
    val instancedX = InstancedStackStatic()
    val instancedI32 = InstancedStackI32()

    val instances = arrayListOf<DrawableStack>(
        instanced,
        instancedWithIdx,
        instancedPSR,
        instancedX,
        instancedI32,
        // if you need extra types, e.g., for MeshSpawner, just add them :)
        // (a helper function for that (and finding it again) might be added in the future)
    )

    fun bindDraw(pipeline: Pipeline) {
        Texture2D.unbindAllTextures()
        GFXState.blendMode.use(blendMode) {
            GFXState.depthMode.use(depthMode) {
                GFXState.depthMask.use(writeDepth) {
                    GFXState.cullMode.use(cullMode) {
                        GFX.check()
                        drawColors(pipeline)
                        GFX.check()
                    }
                }
            }
        }
    }

    fun setupPlanarReflection(pipeline: Pipeline, shader: Shader, aabb: AABBd) {

        shader.v4f("reflectionCullingPlane", pipeline.reflectionCullingPlane)
        shader.v2f("renderSize", GFXState.currentBuffer.w.toFloat(), GFXState.currentBuffer.h.toFloat())

        val ti = shader.getTextureIndex("reflectionPlane")
        if (ti < 0 || pipeline.planarReflections.isEmpty()) {
            shader.v1b("hasReflectionPlane", false)
            return
        }

        val minVolume = 0.5 * aabb.volume()
        val pos = JomlPools.vec3d.borrow().set(aabb.avgX(), aabb.avgY(), aabb.avgZ())
        val mapBounds = JomlPools.aabbd.borrow()
        val bestPr = if (minVolume.isFinite()) {
            var candidates: Collection<PlanarReflection> = pipeline.planarReflections.filter {
                val lb = it.lastBuffer
                lb != null && lb.pointer != 0
            }
            if (minVolume > 1e-308) candidates = candidates.filter {
                // only if environment map fills >= 50% of the AABB
                val volume = mapBounds
                    .setMin(-1.0, -1.0, -1.0)
                    .setMax(+1.0, +1.0, +1.0)
                    .transform(it.transform!!.globalTransform)
                    .intersectionVolume(aabb)
                volume >= minVolume
            }
            candidates.minByOrNull {
                it.transform!!.distanceSquaredGlobally(pos)
            }
        } else null

        shader.v1b("hasReflectionPlane", bestPr != null)
        if (bestPr != null) {
            val tex = bestPr.lastBuffer!!
            tex.getTexture0().bind(ti, GPUFiltering.LINEAR, Clamping.CLAMP)
            val normal = bestPr.globalNormal
            shader.v3f("reflectionPlaneNormal", normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat())
        }
    }

    fun setupReflectionMap(pipeline: Pipeline, shader: Shader, aabb: AABBd) {
        val envMapSlot = shader.getTextureIndex("reflectionMap")
        if (envMapSlot >= 0) {
            // find the closest environment map
            val minVolume = 0.5 * aabb.volume()
            val pos = JomlPools.vec3d.borrow().set(aabb.avgX(), aabb.avgY(), aabb.avgZ())
            val mapBounds = JomlPools.aabbd.borrow()
            val map = if (minVolume.isFinite()) {
                pipeline.lightStage.environmentMaps.minByOrNull {
                    val isOk = if (minVolume > 1e-308) {
                        // only if environment map fills >= 50% of the AABB
                        val volume = mapBounds
                            .setMin(-1.0, -1.0, -1.0)
                            .setMax(+1.0, +1.0, +1.0)
                            .transform(it.transform!!.globalTransform)
                            .intersectionVolume(aabb)
                        volume >= minVolume
                    } else true
                    if (isOk) it.transform!!.distanceSquaredGlobally(pos)
                    else Double.POSITIVE_INFINITY
                }
            } else null
            val bakedSkyBox = (map?.texture ?: pipeline.bakedSkyBox)?.getTexture0() ?: blackCube
            // todo bug: mipmaps are not updating automatically :/
            bakedSkyBox.bind(
                envMapSlot,
                GPUFiltering.TRULY_NEAREST,
                Clamping.CLAMP
            ) // clamping doesn't really apply here
        }
    }

    fun setupLights(pipeline: Pipeline, shader: Shader, aabb: AABBd, receiveShadows: Boolean) {

        setupPlanarReflection(pipeline, shader, aabb)
        setupReflectionMap(pipeline, shader, aabb)

        val time = Engine.gameTime
        val numberOfLightsPtr = shader["numberOfLights"]
        if (numberOfLightsPtr >= 0) {
            val maxNumberOfLights = RenderView.MAX_FORWARD_LIGHTS
            val lights = pipeline.lights
            val numberOfLights = pipeline.getClosestRelevantNLights(aabb, maxNumberOfLights, lights)
            shader.v1i(numberOfLightsPtr, numberOfLights)
            shader.v1b("receiveShadows", receiveShadows)
            if (numberOfLights > 0) {
                val invLightMatrices = shader["invLightMatrices"]
                val buffer = buffer16x256
                if (invLightMatrices >= 0) {
                    // fill all transforms
                    buffer.limit(12 * numberOfLights)
                    for (i in 0 until numberOfLights) {
                        buffer.position(12 * i)
                        val light = lights[i]!!.light
                        light.invCamSpaceMatrix.putInto(buffer)
                    }
                    buffer.position(0)
                    shader.m4x3Array(invLightMatrices, buffer)
                }
                // and sharpness; implementation depending on type
                val lightIntensities = shader["lightData0"]
                if (lightIntensities >= 0) {
                    // fill all light colors
                    buffer.limit(4 * numberOfLights)
                    for (i in 0 until numberOfLights) {
                        val light = lights[i]!!.light
                        val color = light.color
                        buffer.put(color.x)
                        buffer.put(color.y)
                        buffer.put(color.z)
                        val type = when (light) {
                            is DirectionalLight -> LightType.DIRECTIONAL.id
                            is PointLight -> LightType.POINT.id
                            is SpotLight -> LightType.SPOT.id
                            else -> -1
                        }
                        buffer.put(type + 0.25f)
                    }
                    buffer.position(0)
                    shader.v4Array(lightIntensities, buffer)
                }
                // type, and cone angle (or other data, if required)
                // additional, whether we have a texture, and maybe other data
                val lightTypes = shader["lightData1"]
                if (lightTypes >= 0) {

                    buffer.limit(4 * numberOfLights)
                    val cameraPosition = RenderState.cameraPosition
                    val worldScale = RenderState.worldScale
                    for (i in 0 until numberOfLights) {

                        val lightI = lights[i]!!
                        val light = lightI.light
                        val m = lightI.transform.getDrawMatrix(time)

                        buffer.put(((m.m30 - cameraPosition.x) * worldScale).toFloat())
                        buffer.put(((m.m31 - cameraPosition.y) * worldScale).toFloat())
                        buffer.put(((m.m32 - cameraPosition.z) * worldScale).toFloat())
                        buffer.put(light.getShaderV0(m, worldScale))

                    }
                    buffer.flip()
                    shader.v4Array(lightTypes, buffer)
                }
                val shadowData = shader["shadowData"]
                if (shadowData >= 0) {
                    buffer.limit(4 * numberOfLights)
                    // write all texture indices, and bind all shadow textures (as long as we have slots available)
                    var planarSlot = 0
                    var cubicSlot = 0
                    val maxTextureIndex = 31
                    val planarIndex0 = shader.getTextureIndex("shadowMapPlanar0")
                    val cubicIndex0 = shader.getTextureIndex("shadowMapCubic0")
                    val supportsPlanarShadows = planarIndex0 >= 0
                    val supportsCubicShadows = cubicIndex0 >= 0
                    if (planarIndex0 < 0) planarSlot = Renderers.MAX_PLANAR_LIGHTS
                    if (cubicIndex0 < 0) cubicSlot = Renderers.MAX_CUBEMAP_LIGHTS
                    if (supportsPlanarShadows || supportsCubicShadows) {
                        for (i in 0 until numberOfLights) {
                            buffer.position(4 * i)
                            val light = lights[i]!!.light
                            buffer.put(0f)
                            buffer.put(0f)
                            buffer.put(light.getShaderV1())
                            buffer.put(light.getShaderV2())
                            buffer.position(4 * i)
                            if (light.hasShadow) {
                                if (light is PointLight) {
                                    buffer.put(cubicSlot.toFloat()) // start index
                                    if (cubicSlot < Renderers.MAX_CUBEMAP_LIGHTS) {
                                        val cascades = light.shadowTextures ?: continue
                                        val slot = cubicIndex0 + cubicSlot
                                        if (slot > maxTextureIndex) continue
                                        val texture = cascades[0].depthTexture!!
                                        // bind the texture, and don't you dare to use mipmapping ^^
                                        // (at least without variance shadow maps)
                                        texture.bind(slot, GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
                                        cubicSlot++ // no break necessary
                                    }
                                    buffer.put(cubicSlot.toFloat()) // end index
                                } else {
                                    buffer.put(planarSlot.toFloat()) // start index
                                    if (planarSlot < Renderers.MAX_PLANAR_LIGHTS) {
                                        val cascades = light.shadowTextures ?: continue
                                        for (j in cascades.indices) {
                                            val slot = planarIndex0 + planarSlot
                                            if (slot > maxTextureIndex) break
                                            val texture = cascades[j].depthTexture!!
                                            // bind the texture, and don't you dare to use mipmapping ^^
                                            // (at least without variance shadow maps)
                                            texture.bind(slot, GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
                                            if (++planarSlot >= Renderers.MAX_PLANAR_LIGHTS) break
                                        }
                                    }
                                    buffer.put(planarSlot.toFloat()) // end index
                                }
                            }
                        }
                        // bind the other textures to avoid undefined behaviour, even if we don't use them
                        for (i in planarSlot until Renderers.MAX_PLANAR_LIGHTS) {
                            TextureLib.depthTexture.bindTrulyNearest(planarIndex0 + i)
                        }
                        for (i in cubicSlot until Renderers.MAX_CUBEMAP_LIGHTS) {
                            TextureLib.depthCube.bindTrulyNearest(cubicIndex0 + i)
                        }
                    }
                    buffer.position(0)
                    shader.v4Array(shadowData, buffer)
                }

            }
        }

    }

    fun initShader(shader: Shader, pipeline: Pipeline) {
        // information for the shader, which is material agnostic
        // add all things, the shader needs to know, e.g., light direction, strength, ...
        // (for the cheap shaders, which are not deferred)
        shader.m4x4("transform", RenderState.cameraMatrix)
        shader.m4x4("prevTransform", RenderState.prevCameraMatrix)
        shader.v3f("ambientLight", pipeline.ambient)
        shader.v1b("applyToneMapping", pipeline.applyToneMapping)
    }

    fun DrawRequest.revDistance(dir: Vector3d): Double {
        val w = entity.transform.globalTransform
        return dir.dot(w.m30, w.m31, w.m32)
    }

    @Suppress("unused")
    fun drawColors(pipeline: Pipeline) {

        // the dotViewDir may be easier to calculate, and technically more correct, but it has one major flaw:
        // it changes when the cameraDirection is changing. This ofc is not ok, since it would resort the entire list,
        // and that's expensive

        // to do sorting function, that also uses the materials, so we need to switch seldom?
        // to do and light groups, so we don't need to update lights that often

        // val viewDir = pipeline.frustum.cameraRotation.transform(Vector3d(0.0, 0.0, 1.0))
        drawRequests.size = nextInsertIndex
        val dir = RenderState.cameraDirection
        if (!Input.isKeyDown('l')) when (sorting) {
            Sorting.NO_SORTING -> {
            }
            Sorting.FRONT_TO_BACK -> {
                drawRequests.sortWith { a, b ->
                    val ma = a.revDistance(dir)
                    val mb = b.revDistance(dir)
                    mb.compareTo(ma)
                }
            }
            Sorting.BACK_TO_FRONT -> {
                drawRequests.sortWith { a, b ->
                    val ma = a.revDistance(dir)
                    val mb = b.revDistance(dir)
                    ma.compareTo(mb)
                }
            }
        }

        var lastEntity: Entity? = null
        var lastMesh: Mesh? = null
        var lastShader: Shader? = null
        var lastComp: Component? = null
        var drawnPrimitives = 0L
        var drawCalls = 0L

        val time = Engine.gameTime

        // we could theoretically cluster them to need fewer uploads
        // but that would probably be hard to implement reliably
        val hasLights = maxNumberOfLights > 0
        val needsLightUpdateForEveryMesh =
            ((hasLights && pipeline.lightStage.size > maxNumberOfLights) ||
                    pipeline.lightStage.environmentMaps.isNotEmpty())
        var lastReceiveShadows = false

        pipeline.lights.fill(null)

        // draw non-instanced meshes
        var previousMaterialInScene: Material? = null
        val oqp = occlusionQueryPrepass
        for (index in 0 until nextInsertIndex) {

            val request = drawRequests[index]
            val renderer = request.component
            // todo support this for MeshSpawner (oc in general) and instanced rendering (oqp) as well?
            val oc = (renderer as? MeshComponentBase)?.occlusionQuery
            if (oc != null && oqp && !oc.wasVisible) continue

            GFX.drawnId = request.clickId

            val hasAnimation = (renderer as? MeshComponentBase)?.hasAnimation ?: false
            GFXState.animated.use(hasAnimation) {

                val mesh = request.mesh
                val entity = request.entity

                val transform = entity.transform
                val materialIndex = request.materialIndex
                val material = getMaterial(renderer, mesh, materialIndex)

                oc?.start()

                val shader = getShader(material)
                shader.use()
                bindRandomness(shader)

                val previousMaterialByShader = lastMaterial.put(shader, material)
                if (previousMaterialByShader == null) {
                    initShader(shader, pipeline)
                }

                val receiveShadows = if (renderer is MeshComponentBase) renderer.receiveShadows else true
                if (hasLights) {
                    if (previousMaterialByShader == null ||
                        needsLightUpdateForEveryMesh ||
                        receiveShadows != lastReceiveShadows
                    ) {
                        // upload all light data
                        val aabb = tmpAABBd.set(mesh.getBounds()).transform(transform.getDrawMatrix())
                        setupLights(pipeline, shader, aabb, receiveShadows)
                        lastReceiveShadows = receiveShadows
                    }
                }

                setupLocalTransform(shader, transform, time)

                // the state depends on textures (global) and uniforms (per shader),
                // so test both
                if (previousMaterialByShader != material || previousMaterialInScene != material) {
                    // bind textures for the material
                    // bind all default properties, e.g. colors, roughness, metallic, clear coat/sheen, ...
                    material.bind(shader)
                    previousMaterialInScene = material
                }

                mesh.ensureBuffer()

                // only if the entity or mesh changed
                // not if the material has changed
                // this updates the skeleton and such
                if (entity !== lastEntity ||
                    lastMesh !== mesh ||
                    lastShader !== shader ||
                    lastComp?.javaClass != renderer.javaClass
                ) {
                    val hasAnim = if (renderer is MeshComponentBase && mesh.hasBonesInBuffer)
                        renderer.defineVertexTransform(shader, entity, mesh)
                    else false
                    shader.v1b("hasAnimation", hasAnim)
                    lastEntity = entity
                    lastMesh = mesh
                    lastShader = shader
                    lastComp = renderer
                }

                shaderColor(shader, "tint", -1)
                shader.v1i("hasVertexColors", mesh.hasVertexColors)
                val component = request.component
                shader.v2i(
                    "randomIdData",
                    if (mesh.proceduralLength > 0) 3 else 0,
                    if (component is MeshComponentBase) component.randomTriangleId else 0
                )

                GFXState.cullMode.use(mesh.cullMode * material.cullMode * cullMode) {
                    mesh.draw(shader, materialIndex)
                }

                oc?.stop()

                drawnPrimitives += mesh.numPrimitives
                drawCalls++

            }
        }

        lastMaterial.clear()

        // instanced rendering of all kinds
        for (i in instances.indices) {
            val (dt, dc) = instances[i].draw(pipeline, this, needsLightUpdateForEveryMesh, time, false)
            drawnPrimitives += dt
            drawCalls += dc
        }

        lastMaterial.clear()

        Companion.drawnPrimitives += drawnPrimitives
        Companion.drawCalls += drawCalls

    }

    fun bindRandomness(shader: Shader) {
        val renderer = GFXState.currentRenderer
        val deferred = renderer.deferredSettings
        val target = GFXState.currentBuffer
        if (deferred != null && target is Framebuffer) {
            // define all randomnesses: depends on framebuffer
            // and needs to be set for all shaders
            val layers = deferred.layers2
            for (index in layers.indices) {
                val layer = layers[index]
                val m: Float
                val n: Float
                when (layer.type.internalFormat) {
                    GL_R8, GL_RG8, GL_RGB8, GL_RGBA8 -> {
                        m = 0f
                        n = 1f / ((1L shl 8) - 1f)
                    }
                    GL_R16, GL_RG16, GL_RGB16, GL_RGBA16 -> {
                        m = 0f
                        n = 1f / ((1L shl 16) - 1f)
                    }
                    GL_R32I, GL_RG32I, GL_RGB32I, GL_RGBA32I -> {
                        m = 0f
                        n = 1f / ((1L shl 32) - 1f)
                    }
                    GL_R16F, GL_RG16F, GL_RGB16F, GL_RGBA16F -> {
                        m = 1f / 2048f // 11 bits of mantissa
                        n = 0f
                    }
                    // m != 0, but random rounding cannot be computed with single precision
                    // GL_R32F, GL_RG32F, GL_RGB32F, GL_RGBA32F
                    else -> {
                        m = 0f
                        n = 0f
                    }
                }
                shader.v2f(layer.nameRR, m, n)
            }
        }
    }

    /**
     * drawing only the depth of a scene;
     * for light-shadows or pre-depth
     * */
    fun drawDepths(pipeline: Pipeline) {

        var lastEntity: Entity? = null
        var lastMesh: Mesh? = null

        var drawnPrimitives = 0L
        var drawCalls = 0L
        val time = Engine.gameTime

        val shader = defaultShader.value
        shader.use()

        initShader(shader, pipeline)

        // draw non-instanced meshes
        val cullMode = cullMode
        for (index in 0 until nextInsertIndex) {

            val request = drawRequests[index]
            val renderer = request.component

            val oc = (renderer as? MeshComponentBase)?.occlusionQuery
            oc?.start()

            val mesh = request.mesh
            val entity = request.entity
            val materialIndex = request.materialIndex
            val material = getMaterial(request.component, mesh, materialIndex)

            val transform = entity.transform

            setupLocalTransform(shader, transform, time)

            mesh.ensureBuffer()

            // only if the entity or mesh changed
            // not if the material has changed
            // this updates the skeleton and such
            if (entity !== lastEntity || lastMesh !== mesh) {
                val hasAnim = if (renderer is MeshComponentBase && mesh.hasBonesInBuffer)
                    renderer.defineVertexTransform(shader, entity, mesh)
                else false
                shader.v1b("hasAnimation", hasAnim)
                lastEntity = entity
                lastMesh = mesh
            }

            shaderColor(shader, "tint", -1)
            shader.v1i("hasVertexColors", mesh.hasVertexColors)

            GFXState.cullMode.use(mesh.cullMode * material.cullMode * cullMode) {
                mesh.drawDepth(shader)
            }

            oc?.stop()

            drawnPrimitives += mesh.numPrimitives
            drawCalls++

        }

        GFX.check()

        // draw instanced meshes
        for (i in instances.indices) {
            val (dt, dc) = instances[i].draw(pipeline, this, false, time, true)
            drawnPrimitives += dt
            drawCalls += dc
        }

        GFX.check()

        Companion.drawnPrimitives += drawnPrimitives
        Companion.drawCalls += drawCalls

    }

    private var hadTooMuchSpace = 0
    fun clear() {

        // there is has been much space for many iterations
        if (nextInsertIndex < drawRequests.size shr 1) {
            if (hadTooMuchSpace++ > 100) {
                drawRequests.resize(nextInsertIndex)
            }
        } else hadTooMuchSpace = 0

        nextInsertIndex = 0

        for (i in instances.indices) {
            instances[i].clear()
        }

    }

    fun add(component: Component, mesh: Mesh, entity: Entity, materialIndex: Int, clickId: Int) {
        val nextInsertIndex = nextInsertIndex++
        if (nextInsertIndex >= drawRequests.size) {
            drawRequests.add(DrawRequest(mesh, component, entity, materialIndex, clickId))
        } else {
            val request = drawRequests[nextInsertIndex]
            request.mesh = mesh
            request.component = component
            request.entity = entity
            request.materialIndex = materialIndex
            request.clickId = clickId
        }
    }

    fun addInstanced(
        mesh: Mesh,
        component: MeshComponentBase?,
        entity: Entity,
        material: Material,
        materialIndex: Int,
        clickId: Int
    ) = addInstanced(mesh, component, entity.transform, material, materialIndex, clickId)

    fun addInstanced(
        mesh: Mesh,
        component: MeshComponentBase?,
        transform: Transform,
        material: Material,
        materialIndex: Int,
        clickId: Int
    ) {
        val stack = instancedWithIdx.getOrPut(mesh, material, materialIndex) { mesh1, _, _ ->
            if (mesh1.hasBones) InstancedAnimStack() else InstancedStack()
        }
        addToStack(stack, component, transform, clickId)
    }

    fun addToStack(stack: InstancedStack, component: MeshComponentBase?, transform: Transform, clickId: Int) {
        if (stack is InstancedAnimStack && component is AnimRenderer) {
            if (component.updateAnimState()) {
                val texture = component.getAnimTexture()
                stack.add(
                    transform, clickId, texture,
                    component.prevWeights, component.prevIndices,
                    component.currWeights, component.currIndices
                )
            } else stack.add(transform, clickId)
        } else stack.add(transform, clickId)
    }

    @Suppress("unused")
    fun getMaterial(mesh: Mesh, index: Int): Material {
        return MaterialCache[mesh.materials.getOrNull(index), defaultMaterial]
    }

    fun getMaterial(renderer: Component, mesh: Mesh, index: Int): Material {
        var material = if (renderer is MeshComponentBase) renderer.materials.getOrNull(index) else null
        material = material ?: mesh.materials.getOrNull(index)
        return MaterialCache[material, defaultMaterial]
    }

    fun getShader(material: Material): Shader {
        return (material.shader ?: defaultShader).value
    }

    fun clone() =
        PipelineStage(name, sorting, maxNumberOfLights, blendMode, depthMode, writeDepth, cullMode, defaultShader)

    override val className: String get() = "PipelineStage"
    override val approxSize get() = 5
    override fun isDefaultValue(): Boolean = false

}