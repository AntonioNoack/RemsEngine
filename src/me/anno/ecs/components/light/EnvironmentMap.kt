package me.anno.ecs.components.light

import me.anno.ecs.Entity
import me.anno.ecs.annotations.Range
import me.anno.engine.ui.LineShapes.drawBox
import me.anno.engine.ui.LineShapes.drawCross
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.engine.ui.render.Renderers.pbrRenderer
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.framebuffer.CubemapFramebuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.Sorting
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.image.ImageGPUCache
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.NotSerializedProperty
import me.anno.mesh.Shapes
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Matrices.rotate2
import org.apache.logging.log4j.LogManager
import org.joml.*
import kotlin.math.PI

// todo these could be used as
//  - a) reflection maps
//  - b) environment illumination map

// todo or render from shader
// todo - always find closest to object
// todo - bake surrounding lighting for reflections
// todo - blur
// todo - hdr

// is this a light component?
//  - only 1 per object
//  - closest to object
//  - few per scene (?)

// todo or we could say, that only elements in this AABB are valid receivers :)

/**
 * environment map for reflections,
 * radiance map, sky map, ...
 * */
class EnvironmentMap : LightComponentBase() {

    enum class SourceType {
        TEXTURE, // could have different projections...
        SHADER,
        ENVIRONMENT
    }

    var resolution = 1024

    @Range(0.0, 1.0)
    var near = 0.01

    var type = SourceType.ENVIRONMENT

    var shader: BaseShader? = null
    var textureSource: FileReference = InvalidRef

    @NotSerializedProperty
    var texture: CubemapFramebuffer? = null

    var needsUpdate = true
    var autoUpdate = true

    var samples = 1

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        val mesh = Shapes.cube11Smooth
        mesh.ensureBounds()
        mesh.aabb.transformUnion(globalTransform, aabb)
        return true
    }

    override fun fill(
        pipeline: Pipeline,
        entity: Entity,
        clickId: Int,
        cameraPosition: Vector3d,
        worldScale: Double
    ): Int {
        // todo needs be added to specific array in pipeline, I think :)
        this.clickId = clickId
        return clickId + 1
    }

    override fun onVisibleUpdate(): Boolean {
        if (type != SourceType.TEXTURE) {
            if (texture == null || texture?.samples != samples) {
                texture?.destroy()
                texture = CubemapFramebuffer(
                    "EnvironmentMap",
                    resolution, samples, 1,
                    true, DepthBufferType.TEXTURE_16
                )
                needsUpdate = true
            }
        } else {
            texture?.destroy()
            texture = null
        }
        val texture = texture
        if (texture != null && (needsUpdate || autoUpdate)) {
            needsUpdate = false
            drawBuffer(texture)
        }
        return true
    }

    override fun onDrawGUI(all: Boolean) {
        if (all) {
            drawBox(entity)
            drawCross(entity, crossExtends)
        }
    }

    private fun drawBuffer(texture: CubemapFramebuffer) {

        val entity = entity!!

        val pipeline = pipeline
        pipeline.clear()

        // we don't want a positive feedback-loop
        pipeline.ignoredComponent = this

        val transform = entity.transform
        val resolution = resolution
        val global = transform.globalTransform
        val position = global.getTranslation(tmpV3)
        val rotation = global.getUnnormalizedRotation(tmpQd)
        val sqrt3 = 1.7320508075688772
        val worldScale = sqrt3 / global.getScale(JomlPools.vec3d.borrow()).length()

        val far = 1.0

        val deg90 = PI * 0.5
        val rot2 = tmpQ1.set(rotation).invert()
        val rot3 = tmpQ2

        val cameraMatrix = JomlPools.mat4f.create()
        val root = entity.getRoot(Entity::class)
        GFXState.depthMode.use(DepthMode.CLOSER) {
            texture.draw(resolution, pbrRenderer) { side ->
                texture.clearColor(.7f, .9f, 1f, 1f, true)
                Perspective.setPerspective(
                    cameraMatrix, deg90.toFloat(), 1f,
                    near.toFloat(), far.toFloat(), 0f, 0f
                )
                rotateForCubemap(rot3.identity(), side)
                rot3.mul(rot2)
                cameraMatrix.rotate2(rot3)
                val rotation2 = rot3.invert()
                pipeline.clear()
                pipeline.frustum.definePerspective(
                    near / worldScale, far / worldScale, deg90,
                    resolution, resolution, 1.0,
                    position, rotation2 // needs to be the inverse again
                )
                pipeline.applyToneMapping = false
                pipeline.fill(root, position, worldScale)
                pipeline.draw()
            }
        }
        JomlPools.mat4f.sub(1)

        // todo create irradiance mipmaps: blur & size down, just like bloom

    }

    fun canBind(): Boolean {
        return when (type) {
            SourceType.TEXTURE -> ImageGPUCache.getImage(textureSource, textureTimeout, true) != null
            else -> texture != null
        }
    }

    fun bind(index: Int) {
        when (type) {
            SourceType.TEXTURE -> {
                val texture = ImageGPUCache.getImage(textureSource, textureTimeout, true)
                texture!!.bind(index)
            }
            else -> {
                val buffer = texture
                buffer!!.bindTexture0(0, GPUFiltering.LINEAR, Clamping.CLAMP)
            }
        }
    }

    override fun clone(): EnvironmentMap {
        val clone = EnvironmentMap()
        copy(clone)
        return EnvironmentMap()
    }

    override val className = "EnvironmentMap"

    companion object {

        private val tmpV3 = Vector3d()
        private val tmpQd = Quaterniond()
        private val tmpQ1 = Quaterniond()
        private val tmpQ2 = Quaterniond()

        private val LOGGER = LogManager.getLogger(EnvironmentMap::class)

        val crossExtends = Vector3d(0.1)

        val textureTimeout = 10000L

        val pipeline by lazy {
            val pipeline = Pipeline(DeferredSettingsV2(listOf(), false))
            // we may need a second stage for transparent stuff
            pipeline.defaultStage = PipelineStage(
                "", Sorting.NO_SORTING, 16, null, DepthMode.CLOSER,
                true, CullMode.BACK, ECSShaderLib.pbrModelShader
            )
            pipeline.stages.add(pipeline.defaultStage)
            pipeline
        }

        fun rotateForCubemap(rot3: Quaterniond, side: Int) {
            // rotate based on direction
            // POSITIVE_X, NEGATIVE_X, POSITIVE_Y, NEGATIVE_Y, POSITIVE_Z, NEGATIVE_Z
            when (side) {
                0 -> rot3.rotateY(+PI * 0.5)
                1 -> rot3.rotateY(-PI * 0.5)
                2 -> rot3.rotateX(+PI * 0.5)
                3 -> rot3.rotateX(-PI * 0.5)
                // 4 is already correct
                5 -> rot3.rotateY(PI)
            }
        }

    }

}