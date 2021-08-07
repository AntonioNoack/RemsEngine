package me.anno.engine.raycast

import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.mesh.MeshComponent
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f

class RayHit {

    var distance = 0.0
    var collider: Collider? = null
    var meshComponent: MeshComponent? = null

    val tmpV3a = Vector3d()
    val tmpV3b = Vector3d()
    val tmpV3c = Vector3d()

    val tmpNormal = Vector3d()
    val tmpPosition = Vector3d()

    // mesh data, that we could calculate
    // var triangleIndex: Int = -1
    // var material: Material? = null
    // var uv: Vector2f? = null

    val positionWS = Vector3d()
    val normalWS: Vector3d = Vector3d()
    fun clear() {
        distance = Double.POSITIVE_INFINITY
    }

    fun setFromLocal(
        globalTransform: Matrix4x3d, localHit: Vector3f, localNormal: Vector3f,
        start: Vector3d, direction: Vector3d, end: Vector3d
    ) {
        // transform the local position back
        val hitPosition = positionWS.set(localHit)
        println("hit position $hitPosition from local hit $localHit")
        globalTransform.transformPosition(hitPosition)
        val hitNormal = normalWS.set(localNormal)
        globalTransform.transformDirection(hitNormal)
        // calculate the world space distance
        val distance = hitPosition.distance(start)
        this.distance = distance
        // update the end vector
        end.set(direction).normalize(distance).add(start)
    }
}