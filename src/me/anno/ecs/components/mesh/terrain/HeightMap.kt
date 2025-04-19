package me.anno.ecs.components.mesh.terrain

fun interface HeightMap {
    operator fun get(xi: Int, zi: Int): Float
}