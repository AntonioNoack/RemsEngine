package me.anno.ecs.components.shaders.sdf.shapes

import me.anno.ecs.components.shaders.sdf.modifiers.SDFHalfSpace
import me.anno.utils.pooling.JomlPools
import org.joml.Vector3f

open class SDF2DShape : SDFShape() {

    var axes = "xy"
        set(value) {
            if (value.length == 2 && value.count { it in "xyz" } == 2 && value[0] != value[1]) {
                field = value
            }
        }

    fun bound(min: Vector3f, max: Vector3f) {
        val dir1 = JomlPools.vec3f.create().set(min).sub(max)
        add(SDFHalfSpace(min, dir1))
        dir1.mul(-1f)
        add(SDFHalfSpace(max, dir1))
        JomlPools.vec3f.sub(1)
    }

    fun boundX(min: Float, max: Float) = bound(min, max, 0)
    fun boundY(min: Float, max: Float) = bound(min, max, 1)
    fun boundZ(min: Float, max: Float) = bound(min, max, 2)
    fun bound(min: Float, max: Float, axis: Int) {
        val mv = JomlPools.vec3f.create().set(0f)
        val xv = JomlPools.vec3f.create().set(0f)
        mv.setComponent(axis, min)
        xv.setComponent(axis, max)
        bound(mv, xv)
        JomlPools.vec3f.sub(2)
    }

}