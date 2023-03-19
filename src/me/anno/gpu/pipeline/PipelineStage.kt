package me.anno.gpu.pipeline

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.anim.AnimRenderer
import me.anno.ecs.components.cache.MaterialCache
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.LightType
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.light.SpotLight
import me.anno.ecs.components.mesh.Material
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
import me.anno.io.Saveable
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Matrices.set2
import org.joml.AABBd
import org.joml.Matrix4x3f
import org.joml.Vector3d
import org.lwjgl.opengl.GL30C.*

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
                Attribute("prevInstanceTrans0", 3),
                Attribute("prevInstanceTrans1", 3),
                Attribute("prevInstanceTrans2", 3),
                Attribute("prevInstanceTrans3", 3),
            ), instancedBatchSize, GL_DYNAMIC_DRAW
        )

        val instancedBufferMA = StaticBuffer(
            listOf(
                Attribute("instanceTrans0", 3),
                Attribute("instanceTrans1", 3),
                Attribute("instanceTrans2", 3),
                Attribute("instanceTrans3", 3),
                Attribute("prevInstanceTrans0", 3),
                Attribute("prevInstanceTrans1", 3),
                Attribute("prevInstanceTrans2", 3),
                Attribute("prevInstanceTrans3", 3),
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
                Attribute("instanceI32", AttributeType.SINT32, 1)
            ), instancedBatchSize * 16, GL_DYNAMIC_DRAW
        )

        val tmpAABBd = AABBd()

        fun setupLocalTransform(
            shader: Shader,
            transform: Transform,
            time: Long
        ) {

            val localTransform = transform.getDrawMatrix(time)
            shader.m4x3delta("localTransform", localTransform)
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

            val invLocalUniform = shader["invLocalTransform"]
            if (invLocalUniform >= 0) {
                val invLocal = tmp4x3.set2(localTransform).invert()
                shader.m4x3(invLocalUniform, invLocal)
            }

        }

    }

    var nextInsertIndex = 0
    var instancedSize = 0
    val drawRequests = ArrayList<DrawRequest>()

    val size get() = nextInsertIndex + instancedSize

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

    fun setupLights(pipeline: Pipeline, shader: Shader, request: DrawRequest, receiveShadows: Boolean) =
        setupLights(pipeline, shader, request.entity.aabb, receiveShadows)

    @Suppress("unused_parameter")
    fun setupPlanarReflection(pipeline: Pipeline, shader: Shader, aabb: AABBd) {

        shader.v4f("reflectionCullingPlane", pipeline.reflectionCullingPlane)

        val ti = shader.getTextureIndex("reflectionPlane")
        if (ti < 0) {
            shader.v1b("hasReflectionPlane", false)
            return
        }
        val pr = pipeline.planarReflections
        val bestPr = pr.filter {
            val lb = it.lastBuffer as Texture2D?
            lb != null && lb.pointer != 0
        }.minByOrNull {
            // todo find the by-angle-and-position best matching planar reflection
            // todo don't choose a planar reflection, that is invisible from the camera
            // it.globalNormal.dot(target.direction)
            0f
        }
        shader.v1b("hasReflectionPlane", bestPr != null)
        if (bestPr != null) {
            val tex = bestPr.lastBuffer!!
            tex.bindTrulyNearest(ti)
            val normal = bestPr.globalNormal
            shader.v3f("reflectionPlaneNormal", normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat())
        }
    }

    fun setupReflectionMap(pipeline: Pipeline, shader: Shader, aabb: AABBd) {
        val envMapSlot = shader.getTextureIndex("reflectionMap")
        if (envMapSlot >= 0) {
            // find closest environment map
            // todo should respect size more...
            val pos = JomlPools.vec3d.borrow().set(aabb.avgX(), aabb.avgY(), aabb.avgZ())
            val map = pipeline.lightStage.environmentMaps.minByOrNull {
                it.transform!!.distanceSquaredGlobally(pos)
            }
            val bakedSkyBox = (map?.texture ?: pipeline.bakedSkyBox)?.getTexture0() ?: blackCube
            bakedSkyBox.bindTrulyNearest(envMapSlot)
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
                        light.invWorldMatrix.putInto(buffer)
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
        shader.m4x4("prevTransform", RenderState.prevCamMatrix)
        shader.v3f("ambientLight", pipeline.ambient)
        shader.v1b("applyToneMapping", pipeline.applyToneMapping)
    }

    fun DrawRequest.distanceTo(cameraPosition: Vector3d): Double {
        // - it.entity.transform.dotViewDir(cameraPosition, viewDir)
        return entity.transform.distanceSquaredGlobally(cameraPosition)
    }

    @Suppress("unused")
    fun drawColors(pipeline: Pipeline) {

        // the dotViewDir may be easier to calculate, and technically more correct, but it has one major flaw:
        // it changes when the cameraDirection is changing. This ofc is not ok, since it would resort the entire list,
        // and that's expensive

        // to do sorting function, that also uses the materials, so we need to switch seldom?
        // to do and light groups, so we don't need to update lights that often

        // val viewDir = pipeline.frustum.cameraRotation.transform(Vector3d(0.0, 0.0, 1.0))
        val cameraPosition = RenderState.cameraPosition
        when (sorting) {
            Sorting.NO_SORTING -> {
            }
            Sorting.FRONT_TO_BACK -> {
                drawRequests.sortWith { a, b ->
                    val ma = a.distanceTo(cameraPosition)
                    val mb = b.distanceTo(cameraPosition)
                    ma.compareTo(mb)
                }
            }
            Sorting.BACK_TO_FRONT -> {
                drawRequests.sortWith { a, b ->
                    val ma = a.distanceTo(cameraPosition)
                    val mb = b.distanceTo(cameraPosition)
                    mb.compareTo(ma)
                }
            }
        }

        var lastEntity: Entity? = null
        var lastMesh: Mesh? = null
        var lastShader: Shader? = null
        var drawnPrimitives = 0L
        var drawCalls = 0L

        val time = Engine.gameTime

        // we could theoretically cluster them to need fewer uploads
        // but that would probably be hard to implement reliably
        val hasLights = maxNumberOfLights > 0
        val needsLightUpdateForEveryMesh = hasLights &&
                pipeline.lightStage.size > maxNumberOfLights
        var lastReceiveShadows = false

        pipeline.lights.fill(null)

        // draw non-instanced meshes
        var previousMaterialInScene: Material? = null
        for (index in 0 until nextInsertIndex) {

            val request = drawRequests[index]

            GFX.drawnId = request.clickId

            val hasAnimation = (request.component as? MeshComponentBase)?.hasAnimation ?: false
            GFXState.animated.use(hasAnimation) {

                val mesh = request.mesh
                val entity = request.entity

                val transform = entity.transform
                val renderer = request.component
                val materialIndex = request.materialIndex
                val material = getMaterial(renderer, mesh, materialIndex)

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
                        setupLights(pipeline, shader, request, receiveShadows)
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
                if (entity !== lastEntity || lastMesh !== mesh || lastShader !== shader) {
                    val hasAnim = if (renderer is MeshComponentBase && mesh.hasBonesInBuffer)
                        renderer.defineVertexTransform(shader, entity, mesh)
                    else false
                    shader.v1b("hasAnimation", hasAnim)
                    lastEntity = entity
                    lastMesh = mesh
                    lastShader = shader
                }

                shaderColor(shader, "tint", -1)
                shader.v1i("hasVertexColors", mesh.hasVertexColors)
                val component = request.component
                shader.v2i(
                    "randomIdData",
                    if (mesh.proceduralLength > 0) 3 else 0,
                    if (component is MeshComponentBase) component.randomTriangleId else 0
                )

                if (material.isDoubleSided) {
                    GFXState.cullMode.use(CullMode.BOTH) {
                        mesh.draw(shader, materialIndex)
                    }
                } else {
                    mesh.draw(shader, materialIndex)
                }

                drawnPrimitives += mesh.numPrimitives
                drawCalls++

            }
        }

        lastMaterial.clear()

        // instanced rendering of all kinds
        for (inst in instances) {
            val (dt, dc) = inst.draw(pipeline, this, needsLightUpdateForEveryMesh, time, false)
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
        for (index in 0 until nextInsertIndex) {

            val request = drawRequests[index]
            val mesh = request.mesh
            val entity = request.entity
            val materialIndex = request.materialIndex
            val material = getMaterial(request.component, mesh, materialIndex)

            val transform = entity.transform
            val renderer = request.component

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

            if (material.isDoubleSided) {
                GFXState.cullMode.use(CullMode.BOTH) {
                    mesh.drawDepth(shader)
                }
            } else {
                mesh.drawDepth(shader)
            }

            drawnPrimitives += mesh.numPrimitives
            drawCalls++

        }

        GFX.check()

        // draw instanced meshes
        for (inst in instances) {
            val (dt, dc) = inst.draw(pipeline, this, false, time, true)
            drawnPrimitives += dt
            drawCalls += dc
        }

        GFX.check()

        Companion.drawnPrimitives += drawnPrimitives
        Companion.drawCalls += drawCalls

    }

    private var hadTooMuchSpace = 0
    fun clear() {

        // there is too much space since 100 iterations
        if (nextInsertIndex < drawRequests.size shr 1) {
            if (hadTooMuchSpace++ > 100) {
                drawRequests.clear()
            }
        } else hadTooMuchSpace = 0

        nextInsertIndex = 0
        instancedSize = 0

        for (inst in instances) {
            inst.clear()
        }

    }

    fun add(component: Component, mesh: Mesh, entity: Entity, materialIndex: Int, clickId: Int) {
        if (nextInsertIndex >= drawRequests.size) {
            val request = DrawRequest(mesh, component, entity, materialIndex, clickId)
            drawRequests.add(request)
        } else {
            val request = drawRequests[nextInsertIndex]
            request.mesh = mesh
            request.component = component
            request.entity = entity
            request.materialIndex = materialIndex
            request.clickId = clickId
        }
        nextInsertIndex++
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
        instancedSize++
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

    override val className get() = "PipelineStage"
    override val approxSize get() = 5
    override fun isDefaultValue(): Boolean = false

}