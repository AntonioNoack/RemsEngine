package me.anno.tests.game.flatworld.buildings

import me.anno.ecs.components.mesh.Mesh
import me.anno.tests.game.flatworld.humans.Human

class LivingBuilding(mesh: Mesh) : Building(mesh) {
    val inhabitants = ArrayList<Human>()
    var inhabitantLimit = 50
}