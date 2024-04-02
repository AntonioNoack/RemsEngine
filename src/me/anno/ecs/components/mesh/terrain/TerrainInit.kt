package me.anno.ecs.components.mesh.terrain

import org.joml.Vector3f
import kotlin.math.sqrt

abstract class TerrainInit {

    abstract fun ensure(position: Vector3f, radius: Float, terrain: TriTerrain)

    companion object {
        val k0 = sqrt(3f) * 0.5f
        val k1 = sqrt(3f) / 3f
        const val k2 = 1.1547005f
    }
}