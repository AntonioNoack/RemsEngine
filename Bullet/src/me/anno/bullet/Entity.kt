package me.anno.bullet

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent

val Entity.rigidbodyComponent: Rigidbody? get() = getComponent(Rigidbody::class, false)
