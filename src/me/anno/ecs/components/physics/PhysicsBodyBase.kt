package me.anno.ecs.components.physics

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.collider.CollisionFilters
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty

abstract class PhysicsBodyBase<NativeType> : Component() {

    @Range(0.0, CollisionFilters.NUM_GROUPS - 1.0)
    @Docs("Which collisionGroup this belongs to")
    var collisionGroup = 0

    @DebugProperty
    @Docs("Which collisionGroups to interact with")
    var collisionMask = -1

    @NotSerializedProperty
    val activeColliders = ArrayList<Collider>()

    @DebugProperty
    @NotSerializedProperty
    var nativeInstance: NativeType? = null

    fun invalidatePhysics() {
        invalidatePhysics(entity ?: return)
    }

    abstract fun invalidatePhysics(entity: Entity)

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is PhysicsBodyBase<*>) return
        dst.collisionGroup = collisionGroup
        dst.collisionMask = collisionMask
    }
}