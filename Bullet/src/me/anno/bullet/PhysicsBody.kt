package me.anno.bullet

import com.bulletphysics.collision.dispatch.CollisionObject
import me.anno.ecs.Component
import me.anno.ecs.EntityPhysics.getPhysics
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.components.collider.Collider
import me.anno.engine.serialization.NotSerializedProperty

abstract class PhysicsBody<BulletType : CollisionObject> : Component() {

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
}