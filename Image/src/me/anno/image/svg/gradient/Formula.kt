package me.anno.image.svg.gradient

import org.joml.Vector2f

class Formula(
    val position: Vector2f,
    val directionOrRadius: Vector2f,
    var isCircle: Boolean
) {

    constructor() : this(Vector2f(), Vector2f(), false)

    fun clear() {
        position.set(0f)
        directionOrRadius.set(0f)
        isCircle = false
    }

    override fun toString(): String = "($position, $directionOrRadius, $isCircle)"

}