package me.anno.ecs.components.mesh.spline

import org.joml.Vector2f

object TestProfiles {
    val cubeProfile = SplineProfile(
        listOf(
            Vector2f(-1f, -1f),
            Vector2f(-1f, +1f),
            Vector2f(+1f, +1f),
            Vector2f(+1f, -1f)
        ), null, null, true
    )
}