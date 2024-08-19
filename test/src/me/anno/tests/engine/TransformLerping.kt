package me.anno.tests.engine

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Updatable
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes
import me.anno.utils.types.Floats.toRadians

/**
 * scripts may be expensive, so I thought it may be useful to run
 * some logic only once in a while, and that it may be useful to
 * allow for frame-interpolation including geometry on the engine side;
 *
 * this test demonstrates a script that runs only once per second,
 * but smoothly rotates a cube anyway
 *
 * in retrospect, running scripts may be the cheapest part,
 * and interpolating transforms properly might be quite expensive
 * */
fun main() {
    val scene = Entity()
    val child = Entity(scene)
    child.add(MeshComponent(Shapes.flatCube.front))
    child.add(object : Component(), OnUpdate {
        var skippableUpdates = 0
        override fun onUpdate() {
            if (skippableUpdates-- <= 0) {
                val transform = transform!!
                transform.localRotation = transform.localRotation.rotateY(120.0.toRadians())
                transform.smoothUpdate()
                skippableUpdates = Time.currentFPS.toInt()
            }
        }
    })
    testSceneWithUI("Transform Lerping", scene)
}