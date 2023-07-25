package me.anno.bullet

import me.anno.ecs.Entity

val Entity.rigidbodyComponent: Rigidbody? get() = getComponent(Rigidbody::class, false)
