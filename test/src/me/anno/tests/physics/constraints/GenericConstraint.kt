package me.anno.tests.physics.constraints

import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.StaticBody
import me.anno.bullet.constraints.GenericConstraint
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.systems.Systems
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector3f
import kotlin.math.PI

/**
 * create and test generic constraints
 *
 * todo test all properties
 * */
fun main() {

    ECSRegistry.init()
    val scene = Entity("Scene")
    val physics = BulletPhysics()
    Systems.registerSystem(physics)
    physics.updateInEditMode = true

    val pillarBody = StaticBody()
    Entity("Pillar", scene)
        .add(MeshComponent(flatCube.scaled(Vector3f(0.1f, 1.0f, 0.1f)).front))
        .add(BoxCollider().apply { halfExtents.set(0.1f, 1f, 0.1f) })
        .setRotation(5f.toRadians(), 0f, 0f)
        .add(pillarBody)

    val sliding = GenericConstraint().apply {
        otherPosition.set(0.0, 0.0, 0.0)
        selfPosition.set(0.75, 0.0, 0.0)
        lowerAngleLimit.set(0.0, -PI / 2, 0.0)
        upperAngleLimit.set(0.0, +PI / 2, 0.0)
        other = pillarBody
    }

    Entity("Door", scene)
        .add(MeshComponent(flatCube.scaled(Vector3f(0.45f, 1.0f, 0.04f)).front))
        .add(BoxCollider().apply {
            halfExtents.set(0.45, 1.0, 0.04)
        })
        .setPosition(0.0, 2.3, 0.0)
        .add(DynamicBody())
        .add(sliding)

    // todo this is very unstable... why??

    Entity("Floor", scene)
        .add(MeshComponent(flatCube.front, Material.diffuse(0x333333)))
        .add(BoxCollider())
        .add(StaticBody().apply {
            friction = 1.0
        })
        .setPosition(0.0, -22.0, 0.0)
        .setScale(20f)

    testSceneWithUI("Generic Constraint", scene)
}