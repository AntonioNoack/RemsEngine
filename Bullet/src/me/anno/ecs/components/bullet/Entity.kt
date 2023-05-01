package me.anno.ecs.components.bullet

import me.anno.ecs.Entity

val Entity.rigidbodyComponent: Rigidbody? get() = getComponent(Rigidbody::class, false)
