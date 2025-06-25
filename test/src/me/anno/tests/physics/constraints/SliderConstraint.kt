package me.anno.tests.physics.constraints

import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.StaticBody
import me.anno.bullet.constraints.SliderConstraint
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.systems.Systems
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.types.Floats.toRadians

// todo create and test slider constraint
// -> why does it do nothing?
fun main() {

    ECSRegistry.init()
    val scene = Entity("Scene")
    val physics = BulletPhysics()
    Systems.registerSystem(physics)
    physics.updateInEditMode = true

    val staticCubeBody = StaticBody()
    Entity("Static Cube", scene)
        .add(MeshComponent(flatCube))
        .add(BoxCollider())
        .setRotation(5f.toRadians(), 0f, 0f)
        .add(staticCubeBody)

    val sliding = SliderConstraint().apply {
        enableLinearMotor = true
        motorMaxForce = 10000.0
        targetMotorVelocity = 0.0
        disableCollisionsBetweenLinked = true
        lowerLimit = -0.01
        upperLimit = +0.01
        other = staticCubeBody
    }

    Entity("Moving Cube", scene)
        .add(MeshComponent(flatCube))
        .add(BoxCollider())
        .setPosition(0.0, 2.3, 0.0)
        .add(DynamicBody())
        .add(sliding)

    Entity("Floor", scene)
        .add(MeshComponent(flatCube, Material.diffuse(0x333333)))
        .add(BoxCollider())
        .add(DynamicBody().apply {
            friction = 1.0
        })
        .setPosition(0.0, -22.0, 0.0)
        .setScale(20f)

    testSceneWithUI("Slider Constraint", scene)
}