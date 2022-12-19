package me.anno.ecs.components.light

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.annotations.DebugProperty
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
import me.anno.gpu.shader.Renderer
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.maths.Maths.SQRT3
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Matrices.getScaleLength
import org.joml.*
import kotlin.math.pow

abstract class LightComponent(val lightType: LightType) : LightComponentBase() {

    // todo AES lights, and their textures?

    // todo plane of light: how? https://eheitzresearch.wordpress.com/415-2/ maybe :)
    // todo lines of light: how?
    // todo circle/sphere of light: how?

    // black lamp light?
    @Type("Color3HDR")
    @SerializedProperty
    var color: Vector3f = Vector3f(1f)

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

    val hasShadow get() = shadowMapCascades > 0

    var needsUpdate = true
    var autoUpdate = true

    override fun fill(
        pipeline: Pipeline,
        entity: Entity,
        clickId: Int,
        cameraPosition: Vector3d,
        worldScale: Double
    ): Int {
        // todo if(entity == pipeline.sampleEntity) add floor/setup, so we can see the light
        pipeline.addLight(this, entity, cameraPosition, worldScale)
        return clickId // not itself clickable
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        val mesh = getLightPrimitive()
        mesh.ensureBounds()
        mesh.aabb.transformUnion(globalTransform, aabb)
        return true
    }

    open fun invalidateShadows() {
        needsUpdate = true
    }

    override fun onDrawGUI(all: Boolean) {
        if (all) drawShape()
    }

    abstract fun drawShape()

    abstract fun getLightPrimitive(): Mesh

    @HideInInspector
    @NotSerializedProperty
    var shadowTextures: Array<IFramebuffer>? = null

    var samples = 1

    fun ensureShadowBuffers() {
        if (hasShadow) {
            // only a single one is supported,
            // because more makes no sense
            val isPointLight = lightType == LightType.POINT
            if (isPointLight) shadowMapCascades = 1
            val shadowCascades = shadowTextures
            val targetSize = shadowMapCascades
            val resolution = shadowMapResolution
            val samples = samples
            if (shadowCascades == null ||
                shadowCascades.size != targetSize ||
                shadowCascades.first().samples != samples
            ) {
                if (shadowCascades != null) for (it in shadowCascades) {
                    it.destroy()
                }
                // we currently use a depth bias of 0.005,
                // which is equal to ~ 1/255
                // so a 8 bit depth buffer would be enough
                val depthBufferType = DepthBufferType.TEXTURE_16
                this.shadowTextures = Array(targetSize) {
                    if (isPointLight) {
                        CubemapFramebuffer(
                            "ShadowCubemap[$it]", resolution, samples, 0,
                            false, depthBufferType
                        )
                    } else {
                        Framebuffer(
                            "Shadow[$it]", resolution, resolution, samples, 0,
                            false, depthBufferType
                        )
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
            if (autoUpdate || needsUpdate) {
                needsUpdate = false
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
        cameraMatrix: Matrix4f,
        drawTransform: Matrix4x3d, pipeline: Pipeline,
        resolution: Int, position: Vector3d, rotation: Quaterniond
    )

    @NotSerializedProperty
    @DebugProperty
    var lastDraw = 0L

    open fun updateShadowMaps() {

        lastDraw = Engine.nanoTime

        val pipeline = pipeline
        pipeline.clear()
        val entity = entity!!
        val transform = entity.transform
        val drawTransform = transform.getDrawMatrix()
        val resolution = shadowMapResolution
        val global = transform.globalTransform
        val position = global.getTranslation(RenderState.cameraPosition)
        val rotation = global.getUnnormalizedRotation(JomlPools.quat4d.create())
        val worldScale = SQRT3 / global.getScaleLength()
        RenderState.worldScale = worldScale
        val cameraMatrix = RenderState.cameraMatrix
        val shadowTextures = shadowTextures
        val shadowMapPower = shadowMapPower
        // only fill pipeline once? probably better...
        val renderer = Renderer.nothingRenderer
        val depthMode = DepthMode.CLOSER
        for (i in 0 until shadowMapCascades) {
            val cascadeScale = shadowMapPower.pow(-i.toDouble())
            val texture = shadowTextures!![i]
            updateShadowMap(
                cascadeScale, worldScale,
                cameraMatrix, drawTransform,
                pipeline, resolution,
                position, rotation
            )
            val root = entity.getRoot(Entity::class)
            pipeline.fillDepth(root, position, worldScale)
            GFXState.depthMode.use(depthMode) {
                useFrame(resolution, resolution, true, texture, renderer) {
                    texture.clearDepth()
                    pipeline.drawDepth()
                }
            }
        }

        JomlPools.quat4d.sub(1)

    }

    /**
     * is set by the pipeline,
     * is equal to inv(transform.globalMatrix - cameraPosition)
     * */
    @NotSerializedProperty
    val invWorldMatrix = Matrix4x3f()

    open fun getShaderV0(drawTransform: Matrix4x3d, worldScale: Double): Float = 0f
    open fun getShaderV1(): Float = 0f
    open fun getShaderV2(): Float = 0f

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as LightComponent
        clone.shadowMapCascades = shadowMapCascades
        clone.shadowMapPower = shadowMapPower
        clone.shadowMapResolution = shadowMapResolution
        clone.color = color
        clone.needsUpdate = true
        clone.autoUpdate = autoUpdate
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
            Pipeline(DeferredSettingsV2(listOf(), false))
        }
    }

}