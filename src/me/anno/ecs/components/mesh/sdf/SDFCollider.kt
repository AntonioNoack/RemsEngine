package me.anno.ecs.components.mesh.sdf

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import me.anno.ecs.annotations.Docs
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.collider.MeshCollider.Companion.defaultShape
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.DrawAABB
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.buffer.LineBuffer
import me.anno.maths.Maths.sq
import me.anno.maths.geometry.MarchingCubes
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix4x3d
import org.joml.Vector3d
import kotlin.math.max
import kotlin.math.min

// todo high gravity -> tunneling
// todo position is effecting collider incorrectly (visuals != physics != displayed bounds != visuals)
class SDFCollider : Collider() {

    // could be assigned by hand...
    val sdf get() = entity?.getComponent(SDFComponent::class)

    @Docs("Invisible gap between rigidbodies to simplify computations")
    var margin = BulletGlobals.CONVEX_DISTANCE_MARGIN

    override var isConvex = true

    fun getAABB(t: Transform, aabbMin: javax.vecmath.Vector3d, aabbMax: javax.vecmath.Vector3d) {

        val sdf = sdf ?: return
        val bounds = sdf.globalAABB

        // if t is just scaling + translation, we could simplify this

        aabbMin.set(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
        aabbMax.set(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)

        val tmp = Stack.newVec()
        val basis = t.basis
        for (i in 0 until 8) {
            tmp.set(
                if (i.and(1) != 0) bounds.minX else bounds.maxX,
                if (i.and(2) != 0) bounds.minY else bounds.maxY,
                if (i.and(4) != 0) bounds.minZ else bounds.maxZ
            )
            basis.transform(tmp)
            aabbMin.x = min(aabbMin.x, tmp.x)
            aabbMin.y = min(aabbMin.y, tmp.y)
            aabbMin.z = min(aabbMin.z, tmp.z)
            aabbMax.x = max(aabbMax.x, tmp.x)
            aabbMax.y = max(aabbMax.y, tmp.y)
            aabbMax.z = max(aabbMax.z, tmp.z)
        }

        aabbMin.add(t.origin)
        aabbMax.add(t.origin)
        Stack.subVec(1)
    }

    fun calculateLocalInertia(mass: Double, inertia: javax.vecmath.Vector3d) {
        // inertia of a box, because we have no better idea;
        // we could approximate it, but oh well...
        val sdf = sdf
        val base = mass / 12.0
        if (sdf != null) {
            val bounds = sdf.globalAABB
            val x2 = sq(bounds.deltaX())
            val y2 = sq(bounds.deltaY())
            val z2 = sq(bounds.deltaZ())
            inertia.set(y2 + z2, z2 + x2, x2 + y2)
            inertia.scale(base)
        } else inertia.set(base, base, base)
    }

    var shape: CollisionShape? = null
    override fun createBulletShape(scale: Vector3d): CollisionShape {
        val sdf = sdf ?: return defaultShape
        shape = if (isConvex) {
            ConvexSDFShape(sdf, this)
        } else {
            ConcaveSDFShape(sdf, this)
        }
        return shape!!
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
        val color = guiLineColor
        val transform = transform?.getDrawMatrix()
        (shape as? ConcaveSDFShape)?.run {
            // draw last used triangles for debugging
            MarchingCubes.march(fx, fy, fz, field, 0f, AABBf(sdf.entity!!.aabb), false) { a, b, c ->
                if (transform != null) {
                    transform.transformPosition(a)
                    transform.transformPosition(b)
                    transform.transformPosition(c)
                }
                LineBuffer.addLine(a, b, color)
                LineBuffer.addLine(b, c, color)
                LineBuffer.addLine(c, a, color)
            }
        }
        // draw local aabb
        // DrawAABB.drawAABB(sdf.globalAABB, RenderView.worldScale, color)
        DrawAABB.drawAABB(transform, sdf.localAABB, RenderState.worldScale, color)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFCollider
        dst.isConvex = isConvex
        dst.margin = margin
    }

    override val className: String get() = "SDFCollider"

}