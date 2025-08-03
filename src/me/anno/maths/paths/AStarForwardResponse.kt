package me.anno.maths.paths

fun interface AStarForwardResponse<Node> {
    fun respond(to: Node, distFromTo: Double, distToEnd: Double)
}