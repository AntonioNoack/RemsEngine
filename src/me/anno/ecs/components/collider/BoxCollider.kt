package me.anno.ecs.components.collider

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.raycast.RayQueryLocal
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes.drawBox
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

class BoxCollider : Collider() {

    @SerializedProperty
    var halfExtends = Vector3d(1.0)

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
        dst as BoxCollider
        dst.halfExtends.set(halfExtends)
    }

    override fun raycastClosestHit(query: RayQueryLocal, surfaceNormal: Vector3f?): Float {

        val start = query.start
        val direction = query.direction

        // https://jcgt.org/published/0007/03/04/paper-lowres.pdf
        // "A Ray-Box Intersection Algorithm and Efficient Dynamic Voxel Rendering"
        val hx = abs(halfExtends.x.toFloat())
        val hy = abs(halfExtends.y.toFloat())
        val hz = abs(halfExtends.z.toFloat())

        val eps = 1e-38f
        val maxS = max(abs(start.x / max(eps, hx)), max(abs(start.y / max(eps, hy)), abs(start.z / max(eps, hz))))
        val winding = if (maxS < 1f) -1f else +1f

        val sgnX = if (direction.x < 0f) winding else -winding
        val sgnY = if (direction.y < 0f) winding else -winding
        val sgnZ = if (direction.z < 0f) winding else -winding

        // distance to plane
        val dx = (hx * sgnX - start.x) / direction.x
        val dy = (hy * sgnY - start.y) / direction.y
        val dz = (hz * sgnZ - start.z) / direction.z

        return when {
            dx >= 0f && (abs(start.y + direction.y * dx) < hy && abs(start.z + direction.z * dx) < hz) -> {
                setSurfaceNormal(surfaceNormal, direction, 0)
                dx
            }
            dy >= 0f && (abs(start.z + direction.z * dy) < hz && abs(start.x + direction.x * dy) < hx) -> {
                setSurfaceNormal(surfaceNormal, direction, 1)
                dy
            }
            dz >= 0f && (abs(start.x + direction.x * dz) < hx && abs(start.y + direction.y * dz) < hy) -> {
                setSurfaceNormal(surfaceNormal, direction, 2)
                dz
            }
            else -> Float.POSITIVE_INFINITY
        } * winding
    }

    private fun setSurfaceNormal(surfaceNormal: Vector3f?, direction: Vector3f, i: Int) {
        surfaceNormal ?: return
        surfaceNormal.set(0f)
        surfaceNormal[i] = sign(direction[i])
    }
}