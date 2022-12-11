package me.anno.ecs.components.physics.twod

import me.anno.ecs.Component
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable
import org.jbox2d.dynamics.Body
import org.joml.Vector2f

class Rigidbody2d : Component() {

    var box2dInstance: Body? = null
    var gravityScale = 1f

    var linearDamping = 0f
        set(value) {
            field = value
            box2dInstance?.linearDamping = value
        }

    var linearVelocity = Vector2f()
        get() {
            val bi = box2dInstance
            if (bi != null) {
                val vi = bi.linearVelocity
                field.set(vi.x, vi.y)
            }
            return field
        }
        set(value) {
            field.set(value)
            box2dInstance?.linearVelocity?.set(value.x, value.y)
        }

    var angularDamping = 0f
        set(value) {
            field = value
            box2dInstance?.angularDamping = value
        }

    // todo when editing this, why is the value of angular damping changed as well???
    var angularVelocity = 0f
        get() {
            val bi = box2dInstance
            if (bi != null) {
                field = bi.angularVelocity
            }
            return field
        }
        set(value) {
            field = value
            box2dInstance?.angularVelocity = value
        }

    var preventRotation = false
        set(value) {
            field = value
            box2dInstance?.isFixedRotation = value
        }

    var preventTunneling = false

    var alwaysActive = false
        set(value) {
            field = value
            box2dInstance?.isSleepingAllowed = !value
        }

    fun invalidatePhysics() {
        val entity = entity ?: return
        entity.physics2d?.invalidate(entity)
    }

    fun applyForce(force: Vector2f) {
        applyForce(force.x, force.y)
    }

    fun applyForce(fx: Float, fy: Float) {
        val tmp = Box2dPhysics.vec2f.borrow().set(fx, fy)
        box2dInstance?.applyForceToCenter(tmp)
    }

    fun applyForce(force: Vector2f, point: Vector2f) {
        applyForce(force.x, force.y, point.x, point.y)
    }

    fun applyForce(forceX: Float, forceY: Float, pointX: Float, pointY: Float) {
        val force = Box2dPhysics.vec2f.create().set(forceX, forceY)
        val point = Box2dPhysics.vec2f.create().set(pointX, pointY)
        box2dInstance?.applyForce(force, point)
        Box2dPhysics.vec2f.sub(2)
    }

    fun applyImpulse(impulse: Vector2f, point: Vector2f, wake: Boolean = true) {
        applyImpulse(impulse.x, impulse.y, point.x, point.y, wake)
    }

    fun applyImpulse(impulseX: Float, impulseY: Float, pointX: Float, pointY: Float, wake: Boolean = true) {
        val impulse = Box2dPhysics.vec2f.create().set(impulseX, impulseY)
        val point = Box2dPhysics.vec2f.create().set(pointX, pointY)
        box2dInstance?.applyLinearImpulse(impulse, point, wake)
        Box2dPhysics.vec2f.sub(2)
    }

    fun applyAngularImpulse(impulse: Float) {
        box2dInstance?.applyAngularImpulse(impulse)
    }

    val inertia get() = box2dInstance?.inertia

    override fun clone(): Rigidbody2d {
        val clone = Rigidbody2d()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Rigidbody2d
        clone.gravityScale = gravityScale
        clone.linearDamping = linearDamping
        clone.linearVelocity.set(linearVelocity)
        clone.angularDamping = angularDamping
        clone.angularVelocity = angularVelocity
        clone.preventRotation = preventRotation
        clone.preventTunneling = preventTunneling
        clone.alwaysActive = alwaysActive
    }

    override val className get() = "Rigidbody2d"

}