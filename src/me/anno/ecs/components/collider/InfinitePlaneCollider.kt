package me.anno.ecs.components.collider

import me.anno.engine.raycast.RayQueryLocal
import me.anno.engine.ui.LineShapes.drawBox
import me.anno.gpu.pipeline.Pipeline
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.pow
import kotlin.math.sign

/**
 * Good for creating an endless floor, that nothing can fall through.
 * */
class InfinitePlaneCollider : Collider() {

    override fun union(globalTransform: Matrix4x3, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        aabb.all()
    }

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        return deltaPos.y
    }

    override fun drawShape(pipeline: Pipeline) {
        // draw shape as transparent plane instead?
        for (i in -2..5) {
            val scale = (10.0).pow(i)
            drawBox(
                entity, getLineColor(hasPhysics),
                scale, 0.0, scale
            )
        }
    }

    override fun raycast(query: RayQueryLocal, surfaceNormal: Vector3f?): Float {
        val pos = query.start
        val dir = query.direction
        var distance = pos.y / dir.y
        if (distance < 0f) distance = Float.POSITIVE_INFINITY
        if (distance < query.maxDistance) {
            surfaceNormal?.set(0f, sign(pos.y), 0f)
        }
        return distance
    }
}