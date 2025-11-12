package me.anno.bullet

import com.bulletphysics.collision.dispatch.ActivationState
import com.bulletphysics.collision.dispatch.CollisionDispatcher
import com.bulletphysics.collision.narrowphase.ManifoldPoint
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.collision.shapes.ConvexHullShape
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import me.anno.bullet.bodies.PhysicalBody
import me.anno.bullet.bodies.PhysicsBody
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugPoint
import me.anno.engine.debug.DebugShapes.showDebugArrow
import me.anno.engine.debug.DebugShapes.showDebugPoint
import me.anno.engine.ui.render.DrawAABB.drawAABB
import me.anno.engine.ui.render.RenderState.cameraPosition
import me.anno.gpu.buffer.LineBuffer.addLine
import me.anno.gpu.pipeline.Pipeline
import me.anno.ui.UIColors
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.maps.CountMap
import org.joml.AABBd
import org.joml.Vector3d
import speiger.primitivecollections.IntToObjectHashMap

object BulletRendering {

    fun BulletPhysics.renderGUI(pipeline: Pipeline) {
        drawConstraints(pipeline)
        drawColliders(pipeline)
        drawContactPoints()
        drawBodiesWithAxesAndCenter()
        drawAABBs()
        drawVehicles()
        drawIslands()
        drawActions()
    }

    private fun BulletPhysics.drawIslands() {
        val transform = Transform()
        val min = Vector3d()
        val max = Vector3d()
        val boundsById = IntToObjectHashMap<AABBd>()
        val countMap = CountMap<Int>()
        for (instance in bulletInstance.collisionObjects) {
            val tag = instance.islandTag
            if (tag < 0) continue // no island available
            // get transformed bounds
            instance.getWorldTransform(transform)
            instance.collisionShape!!.getBounds(transform, min, max)
            // add bounds to island
            boundsById.getOrPut(tag) { AABBd() }
                .union(min.x, min.y, min.z, max.x, max.y, max.z)
            countMap.incAndGet(tag)
        }

        // render all islands as AABBs
        val color = UIColors.dodgerBlue
        boundsById.forEach { tag, bounds ->
            if (countMap[tag] > 1) {
                drawAABB(bounds, color)
            } // else a not a true island
        }
    }

    private fun BulletPhysics.drawColliders(pipeline: Pipeline) {
        for ((_, bodyWithScale) in rigidBodies) {
            drawColliders(pipeline, bodyWithScale?.internal ?: continue)
        }
    }

    private fun drawColliders(pipeline: Pipeline, physicsBody: PhysicsBody<*>) {
        val colliders = physicsBody.activeColliders
        for (i in colliders.indices) {
            val collider = colliders.getOrNull(i) ?: continue
            collider.drawShape(pipeline)
        }
    }

    private fun BulletPhysics.drawConstraints(pipeline: Pipeline) {
        for ((_, bodyWithScale) in rigidBodies) {
            val body = bodyWithScale?.internal as? PhysicalBody ?: continue
            drawConstraints(pipeline, body)
        }
    }

    private fun drawConstraints(pipeline: Pipeline, rigidbody: PhysicalBody) {
        val constraints = rigidbody.activeConstraints
        for (i in constraints.indices) {
            val constraint = constraints.getOrNull(i) ?: break
            constraint.onDrawGUI(pipeline, true)
        }
    }

    private fun BulletPhysics.drawContactPoints() {
        val dispatcher = bulletInstance.dispatcher as? CollisionDispatcher ?: return
        val manifolds = dispatcher.manifoldsList
        for (i in manifolds.indices) {
            val contact = manifolds.getOrNull(i) ?: break
            drawContactManifold(contact)
        }
    }

    private fun drawContactManifold(contactManifold: PersistentManifold) {
        for (j in 0 until contactManifold.numContacts) {
            drawContactPoint(contactManifold.getContactPoint(j))
        }
    }

    private fun drawContactPoint(point: ManifoldPoint) {
        val color = UIColors.magenta
        val cam = cameraPosition
        val a = point.positionWorldOnB
        val n = point.normalWorldOnB
        val d = 0.05 * cam.distance(a.x, a.y, a.z)
        val b2 = Vector3d(a.x + n.x * d, a.y + n.y * d, a.z + n.z * d)
        showDebugArrow(DebugLine(a, b2, color, 0f))
    }

