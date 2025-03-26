package me.anno.ecs.components.light

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.Renderers.rawAttributeRenderers
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.CubemapFramebuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FramebufferArray
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.query.GPUClockNanos
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2DArray
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths.max
import me.anno.mesh.Shapes
import me.anno.utils.InternalAPI
import me.anno.utils.pooling.JomlPools
import org.joml.AABBd
import org.joml.Matrix4f
import org.joml.Matrix4x3
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.pow

abstract class LightComponent(val lightType: LightType) : LightComponentBase(), OnDrawGUI {

    // todo AES lights, and their textures?
    // todo light beams: when inside the cone, from that view, then add a little brightness
    // todo concept of static meshes, and optionally only redraw dynamic meshes onto shadow maps

    // black lamp light?
    @Docs("sRGB Color")
    @Type("Color3HDR")
    @SerializedProperty
    var color = Vector3f(1f)

    @Range(0.0, 16.0)
    var shadowMapCascades = 0
        set(value) {
            if (field != value) {
                field = value
                if (value < 1) {
                    ensureShadowBuffers()
                }
            }
        }

    // typical: 2..4
    @Range(1.0, 1000.0)
    @SerializedProperty
    var shadowMapPower = 4f
    var shadowMapResolution = 1024
        set(value) {
            field = max(1, value)
        }

    val hasShadow get() = shadowMapCascades > 0

    @HideInInspector
    @NotSerializedProperty
    var shadowTextures: IFramebuffer? = null

    @NotSerializedProperty
    var timer: GPUClockNanos? = null

    @SerializedProperty
    var depthFunc = if (GFX.supportsClipControl) DepthMode.CLOSE
    else DepthMode.FORWARD_CLOSE

    @NotSerializedProperty
    var rootOverride: PrefabSaveable? = null

