package me.anno.ecs.components.script.coroutinestuff

import me.anno.ecs.Entity
import me.anno.ecs.Transform

abstract class Behaviour {

    var isDead = false

    abstract fun update(entity: Entity, transform: Transform)

}