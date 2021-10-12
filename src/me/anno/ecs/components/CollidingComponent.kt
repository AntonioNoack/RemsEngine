package me.anno.ecs.components

import me.anno.ecs.Component
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.AABBd
import org.joml.Matrix4x3d

abstract class CollidingComponent : Component() {

    var collisionMask: Int = 1

    fun canCollide(collisionMask: Int): Boolean {
        return this.collisionMask.and(collisionMask) != 0
    }

    abstract override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as CollidingComponent
        clone.collisionMask = collisionMask
    }

}