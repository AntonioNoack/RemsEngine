package me.anno.tests.physics.bullet

import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.StaticBody
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.InfinitePlaneCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.systems.Systems.registerSystem
import me.anno.engine.DefaultAssets.icoSphere
import me.anno.engine.DefaultAssets.plane
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.ui.UIColors

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
        .setRotationDegrees(3f, 0f, 0f)
        .add(StaticBody())
        .setScale(10f)

    Entity("Normal Sphere", scene)
        .add(MeshComponent(icoSphere, Material.diffuse(UIColors.greenYellow)))
        .add(SphereCollider())
        .add(DynamicBody())
        .setPosition(-1.5, 1.0, 0.0)

    Entity("Weird Sphere", scene)
        .add(MeshComponent(icoSphere, Material.diffuse(UIColors.fireBrick)))
        .add(SphereCollider())
        .add(DynamicBody().apply {
            centerOfMass.set(0.0, 0.25, 0.0)
            mass = 1.0f
        })
        .setPosition(+1.5, 1.0, 0.0)

    testSceneWithUI("CenterOfMass", scene)
}