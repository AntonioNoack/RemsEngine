package me.anno.tests.physics

import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.systems.Systems
import me.anno.maths.Maths
import org.joml.Vector3d

fun main() {
    libraryTest3d()
    engineTest3d()
}

private fun libraryTest3d() {
    println("Library Test")
    // create test world
    // test gravity and maybe collisions
    val world = BulletPhysics.createBulletWorld()

    world.addRigidBody(
        RigidBody(
            0.0,
            BoxShape(Vector3d(50.0, 5.0, 50.0)),
            Vector3d()
        ).apply {
            val transform = worldTransform
            transform.setIdentity()
            transform.setTranslation(0.0, -10.0, 0.0)
            setInitialTransform(transform)
        }
    )

    val boxBody = RigidBody(
        1.0,
        BoxShape(Vector3d(1.0, 1.0, 1.0)),
        Vector3d()
    ).apply {
        val transform = worldTransform
        transform.setIdentity()
        transform.setTranslation(0.0, +10.0, 0.0)
        setInitialTransform(transform)
    }

    world.addRigidBody(boxBody)

    repeat(10) {
        println(boxBody.getWorldTransform(Transform()).origin)
        world.stepSimulation(1.0, 10, 0.1)
    }
}

private fun engineTest3d() {
    println("Own World Test")
    // create the same world as in test 1, now just with our own classes
    // create test world
    val world = Entity()
    val physics = BulletPhysics()
    Systems.registerSystem(physics)
    val ground = Entity()
    ground.name = "Ground"
    val groundRB = DynamicBody()
    ground.add(groundRB)
    val groundShape = BoxCollider()
    groundShape.halfExtents.set(50.0, 5.0, 50.0)
    ground.setPosition(0.0, -10.0, 0.0)
    ground.add(groundShape)
    world.add(ground)
    // test gravity and maybe collisions
    val box = Entity()
    box.name = "Box"
    val boxRB = DynamicBody()
    box.add(boxRB)
    box.setPosition(0.0, 10.0, 0.0)
    val boxShape = BoxCollider()
    boxRB.mass = 1.0
    boxShape.halfExtents.set(1.0)
    box.add(boxShape)
    world.add(box)
    world.validateTransform()
    groundRB.invalidatePhysics()
    boxRB.invalidatePhysics()
    world.create()
    for (i in 0 until 10) {
        box.validateTransform()
        ground.validateTransform()
        println(
            box.position.toString() + ", " +
                    boxRB.bulletInstance?.getWorldTransform(Transform())?.origin + ", " +
                    groundRB.bulletInstance?.getWorldTransform(Transform())?.origin
        )
        for (j in 0 until 100) physics.step(Maths.MILLIS_TO_NANOS * 10, false)
    }
}