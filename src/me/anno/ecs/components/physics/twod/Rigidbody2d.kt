package me.anno.ecs.components.physics.twod

import me.anno.ecs.Component

class Rigidbody2d : Component() {

    override fun clone(): Rigidbody2d {
        val clone = Rigidbody2d()
        copy(clone)
        return clone
    }

}