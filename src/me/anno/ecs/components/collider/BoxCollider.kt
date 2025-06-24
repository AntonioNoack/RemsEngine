package me.anno.ecs.components.collider

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.collider.SDFUtils.and3SDFs
import me.anno.ecs.components.collider.UnionUtils.unionCube
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.raycast.RayQueryLocal
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes.drawBox
import me.anno.gpu.pipeline.Pipeline
import me.anno.utils.pooling.JomlPools
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f

class BoxCollider : Collider() {

    @SerializedProperty
    @Range(0.0, 1e308)
    var halfExtents = Vector3f(1f)
        set(value) {
            field.set(value)
        }

    override fun union(globalTransform: Matrix4x3, dstUnion: AABBd, tmp: Vector3d) {
        val halfExtents = halfExtents
        unionCube(
            globalTransform, dstUnion,
            halfExtents.x.toDouble(), halfExtents.y.toDouble(), halfExtents.z.toDouble()
        )
    }

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        val halfExtents = halfExtents
        deltaPos.absolute()
        deltaPos.sub(halfExtents.x, halfExtents.y, halfExtents.z)
        return and3SDFs(deltaPos, roundness)
    }

    override fun drawShape(pipeline: Pipeline) {
        val halfExtents = halfExtents
        drawBox(
            entity, getLineColor(hasPhysics),
            halfExtents.x.toDouble(), halfExtents.y.toDouble(), halfExtents.z.toDouble()
        )
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is BoxCollider) return
        dst.halfExtents.set(halfExtents)
    }

    override fun raycast(query: RayQueryLocal, surfaceNormal: Vector3f?): Float {
        val pos = query.start
        val dir = query.direction
        val invDir = JomlPools.vec3f.borrow()
            .set(1f / dir.x, 1f / dir.y, 1f / dir.z)
        val max = query.maxDistance
        val halfExtents = halfExtents
        val distance = JomlPools.aabbf.borrow()
            .setMin(-halfExtents.x, -halfExtents.y, -halfExtents.z).setMax(halfExtents)
            .whereIsRayIntersecting(pos, invDir, 0f)
        if (distance < max && surfaceNormal != null) {
            val px = pos.x + dir.x * distance
            val py = pos.y + dir.y * distance
            val pz = pos.z + dir.z * distance
            surfaceNormal.set(px, py, pz)
        }
        return distance
    }
}