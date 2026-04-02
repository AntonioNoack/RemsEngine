package me.anno.tests.gfx.effect

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponentInChildren
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.anim.AnimationState
import me.anno.ecs.components.light.DirectionalLight
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference
import me.anno.tests.engine.material.createMetallicScene

/**
 * testing Temporal Anti-Aliasing (TAA) on an animated mesh and some metallic things
 * */
fun main() {
    val downloads = getReference("/mnt/Windows/Users/Antonio/Downloads")
    val foxFile = downloads.getChild("3d/azeria/scene.gltf")
    val scene = Entity("Scene")
        .add(createMetallicScene())
    scene.getComponentInChildren(DirectionalLight::class)!!.autoUpdate = 1 // always update shadows
    Entity("Fox", scene)
        .setPosition(3.0, -0.7, 0.0)
        .setScale(0.02f)
        .add(AnimMeshComponent().apply {
            meshFile = foxFile
            animations = listOf(AnimationState(foxFile.getChild("animations/Walk/Imported.json")))
        })
    testSceneWithUI("TAA", scene, RenderMode.TAA)
}