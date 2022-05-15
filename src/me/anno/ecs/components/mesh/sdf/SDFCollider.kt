package me.anno.ecs.components.mesh.sdf

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.Component
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.collider.MeshCollider.Companion.defaultShape
import me.anno.engine.ui.render.DrawAABB
import me.anno.engine.ui.render.RenderView
import me.anno.maths.Maths.sq
import me.anno.utils.types.AABBs.deltaX
import me.anno.utils.types.AABBs.deltaY
import me.anno.utils.types.AABBs.deltaZ
import org.joml.Vector3d
import kotlin.math.max
import kotlin.math.min

// todo test this
class SDFCollider : Collider() {

    // could be assigned by hand...
    val sdf get() = entity?.getComponent(SDFComponent::class)

    override var isConvex = false

    fun getAABB(t: Transform, aabbMin: javax.vecmath.Vector3d, aabbMax: javax.vecmath.Vector3d) {

        val sdf = sdf ?: return
        val bounds = sdf.globalAABB

        // if t is just scaling + translation, we could simplify this

        aabbMin.set(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
        aabbMax.set(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)

        val tmp = javax.vecmath.Vector3d()
        for (i in 0 until 8) {
            tmp.set(
                if (i.and(1) != 0) bounds.minX else bounds.maxX,
                if (i.and(2) != 0) bounds.minY else bounds.maxY,
                if (i.and(4) != 0) bounds.minZ else bounds.maxZ
            )
            t.transform(tmp)
            aabbMin.x = min(aabbMin.x, tmp.x)
            aabbMin.y = min(aabbMin.y, tmp.y)
            aabbMin.z = min(aabbMin.z, tmp.z)
            aabbMax.x = max(aabbMax.x, tmp.x)
            aabbMax.y = max(aabbMax.y, tmp.y)
            aabbMax.z = max(aabbMax.z, tmp.z)
        }
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
            inertia.set(
                y2 + z2, z2 + x2, x2 + y2
            )
            inertia.scale(base)
        } else inertia.set(base, base, base)
    }

    override fun createBulletShape(scale: Vector3d): CollisionShape {
        val sdf = sdf ?: return defaultShape
        return if (isConvex) {
            ConvexSDFShape(sdf, this)
        } else {
            ConcaveSDFShape(sdf, this)
        }
    }

    override fun drawShape() {
        // how? draw bounds...
        val sdf = sdf ?: return
        DrawAABB.drawAABB(sdf.globalAABB, RenderView.worldScale, guiLineColor)
    }

    override fun clone(): Component {
        val clone = SDFCollider()
        copy(clone)
        return clone
    }

    override val className = "SDFCollider"

}