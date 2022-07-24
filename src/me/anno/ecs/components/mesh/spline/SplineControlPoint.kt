package me.anno.ecs.components.mesh.spline

import me.anno.ecs.Component
import org.joml.Vector3d

/**
 * children of SplineMesh,
 * all transforms are in parent space
 * */
class SplineControlPoint : Component() {

    // the profile to the next point
    // var profile = TestProfiles.cubeProfile

    var width = 1.0
    var height = 1.0

    fun invalidate() {
        entity?.parentEntity?.getComponent(SplineMesh::class)
            ?.invalidateMesh()
    }

    fun getLocalPosition(dst: Vector3d, factor: Double): Vector3d =
        transform!!.localTransform.transformPosition(dst.set(factor * width, 0.0, 0.0))

    fun getLocalForward(dst: Vector3d): Vector3d =
        transform!!.localTransform.transformDirection(dst.set(0.0, 0.0, 1.0))

    override fun clone(): Component {
        val clone = SplineControlPoint()
        copy(clone)
        return clone
    }

    override val className: String = "SplineControlPoint"

}