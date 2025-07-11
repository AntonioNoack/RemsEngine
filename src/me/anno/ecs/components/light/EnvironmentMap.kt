package me.anno.ecs.components.light

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.light.PlanarReflection.Companion.clearSky
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.ui.LineShapes.drawBox
import me.anno.engine.ui.LineShapes.drawCross
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.RenderView.Companion.addDefaultLightsIfRequired
import me.anno.engine.ui.render.Renderers.pbrRenderer
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.framebuffer.CubemapFramebuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStageImpl
import me.anno.gpu.query.GPUClockNanos
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.texture.CubemapTexture.Companion.rotateForCubemap
import me.anno.maths.Maths
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.max
import me.anno.mesh.Shapes
import me.anno.utils.pooling.JomlPools
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3d

/**
 * environment map for reflections,
 * radiance map, sky map, ...
 * */
class EnvironmentMap : LightComponentBase(), OnDrawGUI {

    @Range(1.0, 8192.0)
    var resolution = 256

    @Range(0.0, 1e35)
    var near = 0.01f

    @Range(1e-35, 1e38)
    var far = 1e3f

    var shader: BaseShader? = null

    @NotSerializedProperty
    var texture: CubemapFramebuffer? = null

    var samples = 1

    val timer = GPUClockNanos()

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        Shapes.cube11Smooth.getBounds().transformUnion(globalTransform, dstUnion)
    }

    override fun fill(pipeline: Pipeline, transform: Transform) {
        lastDrawn = Time.gameTimeN
        pipeline.lightStage.add(this)
        clickId = pipeline.getClickId(this)
    }

    override fun onVisibleUpdate() {
        if (texture == null || texture?.samples != samples) {
            texture?.destroy()
            texture = CubemapFramebuffer(
                "EnvironmentMap",
                max(1, resolution), samples, listOf(TargetType.Float16x3),
                DepthBufferType.TEXTURE_16
            )
            needsUpdate1 = true
        }
        val texture = texture
        if (texture != null && (needsUpdate1 || needsAutoUpdate())) {
            needsUpdate1 = false
            drawBuffer(texture)
        }
    }

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        if (all) {
            drawBox(entity)
            drawCross(entity, crossExtents)
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
        val position = global.getTranslation(JomlPools.vec3d.create())
        val worldScale = (Maths.SQRT3 / global.getScaleLength()).toFloat()

        val deg90 = PIf * 0.5f
        val camRot = JomlPools.quat4f.create()
        val camRotInv = JomlPools.quat4f.create()

        val cameraMatrix = JomlPools.mat4f.create()
        val root = entity.getRoot(Entity::class)
        root.validateTransform()
        root.getGlobalBounds()
        timeRendering(className, timer) {
            GFXState.depthMode.use(pipeline.defaultStage.depthMode) {
                texture.draw(resolution, pbrRenderer) { side ->

                    Perspective.setPerspective(
                        cameraMatrix, deg90, 1f,
                        near, far, 0f, 0f
                    )
                    rotateForCubemap(camRot.identity(), side)
                    camRot.invert(camRotInv)

                    cameraMatrix.rotate(camRot)

                    pipeline.clear()
                    pipeline.frustum.definePerspective(
                        near / worldScale, far / worldScale, deg90,
                        resolution, 1f,
                        position, camRotInv // needs to be the inverse again
                    )
                    pipeline.applyToneMapping = false
                    pipeline.fill(root)

                    // define RenderState
                    RenderState.cameraMatrix.set(cameraMatrix)
                    RenderState.cameraPosition.set(position)
                    RenderState.cameraRotation.set(camRotInv)
                    RenderState.calculateDirections(true, true)

                    // clear using sky
                    clearSky(pipeline)
                    addDefaultLightsIfRequired(pipeline, root, null)
                    pipeline.bakedSkybox = RenderView.currentInstance?.pipeline?.bakedSkybox
                    pipeline.singlePassWithSky(false)
                }
            }
        }

        JomlPools.mat4f.sub(1)
        JomlPools.vec3d.sub(1)
        JomlPools.quat4f.sub(2)

        // todo create irradiance mipmaps: blur & size down, just like bloom
    }

    override fun destroy() {
        super.destroy()
        texture?.destroy()
        timer.destroy()
    }

    companion object {

        val crossExtents = Vector3d(0.1)

        val pipeline by lazy {
            val pipeline = Pipeline(DeferredSettings(listOf()))
            // we may need a second stage for transparent stuff
            pipeline.defaultStage = PipelineStageImpl(
                "", 16, null,
                if (GFX.supportsClipControl) DepthMode.CLOSE else DepthMode.FORWARD_CLOSE,
                true,
                CullMode.BACK, ECSShaderLib.pbrModelShader
            )
            pipeline.stages.add(pipeline.defaultStage)
            pipeline
        }
    }
}