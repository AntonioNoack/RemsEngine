package me.anno.ecs.components.physics.twod

import me.anno.ecs.Entity
import me.anno.ecs.components.collider.twod.Collider2d
import me.anno.ecs.components.physics.BodyWithScale
import me.anno.ecs.components.physics.Physics
import me.anno.io.serialization.NotSerializedProperty
import org.jbox2d.collision.AABB
import org.jbox2d.collision.RayCastInput
import org.jbox2d.collision.RayCastOutput
import org.jbox2d.collision.shapes.MassData
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.collision.shapes.Shape
import org.jbox2d.collision.shapes.ShapeType
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.*
import org.joml.Matrix3x2f
import org.joml.Matrix4x3d
import org.joml.Vector2f
import org.joml.Vector3d
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * docs: https://box2d.org/documentation/
 * todo stepping like BulletPhysics, maybe join them
 * todo:
 *  - Rigid body physics
 *  - Stable stacking
 *  - Gravity
 *  - Fast persistent contact solver
 *  - Dynamic tree broadphase
 *  - Sliding friction
 *  - Boxes, circles, edges and polygons
 *  - Several joint types: distance, revolute, prismatic, pulley, gear, mouse
 *  - Motors
 *  - Sleeping (removes motionless bodies from simulation until touched)
 *  - Continuous collision detection (accurate solving of fast bodies)
 *  - Ray casts
 *  - Sensors
 *  - Dynamic, Kinematic, and Static bodies
 *  - Liquid particle simulation from Google's LiquidFun
 * Serialization? idk, should be done by the engine
 * */
class Box2dPhysics : Physics<Rigidbody2d, Body>(Rigidbody2d::class) {

    @NotSerializedProperty
    private val world = World(Vec2(gravity.x.toFloat(), gravity.y.toFloat()))

    override fun updateGravity() {
        world.gravity.set(gravity.x.toFloat(), gravity.y.toFloat())
    }

    override fun worldStepSimulation(step: Double) {
        world.step(step.toFloat(), velocityIterations, positionIterations)
    }

    var velocityIterations = 6
    var positionIterations = 2

    override fun createRigidbody(entity: Entity, rigidBody: Rigidbody2d): BodyWithScale<Body>? {


        val colliders = getValidComponents(entity, Collider2d::class).toList()
        return if (colliders.isNotEmpty()) {

            // bullet does not work correctly with scale changes: create larger shapes directly
            val globalTransform = entity.transform.globalTransform

            val scale = globalTransform.getScale(Vector3d())

            // todo collect colliders
            // todo if they are empty, don't create body with scale
            val def = BodyDef()
            val transform = entity.transform
            val global = transform.globalTransform
            def.position.set(global.m30().toFloat(), global.m31().toFloat())
            def.angle = atan2(global.m10(), global.m00()).toFloat() // not perfect, but good enough probably
            def.fixedRotation // todo set that
            def.type = BodyType.DYNAMIC // todo set that depending on state... Kinematic?
            def.allowSleep // todo flag for that
            def.angularDamping // todo set that
            def.angularVelocity // todo set that
            def.bullet
            def.gravityScale
            def.linearDamping
            def.linearVelocity
            def.userData = rigidBody // maybe for callbacks :)
            val body = world.createBody(def)

            val scale2f = Vector2f(scale.x.toFloat(), scale.y.toFloat())

            for (collider in colliders) {
                val (trans, shape) = collider.createBox2dCollider(entity, scale2f)
                // todo translate / rotate / scale the shape...
                // add shape
                val fixDef = FixtureDef()
                fixDef.shape = shape
                fixDef.density = collider.density
                fixDef.friction = collider.friction
                fixDef.restitution
                fixDef.isSensor
                fixDef.filter // todo this probably is the collision mask
                fixDef.userData
                body.createFixture(fixDef)
            }

            return BodyWithScale(body, scale)

        } else null

    }

    override fun onCreateRigidbody(entity: Entity, rigidbody: Rigidbody2d, bodyWithScale: BodyWithScale<Body>) {
        // todo constraints & such
    }

    override fun worldRemoveRigidbody(rigidbody: Body) {
        world.destroyBody(rigidbody)
    }

    override fun isActive(rigidbody: Body) = rigidbody.isActive

    override fun convertTransformMatrix(rigidbody: Body, scale: Vector3d, dstTransform: Matrix4x3d) {
        val pos = rigidbody.position
        val angle = rigidbody.angle
        val c = cos(angle).toDouble()
        val s = sin(angle).toDouble()
        dstTransform.set(// I hope this matrix is correct
            +c * scale.x, -s * scale.x, 0.0,
            +s * scale.y, +c * scale.y, 0.0,
            0.0, 0.0, scale.z,
            pos.x.toDouble(), pos.y.toDouble(), 0.0
        )
    }

    override fun clone(): Box2dPhysics {
        val clone = Box2dPhysics()
        copy(clone)
        return clone
    }

    override val className: String = "Box2dPhysics"

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
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
            println(boxBody.position)
            for (i in 0 until 10) {
                world.step(1f, 1, 1)
                println(boxBody.position)
            }
        }

    }

}