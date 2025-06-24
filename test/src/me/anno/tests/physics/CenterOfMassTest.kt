package me.anno.tests.physics

import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.InfinitePlaneCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.systems.Systems.registerSystem
import me.anno.engine.DefaultAssets.icoSphere
import me.anno.engine.DefaultAssets.plane
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

/**
 * test rolling sphere
 * test rolling sphere with mass offset
 *
 * todo test manipulated dice 🤩
 * */
fun main() {

    val physics = BulletPhysics()
    registerSystem(physics)

    val scene = Entity()
    Entity("Floor", scene)
        .add(MeshComponent(plane))
        .add(InfinitePlaneCollider())
        .setRotationDegrees(10f, 0f, 0f)
        .add(Rigidbody().apply { mass = 0.0 })
        .setScale(10f)

    Entity("Normal Sphere", scene)
        .add(MeshComponent(icoSphere))
        .add(SphereCollider())
        .add(Rigidbody().apply { mass = 1.0 })
        .setPosition(-1.5, 1.0, 0.0)

    Entity("Weird Sphere", scene)
        .add(MeshComponent(icoSphere))
        .add(SphereCollider())
        .add(Rigidbody().apply {
            centerOfMass.set(0.0, 0.01, 0.0)
            mass = 1.0
        })
        .setPosition(+1.5, 1.0, 0.0)

    testSceneWithUI("CenterOfMass", scene)
}