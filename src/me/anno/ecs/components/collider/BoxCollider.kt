package me.anno.ecs.components.collider

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.raycast.RayQueryLocal
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes.drawBox
import me.anno.utils.pooling.JomlPools
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

class BoxCollider : Collider() {

    @SerializedProperty
    var halfExtends = Vector3d(1.0)
        set(value) {
            field.set(value)
        }

    @SerializedProperty
    var margin = 0.05

    override fun union(globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        val halfExtends = halfExtends
        unionCube(globalTransform, aabb, tmp, halfExtends.x, halfExtends.y, halfExtends.z)
    }

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        val halfExtends = halfExtends
        deltaPos.absolute()
        deltaPos.sub(halfExtends.x.toFloat(), halfExtends.y.toFloat(), halfExtends.z.toFloat())
        return and3SDFs(deltaPos)
    }

    override fun drawShape() {
        drawBox(entity, guiLineColor, halfExtends)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is BoxCollider) return
        dst.halfExtends.set(halfExtends)
    }

    override fun raycastClosestHit(query: RayQueryLocal, surfaceNormal: Vector3f?): Float {
        val halfExtends = halfExtends
        val hx = abs(halfExtends.x.toFloat())
        val hy = abs(halfExtends.y.toFloat())
        val hz = abs(halfExtends.z.toFloat())
        val pos = query.start
        val dir = query.direction
        val max = query.maxDistance
        val distance = JomlPools.aabbf.borrow()
            .setMin(-hx, -hy, -hz).setMax(hx, hy, hz)
            .whereIsRayIntersecting(pos, dir, max)
        if (distance < max && surfaceNormal != null) {
            val px = pos.x + dir.x * distance
            val py = pos.y + dir.y * distance
            val pz = pos.z + dir.z * distance
            surfaceNormal.set(px, py, pz).div(max(px, max(py, pz)))
        }
        return distance
    }
}