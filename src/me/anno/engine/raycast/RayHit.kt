package me.anno.engine.raycast

import me.anno.ecs.Component
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.mesh.Mesh
import org.joml.*

/**
 * Stores relevant information for when a ray is cast onto a scene.
 *
 * Unity: RaycastHit
 * Unreal: HitResult
 * Godot: RayCast? Map with properties
 * */
class RayHit {

    /**
     * Closest distance
     * */
    var distance = Double.POSITIVE_INFINITY

    /**
     * Closest hit object
     * */
    var collider: Collider? = null
    var mesh: Mesh? = null
    var component: Component? = null

    // collision algorithms require a bit of temporary storage for convenience
    // this pre-allocates the maximum, it will require
    val tmpVector3fs = Array(16) { Vector3f() }
    val tmpVector3ds = Array(10) { Vector3d() }
    val tmpVector4fs = Array(3) { Vector4f() }
    val tmpMat4x3d = Matrix4x3d()
    val tmpAABBd = AABBd()

    // mesh data, that we could calculate
    // var triangleIndex: Int = -1
    // var material: Material? = null
    // var uv: Vector2f? = null

    /**
     * Position in world space
     * */
    val positionWS = Vector3d()

    /**
     * Normal from geometry in world space
     * might not be normalized!
     * */
    val geometryNormalWS = Vector3d()

    /**
     * Normal from shading normals, in world space
     * might not be normalized!
     * */
    val shadingNormalWS = Vector3d()

    /**
     * Barycentric coordinates within the intersected triangle
     * might not be normalized!
     * */
    val barycentric = Vector3d()

    /**
     * Hit UV coordinates
     * */
    val uv = Vector2d()

    // just a few statistics
    var blasCtr = 0
    var tlasCtr = 0
    var trisCtr = 0

    override fun toString(): String {
        return "RayHit(pos: $positionWS, nor: $geometryNormalWS, dist: $distance)"
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
        val hitNormal = geometryNormalWS.set(localNormal)
        globalTransform.transformDirection(hitNormal)
        shadingNormalWS.set(geometryNormalWS)
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
        val hitNormal = geometryNormalWS.set(localNormal)
        globalTransform?.transformDirection(hitNormal)
        shadingNormalWS.set(geometryNormalWS)
        // calculate the world space distance
        val distance = hitPosition.distance(start)
        this.distance = distance
        // update the end vector
        end.set(direction).normalize(distance).add(start)
    }

}