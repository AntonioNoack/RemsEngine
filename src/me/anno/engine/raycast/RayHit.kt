package me.anno.engine.raycast

import me.anno.ecs.Component
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.mesh.Mesh
import org.joml.*

class RayHit {

    var hitIfInside = false

    var distance = Double.POSITIVE_INFINITY

    var collider: Collider? = null
    var mesh: Mesh? = null
    var component: Component? = null

    // collision algorithms require a bit of temporary storage for convenience
    // this pre-allocates the maximum, it will require
    val tmpVector3fs = Array(10) { Vector3f() }
    val tmpVector3ds = Array(10) { Vector3d() }
    val tmpVector4fs = Array(3) { Vector4f() }
    val tmpMat4x3d = Matrix4x3d()
    val tmpMat4x3d2 = Matrix4x3d()
    val tmpAABBd = AABBd()

    // mesh data, that we could calculate
    // var triangleIndex: Int = -1
    // var material: Material? = null
    // var uv: Vector2f? = null

    val positionWS = Vector3d()
    /**
     * might not be normalized!
     * */
    val normalWS = Vector3d()

    var ctr = 0

    override fun toString(): String {
        return "RayHit(pos: $positionWS, nor: $normalWS, dist: $distance)"
    }

    fun setFromLocal(
        globalTransform: Matrix4x3d,
        localStart: Vector3f,
        localDir: Vector3f,
        localDistance: Float,
        localNormal: Vector3f,
        start: Vector3d,
        direction: Vector3d,
        end: Vector3d
    ) {
        // transform the local position back
        val hitPosition = positionWS.set(localDir).mul(localDistance.toDouble()).add(localStart)
        // LOGGER.info("hit position $hitPosition from local hit $localStart + $localDistance * $localDir")
        globalTransform.transformPosition(hitPosition)
        val hitNormal = normalWS.set(localNormal)
        globalTransform.transformDirection(hitNormal)
        // calculate the world space distance
        val distance = hitPosition.distance(start)
        this.distance = distance
        // update the end vector
        end.set(direction).normalize(distance).add(start)
    }

    fun setFromLocal(
        globalTransform: Matrix4x3d?, localHit: Vector3f, localNormal: Vector3f,
        start: Vector3d, direction: Vector3d, end: Vector3d
    ) {
        // transform the local position back
        val hitPosition = positionWS.set(localHit)
        // LOGGER.info("hit position $hitPosition from local hit $localHit")
        globalTransform?.transformPosition(hitPosition)
        val hitNormal = normalWS.set(localNormal)
        globalTransform?.transformDirection(hitNormal)
        // calculate the world space distance
        val distance = hitPosition.distance(start)
        this.distance = distance
        // update the end vector
        end.set(direction).normalize(distance).add(start)
    }

    fun localToGlobal(
        globalTransform: Matrix4x3d?,
        start: Vector3d,
        direction: Vector3d,
        end: Vector3d,
    ) {
        globalTransform ?: return
        globalTransform.transformPosition(positionWS)
        globalTransform.transformDirection(normalWS)
        val distance = positionWS.distance(start)
        this.distance = distance
        // update the end vector
        end.set(direction).normalize(distance).add(start)
    }
}