package me.anno.tests.game.flatworld.buildings

import me.anno.ecs.Component
import org.joml.Vector3d

data class BuildingInstance(val type: BuildingType, val position: Vector3d, val rotationYDegrees: Double): Component()