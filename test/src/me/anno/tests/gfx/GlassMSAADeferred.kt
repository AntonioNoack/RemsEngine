package me.anno.tests.gfx

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.TRANSPARENT_PASS
import me.anno.mesh.Shapes.flatCube
import me.anno.sdf.shapes.SDFSphere

fun main() {
    // todo fix aliased edge by bright material
    //  reason for it happening:
    //    blitting, then tone mapping -> bad
    //    tone mapping, then blitting -> good
    // this happens e.g., for SSR (single-sample only) -> forward rendering
    testComplex()
}

fun testComplex() {

    val scene = Entity("Scene")
    val glass = Material().apply {
        roughnessMinMax.set(0.1f)
        metallicMinMax.set(1f)
        pipelineStage = TRANSPARENT_PASS
    }

    val meshSphere = IcosahedronModel.createIcosphere(5)
    meshSphere.material = glass.ref
    scene.add(MeshComponent(meshSphere))

    // doesn't support backside rendering yet
    val sdfSphere = SDFSphere()
    sdfSphere.position.set(2.5f, 0f, 0f)
    sdfSphere.highQualityMSAA = true
    sdfSphere.sdfMaterials = listOf(glass.ref)
    sdfSphere.material.pipelineStage = TRANSPARENT_PASS
    scene.add(sdfSphere)

    scene.add(
        Entity(
            "Diffuse Cube",
            MeshComponent(flatCube.front)
        ).setPosition(+5.0, 0.0, 0.0)
    )
    scene.add(Entity("Emissive Cube",
        MeshComponent(flatCube.front).apply {
            materials = listOf(Material().apply {
                diffuseBase.set(0f, 0f, 0f, 1f)
                emissiveBase.set(5f, 5f, 0f)
            }.ref)
        }).setPosition(-2.5, 0.0, 0.0)
    )
    testSceneWithUI("Glass MSAA Deferred", scene) {
        it.renderView.renderMode = RenderMode.MSAA_DEFERRED
    }
}