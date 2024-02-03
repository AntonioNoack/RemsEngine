package me.anno.ecs.components.light

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.light.PlanarReflection.Companion.clearSky
import me.anno.engine.ui.LineShapes.drawBox
import me.anno.engine.ui.LineShapes.drawCross
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.RenderView.Companion.addDefaultLightsIfRequired
import me.anno.engine.ui.render.Renderers.pbrRenderer
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.framebuffer.CubemapFramebuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStageImpl
import me.anno.gpu.pipeline.Sorting
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.texture.CubemapTexture.Companion.rotateForCubemap
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.gpu.GFX
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.max
import me.anno.mesh.Shapes
import me.anno.utils.pooling.JomlPools
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Quaterniond
import org.joml.Vector3d

/**
 * environment map for reflections,
 * radiance map, sky map, ...
 * */
class EnvironmentMap : LightComponentBase() {

    @Range(1.0, 8192.0)
    var resolution = 256

    @Range(0.0, 1.0)
    var near = 0.01

    @Range(1.0, 1e308)
    var far = 1e3

    var shader: BaseShader? = null

    @NotSerializedProperty
    var texture: CubemapFramebuffer? = null

    var samples = 1

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        Shapes.cube11Smooth.getBounds().transformUnion(globalTransform, aabb)
        return true
    }

    override fun fill(
        pipeline: Pipeline,
        entity: Entity,
        clickId: Int
    ): Int {
        lastDrawn = Time.gameTimeN
        pipeline.lightStage.add(this)
        this.clickId = clickId
        return clickId + 1
    }

    override fun onVisibleUpdate(): Boolean {
        if (texture == null || texture?.samples != samples) {
            texture?.destroy()
            texture = CubemapFramebuffer(
                "EnvironmentMap",
                max(1, resolution), samples, arrayOf(TargetType.Float16x3),
                DepthBufferType.TEXTURE_16
            )
            needsUpdate1 = true
        }
        val texture = texture
        if (texture != null && (needsUpdate1 || autoUpdate)) {
            needsUpdate1 = false
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
        pipeline.ignoredEntity = entity
        pipeline.ignoredComponent = null

        val transform = entity.transform
        val resolution = max(4, resolution)
        val global = transform.globalTransform
        val position = global.getTranslation(tmpV3)
        val sqrt3 = 1.7320508075688772
        val worldScale = sqrt3 / global.getScale(JomlPools.vec3d.borrow()).length()

        val deg90 = PIf * 0.5f
        val camRot = tmpQ2
        val camRotInv = tmpQ3

        val cameraMatrix = JomlPools.mat4f.create()
        val root = entity.getRoot(Entity::class)
        root.validateTransform()
        root.getBounds()
        GFXState.depthMode.use(pipeline.defaultStage.depthMode) {
            texture.draw(resolution, pbrRenderer) { side ->

                Perspective.setPerspective(
                    cameraMatrix, deg90, 1f,
                    near.toFloat(), far.toFloat(), 0f, 0f
                )
                rotateForCubemap(camRot.identity(), side)
                camRot.invert(camRotInv)

                cameraMatrix.rotate(camRot)

                pipeline.clear()
                pipeline.frustum.definePerspective(
                    near / worldScale, far / worldScale, deg90.toDouble(),
                    resolution, resolution, 1.0,
                    position, camRotInv // needs to be the inverse again
                )
                pipeline.applyToneMapping = false
                pipeline.fill(root)

                // define RenderState
                RenderState.worldScale = worldScale
                RenderState.cameraMatrix.set(cameraMatrix)
                RenderState.cameraPosition.set(position)
                RenderState.cameraRotation.set(camRotInv)
                RenderState.calculateDirections(true)

                // clear using sky
                clearSky(pipeline)
                addDefaultLightsIfRequired(pipeline, root, null)
                pipeline.bakedSkybox = RenderView.currentInstance?.pipeline?.bakedSkybox
                pipeline.singlePassWithSky(false)
            }
        }
        JomlPools.mat4f.sub(1)

        // todo create irradiance mipmaps: blur & size down, just like bloom
    }

    override val className: String get() = "EnvironmentMap"

    companion object {

        private val tmpV3 = Vector3d()
        private val tmpQ2 = Quaterniond()
        private val tmpQ3 = Quaterniond()

        val crossExtends = Vector3d(0.1)

        val pipeline by lazy {
            val pipeline = Pipeline(DeferredSettings(listOf()))
            // we may need a second stage for transparent stuff
            pipeline.defaultStage = PipelineStageImpl(
                "", Sorting.NO_SORTING, 16, null,
                if (GFX.supportsClipControl) DepthMode.CLOSE
                else DepthMode.FORWARD_CLOSE,
                true, CullMode.BACK, ECSShaderLib.pbrModelShader
            )
            pipeline.stages.add(pipeline.defaultStage)
            pipeline
        }
    }
}