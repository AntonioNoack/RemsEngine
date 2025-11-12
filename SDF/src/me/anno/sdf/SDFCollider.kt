package me.anno.sdf

import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.BoxShape.Companion.boxInertia
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.annotations.Docs
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.physics.CustomBulletCollider
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.DrawAABB
import me.anno.gpu.pipeline.Pipeline
import me.anno.sdf.physics.ConcaveSDFShape
import me.anno.sdf.physics.ConvexSDFShape
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f

// todo high gravity -> tunneling
// todo position is effecting collider incorrectly (visuals != physics != displayed bounds != visuals)
class SDFCollider : Collider(), CustomBulletCollider {

    companion object {
        @JvmField
        val defaultShape = BoxShape(Vector3f(1f))
    }

    // could be assigned by hand...
    val sdf get() = entity?.getComponent(SDFComponent::class)

    @Docs("Invisible gap between rigidbodies to simplify computations")
    var margin = 0.04f // CONVEX_DISTANCE_MARGIN

    override var isConvex = true

    override fun createBulletCollider(scale: Vector3f): Any {
        // todo scale is missing...
        val sdf = sdf ?: return defaultShape
        return if (isConvex) {
            ConvexSDFShape(sdf, this)
        } else {
            ConcaveSDFShape(sdf, this)
        }
    }

    fun calculateLocalInertia(mass: Float, inertia: Vector3f): Vector3f {
        // inertia of a box, because we have no better idea;
        // we could approximate it, but oh well...
        val sdf = sdf
        return if (sdf != null) {
            val bounds = sdf.globalAABB
            boxInertia(
                bounds.deltaX.toFloat() * 0.5f,
                bounds.deltaY.toFloat() * 0.5f,
                bounds.deltaZ.toFloat() * 0.5f,
                mass, inertia
            )
        } else inertia.set(mass / 12.0)
    }

    override fun union(globalTransform: Matrix4x3, dstUnion: AABBd, tmp: Vector3d) {
        sdf?.localAABB?.transformUnion(globalTransform, dstUnion)
    }

    override fun drawShape(pipeline: Pipeline) {
        // how? draw bounds...
        val sdf = sdf ?: return
        val color = colliderLineColor
        val transform = transform?.getDrawMatrix()
        // draw local aabb
        // DrawAABB.drawAABB(sdf.globalAABB, color)
        DrawAABB.drawAABB(transform, sdf.localAABB, color)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SDFCollider) return
        dst.isConvex = isConvex
        dst.margin = margin
    }
}