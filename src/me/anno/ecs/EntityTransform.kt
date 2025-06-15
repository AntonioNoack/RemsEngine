package me.anno.ecs

import me.anno.utils.pooling.JomlPools
import org.joml.Vector3d

object EntityTransform {

    fun Entity?.getLocalXAxis(dst: Vector3d): Vector3d {
        val self = this ?: return dst.set(1.0, 0.0, 0.0)
        return self.transform.getLocalXAxis(dst)
    }

    fun Entity?.getLocalYAxis(dst: Vector3d): Vector3d {
        val self = this ?: return dst.set(0.0, 1.0, 0.0)
        return self.transform.getLocalYAxis(dst)
    }

    fun Entity?.getLocalZAxis(dst: Vector3d): Vector3d {
        val self = this ?: return dst.set(0.0, 0.0, 1.0)
        return self.transform.getLocalZAxis(dst)
    }

    fun Component?.getLocalXAxis(dst: Vector3d = JomlPools.vec3d.borrow()): Vector3d {
        return this?.entity.getLocalXAxis(dst)
    }

    fun Component?.getLocalYAxis(dst: Vector3d = JomlPools.vec3d.borrow()): Vector3d {
        return this?.entity.getLocalYAxis(dst)
    }

    fun Component?.getLocalZAxis(dst: Vector3d = JomlPools.vec3d.borrow()): Vector3d {
        return this?.entity.getLocalZAxis(dst)
    }
}