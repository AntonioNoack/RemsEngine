package me.anno.ecs.components.mesh.spline

import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector2f

object TestProfiles {
    val cubeProfile = PathProfile(
        listOf(
            Vector2f(-1f, -1f),
            Vector2f(-1f, +1f),
            Vector2f(+1f, +1f),
            Vector2f(+1f, -1f)
        ), null,
        IntArrayList(intArrayOf(-1, -1, 0, -1)),
        true
    )
}