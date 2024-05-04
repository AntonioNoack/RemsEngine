package me.anno.tests.game.pacman.logic

import org.joml.Vector2f

abstract class Moveable(var current: Node) {
    val currPosition = Vector2f(current.position)
    val prevPosition = Vector2f(currPosition)
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
                progress -= 1f
                true
            } else false
        }
    }

    private fun updatePosition() {
        current.position.lerp(next.position, progress, currPosition)
    }

    abstract fun findNext(): Node
}