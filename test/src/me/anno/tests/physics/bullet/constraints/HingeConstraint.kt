package me.anno.tests.physics.bullet.constraints

import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.StaticBody
import me.anno.bullet.constraints.HingeConstraint
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.Axis
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.systems.Systems
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector3f

/**
 * create and test hinge constraint
 *
 * If it is broken, restart it.
 * todo why is this non-deterministic, and breaking, sometimes? (why at all)
 * */
fun main() {

    ECSRegistry.init()
    val scene = Entity("Scene")
    val physics = BulletPhysics()
    Systems.registerSystem(physics)
    physics.updateInEditMode = true

    val box0 = Entity("Door", scene)
    box0.add(MeshComponent(flatCube.scaled(Vector3f(0.45f, 1.0f, 0.04f)).front))
    box0.add(BoxCollider().apply {
        halfExtents.set(0.45, 1.0, 0.04)
    })
    box0.setPosition(0.0, 2.3, 0.0)
    val body0 = DynamicBody()
    body0.mass = 1f
    box0.add(body0)

    // todo why is the hinge flopping downwards, when it is invalidated???
    //  - given a second hinge, everything fails catastrophically, when we invalidate a hinge :(

    val pillarBody = StaticBody()
    Entity("Pillar", scene)
        .add(MeshComponent(flatCube.scaled(Vector3f(0.1f, 1.0f, 0.1f)).front))
        .add(BoxCollider().apply { halfExtents.set(0.1f, 1f, 0.1f) })
        .setRotation(5f.toRadians(), 0f, 0f)
        .add(pillarBody)

    // todo one hinge probably should be enough...
    fun addHinge(y: Double) {
        val sliding = HingeConstraint()
        sliding.description = "Toggle 'Enable Motor' to open/close the door"
        sliding.axis = Axis.Y
        sliding.motorTorque = 100.0f
        sliding.motorVelocity = -3.0f
        sliding.enableMotor = false
        sliding.otherPosition.set(0.0, y, 0.0)
        sliding.selfPosition.set(0.55, y, 0.0)
        sliding.other = pillarBody
        box0.add(sliding)
    }
    addHinge(-0.9)
    addHinge(+0.9)

    Entity("Floor", scene)
        .add(MeshComponent(flatCube.front, Material.diffuse(0x333333)))
        .add(BoxCollider())
        .add(DynamicBody().apply {
            friction = 1.0f
        })
        .setPosition(0.0, -22.0, 0.0)
        .setScale(20f)

    testSceneWithUI("Hinge Constraint", scene)
}