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
import me.anno.engine.ui.render.ECSMeshShaderLight.Companion.canUseLightShader
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShaderLight
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
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureHelper
import me.anno.gpu.texture.TextureLib
import me.anno.gpu.texture.TextureLib.blackCube
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.maths.Maths.fract
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.all2
import me.anno.utils.structures.lists.Lists.firstOrNull2
import me.anno.utils.structures.lists.ResetArrayList
import me.anno.utils.structures.maps.KeyTripleMap
import me.anno.utils.types.Matrices.set4x3Delta
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Matrix4x3f
import org.lwjgl.opengl.GL46C.GL_HALF_FLOAT
import kotlin.math.min
import kotlin.reflect.KClass

class PipelineStageImpl(
    var name: String,
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

        val tmpAABBd = AABBd()

        fun KeyTripleMap<IMesh, Material, Int, InstancedStack>.getStack(
            mesh: IMesh, material: Material, matIndex: Int
        ): InstancedStack {
            return getOrPut(mesh, material, matIndex, stackCreator)
        }

        private val stackCreator = { meshI: IMesh, _: Material, _: Int ->
            if (meshI.hasBonesInBuffer) {
                InstancedStack.newAnimStack()
            } else {
                InstancedStack.newInstStack()
            }
        }

        fun bindTransformUniforms(shader: GPUShader, transform: Transform?) {
            if (transform != null) {
                val localTransform = transform.getDrawMatrix()
                tmp4x3.set4x3Delta(localTransform)
                shader.m4x3("localTransform", tmp4x3)

                if (shader.hasUniform("invLocalTransform")) {
                    shader.m4x3("invLocalTransform", tmp4x3.invert())
                }

                if (shader.hasUniform("prevLocalTransform")) {
                    shader.m4x3delta(
                        "prevLocalTransform", transform.getDrawnMatrix(),
                        RenderState.prevCameraPosition
                    )
                }
            } else {
                val localTransform = JomlPools.mat4x3m.create().identity()
                bindTransformUniforms(shader, localTransform)
                JomlPools.mat4x3m.sub(1)
            }
        }

        fun bindTransformUniforms(shader: GPUShader, transform: Matrix4x3) {
            shader.m4x3("localTransform", tmp4x3.set4x3Delta(transform))
            if (shader.hasUniform("prevLocalTransform")) {
                shader.m4x3delta("prevLocalTransform", transform, RenderState.prevCameraPosition)
            }
            if (shader.hasUniform("invLocalTransform")) {
                shader.m4x3("invLocalTransform", tmp4x3.invert())
            }
        }

        fun bindCameraUniforms(shader: GPUShader, applyToneMapping: Boolean) {
            // information for the shader, which is material agnostic
            // add all things, the shader needs to know, e.g., light direction, strength, ...
            // (for the cheap shaders, which are not deferred)
            shader.m4x4("transform", RenderState.cameraMatrix)
            shader.m4x4("prevTransform", RenderState.prevCameraMatrix)
            shader.v1b("applyToneMapping", applyToneMapping)
            shader.v3f("cameraPosition", RenderState.cameraPosition)
            shader.v4f("cameraRotation", RenderState.cameraRotation)
            shader.v1b("reverseDepth", GFXState.depthMode.currentValue.reversedDepth)
        }

        fun bindJitterUniforms(shader: GPUShader) {
            val renderer = GFXState.currentRenderer
            val deferred = renderer.deferredSettings
            val target = GFXState.currentBuffer
            if (deferred != null) {
                shader.v1f("defRRT", fract(Time.gameTime))
                // define all randomnesses: depends on framebuffer
                // and needs to be set for all shaders
                val layers = deferred.storageLayers
                for (index in 0 until min(layers.size, target.numTextures)) {
                    val layer = layers[index]
                    val m: Float // (1+m)*x+n
                    val n: Float
                    val format = target.getTextureIMS(index).internalFormat
                    val numIntBits = TextureHelper.getUnsignedIntBits(format)
                    if (numIntBits != 0) {
                        // it's an integer-based format
                        m = 0f
                        n = 1f / ((1L shl numIntBits) - 1f)
                    } else {
                        val numberType = TextureHelper.getNumberType(format)
                        if (numberType == GL_HALF_FLOAT) {
                            m = 1f / 2048f // 11 bits of mantissa
                            n = 0f
                        } else {
                            // m != 0, but random rounding cannot be computed with single precision
                            // GL_FLOAT
                            m = 0f
                            n = 0f
                        }
                    }
                    shader.v2f(layer.nameRR, m, n)
                }
            }
        }

        private var lastTransform: Transform? = null
        private var lastMesh: IMesh? = null
        private var lastShader: Shader? = null
        private var lastComp: Component? = null

        private fun Any?.getClass(): KClass<*>? {
            return if (this == null) null else this::class
        }

        fun bindSkeletalUniforms(transform: Transform, shader: Shader, mesh: IMesh, renderer: Component) {
            if (lastTransform !== transform ||
                lastMesh !== mesh ||
                lastShader !== shader ||
                lastComp.getClass() != renderer::class
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
        }

        fun bindUtilityUniforms(
            shader: Shader, material: Material, mesh: IMesh,
            renderer: Component
        ) {
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
        }

        fun bindPlanarReflectionUniforms(pipeline: Pipeline, shader: GPUShader, aabb: AABBd) {

            shader.v4f("reflectionCullingPlane", pipeline.reflectionCullingPlane)
            shader.v2f("renderSize", GFXState.currentBuffer.width.toFloat(), GFXState.currentBuffer.height.toFloat())

            val ti = shader.getTextureIndex("reflectionPlane")
            if (ti < 0 || pipeline.planarReflections.isEmpty()) {
                shader.v1b("hasReflectionPlane", false)
                if (ti >= 0) whiteTexture.bindTrulyNearest(ti)
                return
            }

            val minVolume = 0.5 * aabb.volume
            val pos = JomlPools.vec3d.borrow().set(aabb.centerX, aabb.centerY, aabb.centerZ)
            val mapBounds = JomlPools.aabbd.borrow()
            val bestPtr = if (minVolume.isFinite()) {
                var candidates: Collection<PlanarReflection> = pipeline.planarReflections.filter {
                    // todo check if reflection can be visible
                    // doubleSided || it.transform!!.getDrawMatrix().transformDirection(0,0,1).dot(camDirection) > camPos.dot(camDir) (?)
                    val buffer = it.framebuffer
                    buffer != null && buffer.isCreated()
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
            } else pipeline.planarReflections
                .firstOrNull2 { it.framebuffer?.isCreated() == true }

            shader.v1b("hasReflectionPlane", bestPtr != null)
            if (bestPtr != null) {
                val tex = bestPtr.framebuffer!!
                tex.getTexture0().bind(ti, Filtering.LINEAR, Clamping.CLAMP)
                val normal = bestPtr.globalNormal
                shader.v3f("reflectionPlaneNormal", normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat())
            }
        }

        fun bindReflectionMapUniforms(pipeline: Pipeline, shader: GPUShader, aabb: AABBd) {
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

        fun bindLightUniforms(pipeline: Pipeline, shader: GPUShader, aabb: AABBd, receiveShadows: Boolean) {

            bindPlanarReflectionUniforms(pipeline, shader, aabb)
            bindReflectionMapUniforms(pipeline, shader, aabb)

            val numberOfLightsPtr = shader["numberOfLights"]
            if (numberOfLightsPtr >= 0) {
                val maxNumberOfLights = RenderView.MAX_FORWARD_LIGHTS
                val lights = pipeline.lights
                val numberOfLights = pipeline.getClosestRelevantNLights(aabb, maxNumberOfLights, lights)
                shader.v1i(numberOfLightsPtr, numberOfLights)
                shader.v1b("receiveShadows", receiveShadows)
                shader.v1b("canHaveShadows", true)
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
                        buffer.limit(4 * numberOfLights)
                        for (i in 0 until numberOfLights) {
                            val light = lights[i]!!.light
                            buffer
                                .put(light.getShaderV0())
                                .put(light.getShaderV1())
                                .put(light.getShaderV2())
                                .put(light.getShaderV3())
                        }
                        buffer.flip()
                        shader.v4fs(lightTypes, buffer)
                    }
                    val shadowData = shader["lightData2"]
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
                                buffer.put(0f).put(0f).put(0f).put(0f)
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

    // doesn't work yet, why ever
    var occlusionQueryPrepass = false

    fun isEmpty(): Boolean {
        return nextInsertIndex == 0 &&
                instances.all2 { it.isEmpty() }
    }

    val instanced = InstancedStackImpl()
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
        val blendMode = if (GFXState.ditherMode.currentValue == DitherMode.DITHER2X2) null else blendMode
        GFXState.blendMode.use(blendMode) {
            GFXState.depthMode.use(depthMode) {
                GFXState.depthMask.use(writeDepth) {
                    GFXState.cullMode.use(cullMode) {
                        GFXState.drawLines.use(Mesh.drawDebugLines) {
                            GFX.check()
                            draw()
                            GFX.check()
                        }
                    }
                }
            }
        }
    }

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
            val (drawPrimitivesI, drawInstancesI, drawCallsI) =
                instances[i].draw0(pipeline, this, needsLightUpdateForEveryMesh, time, false)
            drawnPrimitives += drawPrimitivesI
            drawnInstances += drawInstancesI
            drawCalls += drawCallsI
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

        val hasAnimation = (renderer as? MeshComponentBase)?.hasAnimation(true, mesh) ?: false
        GFXState.animated.use(hasAnimation) {
            GFXState.vertexData.use(mesh.vertexData) {

                oc?.start()

                val shader = getShader(material)
                shader.use()
                bindJitterUniforms(shader)

                val previousMaterialByShader = lastMaterial.put(shader, material)
                if (previousMaterialByShader == null) {
                    bindCameraUniforms(shader, pipeline.applyToneMapping)
                }

                val receiveShadows = if (renderer is MeshComponentBase) renderer.receiveShadows else true
                if (hasLights) {
                    if (previousMaterialByShader == null ||
                        needsLightUpdateForEveryMesh ||
                        receiveShadows != lastReceiveShadows
                    ) {
                        // upload all light data
                        val aabb = tmpAABBd.set(mesh.getBounds()).transform(transform.getDrawMatrix())
                        bindLightUniforms(pipeline, shader, aabb, receiveShadows)
                        lastReceiveShadows = receiveShadows
                    }
                }

                bindTransformUniforms(shader, transform)

                // the state depends on textures (global) and uniforms (per shader),
                // so test both
                if (previousMaterialByShader != material || previousMaterialInScene != material) {
                    // bind textures for the material
                    // bind all default properties, e.g. colors, roughness, metallic, clear coat/sheen, ...
                    material.bind(shader)
                    previousMaterialInScene = material
                }

                mesh.ensureBuffer()

                bindSkeletalUniforms(transform, shader, mesh, renderer)
                bindUtilityUniforms(shader, material, mesh, renderer)

                val cullMode = mesh.cullMode * material.cullMode * cullMode
                GFXState.cullMode.use(cullMode) {
                    mesh.draw(pipeline, shader, materialIndex, Mesh.drawDebugLines)
                }

                oc?.stop()

                drawnPrimitives += mesh.numPrimitives
                drawnInstances++
                drawCalls++
            }
        }
    }

    private var hadTooMuchSpace = 0
    fun clear() {

        // there has been much space for many iterations
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
        val stack = instanced.data.getStack(mesh, material, materialIndex)
        addToStack(stack, component, transform, mesh)
    }

    fun addToStack(stack: InstancedStack, component: Component, transform: Transform, mesh: IMesh) {
        if (stack is InstancedAnimStack && component is AnimMeshComponent && component.updateAnimState()) {
            val texture = component.getAnimTexture(mesh)
            stack.add(
                transform, component.gfxId, texture,
                component.prevWeights, component.prevIndices,
                component.currWeights, component.currIndices
            )
        } else stack.add(transform, component.gfxId)
    }

    fun getShader(material: Material): Shader {
        val shader0 = material.shader
        if (shader0 != null) return shader0.value
        val shader1 = if (defaultShader == pbrModelShader && material.canUseLightShader()) {
            pbrModelShaderLight
        } else defaultShader
        return shader1.value
    }

    fun clone() =
        PipelineStageImpl(name, maxNumberOfLights, blendMode, depthMode, writeDepth, cullMode, defaultShader)
}