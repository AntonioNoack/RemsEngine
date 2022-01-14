package me.anno.ecs.components.physics

import me.anno.ecs.Component
import me.anno.gpu.GFX
import me.anno.io.serialization.NotSerializedProperty
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World
import org.joml.Vector2f
import kotlin.math.min

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
class Box2dPhysics : Component() {

    var gravity: Vector2f = Vector2f(0f, -9.81f)
        set(value) {
            field = value
            world.gravity.set(value.x, value.y)
        }

    @NotSerializedProperty
    private val world = World(Vec2(gravity.x, gravity.y))

    @NotSerializedProperty
    private var lastTime = 0L

    var maxTimeStep = 0.1f

    var velocityIterations = 6
    var positionIterations = 2

    override fun onUpdate(): Int {
        val time = GFX.gameTime
        val deltaTime = time - lastTime
        if (deltaTime > 0L) {
            lastTime = time
            val deltaTimeF = min(deltaTime * 1e-9f, maxTimeStep)
            step(deltaTimeF)
        }
        return 1
    }

    fun step(dt: Float) {
        world.step(dt, velocityIterations, positionIterations)
        // todo update all children positions like in BulletPhysics

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