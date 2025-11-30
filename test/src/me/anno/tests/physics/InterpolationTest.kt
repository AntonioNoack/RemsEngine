package me.anno.tests.physics

import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.StaticBody
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.InfinitePlaneCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.systems.Systems.registerSystem
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    val physics = BulletPhysics().apply {
        updateInEditMode = true
        stepsPerSecond = 5f
    }
    registerSystem(physics)
    val scene = Entity()
    Entity("Sphere", scene)
        .add(DynamicBody())
        .add(SphereCollider())
        .add(MeshComponent(DefaultAssets.icoSphere))
    Entity("Floor", scene)
        .add(InfinitePlaneCollider())
        .add(StaticBody())
        .setRotationDegrees(10f, 0f, 0f)
    testSceneWithUI("Interpolation Test", scene)
}