package me.anno.maths.bvh

import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.Sorting
import me.anno.utils.OS.documents
import me.anno.utils.OS.downloads
import me.anno.utils.structures.tuples.Quad
import org.joml.*

fun createSampleTLAS(maxNodeSize: Int): Quad<TLASNode, Vector3f, Quaternionf, Float> {

    // create a scene, so maybe load Sponza, and then execute our renderer on TLAS
    @Suppress("SpellCheckingInspection")
    val sources = listOf(
        downloads.getChild("ogldev-source/crytek_sponza/sponza.obj"),
        downloads.getChild("ogldev-source/dabrovic-sponza/sponza.obj"),
        downloads.getChild("ogldev-source/conference-room/conference.obj"),
        documents.getChild("TestScene4.fbx"),
        downloads.getChild("3d/XYZ arrows.obj")
    )

    val source = sources[0]
    val pipeline = Pipeline(DeferredSettingsV2(listOf(DeferredLayerType.COLOR), 1, false))
    pipeline.defaultStage = PipelineStage(
        "default", Sorting.NO_SORTING, 0, null, DepthMode.ALWAYS, true,
        CullMode.BOTH, pbrModelShader
    )

    val prefab = PrefabCache[source]!!
    val scene = prefab.createInstance() as Entity

    scene.validateTransform()
    scene.validateAABBs()

    val aabb = scene.aabb

    val cameraPosition = Vector3d(aabb.avgX(), aabb.avgY(), aabb.maxZ * 1.5f)
    val cameraRotation = Quaterniond()
    val worldScale = 1.0 // used in Rem's Engine for astronomic scales

    pipeline.frustum.setToEverything(cameraPosition, cameraRotation)
    pipeline.fill(scene)

    if (true) {// duplicate object 25 times for testing
        val dx = aabb.deltaX() * 1.1
        val dy = 0.0
        val dz = aabb.deltaZ() * 1.1
        for (i in 0 until 5) {
            for (j in 0 until 5) {
                if (i + j > 0) {
                    val scene2 = prefab.createInstance() as Entity
                    // clone object to test mesh duplication
                    scene2.transform
                        .translateLocal(dx * i, dy, dz * j)
                        .rotateYLocal(0.2 * i)
                        .rotateXLocal(0.2 * j)
                    scene2.validateTransform()
                    scene2.validateAABBs()
                    pipeline.fill(scene2)
                }
            }
        }
    }

    val tlas = BVHBuilder.buildTLAS(pipeline.defaultStage, cameraPosition, worldScale, SplitMethod.MEDIAN, maxNodeSize)
    return Quad(tlas, Vector3f().set(cameraPosition), Quaternionf(cameraRotation), 0.2f)

}
