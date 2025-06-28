package me.anno.box2d

import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Vector2f

class DynamicBody2d : PhysicalBody2d() {

    var gravityScale = 1f

    var linearDamping = 0f
        set(value) {
            field = value
            nativeInstance?.linearDamping = value
        }

    var linearVelocity = Vector2f()
        get() {
            val bi = nativeInstance
            if (bi != null) {
                val vi = bi.linearVelocity
                field.set(vi.x, vi.y)
            }
            return field
        }
        set(value) {
            field.set(value)
            nativeInstance?.linearVelocity?.set(value.x, value.y)
        }

    var angularDamping = 0f
        set(value) {
            field = value
            nativeInstance?.angularDamping = value
        }

    var angularVelocity = 0f
        get() {
            val bi = nativeInstance
            if (bi != null) {
                field = bi.angularVelocity
            }
            return field
        }
        set(value) {
            field = value
            nativeInstance?.angularVelocity = value
        }

    var preventRotation = false
        set(value) {
            field = value
            nativeInstance?.isFixedRotation = value
        }

    var preventTunneling = false

    var alwaysActive = false
        set(value) {
            field = value
            nativeInstance?.isSleepingAllowed = !value
        }

    fun applyForce(force: Vector2f) {
        applyForce(force.x, force.y)
    }

    fun applyForce(fx: Float, fy: Float) {
        val tmp = Box2dPhysics.vec2f.borrow().set(fx, fy)
        nativeInstance?.applyForceToCenter(tmp)
    }

    fun applyForce(force: Vector2f, point: Vector2f) {
        applyForce(force.x, force.y, point.x, point.y)
    }

    fun applyForce(forceX: Float, forceY: Float, pointX: Float, pointY: Float) {
        val pool = Box2dPhysics.vec2f
        val force = pool.create().set(forceX, forceY)
        val point = pool.create().set(pointX, pointY)
        nativeInstance?.applyForce(force, point)
        pool.sub(2)
    }

    @Suppress("unused")
    fun applyImpulse(impulse: Vector2f, point: Vector2f, wake: Boolean = true) {
        applyImpulse(impulse.x, impulse.y, point.x, point.y, wake)
    }

    fun applyImpulse(impulseX: Float, impulseY: Float, pointX: Float, pointY: Float, wake: Boolean = true) {
        val pool = Box2dPhysics.vec2f
        val impulse = pool.create().set(impulseX, impulseY)
        val point = pool.create().set(pointX, pointY)
        nativeInstance?.applyLinearImpulse(impulse, point, wake)
        pool.sub(2)
    }

    @Suppress("unused")
    fun applyAngularImpulse(impulse: Float) {
        nativeInstance?.applyAngularImpulse(impulse)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is DynamicBody2d) return
        dst.gravityScale = gravityScale
        dst.linearDamping = linearDamping
        dst.linearVelocity.set(linearVelocity)
        dst.angularDamping = angularDamping
        dst.angularVelocity = angularVelocity
        dst.preventRotation = preventRotation
        dst.preventTunneling = preventTunneling
        dst.alwaysActive = alwaysActive
    }
}