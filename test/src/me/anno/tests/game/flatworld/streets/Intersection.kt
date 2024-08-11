package me.anno.tests.game.flatworld.streets

import me.anno.ecs.Component
import me.anno.ecs.components.mesh.Mesh

class Intersection : Component() {
    val segments = ArrayList<ReversibleSegment>()
    var mesh: Mesh? = null
}