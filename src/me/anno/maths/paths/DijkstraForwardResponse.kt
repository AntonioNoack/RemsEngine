package me.anno.maths.paths

fun interface DijkstraForwardResponse<Node> {
    fun respond(to: Node, distFromTo: Double)
}