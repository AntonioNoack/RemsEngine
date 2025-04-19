package me.anno.ecs.components.mesh.terrain

fun interface ColorMap {
    operator fun get(xi: Int, zi: Int): Int
}