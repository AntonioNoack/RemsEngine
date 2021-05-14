package me.anno.image.svg.gradient

import me.anno.utils.types.Vectors.print
import org.joml.Vector2d

class Formula(
    val position: Vector2d,
    val directionOrRadius: Vector2d,
    var isCircle: Boolean
) {

    constructor() : this(Vector2d(), Vector2d(), false)

    fun clear() {
        position.set(0.0)
        directionOrRadius.set(0.0)
        isCircle = false
    }

    override fun toString(): String = "(${position.print()} ${directionOrRadius.print()} $isCircle)"

}