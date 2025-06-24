package me.anno.ecs.components.collider

import me.anno.utils.pooling.JomlPools
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3d

object UnionUtils {

    fun unionRing(globalTransform: Matrix4x3, dstUnion: AABBd, axis: Axis, r: Double, h: Double) {
        val min = JomlPools.vec3d.create()
        val max = JomlPools.vec3d.create()

        min.set(-r)
        max.set(+r)

        min[axis.id] = h
        max[axis.id] = h

        val tmp = JomlPools.aabbd.borrow()
        tmp.setMin(min)
        tmp.setMax(max)
        tmp.transformUnion(globalTransform, dstUnion)

        JomlPools.vec3d.sub(2)
    }

    fun unionCube(globalTransform: Matrix4x3, dstUnion: AABBd, hx: Double, hy: Double, hz: Double) {
        unionBox(globalTransform, dstUnion, -hx, -hy, -hz, hx, hy, hz)
    }

    fun unionBox(
        globalTransform: Matrix4x3, dstUnion: AABBd,
        minX: Double, minY: Double, minZ: Double,
        maxX: Double, maxY: Double, maxZ: Double
    ) {
        val tmp = JomlPools.aabbd.borrow()
        tmp.setMin(minX, minY, minZ)
        tmp.setMax(maxX, maxY, maxZ)
        tmp.transformUnion(globalTransform, dstUnion)
    }

    fun unionPoint(globalTransform: Matrix4x3, dstUnion: AABBd, tmp: Vector3d, x: Double, y: Double, z: Double) {
        dstUnion.union(globalTransform.transformPosition(tmp.set(x, y, z)))
    }
}