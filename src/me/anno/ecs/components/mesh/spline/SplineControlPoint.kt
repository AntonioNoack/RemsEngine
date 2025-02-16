package me.anno.ecs.components.mesh.spline

import me.anno.ecs.Component
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.utils.pooling.JomlPools
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * children of SplineMesh,
 * all transforms are in parent space
 * */
class SplineControlPoint : Component() {

    // profile; todo to next point: connect different profiles
    var profile: SplineProfile = TestProfiles.cubeProfile

    var width = 1.0
    var height = 1.0

    fun invalidate() {
        entity?.parentEntity?.getComponent(SplineMesh::class)
            ?.invalidateMesh()
    }

    private val localTransform get() = transform!!.getLocalTransform(JomlPools.mat4x3m.borrow())

    fun getLocalPosition(dst: Vector3d, fx: Double, fz: Double = 0.0): Vector3d =
        localTransform.transformPosition(dst.set(fx * width, 0.0, fz * width))

    fun getLocalForward(dst: Vector3f): Vector3f =
        localTransform.transformDirection(dst.set(0f, 0f, 1f))

    fun getLocalForward(dst: Vector3d): Vector3d =
        localTransform.transformDirection(dst.set(0f, 0f, 1f))

    fun getLocalUp(dst: Vector3f): Vector3f =
        localTransform.transformDirection(dst.set(0f, 1f, 0f))

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SplineControlPoint) return
        dst.profile = profile
        dst.width = width
        dst.height = height
    }
}