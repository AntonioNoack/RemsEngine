package me.anno.bullet.bodies

import com.bulletphysics.dynamics.RigidBody
import me.anno.bullet.constraints.Constraint
import me.anno.ecs.EntityTransform.getLocalXAxis
import me.anno.ecs.EntityTransform.getLocalYAxis
import me.anno.ecs.EntityTransform.getLocalZAxis
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.DebugWarning
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Group
import me.anno.ecs.annotations.Order
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.physics.Physics.Companion.hasValidComponents
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import org.joml.Vector3d
import org.joml.Vector3f

abstract class PhysicalBody : PhysicsBody<RigidBody>() {

    @NotSerializedProperty
    val activeConstraints = ArrayList<Constraint<*>>()

    /**
     * Slowing down on contact. 1.0 = high traction, 0.0 = like on black ice
     * */
    @Group("Movement")
    @Range(0.0, 1.0)
    var friction = 0.5f
        set(value) {
            field = value
            nativeInstance?.friction = friction
        }

    @Docs("How elastic a body is, 1 = fully elastic, 0 = all energy absorbed (knead)")
    @Range(0.0, 1.0)
    var restitution = 0f
        set(value) {
            field = value
            nativeInstance?.restitution = value
        }

    @DebugWarning
    @NotSerializedProperty
    @Suppress("unused")
    val isMissingCollider: String?
        get() {
            val entity = entity
            val ok = entity != null && hasValidComponents(entity, PhysicalBody::class, Collider::class)
            return if (ok) null else "True"
        }

    // getter not needed, is updated automatically from BulletPhysics.kt
    @Group("Movement")
    var globalLinearVelocity: Vector3f = Vector3f()
        set(value) {
            field.set(value)
            nativeInstance?.setLinearVelocity(value)
        }

    @Group("Movement")
    var localLinearVelocity: Vector3f = Vector3f()
        get() {
            transform?.globalTransform?.transformDirectionInverse(globalLinearVelocity, field)
            return field
        }
        set(value) {
            field.set(value)
            val bi = nativeInstance
            val tr = transform
            if (tr != null && bi != null) {
                val global = tr.globalTransform.transformDirection(field, Vector3f())
                bi.setLinearVelocity(global)
            }
        }

    @DebugProperty
    @Order(0)
    @Group("Movement")
    val localLinearVelocityX: Double
        get() = globalLinearVelocity.dot(getLocalXAxis())

    @DebugProperty
    @Order(1)
    @Group("Movement")
    val localLinearVelocityY: Double
        get() = globalLinearVelocity.dot(getLocalYAxis())

    @DebugProperty
    @Order(2)
    @Group("Movement")
    val localLinearVelocityZ: Double
        get() = globalLinearVelocity.dot(getLocalZAxis())

    // getter not needed, is updated automatically from BulletPhysics.kt
    @Group("Rotation")
    var globalAngularVelocity: Vector3f = Vector3f()
        set(value) {
            field.set(value)
            nativeInstance?.setAngularVelocity(value)
        }

    @Group("Movement")
    var localAngularVelocity: Vector3f = Vector3f()
        get() {
            transform?.globalTransform?.transformDirectionInverse(globalAngularVelocity, field)
            return field
        }
        set(value) {
            field.set(value)
            val bi = nativeInstance
            val tr = transform
            if (tr != null && bi != null) {
                val global = tr.globalTransform.transformDirection(field, Vector3f())
                bi.setAngularVelocity(global)
            }
        }

    @DebugProperty
    @Order(0)
    @Group("Rotation")
    val localAngularVelocityX: Double
        get() = globalAngularVelocity.dot(getLocalXAxis())

    @DebugProperty
    @Order(1)
    @Group("Rotation")
    val localAngularVelocityY: Double
        get() = globalAngularVelocity.dot(getLocalYAxis())

    @DebugProperty
    @Order(2)
    @Group("Rotation")
    val localAngularVelocityZ: Double
        get() = globalAngularVelocity.dot(getLocalZAxis())

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is PhysicalBody) return
        dst.friction = friction
        dst.restitution = restitution
        dst.globalLinearVelocity = globalLinearVelocity
        dst.globalAngularVelocity = globalAngularVelocity
    }
}