package me.anno.bulletjme

import com.jme3.bounding.BoundingBox
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape
import com.jme3.bullet.collision.shapes.CollisionShape
import com.jme3.bullet.collision.shapes.CompoundCollisionShape
import com.jme3.bullet.collision.shapes.ConeCollisionShape
import com.jme3.bullet.collision.shapes.CylinderCollisionShape
import com.jme3.bullet.collision.shapes.HullCollisionShape
import com.jme3.bullet.collision.shapes.MeshCollisionShape
import com.jme3.bullet.collision.shapes.SphereCollisionShape
import com.jme3.bullet.collision.shapes.infos.IndexedMesh
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Quaternion
import com.jme3.math.Transform
import com.jme3.math.Vector3f
import me.anno.bulletjme.Rigidbody.Companion.q
import me.anno.bulletjme.Rigidbody.Companion.v
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.CapsuleCollider
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.collider.ConeCollider
import me.anno.ecs.components.collider.ConvexCollider
import me.anno.ecs.components.collider.CylinderCollider
import me.anno.ecs.components.collider.MeshCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.components.physics.BodyWithScale
import me.anno.ecs.components.physics.Physics
import me.anno.utils.pooling.ByteBufferPool
import org.joml.Matrix4x3d
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.max

open class BulletPhysics : Physics<Rigidbody, PhysicsRigidBody>(Rigidbody::class) {

    companion object {
        @JvmField
        val defaultShape = BoxCollisionShape(v(1.0, 1.0, 1.0))
    }

    init {
        synchronousPhysics = true
    }

    var fixedStep = 0.0
    var maxSubSteps = 10

