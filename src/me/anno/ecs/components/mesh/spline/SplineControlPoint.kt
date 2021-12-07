package me.anno.ecs.components.mesh.spline

import me.anno.ecs.Component
import org.joml.Vector3d
import kotlin.math.cos
import kotlin.math.sin

/**
 * children of SplineMesh,
 * all transforms are in parent space
 * */
class SplineControlPoint : Component() {

    // the profile to the next point
    var profile = TestProfiles.cubeProfile

    var width = 1.0
    var height = 1.0

    var deltaHalfAngle = 0.0
        set(value) {
            if(field != value){
                field = value
                cos = cos(value)
                sin = sin(value)
            }
        }

    private var cos = 1.0
    private var sin = 0.0

    fun invalidate(){
        entity?.parentEntity?.getComponent(SplineMesh::class)
            ?.invalidateMesh()
    }

    fun getP0(dst: Vector3d): Vector3d {
        val transform = transform!!.localTransform
        val width = width
        return transform.transformPosition(
            dst.set(-cos * width, 0.0, -sin * width)
        )
    }

    fun getN0(dst: Vector3d): Vector3d {
        val transform = transform!!.localTransform
        return transform.transformDirection(
            dst.set(-sin, 0.0, cos)
        )
    }

    fun getP1(dst: Vector3d): Vector3d {
        val transform = transform!!.localTransform
        val width = width
        return transform.transformPosition(
            dst.set(+cos * width, 0.0, -sin * width)
        )
    }

    fun getN1(dst: Vector3d): Vector3d {
        val transform = transform!!.localTransform
        return transform.transformDirection(
            dst.set(+sin, 0.0, cos)
        )
    }

    override fun clone(): Component {
        val clone = SplineControlPoint()
        copy(clone)
        return clone
    }

    override val className: String = "SplineControlPoint"

}