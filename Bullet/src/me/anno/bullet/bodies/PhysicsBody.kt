package me.anno.bullet.bodies

import com.bulletphysics.collision.dispatch.CollisionObject
import me.anno.bullet.BulletPhysics
import me.anno.ecs.Component
import me.anno.ecs.EntityPhysics.getPhysics
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty

abstract class PhysicsBody<BulletType : CollisionObject> : Component() {

    @Range(-1.0, 15.0)
    @Docs("Which collisionGroup this is, or -1 for ghost objects")
    var collisionGroup = 0

    @DebugProperty
    @Docs("Which collisionGroups to interact with")
    var collisionMask = 0xffff

    @NotSerializedProperty
    val activeColliders = ArrayList<Collider>()

    @DebugProperty
    @NotSerializedProperty
    var bulletInstance: BulletType? = null

    fun invalidatePhysics() {
        val entity = entity ?: return
        getPhysics(BulletPhysics::class)
            ?.invalidate(entity)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is PhysicsBody<*>) return
        dst.collisionGroup = collisionGroup
        dst.collisionMask = collisionMask
    }
}