    val bulletInstance = PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT)

    override fun updateGravity() {
        bulletInstance.setGravity(v(gravity))
    }

    override fun removeConstraints(entity: Entity) {
        // idk...
    }

    override fun onCreateRigidbody(
        entity: Entity,
        rigidbody: Rigidbody,
        bodyWithScale: BodyWithScale<Rigidbody, PhysicsRigidBody>
    ) {
        bulletInstance.addCollisionObject(bodyWithScale.external)
        rigidbody.bulletInstance = bodyWithScale.external
        registerNonStatic(entity, rigidbody.isStatic, bodyWithScale)
        // todo constraints
    }

    fun createBulletCollider(collider: Collider, base: Entity, scale: Vector3d): Pair<Transform, CollisionShape> {
        val transform0 = collider.entity!!.fromLocalToOtherLocal(base)
        // there may be extra scale hidden in there
        val extraScale = transform0.getScale(Vector3d())
        val totalScale = Vector3d(scale).mul(extraScale)
        val shape = createBulletShape(collider, totalScale)
        val transform = mat4x3ToTransform(transform0, extraScale)
        return transform to shape
    }

    private fun createBulletShape(collider: Collider, totalScale: Vector3d): CollisionShape {
        return when (collider) {
            is MeshCollider -> {
                // todo convex/concave -> Hull/MeshCollisionShape
                // collider.createBulletShape(scale)
                val mesh = collider.mesh!!
                val srcPos = mesh.positions!!
                val srcIdx = mesh.indices ?: IntArray(srcPos.size / 3) { it }
                val dstPos = ByteBufferPool.allocateDirect(srcPos.size * 4).asFloatBuffer()
                val dstIdx = ByteBufferPool.allocateDirect(srcIdx.size * 4).asIntBuffer()
                dstPos.put(srcPos).flip()
                dstIdx.put(srcIdx).flip()
                MeshCollisionShape(true, IndexedMesh(dstPos, dstIdx))
            }
            is CapsuleCollider -> {
                // todo scale?
                CapsuleCollisionShape(
                    collider.radius.toFloat(),
                    collider.halfHeight.toFloat() * 2f,
                    collider.axis
                )
            }
            is ConeCollider -> {
                ConeCollisionShape(
                    collider.radius.toFloat(),
                    collider.height.toFloat(),
                    collider.axis
                )
            }
            is ConvexCollider -> {
                val points = collider.points!!
                HullCollisionShape(
                    (0 until points.size / 3).map {
                        Vector3f(points[it * 3], points[it * 3 + 1], points[it * 3 + 2])
                    }
                )
            }
            is CylinderCollider -> {
                CylinderCollisionShape(
                    collider.radius.toFloat(),
                    collider.halfHeight.toFloat() * 2f,
                    collider.axis
                )
            }
            // is CircleCollider -> SphereShape(collider.radius * scale.dot(0.33, 0.34, 0.33))
            is SphereCollider -> {
                SphereCollisionShape(collider.radius.toFloat())
            }
            is BoxCollider -> {
                BoxCollisionShape(
                    collider.halfExtends.x.toFloat(),
                    collider.halfExtends.y.toFloat(),
                    collider.halfExtends.z.toFloat()
                )
            }
            /*is RectCollider -> {
                val halfExtends = collider.halfExtends
                return BoxShape(
                    javax.vecmath.Vector3d(
                        halfExtends.x * scale.x,
                        halfExtends.y * scale.y,
                        scale.z
                    )
                )
            }*/
            // is CustomBulletCollider -> collider.createBulletCollider(scale) as CollisionShape
            // is SDFCollider -> collider.createBulletShape(scale)
            else -> defaultShape
        }
    }

    private fun mat4x3ToTransform(transform0: Matrix4x3d, extraScale: Vector3d): Transform {
        val pos = transform0.getTranslation(Vector3d())
        val rot = transform0.getUnnormalizedRotation(Quaterniond())
        val sca = transform0.getScale(Vector3d())
        return Transform()
            .setTranslation(v(pos.x, pos.y, pos.z))
            .setRotation(Quaternion(rot.x.toFloat(), rot.y.toFloat(), rot.z.toFloat(), rot.w.toFloat()))
            .setScale(v(sca.x * extraScale.x, sca.y * extraScale.y, sca.z * extraScale.z))
    }

    private fun createCollider(entity: Entity, colliders: List<Collider>, scale: Vector3d): CollisionShape {
        val firstCollider = colliders.first()
        return if (colliders.size == 1 && firstCollider.entity === entity) {
            // there is only one, and no transform needs to be applied -> use it directly
            createBulletShape(firstCollider, scale)
        } else {
            val jointCollider = CompoundCollisionShape()
            for (collider in colliders) {
                val (transform, subCollider) = createBulletCollider(collider, entity, scale)
                jointCollider.addChildShape(subCollider, transform)
            }
            jointCollider
        }
    }

    override fun createRigidbody(entity: Entity, src: Rigidbody): BodyWithScale<Rigidbody, PhysicsRigidBody>? {
        val colliders = getValidComponents(entity, Collider::class)
            .filter { it.hasPhysics }.toList()
        return if (colliders.isNotEmpty()) {

            // bullet does not work correctly with scale changes: create larger shapes directly
            val globalTransform = entity.transform.globalTransform
            val scale = globalTransform.getScale(Vector3d())

            // copy all knowledge from ecs to bullet
            val collider = createCollider(entity, colliders, scale)

            val mass = max(0.0, src.mass)
            // todo set inertia
            // if (mass > 0.0) collider.calculateLocalInertia(mass, inertia)

            val dst = PhysicsRigidBody(collider, mass.toFloat())
            val pos = globalTransform.getTranslation(Vector3d())
            val rot = globalTransform.getUnnormalizedRotation(Quaterniond())
            dst.setPhysicsLocation(v(pos))
            dst.setPhysicsRotation(q(rot.x, rot.y, rot.z, rot.w))
            dst.setPhysicsScale(v(scale))
            dst.friction = src.friction.toFloat()
            dst.restitution = src.restitution.toFloat()
            dst.linearDamping = src.linearDamping.toFloat()
            dst.angularDamping = src.angularDamping.toFloat()
            dst.linearSleepingThreshold = src.linearSleepingThreshold.toFloat()
            dst.angularSleepingThreshold = src.angularSleepingThreshold.toFloat()
            dst.deactivationTime = src.sleepingTimeThreshold.toFloat()
            dst.ccdMotionThreshold = 1e-3f
            val box = collider.boundingBox(Vector3f(), Quaternion(), BoundingBox())
            dst.ccdSweptSphereRadius = max(max(box.xExtent, box.yExtent), box.zExtent)

            BodyWithScale(src, dst, scale)
        } else null
    }

    override fun worldStepSimulation(step: Double) {
        bulletInstance.setMaxSubSteps(maxSubSteps)
        bulletInstance.update(step.toFloat(), maxSubSteps)
    }

    override fun convertTransformMatrix(rigidbody: PhysicsRigidBody, scale: Vector3d, dstTransform: Matrix4x3d) {
        val pos = rigidbody.getPhysicsLocation(Vector3f())
        val rot = rigidbody.getPhysicsRotation(Quaternion())
        val sca = rigidbody.getScale(Vector3f())
        dstTransform.translationRotateScale(
            pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
            rot.x.toDouble(), rot.y.toDouble(), rot.z.toDouble(), rot.w.toDouble(),
            sca.x.toDouble(), sca.y.toDouble(), sca.z.toDouble()
        )
    }

    override fun isActive(rigidbody: PhysicsRigidBody): Boolean {
        return rigidbody.isActive
    }

    override fun worldRemoveRigidbody(rigidbody: PhysicsRigidBody) {
        bulletInstance.remove(rigidbody)
    }
}