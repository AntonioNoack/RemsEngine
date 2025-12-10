package me.anno.gpu.pipeline

import me.anno.config.DefaultConfig
import me.anno.ecs.components.light.sky.SkyboxBase
import me.anno.ecs.components.light.sky.SkyboxUpscaler
import me.anno.ecs.components.light.sky.shaders.SkyUpscaleShader
import me.anno.ecs.components.mesh.Mesh.Companion.drawDebugLines
import me.anno.ecs.components.mesh.material.Materials.getMaterial
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.Renderers
import me.anno.gpu.CullMode
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.framebuffer.CubemapFramebuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.bindCameraUniforms
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.bindJitterUniforms
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.bindLightUniforms
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.bindTransformUniforms
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.Filtering
import me.anno.maths.Maths
import me.anno.utils.GFXFeatures
import me.anno.utils.pooling.JomlPools
import kotlin.math.max
import kotlin.math.min

object DrawSky {

    fun drawSky(pipeline: Pipeline) {
        GFXState.depthMode.use(pipeline.defaultStage.depthMode) {
            GFXState.depthMask.use(false) {
                GFXState.blendMode.use(null) {
                    GFXState.cullMode.use(CullMode.BACK) {
                        drawSky0(pipeline)
                    }
                }
            }
        }
    }

    fun drawSky0(pipeline: Pipeline) {
        timeRendering("DrawSky", pipeline.skyTimer) {
            GFXState.drawingSky.use(true) {
                drawSky1(pipeline)
            }
        }
    }

    fun drawSky1(pipeline: Pipeline) {
        val skybox = pipeline.skybox
        val target = GFXState.currentBuffer
        val renderer = GFXState.currentRenderer
        val deferred = renderer.deferredSettings
        val defaultMaxRes = if (GFXFeatures.hasWeakGPU) 256 else 4096
        val resolution = DefaultConfig["gpu.maxSkyResolution", defaultMaxRes]
        if (deferred != null && max(target.width, target.height) * 2 > resolution * 3) {
            // draw sky at lower resolution, then upscale (using cubic interpolation)
            val w = min(target.width, resolution)
            val h = min(target.height, resolution)
            val lowRes = FBStack["skyLowRes", w, h, TargetType.Float16x3, 1, DepthBufferType.NONE]
            val renderer1 = Renderers.rawAttributeRenderers[DeferredLayerType.EMISSIVE]
            useFrame(lowRes, renderer1) {
                drawSky2(pipeline, skybox)
            }
            SkyUpscaleShader.source = lowRes.getTexture0()
            drawSky2(pipeline, SkyboxUpscaler)
        } else drawSky2(pipeline, skybox)
    }

    private fun drawSky2(pipeline: Pipeline, sky: SkyboxBase) {
        val mesh = sky.getMesh()
        val allAABB = JomlPools.aabbd.create()
        val scale = if (RenderState.isPerspective) 1f
        else 2f * max(RenderState.fovXRadians, RenderState.fovYRadians)
        allAABB.all()
        for (i in 0 until mesh.numMaterials) {
            val material = getMaterial(sky.materials, mesh.materials, i)
            val shader = (material.shader ?: pbrModelShader).value
            shader.use()
            bindCameraUniforms(shader, pipeline.applyToneMapping)
            bindJitterUniforms(shader)
            bindLightUniforms(pipeline, shader, allAABB, false)
            bindTransformUniforms(shader, sky.transform)
            shader.v1b("hasAnimation", false)
            shader.v4f("tint", 1f)
            shader.v1f("finalAlpha", 1f)
            shader.v1i("hasVertexColors", 0)
            shader.v2i("randomIdData", 6, 123456)
            shader.v1f("meshScale", scale)
            shader.v4f("finalId", -1)
            material.bind(shader)
            mesh.draw(pipeline, shader, i)
        }
        JomlPools.aabbd.sub(1)
    }

    fun bakeSkybox(pipeline: Pipeline, resolution: Int) {
        timeRendering("BakeSkybox", pipeline.skyboxTimer) {
            GFXState.drawingSky.use(true) {
                bakeSkybox0(pipeline, resolution)
            }
        }
    }

    private fun bakeSkybox0(pipeline: Pipeline, resolution: Int) {
        val self = RenderView.currentInstance
        val renderMode = self?.renderMode
        if (self != null && drawDebugLines(renderMode)) {
            self.renderMode = RenderMode.DEFAULT
        }
        // todo only update skybox every n frames
        //  maybe even only one side at a time
        val framebuffer = pipeline.bakedSkybox ?: CubemapFramebuffer(
            "skyBox", resolution, 1,
            listOf(TargetType.Float16x3), DepthBufferType.NONE
        )
        val renderer = Renderers.rawAttributeRenderers[DeferredLayerType.EMISSIVE]
        val skyRot = JomlPools.quat4f.create()
        val cameraMatrix = JomlPools.mat4f.create()
        framebuffer.draw(renderer) { side ->
            val sky = pipeline.skybox
            // draw sky
            // could be optimized to draw a single triangle instead of a full cube for each side
            CubemapTexture.rotateForCubemap(skyRot.identity(), side)
            val shader = (sky.shader ?: pbrModelShader).value
            shader.use()
            Perspective.setPerspective(
                cameraMatrix, Maths.PIf * 0.5f, 1f,
                0.1f, 10f, 0f, 0f
            )
            cameraMatrix.rotate(skyRot)
            shader.m4x4("transform", cameraMatrix)
            if (side == 0) {
                shader.v1i("hasVertexColors", 0)
                sky.material.bind(shader)
            }// else already set
            shader.v3f("cameraPosition", RenderState.cameraPosition)
            shader.v4f("cameraRotation", RenderState.cameraRotation)
            shader.v1f("meshScale", 1f)
            shader.v1b("isPerspective", false)
            shader.v1b("reversedDepth", false) // depth doesn't matter
            sky.getMesh().draw(pipeline, shader, 0)
        }
        JomlPools.quat4f.sub(1)
        JomlPools.mat4f.sub(1)
        if (!GFXFeatures.hasWeakGPU) {
            // calculate mipmap levels
            // performance impact of this: 230->210 fps, so 0.4ms on RTX 3070
            framebuffer.textures[0].bind(0, Filtering.LINEAR)
        }
        pipeline.bakedSkybox = framebuffer
        if (renderMode != null) {
            self.renderMode = renderMode
        }
    }
}