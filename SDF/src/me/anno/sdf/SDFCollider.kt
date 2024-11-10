package me.anno.sdf

import com.bulletphysics.collision.shapes.BoxShape
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.annotations.Docs
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.physics.CustomBulletCollider
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.DrawAABB
import me.anno.engine.ui.render.RenderState
import me.anno.maths.Maths.sq
import me.anno.sdf.physics.ConcaveSDFShape
import me.anno.sdf.physics.ConvexSDFShape
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d

// todo high gravity -> tunneling
// todo position is effecting collider incorrectly (visuals != physics != displayed bounds != visuals)
class SDFCollider : Collider(), CustomBulletCollider {

    companion object {
        @JvmField
        val defaultShape = BoxShape(javax.vecmath.Vector3d(1.0, 1.0, 1.0))
    }

    // could be assigned by hand...
    val sdf get() = entity?.getComponent(SDFComponent::class)

    @Docs("Invisible gap between rigidbodies to simplify computations")
    var margin = 0.04 // CONVEX_DISTANCE_MARGIN

    override var isConvex = true

    override fun createBulletCollider(scale: Vector3d): Any {
        // todo scale is missing...
        val sdf = sdf ?: return defaultShape
        return if (isConvex) {
            ConvexSDFShape(sdf, this)
        } else {
            ConcaveSDFShape(sdf, this)
        }
    }

    fun calculateLocalInertia(mass: Double, inertia: javax.vecmath.Vector3d) {
        // inertia of a box, because we have no better idea;
        // we could approximate it, but oh well...
        val sdf = sdf
        val base = mass / 12.0
        if (sdf != null) {
            val bounds = sdf.globalAABB
            val x2 = sq(bounds.deltaX)
            val y2 = sq(bounds.deltaY)
            val z2 = sq(bounds.deltaZ)
            inertia.set(y2 + z2, z2 + x2, x2 + y2)
            inertia.scale(base)
        } else inertia.set(base, base, base)
    }

    override fun union(globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        val sdf = sdf ?: return
        sdf.localAABB.apply {
            union(globalTransform, aabb, tmp, minX, minY, minZ)
            union(globalTransform, aabb, tmp, minX, minY, maxZ)
            union(globalTransform, aabb, tmp, minX, maxY, minZ)
            union(globalTransform, aabb, tmp, minX, maxY, maxZ)
            union(globalTransform, aabb, tmp, maxX, minY, minZ)
            union(globalTransform, aabb, tmp, maxX, minY, maxZ)
            union(globalTransform, aabb, tmp, maxX, maxY, minZ)
            union(globalTransform, aabb, tmp, maxX, maxY, maxZ)
        }
    }

    override fun drawShape() {
        // how? draw bounds...
        val sdf = sdf ?: return
        val color = getLineColor(hasPhysics)
        val transform = transform?.getDrawMatrix()
        // draw local aabb
        // DrawAABB.drawAABB(sdf.globalAABB, RenderView.worldScale, color)
        DrawAABB.drawAABB(transform, sdf.localAABB, RenderState.worldScale, color)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SDFCollider) return
        dst.isConvex = isConvex
        dst.margin = margin
    }
}