    override fun fill(pipeline: Pipeline, transform: Transform) {
        // add shape for testing, so the light is visible
        if (entity == Pipeline.sampleEntity) {
            pipeline.addMesh(shapeForTesting, this, transform)
        }
        pipeline.addLight(this, transform)
        return super.fill(pipeline, transform)
    }

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd): Boolean {
        getLightPrimitive().getBounds().transformUnion(globalTransform, dstUnion)
        return true
    }

    open fun invalidateShadows() {
        needsUpdate1 = true
    }

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        drawShape(pipeline)
    }

    abstract fun drawShape(pipeline: Pipeline)

    abstract fun getLightPrimitive(): Mesh

    fun ensureShadowBuffers() {
        if (hasShadow) {
            // only a single one is supported for PointLights,
            // because more makes no sense
            val isPointLight = lightType == LightType.POINT
            if (isPointLight) shadowMapCascades = 1
            val shadowTextures = shadowTextures
            val targetSize = shadowMapCascades
            val resolution = shadowMapResolution
            if (timer == null) timer = GPUClockNanos()
            if (shadowTextures == null ||
                (shadowTextures is Texture2DArray && shadowTextures.layers != targetSize) ||
                shadowTextures.depthTexture?.depthFunc != depthFunc
            ) {
                shadowTextures?.destroy()
                // we currently use a depth bias of 0.005,
                // which is equal to ~ 1/255,
                // so an 8 bit depth buffer would be enough
                val depthBufferType =
                    if (GFX.supportsDepthTextures) DepthBufferType.TEXTURE_16 else DepthBufferType.INTERNAL
                val targets = if (GFX.supportsDepthTextures) emptyList() else listOf(TargetType.Float16x1)
                this.shadowTextures = if (lightType.shadowMapType == GLSLType.SCubeShadow) {
                    CubemapFramebuffer(
                        "ShadowCubemap", resolution, 1, targets, depthBufferType
                    ).apply {
                        ensure()
                        val depthTexture = depthTexture
                        if (depthTexture != null) {
                            depthTexture.filtering = Filtering.TRULY_LINEAR
                            depthTexture.depthFunc = depthFunc
                        }
                    }
                } else {
                    FramebufferArray(
                        "Shadow", resolution, resolution, targetSize, 1, targets, depthBufferType
                    ).apply {
                        ensure()
                        val depthTexture = depthTexture
                        if (depthTexture != null) {
                            depthTexture.filtering = Filtering.TRULY_LINEAR
                            depthTexture.depthFunc = depthFunc
                        }
                    }
                }
            }
        } else {
            shadowTextures?.destroy()
            shadowTextures = null
            timer?.destroy()
        }
    }

    override fun onVisibleUpdate() {
        if (hasShadow) {
            if (needsAutoUpdate() || needsUpdate1) {
                needsUpdate1 = false
                ensureShadowBuffers()
                if (hasShadow) {
                    updateShadowMaps()
                }
            }
        }
    }

    abstract fun updateShadowMap(
        cascadeScale: Float,
        dstCameraMatrix: Matrix4f,
        dstCameraPosition: Vector3d,
        cameraRotation: Quaternionf, cameraDirection: Vector3f,
        drawTransform: Matrix4x3, pipeline: Pipeline,
        resolution: Int,
    )

    open fun updateShadowMaps() {

        lastDrawn = Time.gameTimeN

        val pipeline = pipeline
        val entity = entity!!
        entity.validateTransform()

        val transform = entity.transform
        val drawTransform = transform.getDrawMatrix()
        val resolution = shadowMapResolution
        val global = transform.globalTransform
        // val originalPosition = Vector3d(RenderState.cameraPosition) // test frustum
        val position = global.getTranslation(RenderState.cameraPosition)
        val rotation = global.getUnnormalizedRotation(RenderState.cameraRotation)
        val direction = rotation.transform(RenderState.cameraDirection.set(0.0, 0.0, -1.0))
        // val originalWorldScale = RenderState.worldScale // test frustum
        val result = shadowTextures as FramebufferArray
        val shadowMapPower = shadowMapPower
        // only fill pipeline once? probably better...
        val tmpPos = JomlPools.vec3d.create().set(position)
        timeRendering(className, timer) {
            GFXState.depthMode.use(pipeline.defaultStage.depthMode) {
                GFXState.ditherMode.use(ditherMode) {
                    result.draw(renderer) { i ->
                        // reset position
                        position.set(tmpPos)
                        pipeline.clear()
                        val cascadeScale = shadowMapPower.pow(-i)
                        updateShadowMap(
                            cascadeScale,
                            RenderState.cameraMatrix,
                            position, rotation, direction,
                            drawTransform, pipeline, resolution
                        )
                        val isPerspective = abs(RenderState.cameraMatrix.m33) < 0.5f
                        RenderState.calculateDirections(isPerspective)
                        val root = rootOverride ?: entity.getRoot(Entity::class)
                        pipeline.fill(root)
                        // decals and transparent objects are irrelevant for shadows
                        pipeline.stages.getOrNull(PipelineStage.DECAL.id)?.clear()
                        pipeline.stages.getOrNull(PipelineStage.TRANSPARENT.id)?.clear()
                        result.clearColor(0, depth = true)
                        pipeline.singlePassWithoutSky()
                    }
                }
            }
        }
        JomlPools.vec3d.sub(1)
    }

    /**
     * is set by the pipeline,
     * is equal to invert((transform.globalMatrix - cameraPosition))
     * */
    @NotSerializedProperty
    val invCamSpaceMatrix = Matrix4x3f()

    open fun getShaderV0(): Float = 0f
    open fun getShaderV1(): Float = 0f
    open fun getShaderV2(): Float = 0f
    open fun getShaderV3(): Float = 0f

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is LightComponent) return
        dst.shadowMapCascades = shadowMapCascades
        dst.shadowMapPower = shadowMapPower
        dst.shadowMapResolution = shadowMapResolution
        dst.color = color
        dst.needsUpdate1 = true
        dst.autoUpdate = autoUpdate
    }

    override fun destroy() {
        super.destroy()
        shadowTextures?.destroy()
        shadowTextures = null
    }

    companion object {

        @JvmStatic
        @InternalAPI
        val renderer = rawAttributeRenderers[DeferredLayerType.DEPTH]

        @JvmStatic
        @InternalAPI
        val shapeForTesting by lazy {
            val mesh = Shapes.flatCube.linear(Vector3f(0f, 0f, 0.4f), Vector3f(0.5f)).back
            // make one side metallic for testing
            mesh.materialIds = IntArray(mesh.positions!!.size / 9) { it.and(1) }
            mesh.numMaterials = 2
            val metallic = Material()
            metallic.metallicMinMax.set(1f)
            metallic.roughnessMinMax.set(0.01f)
            mesh.materials = listOf(InvalidRef, metallic.ref)
            mesh
        }

        @JvmStatic
        @InternalAPI
        val pipeline by lazy {
            Pipeline(DeferredSettings(listOf())).apply {
                defaultStage.maxNumberOfLights = 0
            }
        }
    }
}