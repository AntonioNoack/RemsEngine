package me.anno.tests.game.pacman.logic

import me.anno.maths.Maths.fract
import me.anno.maths.Maths.mix
import org.joml.Vector2f

abstract class Moveable(var current: Node) {
    val position = Vector2f(current.position)
    var next = current.neighbors.randomOrNull() ?: current

    var progress = 0f

    fun tick(dt: Float) {
        if (stepProgress(dt)) {
            val foundNext = findNext()
            current = next
            next = foundNext
        }
        updatePosition()
    }

    private fun stepProgress(dt: Float): Boolean {
        if (current == next) {
            progress = 0f
            return true
        } else {
            progress += dt
            return if (progress >= 1f) {
                progress = fract(progress)
                true
            } else false
        }
    }

    private fun updatePosition() {
        val curr = current.position
        val next = next.position
        position.set(
            mix(curr.x.toFloat(), next.x.toFloat(), progress),
            mix(curr.y.toFloat(), next.y.toFloat(), progress)
        )
    }

    abstract fun findNext(): Node
}