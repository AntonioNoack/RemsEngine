package me.anno.bullet.bodies

import com.bulletphysics.collision.dispatch.CollisionObject
import me.anno.bullet.BulletPhysics
import me.anno.ecs.Entity
import me.anno.ecs.EntityPhysics.getPhysics
import me.anno.ecs.components.physics.PhysicsBodyBase

abstract class PhysicsBody<BulletType : CollisionObject> :
    PhysicsBodyBase<BulletType>() {

    override fun invalidatePhysics(entity: Entity) {
        getPhysics(BulletPhysics::class)
            ?.invalidate(entity)
    }
}