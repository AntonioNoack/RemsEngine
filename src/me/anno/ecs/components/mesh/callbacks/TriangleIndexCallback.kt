package me.anno.ecs.components.mesh.callbacks

fun interface TriangleIndexCallback {
    fun run(a: Int, b: Int, c: Int)
}