package me.anno.tests.physics

import me.anno.ecs.Entity
import me.anno.ecs.components.collider.twod.RectCollider
import me.anno.ecs.components.physics.twod.Box2dPhysics
import me.anno.ecs.components.physics.twod.Rigidbody2d
import me.anno.maths.Maths
import me.anno.utils.types.Floats.toDegrees
import me.anno.utils.types.Vectors.print
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World
import org.joml.Vector3d

fun main() {
    test1()
    test2()
}

private fun test1() {
    println("Library Test")
    // works, just why is it not accelerating?
    // create test world
    val world = World(Vec2(0f, -9.81f))
    // test gravity and maybe collisions
    val groundDef = BodyDef()
    groundDef.position.set(0f, -10f)
    val groundBody = world.createBody(groundDef)
    val groundShape = PolygonShape()
    groundShape.setAsBox(50f, 5f)
    groundBody.createFixture(groundShape, 0f)
    val boxDef = BodyDef()
    boxDef.type = BodyType.DYNAMIC
    boxDef.position.set(0f, 10f)
    val boxBody = world.createBody(boxDef)
    val boxShape = PolygonShape()
    boxShape.setAsBox(1f, 1f)
    val fixtureDef = FixtureDef()
    fixtureDef.shape = boxShape
    fixtureDef.density = 1f
    fixtureDef.friction = 0.3f
    boxBody.createFixture(fixtureDef)
    for (i in 0 until 10) {
        println(boxBody.position.toString() + ", " + boxBody.angle.toDegrees() + "°")
        world.step(1f, 1, 1)
    }
}

private fun test2() {
    // why is the result slightly different?
    println("Own World Test")
    // create the same world as in test 1, now just with our own classes
    // create test world
    val world = Entity()
    val physics = Box2dPhysics()
    physics.velocityIterations = 1
    physics.positionIterations = 1
    world.add(physics)
    val ground = Entity()
    val groundRB = Rigidbody2d()
    ground.add(groundRB)
    val groundShape = RectCollider()
    groundShape.halfExtends.set(50f, 5f)
    ground.position = Vector3d(0.0, -10.0, 0.0)
    ground.add(groundShape)
    world.add(ground)
    // test gravity and maybe collisions
    val box = Entity()
    val boxRB = Rigidbody2d()
    box.add(boxRB)
    box.position = Vector3d(0.0, 10.0, 0.0)
    val boxShape = RectCollider()
    boxShape.density = 1f
    boxShape.halfExtends.set(1f, 1f)
    box.add(boxShape)
    world.add(box)
    world.validateTransform()
    groundRB.invalidatePhysics()
    boxRB.invalidatePhysics()
    for (i in 0 until 10) {
        box.validateTransform()
        ground.validateTransform()
        println(box.position.print() + ", " + (box.rotation.getEulerAnglesYXZ(Vector3d()).z.toDegrees()) + "°")
        physics.step(Maths.MILLIS_TO_NANOS * 1000, false)
    }
}