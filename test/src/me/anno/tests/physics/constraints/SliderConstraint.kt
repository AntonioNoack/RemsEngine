package me.anno.tests.physics.constraints

import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.bullet.constraints.SliderConstraint
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.Color.black
import me.anno.utils.types.Floats.toRadians

// todo create and test slider constraint
// -> why does it do nothing?
fun main() {

    ECSRegistry.init()
    val scene = Entity("Scene")
    val physics = BulletPhysics()
    scene.add(physics)
    physics.updateInEditMode = true

    val box0 = Entity("Moving Cube", scene)
    box0.add(MeshComponent(flatCube.front))
    box0.add(BoxCollider())
    box0.setPosition(0.0, 2.3, 0.0)
    val body0 = Rigidbody()
    body0.mass = 1.0
    box0.add(body0)

    val box1 = Entity("Static Cube", scene)
    box1.add(MeshComponent(flatCube.front))
    box1.add(BoxCollider())
    box1.setRotation(5.0.toRadians(), 0.0, 0.0)
    val body1 = Rigidbody()
    box1.add(body1)

    val sliding = SliderConstraint()
    sliding.enableLinearMotor = true
    sliding.motorMaxForce = 10000.0
    sliding.targetMotorVelocity = 0.0
    sliding.disableCollisionsBetweenLinked = true
    sliding.lowerLimit = -0.01
    sliding.upperLimit = +0.01
    sliding.other = body1
    box0.add(sliding)

    val floor = Entity("Floor", scene)
    floor.add(MeshComponent(flatCube.front, Material.diffuse(0x333333 or black)))
    floor.add(BoxCollider())
    floor.add(Rigidbody().apply {
        friction = 1.0
    })
    floor.setPosition(0.0, -22.0, 0.0)
    floor.setScale(20.0)

    testSceneWithUI("Slider Constraint", scene)
}