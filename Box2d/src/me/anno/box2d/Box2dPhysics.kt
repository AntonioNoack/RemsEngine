package me.anno.box2d

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.physics.BodyWithScale
import me.anno.ecs.components.physics.Physics
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.maths.Maths.SQRT3
import me.anno.maths.Maths.sq
import me.anno.utils.Logging.hash32
import me.anno.utils.pooling.Stack
import org.apache.logging.log4j.LogManager
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.MassData
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World
import org.joml.Matrix4x3d
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * docs: https://box2d.org/documentation/
 * done stepping like BulletPhysics, maybe join them
 * todo:
 *  - Several joint types: distance, revolute, prismatic, pulley, gear, mouse
 *  - Motors
 *  - Ray casts
 *  - Sensors
 *  - Liquid particle simulation from Google's LiquidFun ( https://google.github.io/liquidfun/ )
 * done and partially tested:
 * stable stacking
 * Dynamic, Kinematic, and Static bodies; Kinematic skipped for now, as I don't understand it's use-case yet
 * Rigid body physics
 * Continuous collision detection (accurate solving of fast bodies)
 * Sliding friction
 * Gravity
 * Sleeping (removes motionless bodies from simulation until touched)
 * Serialization? idk, should be done by the engine
 * */
class Box2dPhysics : Physics<Rigidbody2d, Body>(Rigidbody2d::class) {

    var velocityIterations = 6
    var positionIterations = 2

    @NotSerializedProperty
    private val world = World(Vec2(gravity.x.toFloat(), gravity.y.toFloat()))

    init {
        Settings.maxTranslation = 1e6f
        Settings.maxTranslationSquared = sq(Settings.maxTranslation)
    }

    override fun invalidate(entity: Entity) {
        val rb = entity.getComponent(Rigidbody2d::class, false)?.entity ?: return
        if (printValidations) LOGGER.debug("Invalidated {}", hash32(this))
        invalidEntities.add(rb)
    }

    override fun updateGravity() {
        world.gravity.set(gravity.x.toFloat(), gravity.y.toFloat())
    }

    override fun worldStepSimulation(step: Double) {
        world.step(step.toFloat(), velocityIterations, positionIterations)
    }

    override fun createRigidbody(entity: Entity, rigidBody: Rigidbody2d): BodyWithScale<Body>? {

        val colliders = getValidComponents(entity, Collider2d::class).toList()

        return if (colliders.isNotEmpty()) {

            entity.validateTransform()

            val massData = MassData()
            var mass = 0f
            val shapes = colliders.map { collider ->
                val trans = collider.entity!!.fromLocalToOtherLocal(entity)
                val shape = when (collider) {
                    is CircleCollider -> {
                        val shape = CircleShape()
                        shape.radius = collider.radius
                        shape
                    }
                    is RectCollider -> {
                        val shape = PolygonShape()
                        val halfExtends = collider.halfExtends
                        shape.setAsBox(halfExtends.x, halfExtends.y)
                        shape
                    }
                    else -> throw NotImplementedError()
                }
                when (shape) {
                    is CircleShape -> {
                        shape.m_p.set(trans.m30.toFloat(), trans.m31.toFloat())
                        shape.radius *= (trans.getScaleLength() / SQRT3).toFloat()
                        // rotation doesn't matter
                    }
                    is PolygonShape -> {
                        if (
                            abs(trans.m00 - 1.0) +
                            abs(trans.m10) +
                            abs(trans.m30) +
                            abs(trans.m11 - 1.0) +
                            abs(trans.m11) +
                            abs(trans.m31) > 1e-5
                        ) {
                            val v0 = shape.vertices
                            // copy is unfortunately needed
                            val vertices = Array(v0.size) { i -> Vec2(v0[i]) } // must be an array
                            // transform all vertices individually
                            // translation, rotation and scale are all included automatically :)
                            for (i in 0 until shape.m_count) {
                                // this is correct, at least for an identity transform
                                val vert = vertices[i]
                                val vx = vert.x
                                val vy = vert.y
                                vert.set(
                                    (trans.m00 * vx + trans.m10 * vy + trans.m30).toFloat(),
                                    (trans.m01 * vx + trans.m11 * vy + trans.m31).toFloat(),
                                )
                            }
                            shape.set(
                                vertices,
                                shape.m_count
                            )
                        }
                    }
                    else -> LOGGER.warn("todo implement shape ${shape::class}")
                }
                if (collider.hasPhysics) {
                    shape.computeMass(massData, collider.density)
                    mass += massData.mass
                }
                shape
            }

            val def = BodyDef()
            val transform = entity.transform
            val global = transform.globalTransform
            def.position.set(global.m30.toFloat(), global.m31.toFloat())
            def.angle = -atan2(global.m10, global.m00).toFloat() // not perfect, but good enough probably
            def.type = if (mass > 0f) BodyType.DYNAMIC else BodyType.STATIC // set that depending on state... Kinematic?
            def.bullet = rigidBody.preventTunneling
            def.gravityScale = rigidBody.gravityScale
            def.linearDamping = rigidBody.linearDamping
            val lv = rigidBody.linearVelocity
            def.linearVelocity.set(lv.x, lv.y)
            def.angularDamping = rigidBody.angularDamping
            def.angularVelocity = rigidBody.angularVelocity
            def.fixedRotation = rigidBody.preventRotation
            // def.userData = rigidBody // maybe for callbacks :)

            val body = world.createBody(def)
            body.isSleepingAllowed = !rigidBody.alwaysActive

            for (index in colliders.indices) {
                val collider = colliders[index]
                val shape = shapes[index]
                // add shape
                val fixDef = FixtureDef()
                fixDef.shape = shape
                fixDef.density = collider.density
                fixDef.friction = collider.friction
                fixDef.restitution = collider.restitution
                fixDef.isSensor = !collider.hasPhysics
                fixDef.filter.maskBits = collider.collisionMask
                // fixDef.userData = collider // maybe could be used :)
                body.createFixture(fixDef)
            }

            return BodyWithScale(body, Vector3d(1.0))
        } else null
    }

    override fun onCreateRigidbody(entity: Entity, rigidbody: Rigidbody2d, bodyWithScale: BodyWithScale<Body>) {

        val body = bodyWithScale.body

        rigidbody.box2dInstance = body
        rigidBodies[entity] = bodyWithScale

        registerNonStatic(entity, !(body.fixtureList.density > 0f), bodyWithScale)

        // todo constraints
    }

    override fun removeConstraints(entity: Entity) {
        // todo constraints for Physics2d
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
        dstTransform.set(
            +c, +s, 0.0,
            -s, +c, 0.0,
            0.0, 0.0, 1.0,
            pos.x.toDouble(), pos.y.toDouble(), 0.0
        )
    }

    companion object {
        private val LOGGER = LogManager.getLogger(Box2dPhysics::class)
        val vec2f = Stack { Vec2() }
    }
}