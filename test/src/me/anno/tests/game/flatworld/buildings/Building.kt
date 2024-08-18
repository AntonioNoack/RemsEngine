package me.anno.tests.game.flatworld.buildings

import me.anno.ecs.Component
import me.anno.ecs.components.mesh.Mesh
import org.joml.Vector3d

open class Building(val mesh: Mesh) : Component() {
    val position = Vector3d()
    var rotationYDegrees = 0.0
}