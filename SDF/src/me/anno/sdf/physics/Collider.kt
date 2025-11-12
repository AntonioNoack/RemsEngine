package me.anno.sdf.physics

import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import me.anno.sdf.SDFCollider
import org.joml.Vector3d
import kotlin.math.max
import kotlin.math.min

fun SDFCollider.getAABB(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {

    val sdf = sdf ?: return
    val bounds = sdf.globalAABB

    // if t is just scaling + translation, we could simplify this

    aabbMin.set(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
    aabbMax.set(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)

    val tmp = Stack.newVec3f()
    val basis = t.basis
    for (i in 0 until 8) {
        tmp.set(
            if (i.and(1) != 0) bounds.minX else bounds.maxX,
            if (i.and(2) != 0) bounds.minY else bounds.maxY,
            if (i.and(4) != 0) bounds.minZ else bounds.maxZ
        )
        basis.transform(tmp)
        aabbMin.x = min(aabbMin.x, tmp.x.toDouble())
        aabbMin.y = min(aabbMin.y, tmp.y.toDouble())
        aabbMin.z = min(aabbMin.z, tmp.z.toDouble())
        aabbMax.x = max(aabbMax.x, tmp.x.toDouble())
        aabbMax.y = max(aabbMax.y, tmp.y.toDouble())
        aabbMax.z = max(aabbMax.z, tmp.z.toDouble())
    }

    aabbMin.add(t.origin)
    aabbMax.add(t.origin)
    Stack.subVec3d(1)
}
