package me.anno.bulletjme

import me.anno.ecs.Entity
import me.anno.ecs.components.collider.BoxCollider
import me.anno.maths.Maths.MILLIS_TO_NANOS

fun main() {
    BulletJMEMod().onPreInit()
    val scene = Entity()
    val cube = Entity()
    cube.add(Rigidbody().apply {
        mass = 1.0
    })
    cube.add(BoxCollider())
    scene.add(cube)
    val physics = BulletPhysics()
    scene.add(physics)
    for (i in 0 until 10) {
        physics.step(100 * MILLIS_TO_NANOS, false)
        println(cube.position)
        // PhysicsDumper().dump(physics.bulletInstance)
    }
}