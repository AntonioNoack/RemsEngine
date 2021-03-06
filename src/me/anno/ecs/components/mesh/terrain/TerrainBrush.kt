package me.anno.ecs.components.mesh.terrain

import org.joml.Matrix3f
import org.joml.Vector3f

abstract class TerrainBrush {

    abstract fun apply(
        editorPosition: Vector3f, editorMatrix: Matrix3f,
        radius: Float, strength: Float,
        point: Vector3f
    )

}