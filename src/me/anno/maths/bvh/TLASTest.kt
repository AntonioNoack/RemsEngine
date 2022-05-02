package me.anno.maths.bvh

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.gpu.DepthMode
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.pipeline.CullMode
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.Sorting
import me.anno.utils.Clock
import me.anno.utils.OS.downloads
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import java.lang.Math.PI

fun main() {

    // todo create gpu data for bvh
    // todo gpu renderer

    val w = 2048
    val h = w * 3 / 4

    // 2048x1536
    val cpuBVH = true // 2150 ns/pixel on 8 tri/node max; 1705 ns/pixel on 4 or 2 incl. png encoding
    val gpuBVH = true

    if (gpuBVH) {
        ECSRegistry.initWithGFX(w, h)
    } else {
        ECSRegistry.initNoGFX()
    }

    // create a scene, so maybe load Sponza, and then execute our renderer on TLAS
    val source = downloads.getChild("ogldev-source/crytek_sponza/sponza.obj")
    val pipeline = Pipeline(DeferredSettingsV2(listOf(DeferredLayerType.COLOR), false))
    pipeline.defaultStage = PipelineStage(
        "default", Sorting.NO_SORTING, 0, null, DepthMode.ALWAYS, true,
        CullMode.BOTH, pbrModelShader
    )

    val scene = PrefabCache[source]!!.getSampleInstance() as Entity

    // scene is in centimeters
    val cameraPosition = Vector3d(0.0, 100.0, -50.0)
    val cameraRotation = Quaterniond()
        .rotateY(PI * 0.5)
    val worldScale = 1.0 // used in Rem's Engine for astronomic scales

    pipeline.frustum.setToEverything(cameraPosition, cameraRotation)

    pipeline.fill(scene, cameraPosition, worldScale)

    val bvh = BVHBuilder.buildTLAS(pipeline.defaultStage, cameraPosition, worldScale, SplitMethod.MEDIAN, 4)

    val cx = (w - 1) * 0.5
    val cy = (h - 1) * 0.5
    val fovZ = -w * 0.2 // why is this soo large?

    val cameraPosition2 = Vector3f().set(cameraPosition)
    val cameraRotation2 = Quaternionf(cameraRotation)

    val clock = Clock()

    if (cpuBVH) {
        clock.start()
        renderOnCPU(w, h, "bvh/sponza-cpu-bvh.png", cx, cy, fovZ, cameraPosition2, cameraRotation2, bvh)
        clock.stop("cpu-bvh", w * h)
    }

    if (gpuBVH) {
        // add gpu renderer here...
    }

    Engine.requestShutdown()

}