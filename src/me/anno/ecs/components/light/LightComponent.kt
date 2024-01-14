package me.anno.ecs.components.light

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.DepthMode
import me.anno.gpu.DitherMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.*
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2DArray
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.maths.Maths.SQRT3
import me.anno.maths.Maths.max
import me.anno.utils.pooling.JomlPools
import org.joml.*
import kotlin.math.abs
import kotlin.math.pow

abstract class LightComponent(val lightType: LightType) : LightComponentBase() {

    // todo AES lights, and their textures?

    // todo plane of light: how? https://eheitzresearch.wordpress.com/415-2/ maybe :)
    // todo lines of light: how?
    // todo circle/sphere of light: how?

    // black lamp light?
    @Type("Color3HDR")
    @SerializedProperty
    var color = Vector3f(1f)

    @Range(0.0, 16.0)
    var shadowMapCascades = 0
        set(value) {
            if (field != value) {
                field = value
                entity?.invalidateUpdates()
                if (value < 1) {
                    ensureShadowBuffers()
                }
            }
        }

    // typical: 2..4
    @Range(1.0, 1000.0)
    @SerializedProperty
    var shadowMapPower = 4.0
    var shadowMapResolution = 1024
        set(value) {
            field = max(1, value)
        }

    val hasShadow get() = shadowMapCascades > 0

    var needsUpdate1 = true
    var autoUpdate = true

    @HideInInspector
    @NotSerializedProperty
    var shadowTextures: IFramebuffer? = null

    @SerializedProperty
    var depthFunc = DepthMode.CLOSE

    @NotSerializedProperty
    var rootOverride: PrefabSaveable? = null

    @SerializedProperty
    var ditherMode = DitherMode.DITHER2X2

    override fun fill(
        pipeline: Pipeline,
        entity: Entity,
        clickId: Int
    ): Int {
        // todo if(entity == pipeline.sampleEntity) add floor/setup, so we can see the light
        pipeline.addLight(this, entity)
        return super.fill(pipeline, entity, clickId)
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        getLightPrimitive().getBounds().transformUnion(globalTransform, aabb)
        return true
    }

    open fun invalidateShadows() {
        needsUpdate1 = true
    }

    override fun onDrawGUI(all: Boolean) {
        drawShape()
    }

    abstract fun drawShape()

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
                val targets = if (GFX.supportsDepthTextures) emptyArray() else arrayOf(TargetType.Float16x1)
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
        }
    }

    override fun onVisibleUpdate(): Boolean {
        return if (hasShadow) {
            if (autoUpdate || needsUpdate1) {
                needsUpdate1 = false
                ensureShadowBuffers()
                if (hasShadow) {
                    updateShadowMaps()
                }
            }
            true
        } else false
    }

    abstract fun updateShadowMap(
        cascadeScale: Double, worldScale: Double,
        dstCameraMatrix: Matrix4f,
        dstCameraPosition: Vector3d,
        cameraRotation: Quaterniond, cameraDirection: Vector3d,
        drawTransform: Matrix4x3d, pipeline: Pipeline,
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
        val worldScale = SQRT3 / global.getScaleLength()
        val direction = rotation.transform(RenderState.cameraDirection.set(0.0, 0.0, -1.0))
        // val originalWorldScale = RenderState.worldScale // test frustum
        RenderState.worldScale = worldScale
        val result = shadowTextures as FramebufferArray
        val shadowMapPower = shadowMapPower
        // only fill pipeline once? probably better...
        val renderer = Renderer.nothingRenderer
        val tmpPos = JomlPools.vec3d.create().set(position)
        GFXState.depthMode.use(DepthMode.CLOSE) {
            GFXState.ditherMode.use(ditherMode) {
                result.draw(renderer) { i ->
                    if (i > 0) { // reset position and rotation
                        position.set(tmpPos)
                    }
                    pipeline.clear()
                    val cascadeScale = shadowMapPower.pow(-i.toDouble())
                    updateShadowMap(
                        cascadeScale, worldScale,
                        RenderState.cameraMatrix,
                        position, rotation, direction,
                        drawTransform, pipeline, resolution
                    )
                    /* test frustum, breaks cascades though (because cameraMatrix isn't reset)
                        RenderState.cameraPosition.set(originalPosition)
                        RenderState.worldScale = originalWorldScale
                        pipeline.frustum.showPlanes()
                        RenderState.worldScale = worldScale
                        position.set(tmpPos)
                    */
                    val isPerspective = abs(RenderState.cameraMatrix.m33) < 0.5f
                    RenderState.calculateDirections(isPerspective)
                    val root = rootOverride ?: entity.getRoot(Entity::class)
                    pipeline.fill(root)
                    result.clearColor(0, depth = true)
                    pipeline.drawWithoutSky(false)
                }
            }
        }
        JomlPools.vec3d.sub(1)
    }

    /**
     * is set by the pipeline,
     * is equal to invert((transform.globalMatrix - cameraPosition) * worldScale)
     * */
    @NotSerializedProperty
    val invCamSpaceMatrix = Matrix4x3f()

    open fun getShaderV0(): Float = 0f
    open fun getShaderV1(): Float = 0f
    open fun getShaderV2(): Float = 0f

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as LightComponent
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
        val pipeline by lazy {
            Pipeline(DeferredSettings(listOf())).apply {
                defaultStage.maxNumberOfLights = 0
                // all stages are the same
                for (i in 0 until 15) stages.add(defaultStage)
            }
        }
    }
}