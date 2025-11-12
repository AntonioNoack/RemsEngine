package me.anno.tests.physics.bullet

import com.bulletphysics.collision.broadphase.CollisionAlgorithmConstructionInfo
import com.bulletphysics.collision.dispatch.CollisionDispatcher
import com.bulletphysics.collision.dispatch.ConvexPlaneCollisionAlgorithm
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.CompoundShape
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.collision.shapes.StaticPlaneShape
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
import me.anno.utils.assertions.assertEquals
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test

// to do test a compound shape vs a regular shape,
//  check all properties, and see that they're equal!
class CompoundColliderTest {

    val base: CollisionShape = SphereShape(1f)
    val compound: CollisionShape = CompoundShape().apply {
        addChildShape(Transform(), base)
    }

    val baseBody = RigidBody(1f, base, base.calculateLocalInertia(1f, Vector3f()))
    val compoundBody = RigidBody(1f, compound, compound.calculateLocalInertia(1f, Vector3f()))

    val floor = StaticPlaneShape(Vector3f(0.0, 1.0, 0.0), -1.0)
    val floorBody = RigidBody(0f, floor)

    @Test
    fun testLocalInertia() {
        val baseInertia = base.calculateLocalInertia(1f, Vector3f())
        val compoundInertia = compound.calculateLocalInertia(1f, Vector3f())
        assertEquals(baseInertia, compoundInertia)
    }

    @Test
    fun testInvInertia() {
        assertEquals(baseBody.invInertiaLocal, compoundBody.invInertiaLocal)
        assertEquals(baseBody.invInertiaTensorWorld, compoundBody.invInertiaTensorWorld)
    }

    @Test
    fun testGetBounds() {
        val t = Transform()
        t.basis.rotateYXZ(0.1f, 0.2f, 0.3f)
        val min1 = Vector3d()
        val max1 = Vector3d()
        val base = BoxShape(Vector3f(1f)) // needed here, sphere has a better implementation
        base.getBounds(t, min1, max1)
        val min0 = Vector3d()
        val max0 = Vector3d()
        compound.getBounds(t, min0, max0)
        assertEquals(min1, min0)
        assertEquals(max1, max0)
    }

    @Test
    fun testCollisionAlgorithms() {

        val dispatcher = CollisionDispatcher(DefaultCollisionConfiguration())
        val ci = CollisionAlgorithmConstructionInfo()
        ci.dispatcher1 = dispatcher

        val baseAlgo = ConvexPlaneCollisionAlgorithm()
        val baseManifold = PersistentManifold()
        baseManifold.init(baseBody, floorBody)
        ci.manifold = baseManifold
        baseAlgo.init(baseManifold, ci, baseBody, floorBody, false)
       /* baseAlgo.calculateTimeOfImpact() // not really implemented -> I don't think we need to test this
        baseAlgo.getAllContactManifolds() // just returns manifold stored inside
        baseAlgo.processCollision()
        // todo make them collide slightly and see what their response is

        val compoundAlgo = CompoundCollisionAlgorithm()
        compoundAlgo.calculateTimeOfImpact()
        compoundAlgo.getAllContactManifolds()
        compoundAlgo.processCollision()*/
    }
}