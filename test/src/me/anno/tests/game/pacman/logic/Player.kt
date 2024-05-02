package me.anno.tests.game.pacman.logic

import org.joml.Vector2i

class Player(node: Node) : Moveable(node) {
    val requestedMovement = Vector2i()
    var lookLeft = false
    var points = 0
    var lives = 3
    var wasKilled = false

    override fun findNext(): Node {
        val c = current.position
        val n = next.position
        val dir = requestedMovement
        val candidates = next.neighbors
        return candidates.firstOrNull { // go where the player requested
            val nn = it.position
            dir.dot(nn.x - n.x, nn.y - n.y) > 0
        } ?: candidates.firstOrNull { // go straight otherwise
            val nn = it.position
            nn.x - n.x == n.x - c.x && nn.y - n.y == n.y - c.y
        } ?: next // wait
    }
}