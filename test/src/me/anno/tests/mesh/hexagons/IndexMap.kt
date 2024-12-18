package me.anno.tests.mesh.hexagons

fun interface IndexMap {
    operator fun get(index: Long): Int
}