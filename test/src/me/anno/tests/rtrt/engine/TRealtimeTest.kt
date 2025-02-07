package me.anno.tests.rtrt.engine

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.gpu.CullMode
import me.anno.gpu.GFXState.alwaysDepthMode
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStageImpl
import me.anno.language.translation.NameDesc
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.SplitMethod
import me.anno.maths.bvh.TLASNode
import me.anno.tests.LOGGER
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.input.EnumInput
import me.anno.utils.Clock
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

fun main() {
    OfficialExtensions.initForTests()
    val clock = Clock("TRealtimeTest")
    val (tlas, cameraPosition, cameraRotation, fovZFactor) = createSampleTLAS(16, clock)
    run(tlas, cameraPosition, cameraRotation, fovZFactor)
    LOGGER.debug("Finished")
}

data class SampleTLAS(
    val root: TLASNode,
    val cameraPosition: Vector3f,
    val cameraRotation: Quaternionf,
    val fovZFactor: Float
)

fun createSampleTLAS(maxNodeSize: Int, clock: Clock): SampleTLAS {

    // create a scene, so maybe load Sponza, and then execute our renderer on TLAS
    @Suppress("SpellCheckingInspection")
    val sources = listOf(
        OS.downloads.getChild("3d/ogldev-source/crytek_sponza/sponza.fbx"),
        OS.downloads.getChild("3d/ogldev-source/dabrovic-sponza/sponza.obj"),
        OS.downloads.getChild("3d/ogldev-source/conference-room/conference.obj"),
        OS.documents.getChild("TestScene4.fbx"),
        OS.downloads.getChild("3d/XYZ arrows.obj")
    )

    val source = sources[0]
    val pipeline = Pipeline(DeferredSettings(listOf(DeferredLayerType.COLOR)))
    pipeline.defaultStage = PipelineStageImpl(
        "default", 0, null, alwaysDepthMode, true, CullMode.BOTH,
        ECSShaderLib.pbrModelShader
    )

    val prefab = PrefabCache[source] ?: throw IllegalStateException("Missing $source")
    val scene = prefab.createInstance() as Entity
    clock.stop("Loading Mesh")

    scene.validateTransform()

    val aabb = scene.getGlobalBounds()

    val cameraPosition = Vector3d(aabb.centerX, aabb.centerY, aabb.maxZ * 1.5f)
    val cameraRotation = Quaterniond()
    val worldScale = 1.0 // used in Rem's Engine for astronomic scales

    pipeline.frustum.setToEverything(cameraPosition, cameraRotation)
    pipeline.fill(scene)

    if (true) {// duplicate object 25 times for testing
        val dx = aabb.deltaX * 1.1
        val dy = 0.0
        val dz = aabb.deltaZ * 1.1
        for (i in 0 until 5) {
            for (j in 0 until 5) {
                if (i + j > 0) {
                    val scene2 = prefab.createInstance() as Entity
                    // clone object to test mesh duplication
                    scene2.transform
                        .translateLocal(dx * i, dy, dz * j)
                        .rotateLocalY(0.2 * i)
                        .rotateLocalX(0.2 * j)
                    scene2.validateTransform()
                    scene2.getGlobalBounds()
                    pipeline.fill(scene2)
                }
            }
        }
    }
    clock.stop("Building Scene")

    val tlas =
        BVHBuilder.buildTLAS(pipeline.defaultStage, cameraPosition, worldScale, SplitMethod.MEDIAN_APPROX, maxNodeSize)
    clock.stop("Building BLAS")

    return SampleTLAS(tlas!!, Vector3f(cameraPosition), Quaternionf(cameraRotation), 0.2f)
}

fun run(
    bvh: TLASNode,
    pos: Vector3f, rot: Quaternionf,
    fovZFactor: Float
) {
    LogManager.disableLogger("WorkSplitter")
    testUI3("TLAS - Realtime") {

        val main = PanelListY(style)
        val controls = createControls(pos, rot, bvh.bounds, main)

        val list = CustomList(false, style)

        var scale = 4
        list.add(createCPUPanel(scale, pos, rot, fovZFactor, bvh, controls))

        // gpu is fast enough :)
        scale = 1
        val useComputeShader = true
        list.add(createGPUPanel(scale, pos, rot, fovZFactor, bvh, controls, useComputeShader, false))
        list.add(createGPUPanel(scale, pos, rot, fovZFactor, bvh, controls, useComputeShader, true))
        list.add(createGPUPanel(scale, pos, rot, fovZFactor, bvh, controls, useComputeShader = false, false))

        main.add(list)
        main.add(typeInput())
        list.weight = 1f
        main

    }
}

fun typeInput() = EnumInput(
    NameDesc("Draw Mode"),
    NameDesc(drawMode.name),
    DrawMode.entries.map { NameDesc(it.name) },
    style
).setChangeListener { _, index, _ ->
    drawMode = DrawMode.entries[index]
}