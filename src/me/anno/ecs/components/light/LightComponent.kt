package me.anno.ecs.components.light

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.CubemapFramebuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.maths.Maths.SQRT3
import me.anno.maths.Maths.max
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
        val mesh = getLightPrimitive()
        mesh.getBounds()
        mesh.aabb.transformUnion(globalTransform, aabb)
        return true
    }

    open fun invalidateShadows() {
        needsUpdate1 = true
    }

    override fun onDrawGUI(all: Boolean) {
        if (all) drawShape()
    }

    abstract fun drawShape()

    abstract fun getLightPrimitive(): Mesh

    @HideInInspector
    @NotSerializedProperty
    var shadowTextures: Array<IFramebuffer>? = null

    var depthFunc = DepthMode.CLOSER

    fun ensureShadowBuffers() {
        if (hasShadow) {
            // only a single one is supported for PointLights,
            // because more makes no sense
            val isPointLight = lightType == LightType.POINT
            if (isPointLight) shadowMapCascades = 1
            val shadowCascades = shadowTextures
            val targetSize = shadowMapCascades
            val resolution = shadowMapResolution
            if (shadowCascades == null || shadowCascades.size != targetSize ||
                (shadowCascades.firstOrNull()?.depthTexture as? Texture2D)?.depthFunc != depthFunc
            ) {
                if (shadowCascades != null) for (i in shadowCascades.indices) {
                    shadowCascades[i].destroy()
                }
                // we currently use a depth bias of 0.005,
                // which is equal to ~ 1/255,
                // so an 8 bit depth buffer would be enough
                val depthBufferType = DepthBufferType.TEXTURE_16
                this.shadowTextures = Array(targetSize) {
                    if (lightType.shadowMapType == GLSLType.SCubeShadow) {
                        CubemapFramebuffer(
                            "ShadowCubemap[$it]", resolution, 1, 0,
                            false, depthBufferType
                        ).apply {
                            ensure()
                            depthTexture!!.filtering = GPUFiltering.TRULY_LINEAR
                            depthTexture!!.depthFunc = depthFunc
                        }
                    } else {
                        Framebuffer(
                            "Shadow[$it]", resolution, resolution, 1, 0,
                            false, depthBufferType
                        ).apply {
                            ensure()
                            depthTexture!!.filtering = GPUFiltering.TRULY_LINEAR
                            depthTexture!!.depthFunc = depthFunc
                        }
                    }
                }
            }
        } else {
            val st = shadowTextures
            if (st != null) {
                for (it in st) {
                    it.destroy()
                }
                shadowTextures = null
            }
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
        dstCameraDirection: Vector3d,
        drawTransform: Matrix4x3d, pipeline: Pipeline,
        resolution: Int, position: Vector3d, rotation: Quaterniond
    )

    open fun updateShadowMaps() {

        lastDrawn = Engine.gameTime

        val pipeline = pipeline
        pipeline.clear()
        val entity = entity!!
        entity.validateTransform()

        val transform = entity.transform
        val drawTransform = transform.getDrawMatrix()
        val resolution = shadowMapResolution
        val global = transform.globalTransform
        val position = global.getTranslation(RenderState.cameraPosition)
        val rotation = global.getUnnormalizedRotation(RenderState.cameraRotation)
        val worldScale = SQRT3 / global.getScaleLength()
        rotation.transform(RenderState.cameraDirection.set(0.0, 0.0, -1.0))
        RenderState.worldScale = worldScale
        val shadowTextures = shadowTextures
        val shadowMapPower = shadowMapPower
        // only fill pipeline once? probably better...
        val renderer = Renderer.nothingRenderer
        GFXState.depthMode.use(DepthMode.CLOSER) {
            for (i in 0 until shadowMapCascades) {
                val cascadeScale = shadowMapPower.pow(-i.toDouble())
                val texture = shadowTextures!![i]
                updateShadowMap(
                    cascadeScale, worldScale,
                    RenderState.cameraMatrix,
                    RenderState.cameraPosition,
                    RenderState.cameraDirection,
                    drawTransform,
                    pipeline, resolution,
                    position, rotation
                )
                val isPerspective = abs(RenderState.cameraMatrix.m33) < 0.5f
                RenderState.calculateDirections(isPerspective)
                val root = entity.getRoot(Entity::class)
                pipeline.fillDepth(root, position, worldScale)
                useFrame(resolution, resolution, true, texture, renderer) {
                    texture.clearDepth()
                    pipeline.defaultStage.drawColors(pipeline)
                }
            }
        }
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
        val st = shadowTextures
        if (st != null) {
            for (it in st) {
                it.destroy()
            }
            shadowTextures = null
        }
    }

    companion object {
        @JvmStatic
        val pipeline by lazy {
            Pipeline(DeferredSettingsV2(listOf(), 1, false)).apply {
                defaultStage.maxNumberOfLights = 0
                // all stages are the same
                for (i in 0 until 15) stages.add(defaultStage)
            }
        }
    }
}