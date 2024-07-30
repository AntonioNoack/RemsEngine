package me.anno.gpu.pipeline

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Transform
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.light.PlanarReflection
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.Renderers
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.DitherMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.M4x3Delta.buffer16x256
import me.anno.gpu.M4x3Delta.m4x3delta
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.BufferUsage
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureHelper
import me.anno.gpu.texture.TextureLib
import me.anno.gpu.texture.TextureLib.blackCube
import me.anno.input.Input
import me.anno.maths.Maths.fract
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.all2
import me.anno.utils.types.Matrices.set4x3Delta
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Vector3d
import org.lwjgl.opengl.GL46C.GL_BYTE
import org.lwjgl.opengl.GL46C.GL_HALF_FLOAT
import org.lwjgl.opengl.GL46C.GL_INT
import org.lwjgl.opengl.GL46C.GL_SHORT
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_INT
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_SHORT

class PipelineStageImpl(
    var name: String,
    var sorting: Sorting,
    var maxNumberOfLights: Int,
    var blendMode: BlendMode?,
    var depthMode: DepthMode,
    var writeDepth: Boolean,
    var cullMode: CullMode,
    var defaultShader: BaseShader
) {

    companion object {

        val OPAQUE_PASS = PipelineStage.OPAQUE
        val TRANSPARENT_PASS = PipelineStage.TRANSPARENT
        val DECAL_PASS = PipelineStage.DECAL

        var drawnPrimitives = 0L
        var drawnInstances = 0L
        var drawCalls = 0L
        var drawCallId = 0

        val lastMaterial = HashMap<Shader, Material>(64)
        private val tmp4x3 = Matrix4x3f()

        // 16k is ~ 20% better than 1024: 9 fps instead of 7 fps with 150k instanced lights on my RX 580
        const val instancedBatchSize = 1024 * 16

        val instancedBuffer = StaticBuffer(
            "instanced", listOf(
                Attribute("instanceTrans0", 4),
                Attribute("instanceTrans1", 4),
                Attribute("instanceTrans2", 4),
                Attribute("instanceFinalId", AttributeType.UINT8_NORM, 4)
            ), instancedBatchSize, BufferUsage.DYNAMIC
        )

        val instancedBufferA = StaticBuffer(
            "instancedA", listOf(
                Attribute("instanceTrans0", 4),
                Attribute("instanceTrans1", 4),
                Attribute("instanceTrans2", 4),
                Attribute("animWeights", 4),
                Attribute("animIndices", 4),
                Attribute("instanceFinalId", AttributeType.UINT8_NORM, 4)
            ), instancedBatchSize, BufferUsage.DYNAMIC
        )

        val instancedBufferM = StaticBuffer(
            "instancedM", listOf(
                Attribute("instanceTrans0", 4),
                Attribute("instanceTrans1", 4),
                Attribute("instanceTrans2", 4),
                Attribute("instancePrevTrans0", 4),
                Attribute("instancePrevTrans1", 4),
                Attribute("instancePrevTrans2", 4),
                Attribute("instanceFinalId", AttributeType.UINT8_NORM, 4)
            ), instancedBatchSize, BufferUsage.DYNAMIC
        )

        val instancedBufferMA = StaticBuffer(
            "instancedMA", listOf(
                Attribute("instanceTrans0", 4),
                Attribute("instanceTrans1", 4),
                Attribute("instanceTrans2", 4),
                Attribute("instancePrevTrans0", 4),
                Attribute("instancePrevTrans1", 4),
                Attribute("instancePrevTrans2", 4),
                // todo somehow add instance-prefix
                Attribute("animWeights", 4),
                Attribute("animIndices", 4),
                Attribute("prevAnimWeights", 4),
                Attribute("prevAnimIndices", 4),
                Attribute("instanceFinalId", AttributeType.UINT8_NORM, 4)
            ), instancedBatchSize, BufferUsage.DYNAMIC
        )

        val instancedBufferSlim = StaticBuffer(
            "instancedSlim", listOf(
                Attribute("instancePosSize", 4),
                Attribute("instanceRot", 4),
            ),
            instancedBatchSize * 2, BufferUsage.DYNAMIC
        )

        val instancedBufferI32 = StaticBuffer(
            "instancedI32", listOf(
                Attribute("instanceI32", AttributeType.SINT32, 1, true)
            ), instancedBatchSize * 16, BufferUsage.DYNAMIC
        )

        val tmpAABBd = AABBd()

        fun setupLocalTransform(
            shader: GPUShader,
            transform: Transform?,
            time: Long
        ) {
            if (transform != null) {
                val localTransform = transform.getDrawMatrix(time)
                tmp4x3.set4x3Delta(localTransform)
                shader.m4x3("localTransform", tmp4x3)

                if (shader.hasUniform("invLocalTransform")) {
                    shader.m4x3("invLocalTransform", tmp4x3.invert())
                }

                shader.v1f("worldScale", RenderState.worldScale)

                if (shader.hasUniform("prevLocalTransform")) {
                    val prevWorldScale = RenderState.prevWorldScale
                    shader.m4x3delta(
                        "prevLocalTransform", transform.getDrawnMatrix(time),
                        RenderState.prevCameraPosition, prevWorldScale
                    )
                    shader.v1f("prevWorldScale", prevWorldScale)
                }
            } else {
                val localTransform = JomlPools.mat4x3d.create().identity()
                setupLocalTransform(shader, localTransform)
                JomlPools.mat4x3d.sub(1)
            }
        }

        fun setupLocalTransform(shader: GPUShader, transform: Matrix4x3d) {
            shader.m4x3("localTransform", tmp4x3.set4x3Delta(transform))
            shader.v1f("worldScale", RenderState.worldScale)
            if (shader.hasUniform("prevLocalTransform")) {
                val prevWorldScale = RenderState.prevWorldScale
                shader.m4x3delta(
                    "prevLocalTransform", transform,
                    RenderState.prevCameraPosition, prevWorldScale
                )
                shader.v1f("prevWorldScale", prevWorldScale)
            }
            if (shader.hasUniform("invLocalTransform")) {
                shader.m4x3("invLocalTransform", tmp4x3.invert())
            }
        }

        fun initShader(shader: GPUShader, applyToneMapping: Boolean) {
            // information for the shader, which is material agnostic
            // add all things, the shader needs to know, e.g., light direction, strength, ...
            // (for the cheap shaders, which are not deferred)
            shader.m4x4("transform", RenderState.cameraMatrix)
            shader.m4x4("prevTransform", RenderState.prevCameraMatrix)
            shader.v1b("applyToneMapping", applyToneMapping)
            shader.v1f("worldScale", RenderState.worldScale)
            shader.v3f("cameraPosition", RenderState.cameraPosition)
            shader.v4f("cameraRotation", RenderState.cameraRotation)
            shader.v1b("reverseDepth", GFX.supportsClipControl)
        }

        fun bindRandomness(shader: GPUShader) {
            val renderer = GFXState.currentRenderer
            val deferred = renderer.deferredSettings
            val target = GFXState.currentBuffer
            if (deferred != null && target is Framebuffer) {
                shader.v1f("defRRT", fract(Time.gameTime))
                // define all randomnesses: depends on framebuffer
                // and needs to be set for all shaders
                val layers = deferred.storageLayers
                for (index in layers.indices) {
                    val layer = layers[index]
                    val m: Float // (1+m)*x+n
                    val n: Float
                    when (TextureHelper.getNumberType(layer.type.internalFormat)) {
                        GL_UNSIGNED_BYTE.inv() -> {
                            m = 0f
                            n = 1f / ((1L shl 8) - 1f)
                        }
                        GL_BYTE.inv() -> {
                            m = 0f
                            n = 1f / ((1L shl 7) - 1f)
                        }
                        GL_UNSIGNED_SHORT.inv() -> {
                            m = 0f
                            n = 1f / ((1L shl 16) - 1f)
                        }
                        GL_SHORT.inv() -> {
                            m = 0f
                            n = 1f / ((1L shl 15) - 1f)
                        }
                        GL_UNSIGNED_INT.inv() -> {
                            m = 0f
                            n = 1f / ((1L shl 32) - 1f)
                        }
                        GL_INT.inv() -> {
                            m = 0f
                            n = 1f / ((1L shl 31) - 1f)
                        }
                        GL_HALF_FLOAT -> {
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

        fun setupPlanarReflection(pipeline: Pipeline, shader: GPUShader, aabb: AABBd) {

            shader.v4f("reflectionCullingPlane", pipeline.reflectionCullingPlane)
            shader.v2f("renderSize", GFXState.currentBuffer.width.toFloat(), GFXState.currentBuffer.height.toFloat())

            val ti = shader.getTextureIndex("reflectionPlane")
            if (ti < 0 || pipeline.planarReflections.isEmpty()) {
                shader.v1b("hasReflectionPlane", false)
                return
            }

            val minVolume = 0.5 * aabb.volume
            val pos = JomlPools.vec3d.borrow().set(aabb.centerX, aabb.centerY, aabb.centerZ)
            val mapBounds = JomlPools.aabbd.borrow()
            val bestPr = if (minVolume.isFinite()) {
                var candidates: Collection<PlanarReflection> = pipeline.planarReflections.filter {
                    // todo check if reflection can be visible
                    // doubleSided || it.transform!!.getDrawMatrix().transformDirection(0,0,1).dot(camDirection) > camPos.dot(camDir) (?)
                    val buffer = it.framebuffer
                    buffer != null && buffer.pointer != 0
                }
                if (minVolume > 1e-308) candidates = candidates.filter {
                    // todo use projected 2d comparison instead
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
                val tex = bestPr.framebuffer!!
                tex.getTexture0().bind(ti, Filtering.LINEAR, Clamping.CLAMP)
                val normal = bestPr.globalNormal
                shader.v3f("reflectionPlaneNormal", normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat())
            }
        }

        fun setupReflectionMap(pipeline: Pipeline, shader: GPUShader, aabb: AABBd) {
            val envMapSlot = shader.getTextureIndex("reflectionMap")
            if (envMapSlot >= 0) {
                // find the closest environment map
                val minVolume = 0.5 * aabb.volume
                val pos = JomlPools.vec3d.borrow().set(aabb.centerX, aabb.centerY, aabb.centerZ)
                val mapBounds = JomlPools.aabbd.borrow()
                val map = if (minVolume.isFinite()) {
                    pipeline.lightStage.environmentMaps.minByOrNull {
                        val transform = it.transform!!
                        val isOk = if (minVolume > 1e-308) {
                            // only if environment map fills >= 50% of the AABB
                            val volume = mapBounds
                                .setMin(-1.0, -1.0, -1.0)
                                .setMax(+1.0, +1.0, +1.0)
                                .transform(transform.globalTransform)
                                .intersectionVolume(aabb)
                            volume >= minVolume
                        } else true
                        if (isOk) transform.distanceSquaredGlobally(pos)
                        else Double.POSITIVE_INFINITY
                    }
                } else null
                var mapTexture = map?.texture
                if (mapTexture?.isCreated != true) mapTexture = null
                mapTexture = mapTexture ?: pipeline.bakedSkybox
                if (mapTexture?.isCreated != true) mapTexture = null
                val bakedSkyBox = mapTexture?.getTexture0() ?: blackCube
                bakedSkyBox.bind(
                    envMapSlot,
                    Filtering.LINEAR,
                    Clamping.CLAMP
                ) // clamping doesn't really apply here
            }
        }

        fun setupLights(pipeline: Pipeline, shader: GPUShader, aabb: AABBd, receiveShadows: Boolean) {

            setupPlanarReflection(pipeline, shader, aabb)
            setupReflectionMap(pipeline, shader, aabb)

            val numberOfLightsPtr = shader["numberOfLights"]
            if (numberOfLightsPtr >= 0) {
                val maxNumberOfLights = RenderView.MAX_FORWARD_LIGHTS
                val lights = pipeline.lights
                val numberOfLights = pipeline.getClosestRelevantNLights(aabb, maxNumberOfLights, lights)
                shader.v1i(numberOfLightsPtr, numberOfLights)
                shader.v1b("receiveShadows", receiveShadows)
                if (numberOfLights > 0) {
                    val buffer = buffer16x256
                    val invLightMatrices = shader["invLightMatrices"]
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
                            buffer.put(light.lightType.id + 0.25f)
                        }
                        buffer.position(0)
                        shader.v4fs(lightIntensities, buffer)
                    }
                    // type, and cone angle (or other data, if required)
                    // additional, whether we have a texture, and maybe other data
                    val lightTypes = shader["lightData1"]
                    if (lightTypes >= 0) {
                        buffer.limit(numberOfLights)
                        for (i in 0 until numberOfLights) {
                            val light = lights[i]!!.light
                            buffer.put(light.getShaderV0())
                        }
                        buffer.flip()
                        shader.v1fs(lightTypes, buffer)
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
                                            val cascades = light.shadowTextures
                                            val slot = cubicIndex0 + cubicSlot
                                            if (slot <= maxTextureIndex && cascades != null) {
                                                val texture = cascades.depthTexture ?: cascades.getTexture0()
                                                if (texture.isCreated()) {
                                                    // bind the texture, and don't you dare to use mipmapping ^^
                                                    // (at least without variance shadow maps)
                                                    texture.bind(slot, Filtering.TRULY_LINEAR, Clamping.CLAMP)
                                                    cubicSlot++ // no break necessary
                                                }
                                            }
                                        }
                                        buffer.put(cubicSlot.toFloat()) // end index
                                    } else {
                                        buffer.put(planarSlot.toFloat()) // start index
                                        if (planarSlot < Renderers.MAX_PLANAR_LIGHTS) {
                                            val cascades = light.shadowTextures
                                            val slot = planarIndex0 + planarSlot
                                            if (slot <= maxTextureIndex && cascades != null) {
                                                val texture = cascades.depthTexture ?: cascades.getTexture0()
                                                if (texture.isCreated()) {
                                                    // bind the texture, and don't you dare to use mipmapping ^^
                                                    // (at least without variance shadow maps)
                                                    texture.bind(slot, Filtering.TRULY_LINEAR, Clamping.CLAMP)
                                                    planarSlot++
                                                }
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
                        shader.v4fs(shadowData, buffer)
                    }
                }
            }
        }
    }

    var nextInsertIndex = 0
    val drawRequests = ResetArrayList<DrawRequest>()

    class ResetArrayList<V>(cap: Int = 16) {

        var size = 0

        private val content = ArrayList<V>(cap)

        fun resize(newSize: Int) {
            if (newSize > content.size) {
                content.subList(newSize, content.size).clear()
                content.trimToSize()
            }
        }

        fun add(element: V) {
            if (size >= content.size) content.add(element)
            else content[size] = element
            size++
        }

        operator fun get(index: Int): V = content[index]
        fun sortWith(comp: Comparator<V>) {
            content.subList(0, size).sortWith(comp)
        }
    }

    // doesn't work yet, why ever
    var occlusionQueryPrepass = false

    fun isEmpty(): Boolean {
        return nextInsertIndex == 0 &&
                instances.all2 { it.isEmpty() }
    }

    val instanced = InstancedStack.Impl()
    val instancedTRS = InstancedTRSStack()
    val instancedStatic = InstancedStaticStack()

    @Suppress("RemoveExplicitTypeArguments")
    val instances = arrayListOf<DrawableStack>(
        instanced,
        instancedTRS,
        instancedStatic,
        // if you need extra types, e.g., for MeshSpawner, just add them :)
        // (a helper function for that (and finding it again) might be added in the future)
    )

    fun bindDraw(pipeline: Pipeline, drawn: PipelineStageImpl = this) {
        bind { drawn.draw(pipeline) }
    }

    fun bind(draw: () -> Unit) {
        val blendMode = if (GFXState.ditherMode.currentValue == DitherMode.DITHER2X2) null
        else this.blendMode
        GFXState.blendMode.use(blendMode) {
            GFXState.depthMode.use(depthMode) {
                GFXState.depthMask.use(writeDepth) {
                    GFXState.cullMode.use(cullMode) {
                        GFX.check()
                        draw()
                        GFX.check()
                    }
                }
            }
        }
    }

    fun DrawRequest.getZDistance(dir: Vector3d): Double {
        val w = transform.globalTransform
        return dir.dot(w.m30, w.m31, w.m32)
    }

    var lastTransform: Transform? = null
    var lastMesh: IMesh? = null
    var lastShader: Shader? = null
    var lastComp: Component? = null
    var lastReceiveShadows = false
    var previousMaterialInScene: Material? = null
    var hasLights = false
    var needsLightUpdateForEveryMesh = false

    fun draw(pipeline: Pipeline) {

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
                    val ma = a.getZDistance(dir)
                    val mb = b.getZDistance(dir)
                    mb.compareTo(ma)
                }
            }
            Sorting.BACK_TO_FRONT -> {
                drawRequests.sortWith { a, b ->
                    val ma = a.getZDistance(dir)
                    val mb = b.getZDistance(dir)
                    ma.compareTo(mb)
                }
            }
        }

        var drawnPrimitives = 0L
        var drawnInstances = 0L
        var drawCalls = 0L

        val time = Time.gameTimeN

        // we could theoretically cluster them to need fewer uploads
        // but that would probably be hard to implement reliably
        hasLights = maxNumberOfLights > 0
        needsLightUpdateForEveryMesh =
            ((hasLights && pipeline.lightStage.size > maxNumberOfLights) ||
                    pipeline.lightStage.environmentMaps.isNotEmpty() ||
                    pipeline.planarReflections.size > 1)

        pipeline.lights.fill(null)

        // draw non-instanced meshes
        for (index in 0 until nextInsertIndex) {
            val request = drawRequests[index]
            draw(
                pipeline,
                request.transform,
                request.component,
                request.material,
                request.materialIndex,
                request.mesh
            )
        }

        clearLastElements()

        // instanced rendering of all kinds
        for (i in instances.indices) {
            val (dt, di, dc) = instances[i].draw0(pipeline, this, needsLightUpdateForEveryMesh, time, false)
            drawnPrimitives += dt
            drawnInstances += di
            drawCalls += dc
        }

        clearLastElements()

        Companion.drawnPrimitives += drawnPrimitives
        Companion.drawnInstances += drawnInstances
        Companion.drawCalls += drawCalls
    }

    private fun clearLastElements() {
        // clear this to not prevent potential GC
        lastTransform = null
        lastMesh = null
        lastShader = null
        lastComp = null
        lastReceiveShadows = false
        previousMaterialInScene = null
        lastMaterial.clear()
    }

    fun draw(
        pipeline: Pipeline,
        transform: Transform,
        renderer: Component,
        material: Material,
        materialIndex: Int,
        mesh: IMesh
    ) {

        val oqp = occlusionQueryPrepass
        // todo support this for MeshSpawner (oc in general) and instanced rendering (oqp) as well?
        val oc = (renderer as? MeshComponentBase)?.occlusionQuery
        if (oc != null && oqp && !oc.wasVisible) return

        val hasAnimation = (renderer as? MeshComponentBase)?.hasAnimation ?: false
        GFXState.animated.use(hasAnimation) {
            GFXState.vertexData.use(mesh.vertexData) {

                val ocq = oc?.start()

                val shader = getShader(material)
                shader.use()
                bindRandomness(shader)

                val previousMaterialByShader = lastMaterial.put(shader, material)
                if (previousMaterialByShader == null) {
                    initShader(shader, pipeline.applyToneMapping)
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

                setupLocalTransform(shader, transform, Time.gameTimeN)

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
                if (lastTransform !== transform ||
                    lastMesh !== mesh ||
                    lastShader !== shader ||
                    lastComp == null ||
                    lastComp!!::class != renderer::class
                ) {
                    val hasAnim = if (renderer is MeshComponentBase && mesh.hasBonesInBuffer)
                        renderer.defineVertexTransform(shader, transform, mesh)
                    else false
                    shader.v1b("hasAnimation", hasAnim)
                    lastTransform = transform
                    lastMesh = mesh
                    lastShader = shader
                    lastComp = renderer
                }

                shader.v4f("tint", 1f)
                shader.v1i("hasVertexColors", if (material.enableVertexColors) mesh.hasVertexColors else 0)
                val renderMode = RenderView.currentInstance?.renderMode
                val finalId = if (renderMode == RenderMode.DRAW_CALL_ID) drawCallId++ else renderer.gfxId
                shader.v4f("finalId", finalId)
                shader.v2i(
                    "randomIdData",
                    if (mesh.proceduralLength > 0) 3 else 0,
                    if (renderer is MeshComponentBase) renderer.randomTriangleId else 0
                )

                GFXState.cullMode.use(mesh.cullMode * material.cullMode * cullMode) {
                    mesh.draw(pipeline, shader, materialIndex, Mesh.drawDebugLines)
                }

                oc?.stop(ocq!!)

                drawnPrimitives += mesh.numPrimitives
                drawnInstances++
                drawCalls++
            }
        }
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

    fun add(component: Component, mesh: IMesh, transform: Transform, material: Material, materialIndex: Int) {
        val nextInsertIndex = nextInsertIndex++
        if (nextInsertIndex >= drawRequests.size) {
            drawRequests.add(DrawRequest(mesh, component, transform, material, materialIndex))
        } else {
            val request = drawRequests[nextInsertIndex]
            request.mesh = mesh
            request.component = component
            request.transform = transform
            request.material = material
            request.materialIndex = materialIndex
        }
    }

    fun addInstanced(
        mesh: IMesh, component: Component, transform: Transform,
        material: Material, materialIndex: Int
    ) {
        val stack = instanced.data.getOrPut(mesh, material, materialIndex) { mesh1, _, _ ->
            if (mesh1.hasBonesInBuffer) InstancedAnimStack() else InstancedStack()
        }
        addToStack(stack, component, transform)
    }

    fun addToStack(stack: InstancedStack, component: Component, transform: Transform) {
        if (stack is InstancedAnimStack &&
            component is AnimMeshComponent &&
            component.updateAnimState()
        ) {
            val texture = component.getAnimTexture()
            stack.add(
                transform, component.gfxId, texture,
                component.prevWeights, component.prevIndices,
                component.currWeights, component.currIndices
            )
        } else stack.add(transform, component.gfxId)
    }

    fun getShader(material: Material): Shader {
        return (material.shader ?: defaultShader).value
    }

    fun clone() =
        PipelineStageImpl(name, sorting, maxNumberOfLights, blendMode, depthMode, writeDepth, cullMode, defaultShader)
}