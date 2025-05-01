package me.anno.ecs.components.collider

import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.debug.DebugAABB
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes
import me.anno.engine.raycast.RayQueryLocal
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes.drawBox
import me.anno.gpu.pipeline.Pipeline
import me.anno.utils.pooling.JomlPools
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.min

class BoxCollider : Collider() {

    @SerializedProperty
    @Range(0.0, 1e308)
    var halfExtends = Vector3f(1f)
        set(value) {
            field.set(value)
        }

    @SerializedProperty
    var margin = 0.04f

    override fun union(globalTransform: Matrix4x3, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        val halfExtends = halfExtends
        unionCube(
            globalTransform, aabb, tmp,
            halfExtends.x.toDouble(), halfExtends.y.toDouble(), halfExtends.z.toDouble()
        )
    }

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        val halfExtends = halfExtends
        deltaPos.absolute()
        deltaPos.sub(halfExtends.x, halfExtends.y, halfExtends.z)
        return and3SDFs(deltaPos)
    }

    override fun drawShape(pipeline: Pipeline) {
        val halfExtends = halfExtends
        drawBox(
            entity, getLineColor(hasPhysics),
            halfExtends.x.toDouble(), halfExtends.y.toDouble(), halfExtends.z.toDouble()
        )
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is BoxCollider) return
        dst.halfExtends.set(halfExtends)
    }

    override fun raycast(query: RayQueryLocal, surfaceNormal: Vector3f?): Float {
        val pos = query.start
        val dir = query.direction
        val invDir = JomlPools.vec3f.borrow()
            .set(1f / dir.x, 1f / dir.y, 1f / dir.z)
        val max = query.maxDistance
        val halfExtends = halfExtends
        val distance = JomlPools.aabbf.borrow()
            .setMin(-halfExtends.x, -halfExtends.y, -halfExtends.z).setMax(halfExtends)
            .whereIsRayIntersecting(pos, invDir, margin)
        if (distance < max && surfaceNormal != null) {
            val px = pos.x + dir.x * distance
            val py = pos.y + dir.y * distance
            val pz = pos.z + dir.z * distance
            surfaceNormal.set(px, py, pz)
        }
        return distance
    }
}