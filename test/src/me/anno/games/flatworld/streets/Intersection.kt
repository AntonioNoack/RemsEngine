package me.anno.games.flatworld.streets

import me.anno.ecs.Component
import me.anno.ecs.components.mesh.Mesh

class Intersection : Component() {
    val segments = ArrayList<ReversibleSegment>()
    var streetMesh: Mesh? = null
    // todo extra mesh for decorations
    // todo extra mesh for road markings
}