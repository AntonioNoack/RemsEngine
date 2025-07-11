package me.anno.ecs.components.collider

import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.components.FillSpace
import me.anno.engine.raycast.RayQuery

abstract class CollidingComponent : Component(), FillSpace {

    @DebugProperty
    @Docs("Which collisionGroups to interact with")
    var collisionMask = 0xffff

    fun canCollide(collisionMask: Int) = this.collisionMask.and(collisionMask) != 0

    /** whether the typeMask includes this type, see Raycast.kt */
    open fun hasRaycastType(typeMask: Int) = true

    /**
     * returns whether the object was hit
     * */
    open fun raycast(query: RayQuery): Boolean = false

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is CollidingComponent) return
        dst.collisionMask = collisionMask
    }
}