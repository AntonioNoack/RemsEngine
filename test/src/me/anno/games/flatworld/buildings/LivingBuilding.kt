package me.anno.games.flatworld.buildings

import me.anno.ecs.components.mesh.Mesh
import me.anno.games.flatworld.humans.Human

class LivingBuilding(mesh: Mesh) : Building(mesh) {
    val inhabitants = ArrayList<Human>()
    var inhabitantLimit = 50
}