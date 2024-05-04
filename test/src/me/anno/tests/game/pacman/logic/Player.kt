package me.anno.tests.game.pacman.logic

import org.joml.Vector2f

class Player(node: Node) : Moveable(node) {
    val requestedMovement = Vector2f()
    var lookLeft = false // todo when we can load the ghost, rotate player and enemies in 3d
    var points = 0 // todo visualize in 2d and 3d
    var lives = 3 // todo visualize in 2d and 3d
    var wasKilled = false // todo visualize in 2d and 3d

    // todo slightly rotate player towards go-to-direction in 2d

    override fun findNext(): Node {
        val c = current.position
        val n = next.position
        val dir = requestedMovement
        val candidates = next.neighbors
        return candidates.firstOrNull { // go where the player requested
            val nn = it.position
            dir.dot(nn.x - n.x, nn.y - n.y) > 0f
        } ?: candidates.firstOrNull { // go straight otherwise
            val nn = it.position
            nn.x - n.x == n.x - c.x && nn.y - n.y == n.y - c.y
        } ?: next // wait
    }
}