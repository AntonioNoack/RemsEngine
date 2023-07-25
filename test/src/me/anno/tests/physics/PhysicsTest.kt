package me.anno.tests.physics

import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.SphereCollider

fun main() {

    val world = Entity()
    val physics = BulletPhysics()
    world.add(physics)

    world.create()

    val gravity = -9.81
    val duration = 1.0

    val y0 = 100.0

    val targetFallLength = 0.5 * gravity * duration * duration

    val sphere = Entity()
    sphere.transform.globalTransform.translate(-1.0, y0, 0.0)
    sphere.add(Rigidbody().apply { mass = 1.0 })
    sphere.add(SphereCollider())
    physics.addOrGet(sphere)

    // moves the other sphere to the side
    val sphere2 = Entity()
    sphere2.transform.globalTransform.translate(1.0, y0, 0.0)
    sphere2.add(Rigidbody().apply { mass = 1.0 })
    sphere2.add(SphereCollider())
    physics.addOrGet(sphere2)

    val steps = (duration * 60.0).toInt()
    for (i in 0 until steps) physics.step((duration / steps * 1e9).toLong(), true)

    println(sphere.transform.globalTransform.m30)
    println(y0 - sphere.transform.globalTransform.m31)
    println(sphere.transform.globalTransform.m32)
    // println(sphere.transform.time)

    println("target: $targetFallLength")


}