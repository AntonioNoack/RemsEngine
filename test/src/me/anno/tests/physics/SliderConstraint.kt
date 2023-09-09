package me.anno.tests.physics

import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.bullet.constraints.SliderConstraint
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.Skybox
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.types.Floats.toRadians

// todo create and test slider constraint
// -> why does it do nothing?
fun main() {

    ECSRegistry.init()
    val scene = Entity("Scene")
    val physics = BulletPhysics()
    scene.add(physics)
    physics.updateInEditMode = true
    scene.add(Skybox())

    val box0 = Entity()
    box0.add(MeshComponent(flatCube.front))
    box0.add(BoxCollider())
    box0.position = box0.position.set(0.0, 2.3, 0.0)
    val body0 = Rigidbody()
    body0.mass = 1.0
    box0.add(body0)
    scene.add(box0)

    val box1 = Entity()
    box1.add(MeshComponent(flatCube.front))
    box1.add(BoxCollider())
    box1.rotation = box1.rotation.rotateX(5.0.toRadians())
    val body1 = Rigidbody()
    box1.add(body1)
    scene.add(box1)

    val sliding = SliderConstraint()
    box0.add(sliding)
    sliding.enableLinearMotor = true
    sliding.motorMaxForce = 10000.0
    sliding.targetMotorVelocity = 0.0
    sliding.disableCollisionsBetweenLinked = true
    sliding.other = body1

    testSceneWithUI("Slider Constraint", scene)
}