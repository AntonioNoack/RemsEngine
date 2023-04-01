package me.anno.tests.shader

import me.anno.ecs.Entity
import me.anno.ecs.components.light.PlanarReflection
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.SkyBox
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.utils.OS.documents
import kotlin.math.PI

fun main() {
    ECSRegistry.initMeshes()
    registerCustomClass(PlanarReflection())
    registerCustomClass(SkyBox())
    val scene = Entity()
    scene.add(Entity().apply {
        add(PlanarReflection())
        position = position.set(0.0, -0.01, 0.0)
        rotation = rotation.identity().rotateX(-PI / 2)
    })
    scene.add(SkyBox())
    scene.add(MeshComponent(documents.getChild("plane.obj")).apply {
        materials = listOf(Material().apply {
            metallicMinMax.set(1f)
            roughnessMinMax.set(0.3f)
        }.ref)
    })
    scene.add(Entity().apply {
        add(MeshComponent(documents.getChild("monkey.obj")))
        position = position.set(0.0, 0.3, 0.0)
        scale = scale.set(0.1)
    })
    testSceneWithUI(scene) {
        // doesn't have ssao yet
        it.renderer.renderMode = RenderMode.FORCE_NON_DEFERRED
    }
}