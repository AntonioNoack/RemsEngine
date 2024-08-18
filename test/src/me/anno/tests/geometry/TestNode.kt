package me.anno.tests.geometry

import org.joml.Vector2d

class AStarNode(val position: Vector2d) {
    val links = HashMap<AStarNode, Double>()
    fun distance(other: AStarNode): Double {
        return links[other] ?: position.distance(other.position)
    }
}