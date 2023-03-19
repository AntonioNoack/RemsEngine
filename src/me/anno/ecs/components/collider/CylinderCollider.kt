package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.CylinderShape
import com.bulletphysics.collision.shapes.CylinderShapeX
import com.bulletphysics.collision.shapes.CylinderShapeZ
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.LineShapes.drawCircle
import me.anno.io.serialization.SerializedProperty
import me.anno.maths.Maths.length
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs

class CylinderCollider : Collider() {

    /** which axis the height is for, x = 0, y = 1, z = 2 */
    @Range(0.0, 2.0)
    @SerializedProperty
    var axis = 1

    @SerializedProperty
    var halfHeight = 1.0

    @SerializedProperty
    var radius = 1.0

    override fun union(globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        // union the two rings
        val h = halfHeight
        val r = radius
        unionRing(globalTransform, aabb, tmp, axis, r, +h, preferExact)
        unionRing(globalTransform, aabb, tmp, axis, r, -h, preferExact)
    }

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        val halfHeight = halfHeight.toFloat()
        val radius = radius.toFloat()
        val circle = when (axis) {
            0 -> length(deltaPos.y, deltaPos.z)
            1 -> length(deltaPos.x, deltaPos.z)
            else -> length(deltaPos.x, deltaPos.y)
        } - radius
        val box = abs(deltaPos[axis]) - halfHeight
        deltaPos.x = circle
        deltaPos.y = box
        return and2SDFs(deltaPos, roundness.toFloat())
    }

    override fun createBulletShape(scale: Vector3d): CollisionShape {
        return when (axis) {
            0 -> CylinderShapeX(javax.vecmath.Vector3d(halfHeight * scale.x, radius * scale.y, radius * scale.z))
            1 -> CylinderShape(javax.vecmath.Vector3d(radius * scale.x, halfHeight * scale.y, radius * scale.z))
            2 -> CylinderShapeZ(javax.vecmath.Vector3d(radius * scale.x, radius * scale.y, halfHeight * scale.z))
            else -> throw RuntimeException()
        }
    }

    override fun drawShape() {
        val h = halfHeight
        val r = radius
        val e = entity
        when(axis){
            0 -> {
                LineShapes.drawLine(e, -h, -r, 0.0, +h, -r, 0.0)
                LineShapes.drawLine(e, -h, +r, 0.0, +h, +r, 0.0)
                LineShapes.drawLine(e, -h, 0.0, -r, +h, 0.0, -r)
                LineShapes.drawLine(e, -h, 0.0, +r, +h, 0.0, +r)
                drawCircle(e, r, 1, 2, +h)
                drawCircle(e, r, 1, 2, -h)
            }
            1 -> {
                LineShapes.drawLine(e, -r, -h, 0.0, -r, +h, 0.0)
                LineShapes.drawLine(e, +r, -h, 0.0, +r, +h, 0.0)
                LineShapes.drawLine(e, 0.0, -h, -r, 0.0, +h, -r)
                LineShapes.drawLine(e, 0.0, -h, +r, 0.0, +h, +r)
                drawCircle(e, r, 0, 2, +h)
                drawCircle(e, r, 0, 2, -h)
            }
            2 -> {
                LineShapes.drawLine(e, -r, 0.0, -h, -r, 0.0, +h)
                LineShapes.drawLine(e, +r, 0.0, -h, +r, 0.0, +h)
                LineShapes.drawLine(e, 0.0, -r, -h, 0.0, -r, +h)
                LineShapes.drawLine(e, 0.0, +r, -h, 0.0, +r, +h)
                drawCircle(e, r, 0, 1, +h)
                drawCircle(e, r, 0, 1, -h)
            }
        }
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as CylinderCollider
        clone.axis = axis
        clone.halfHeight = halfHeight
        clone.radius = radius
    }

    override val className get() = "CylinderCollider"

}