package me.anno.ecs.systems

import me.anno.ecs.Entity
import me.anno.ecs.System
import me.anno.utils.structures.Collections.setContains

object MotionVectorSystem : System() {

    override val priority: Int
        get() = -10_000_000 // should be really, really early

    private val entities = HashSet<Entity>(4096)

    override fun setContains(entity: Entity, contains: Boolean) {
        entities.setContains(entity, contains)
    }

    override fun onUpdate() {
        for (entity in entities) {
            // looks weird, but since smooth and teleport go away anyway,
            // this should be correct
            entity.transform.teleportUpdate()
        }
    }
}