package me.anno.tests.physics.bullet

import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.dynamics.RigidBody
import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.StaticBody
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.systems.Systems
import me.anno.tests.physics.testStep
import org.joml.Vector3f

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
            0f,
            BoxShape(Vector3f(50.0, 5.0, 50.0)),
            Vector3f()
        ).apply {
            val transform = worldTransform
            transform.setIdentity()
            transform.setTranslation(0.0, -10.0, 0.0)
            setInitialTransform(transform)
        }
    )

    val boxBody = RigidBody(
        1f,
        BoxShape(Vector3f(1.0, 1.0, 1.0)),
        Vector3f()
    ).apply {
        val transform = worldTransform
        transform.setIdentity()
        transform.setTranslation(0.0, +10.0, 0.0)
        setInitialTransform(transform)
    }

    world.addRigidBody(boxBody)

    repeat(10) {
        println(boxBody.worldTransform.origin)
        world.stepSimulation(1.0f, 10, 0.1f)
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
    val groundRB = StaticBody()
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
    boxRB.mass = 1.0f
    boxShape.halfExtents.set(1.0)
    box.add(boxShape)
    world.add(box)
    world.validateTransform()
    groundRB.invalidatePhysics()
    boxRB.invalidatePhysics()
    world.onEnable()
    repeat(10) {
        box.validateTransform()
        ground.validateTransform()
        println(
            box.position.toString() + ", " +
                    boxRB.nativeInstance?.worldTransform?.origin + ", " +
                    groundRB.nativeInstance?.worldTransform?.origin
        )
        physics.stepsPerSecond = 100f
        repeat(100) {
            physics.testStep()
        }
    }
}