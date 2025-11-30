package me.anno.tests.physics.bullet

import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.systems.Systems
import me.anno.tests.physics.testStep

fun main() {

    val world = Entity()
    val physics = BulletPhysics()
    Systems.registerSystem(physics)

    world.onEnable()

    val gravity = -9.81
    val duration = 1.0

    val y0 = 100.0

    val targetFallLength = 0.5 * gravity * duration * duration

    val sphere = Entity()
    sphere.transform.globalTransform.translate(-1.0, y0, 0.0)
    sphere.add(DynamicBody())
    sphere.add(SphereCollider())
    physics.addOrGet(sphere)

    // moves the other sphere to the side
    val sphere2 = Entity()
    sphere2.transform.globalTransform.translate(1.0, y0, 0.0)
    sphere2.add(DynamicBody())
    sphere2.add(SphereCollider())
    physics.addOrGet(sphere2)

    val steps = (duration * 60.0).toInt()
    physics.stepsPerSecond = (steps / duration).toFloat()
    repeat(steps) {
        physics.testStep()
    }

    println(sphere.transform.globalTransform.m30)
    println(y0 - sphere.transform.globalTransform.m31)
    println(sphere.transform.globalTransform.m32)
    // println(sphere.transform.time)

    println("target: $targetFallLength")
}