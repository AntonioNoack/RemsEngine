package me.anno.ecs.components.mesh.spline

import me.anno.ecs.Component
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Vector3d

/**
 * children of SplineMesh,
 * all transforms are in parent space
 * */
class SplineControlPoint : Component() {

    // profile; todo to next point: connect different profiles
    var profile: PathProfile = TestProfiles.cubeProfile

    var width = 1.0
    var height = 1.0

    fun invalidate() {
        entity?.parentEntity?.getComponent(SplineMesh::class)
            ?.invalidateMesh()
    }

    fun getLocalPosition(dst: Vector3d, fx: Double, fz: Double = 0.0): Vector3d =
        transform!!.localTransform.transformPosition(dst.set(fx * width, 0.0, fz * width))

    fun getLocalForward(dst: Vector3d): Vector3d =
        transform!!.localTransform.transformDirection(dst.set(0.0, 0.0, 1.0))

    fun getLocalUp(dst: Vector3d): Vector3d =
        transform!!.localTransform.transformDirection(dst.set(0.0, 1.0, 0.0))

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SplineControlPoint
        dst.profile = profile
        dst.width = width
        dst.height = height
    }

    override val className get() = "SplineControlPoint"

}