package me.anno.engine.raycast

import me.anno.ecs.Component
import me.anno.ecs.components.mesh.Mesh
import me.anno.utils.structures.lists.Lists.createArrayList
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector2d
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f

/**
 * Stores relevant information for when a ray is cast onto a scene.
 *
 * Unity: RaycastHit
 * Unreal: HitResult
 * Godot: RayCast? Map with properties
 * */
class RayHit(maxDistance: Double) {

    constructor() : this(Double.POSITIVE_INFINITY)

    /**
     * Closest distance
     * */
    var distance = maxDistance

    /**
     * Closest hit object
     * */
    var mesh: Mesh? = null
    var component: Component? = null
    var triangleIndex = 0

    // collision algorithms require a bit of temporary storage for convenience
    // this pre-allocates the maximum, it will require
    val tmpVector3fs = createArrayList(16) { Vector3f() }
    val tmpVector3ds = createArrayList(10) { Vector3d() }
    val tmpVector4fs = createArrayList(3) { Vector4f() }
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
        localStart: Vector3f, localDirection: Vector3f, localDistance: Float, localNormal: Vector3f,
        query: RayQuery
    ) {
        // transform the local position back
        val hitPosition = positionWS.set(localDirection).mul(localDistance.toDouble()).add(localStart)
        // LOGGER.info("hit position $hitPosition from local hit $localStart + $localDistance * $localDir")
        globalTransform.transformPosition(hitPosition)
        val hitNormal = geometryNormalWS.set(localNormal)
        globalTransform.transformDirection(hitNormal)
        shadingNormalWS.set(geometryNormalWS)
        // calculate the world space distance
        val distance = hitPosition.distance(query.start)
        this.distance = distance
        // update the end vector
        query.end.set(query.direction).normalize(distance).add(query.start)
    }

    fun setFromLocal(
        globalTransform: Matrix4x3d?,
        localHit: Vector3f, localNormal: Vector3f,
        query: RayQuery
    ) {
        // transform the local position back
        val hitPosition = positionWS.set(localHit)
        // LOGGER.info("hit position $hitPosition from local hit $localHit")
        globalTransform?.transformPosition(hitPosition)
        val hitNormal = geometryNormalWS.set(localNormal)
        globalTransform?.transformDirection(hitNormal)
        shadingNormalWS.set(geometryNormalWS)
        // calculate the world space distance
        val distance = hitPosition.distance(query.start)
        this.distance = distance
        // update the end vector
        query.end.set(query.direction).normalize(distance).add(query.start)
    }
}