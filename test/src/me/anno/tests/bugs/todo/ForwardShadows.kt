package me.anno.tests.bugs.todo

import me.anno.ecs.Entity
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference
import me.anno.utils.types.Floats.toRadians

fun main() {
    // fixed: shadow was missing
    // fixed: the sphere is wayyy to glossy for roughness=1
    // todo bug: stuff is too bright, probably accidentally linear instead of sRGB somewhere
    DefaultAssets.init()
    val scene = Entity()
    scene.add(MeshComponent(getReference("meshes/PlaneY.json")))
    Entity(scene)
        .setPosition(0.0, 0.5, 0.0)
        .setScale(0.5)
        .add(MeshComponent(IcosahedronModel.createIcosphere(2)))
    Entity(scene)
        .setRotation((-30.0).toRadians(), 0.0, 0.0)
        .add(DirectionalLight().apply { shadowMapCascades = 1 })
    testSceneWithUI("Forward Shadows", scene) {
        it.renderer.renderMode = RenderMode.FORWARD
    }
}