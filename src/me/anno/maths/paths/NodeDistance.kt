package me.anno.maths.paths

fun interface NodeDistance<Node> {
    fun get(from: Node, to: Node): Double
}