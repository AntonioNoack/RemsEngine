package me.anno.ecs.components.mesh.spline

import me.anno.ecs.Component
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Vector3d
import kotlin.math.cos
import kotlin.math.sin

/**
 * children of SplineMesh,
 * all transforms are in parent space
 * */
class SplineControlPoint : Component() {

    // the profile to the next point
    // var profile = TestProfiles.cubeProfile

    var width = 1f
    var height = 1f

    fun invalidate() {
        entity?.parentEntity?.getComponent(SplineMesh::class)
            ?.invalidateMesh()
    }

    fun localToParentPos(x: Double, y: Double, dst: Vector3d): Vector3d {
        val transform = transform!!.localTransform
        return transform.transformPosition(dst.set(x * width, y * height, 0.0))
    }

    fun localToParentDir(x: Double, y: Double, dst: Vector3d): Vector3d {
        val transform = transform!!.localTransform
        return transform.transformDirection(dst.set(x, y, 0.0)).normalize()
    }

    override fun clone(): SplineControlPoint {
        val clone = SplineControlPoint()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SplineControlPoint
        clone.width = width
        clone.height = height
    }

    override val className: String = "SplineControlPoint"

}