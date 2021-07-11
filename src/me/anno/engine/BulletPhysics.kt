package me.anno.engine

import com.bulletphysics.collision.broadphase.DbvtBroadphase
import com.bulletphysics.collision.dispatch.CollisionDispatcher
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration
import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.CompoundShape
import com.bulletphysics.dynamics.DiscreteDynamicsWorld
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.RigidBodyConstructionInfo
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver
import com.bulletphysics.linearmath.DefaultMotionState
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.physics.Rigidbody
import org.joml.Matrix4x3d
import javax.vecmath.Matrix4d
import javax.vecmath.Quat4d
import javax.vecmath.Vector3d
import kotlin.math.max

class BulletPhysics {

    companion object {
        fun convertMatrix(ourTransform: Matrix4x3d): Matrix4d {
            return Matrix4d(// we have to transpose the matrix, because joml uses Axy and vecmath uses Ayx
                ourTransform.m00(), ourTransform.m10(), ourTransform.m20(), ourTransform.m30(),
                ourTransform.m01(), ourTransform.m11(), ourTransform.m21(), ourTransform.m31(),
                ourTransform.m02(), ourTransform.m12(), ourTransform.m22(), ourTransform.m32(),
                0.0, 0.0, 0.0, 1.0
            )
        }
    }

    // todo a play button
    // todo test the physics

    // I use jBullet2, however I have modified it to use doubles for everything
    // this may be bad for performance, but it also allows our engine to run much larger worlds
    // if we need top-notch-performance, I just should switch to a native implementation

    val world = createBulletWorldWithGroundNGravity()

    val map = HashMap<Entity, RigidBody>()

    fun add(entity: Entity) {
        // todo add including constraints and such...
        // todo colliders in children?
        val base = entity.getComponent<Rigidbody>() ?: return
        if (base.isEnabled) {
            // todo only collect colliders, which are appropriate for this: stop at any other rigidbody
            // todo also only collect physics colliders, not click-colliders
            val colliders = entity.getComponentsInChildren<Collider>()
                .filter { it.isEnabled }
            if (colliders.isNotEmpty()) {

                // copy all knowledge from ecs to bullet
                val jointCollider = CompoundShape()
                val mass = max(0.0, base.mass)
                for (collider in colliders) {
                    val (transform, subCollider) = collider.createBulletCollider(entity)
                    jointCollider.addChildShape(transform, subCollider)
                }

                val inertia = Vector3d()
                if (mass > 0) jointCollider.calculateLocalInertia(mass, inertia)

                val bulletTransform = Transform(convertMatrix(entity.transform.globalTransform))

                // convert the center of mass to a usable transform
                val com0 = base.centerOfMass
                val com1 = Vector3d(com0.x, com0.y, com0.z)
                val com2 = Transform(Matrix4d(Quat4d(0.0, 0.0, 0.0, 1.0), com1, 1.0))

                // create the motion state
                val motionState = DefaultMotionState(bulletTransform, com2)
                val rbInfo = RigidBodyConstructionInfo(mass, motionState, jointCollider, inertia)
                val body = RigidBody(rbInfo)
                map[entity] = body

            }
        }
    }

    fun remove(entity: Entity) {
        val rigid = map[entity] ?: return
        world.removeRigidBody(rigid)
        map.remove(entity)
    }

    fun update(entity: Entity) {
        remove(entity)
        add(entity)
    }

    fun step(dt: Double) {
        world.stepSimulation(dt)
        // todo update all transforms, where needed
    }

    fun createBulletWorld(): DiscreteDynamicsWorld {
        val collisionConfig = DefaultCollisionConfiguration()
        val dispatcher = CollisionDispatcher(collisionConfig)
        val bp = DbvtBroadphase()
        val solver = SequentialImpulseConstraintSolver()
        return DiscreteDynamicsWorld(dispatcher, bp, solver, collisionConfig)
    }

    fun createBulletWorldWithGroundNGravity(): DiscreteDynamicsWorld {
        val world = createBulletWorld()
        world.setGravity(Vector3d(0.0, -9.81, 0.0))
        addGround(world)
        return world
    }

    fun addGround(world: DiscreteDynamicsWorld) {

        val ground = BoxShape(Vector3d(2000.0, 100.0, 2000.0))

        val groundTransform = Transform()
        groundTransform.setIdentity()
        groundTransform.origin.set(0.0, -100.0, 0.0)

        // mass = 0f = static
        val motionState = DefaultMotionState(groundTransform)
        val rbInfo = RigidBodyConstructionInfo(0.0, motionState, ground, Vector3d())
        val body = RigidBody(rbInfo)

        world.addRigidBody(body)

    }

}