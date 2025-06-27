package me.anno.bullet.bodies

import com.bulletphysics.dynamics.RigidBody
import me.anno.bullet.constraints.Constraint
import me.anno.ecs.annotations.DebugWarning
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Group
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.physics.Physics.Companion.hasValidComponents
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty

abstract class PhysicalBody : PhysicsBody<RigidBody>() {

    @NotSerializedProperty
    val linkedConstraints = ArrayList<Constraint<*>>()

    /**
     * Slowing down on contact. 1.0 = high traction, 0.0 = like on black ice
     * */
    @Group("Movement")
    @Range(0.0, 1.0)
    var friction: Double = 0.5
        set(value) {
            field = value
            bulletInstance?.friction = friction
        }

    @Docs("How elastic a body is, 1 = fully elastic, 0 = all energy absorbed (knead)")
    @Range(0.0, 1.0)
    var restitution = 0.0
        set(value) {
            field = value
            bulletInstance?.restitution = value
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

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is PhysicalBody) return
        dst.friction = friction
        dst.restitution = restitution
    }
}