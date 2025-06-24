package me.anno.tests.physics.constraints

import me.anno.bullet.BulletPhysics
import me.anno.bullet.DynamicBody
import me.anno.bullet.constraints.PointConstraint
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

/**
 * create and test point constraints
 * */
fun main() {

    ECSRegistry.init()
    val scene = Entity("Scene")
    val physics = BulletPhysics()
    Systems.registerSystem(physics)
    physics.updateInEditMode = true

    val box0 = Entity("Door", scene)
        .add(MeshComponent(flatCube.scaled(Vector3f(0.45f, 1.0f, 0.04f)).front))
        .add(BoxCollider().apply {
            halfExtents.set(0.45, 1.0, 0.04)
        })
        .setPosition(0.0, 2.3, 0.0)
        .add(DynamicBody().apply {
            mass = 1.0
            angularDamping = 0.1
        })

    val pillarRB = DynamicBody()
    Entity("Pillar", scene)
        .add(MeshComponent(flatCube.scaled(Vector3f(0.1f, 1.0f, 0.1f)).front))
        .add(BoxCollider().apply { halfExtents.set(0.1f, 1f, 0.1f) })
        .setRotation(5f.toRadians(), 0f, 0f)
        .add(pillarRB)

    fun addHinge(y: Double) {
        val constraint = PointConstraint()
        constraint.otherPosition.set(0.0, y, 0.0)
        constraint.selfPosition.set(0.55, y, 0.0)
        constraint.other = pillarRB
        box0.add(constraint)
    }
    addHinge(-0.9)
    addHinge(+0.9)

    Entity("Floor", scene)
        .add(MeshComponent(flatCube.front, Material.diffuse(0x333333)))
        .add(BoxCollider())
        .add(DynamicBody().apply {
            friction = 1.0
        })
        .setPosition(0.0, -1.5, 0.0)
        .setScale(1f, 0.3f, 1f)

    testSceneWithUI("Point Constraint", scene)
}