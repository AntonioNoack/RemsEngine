package me.anno.games.pacman.logic

import org.joml.Vector2f

class Node(val position: Vector2f) {
    val neighbors = ArrayList<Node>(4)
}