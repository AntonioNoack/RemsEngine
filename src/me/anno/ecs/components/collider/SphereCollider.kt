package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.SphereShape
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.gui.LineShapes.drawSphere
import me.anno.io.serialization.SerializedProperty
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f

class SphereCollider : Collider() {

    @SerializedProperty
    var radius = 1.0

    override fun clone(): SphereCollider {
        val clone = SphereCollider()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SphereCollider
        clone.radius = radius
    }

    override fun union(globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        // otherwise just use a cube and its 8 sides
        val r = radius
        if (preferExact) {
            // if prefer exact, then use multiple outer-sphere points
            // outer ring != outer sphere, increase the radius once more!
            // val rEstimate = 1.13174 * r // estimated / visualized in Blender, approximated,
            // and then accurately calculated
            val rExact = OUTER_SPHERE_RADIUS_X8 / INV_COSINE_22_5 * r // 1.1315167192268571
            for (axis in 0..2) {
                unionRing(globalTransform, aabb, tmp, axis, rExact, 0.0, preferExact)
            }
            // we could create a better approximation by picking points ourselves, and calculating their outer radius
        } else {
            unionCube(globalTransform, aabb, tmp, r, r, r)
        }
    }

    /*override fun getSignedDistance(deltaPosition: Vector3d, movement: Vector3d): Double {
        return deltaPosition.length() - radius
    }*/

    override fun getSignedDistance(deltaPos: Vector3f, outNormal: Vector3f): Float {
        outNormal.set(deltaPos).normalize()
        return deltaPos.length() - radius.toFloat()
    }

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        return deltaPos.length() - radius.toFloat()
    }

    override fun createBulletShape(scale: Vector3d): CollisionShape {
        return SphereShape(radius * scale.dot(0.33, 0.34, 0.33))
    }

    override fun drawShape() {
        drawSphere(entity, radius)
    }

    override val className get() = "SphereCollider"

}