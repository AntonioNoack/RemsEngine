package me.anno.tests.game.pacman.logic

import org.joml.Vector2i

class Node(val position: Vector2i) {
    val neighbors = ArrayList<Node>(4)
}