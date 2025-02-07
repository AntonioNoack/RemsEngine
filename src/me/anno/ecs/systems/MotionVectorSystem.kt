package me.anno.ecs.systems

import me.anno.ecs.Entity
import me.anno.ecs.System
import me.anno.ecs.Transform
import me.anno.utils.structures.sets.FastIteratorSet

object MotionVectorSystem : System() {

    override val priority: Int
        get() = -10_000_000 // should be really, really early

    private val transforms = FastIteratorSet<Transform>()

    override fun setContains(entity: Entity, contains: Boolean) {
        transforms.setContains(entity.transform, contains)
    }

    override fun onUpdate() {
        val entities = transforms.asList()
        for (i in entities.indices) {
            entities[i].teleportUpdate()
        }
    }
}