    private fun BulletPhysics.drawAABBs() {

        val tmpTrans = Stack.newTrans()
        val minAabb = Stack.newVec3d()
        val maxAabb = Stack.newVec3d()

        val collisionObjects = bulletInstance.collisionObjects

        val bounds = JomlPools.aabbd.create()
        for (i in collisionObjects.indices) {

            val colObj = collisionObjects[i]
            val color = when (colObj.activationState) {
                ActivationState.ACTIVE -> 0xffffff
                ActivationState.SLEEPING -> 0x333333
                ActivationState.WANTS_DEACTIVATION -> 0x00ffff
                ActivationState.ALWAYS_ACTIVE -> 0xff0000
                ActivationState.DISABLE_SIMULATION -> 0xffff00
            }.withAlpha(255)

            // todo draw the local coordinate arrows
            // debugDrawObject(colObj.getWorldTransform(tmpTrans), colObj.collisionShape, color)

            try {
                val shape = colObj.collisionShape!!
                shape.getBounds(colObj.getWorldTransform(tmpTrans), minAabb, maxAabb)
                if (shape is ConvexHullShape) {
                    forLoopSafely(shape.points.size, 3) { idx ->
                        val p1 = Vector3d(shape.points, idx)
                        tmpTrans.transformPosition(p1)
                        showDebugPoint(DebugPoint(p1, -1, 0f))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            drawAABB(
                bounds
                    .setMin(minAabb.x, minAabb.y, minAabb.z)
                    .setMax(maxAabb.x, maxAabb.y, maxAabb.z),
                color
            )
        }

        JomlPools.aabbd.sub(1)
        Stack.subTrans(1)
        Stack.subVec3d(2)
    }

    private fun BulletPhysics.drawBodiesWithAxesAndCenter() {
        val bodies = bulletInstance.collisionObjects
        val pos3f = JomlPools.vec3f.borrow()
        val pos3d = JomlPools.vec3d.borrow()
        val colorX = UIColors.axisXColor
        val colorY = UIColors.axisYColor
        val colorZ = UIColors.axisZColor
        for (i in bodies.indices) {
            val transform = bodies[i].worldTransform
            val center = transform.origin
            val basis = transform.basis
            val scale = 0.1f * center.distance(cameraPosition).toFloat()

            basis.getColumn(0, pos3f).mul(scale)
            pos3d.set(pos3f).add(center)
            addLine(center, pos3d, colorX)

            basis.getColumn(1, pos3f).mul(scale)
            pos3d.set(pos3f).add(center)
            addLine(center, pos3d, colorY)

            basis.getColumn(2, pos3f).mul(scale)
            pos3d.set(pos3f).add(center)
            addLine(center, pos3d, colorZ)
        }
    }

    private fun BulletPhysics.drawVehicles() {

        val world = bulletInstance
        val vehicles = world.vehicles

        val tmp = Stack.newVec3d()
        val mat = Stack.newTrans()

        for (i in vehicles.indices) {
            val vehicle = vehicles[i]
            val wheels = vehicle.wheels
            for (j in wheels.indices) {

                val wheel = wheels[j]
                val wheelColor = (if (wheel.raycastInfo.isInContact) 0x0000ff else 0xff0000) or black

                vehicle.getChassisWorldTransform(mat).inverse()

                val wheelPosWS = wheel.worldTransform.origin

                mat.transformDirection(wheel.wheelAxleCS, tmp)
                tmp.add(wheelPosWS)
                addLine(wheelPosWS, tmp, wheelColor)

                mat.transformDirection(wheel.wheelDirectionCS, tmp)
                tmp.add(wheelPosWS)
                addLine(wheelPosWS, tmp, wheelColor)

                val contact = wheel.raycastInfo.contactPointWS
                addLine(wheelPosWS, contact, wheelColor)
            }
        }

        Stack.subVec3d(1)
        Stack.subTrans(1)
    }

    private fun BulletPhysics.drawActions() {
        val actions = bulletInstance.actions
        for (i in actions.indices) {
            val action = actions[i] ?: break
            action.debugDraw(BulletDebugDraw)
        }
    }
}