package org.recast4j.detour.extras.jumplink

import org.joml.Vector3f

class EdgeSampler(edge: Edge, val trajectory: Trajectory) {
    val start = GroundSegment()
    val end = ArrayList<GroundSegment>()
    val ax = Vector3f(edge.b).sub(edge.a)
    val ay = Vector3f(0f, 1f, 0f)
    val az = Vector3f()

    init {
        ax.normalize()
        az.set(ax.z, 0f, -ax.x).normalize()
    }
}