package me.anno.tests.physics.constraints

import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.bullet.constraints.PointConstraint
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
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
    scene.add(physics)
    physics.updateInEditMode = true

    val box0 = Entity("Door", scene)
    box0.add(MeshComponent(flatCube.scaled(Vector3f(0.45f, 1.0f, 0.04f)).front))
    box0.add(BoxCollider().apply {
        halfExtends.set(0.45, 1.0, 0.04)
    })
    box0.setPosition(0.0, 2.3, 0.0)
    val body0 = Rigidbody()
    body0.mass = 1.0
    box0.add(body0)

    val box1 = Entity("Pillar", scene)
    box1.add(MeshComponent(flatCube.scaled(Vector3f(0.1f, 1.0f, 0.1f)).front))
    box1.add(BoxCollider().apply { halfExtends.set(0.1f, 1f, 0.1f) })
    box1.setRotation(5.0.toRadians(), 0.0, 0.0)
    val body1 = Rigidbody()
    box1.add(body1)

    fun addHinge(y: Double) {
        val sliding = PointConstraint()
        sliding.otherPosition.set(0.0, y, 0.0)
        sliding.selfPosition.set(0.55, y, 0.0)
        sliding.other = body1
        box0.add(sliding)
    }
    addHinge(-0.9)
    addHinge(+0.9)

    val floor = Entity("Floor", scene)
    floor.add(MeshComponent(flatCube.front, Material.diffuse(0x333333)))
    floor.add(BoxCollider())
    floor.add(Rigidbody().apply {
        friction = 1.0
    })
    floor.setPosition(0.0, -22.0, 0.0)
    floor.setScale(20.0)

    testSceneWithUI("Point Constraint", scene)
}