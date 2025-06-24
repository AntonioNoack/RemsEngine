package me.anno.bullet

import com.bulletphysics.collision.dispatch.ActivationState
import com.bulletphysics.collision.dispatch.CollisionDispatcher
import com.bulletphysics.collision.narrowphase.ManifoldPoint
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.collision.shapes.ConvexHullShape
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugPoint
import me.anno.engine.debug.DebugShapes
import me.anno.engine.ui.render.DrawAABB.drawAABB
import me.anno.engine.ui.render.RenderState.cameraPosition
import me.anno.gpu.buffer.LineBuffer.putRelativeLine
import me.anno.gpu.pipeline.Pipeline
import me.anno.ui.UIColors
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.maps.CountMap
import org.joml.AABBd
import org.joml.Vector3d

object BulletRendering {

    fun BulletPhysics.renderGUI(pipeline: Pipeline) {
        drawConstraints(pipeline)
        drawColliders(pipeline)
        drawContactPoints()
        drawBodiesWithAxesAndCenter()
        drawAABBs()
        drawVehicles()
        drawIslands()
    }

    private fun BulletPhysics.drawIslands() {
        val transform = Transform()
        val min = Vector3d()
        val max = Vector3d()
        val boundsById = HashMap<Int, AABBd>()
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
        for ((tag, bounds) in boundsById) {
            if (countMap[tag] == 1) continue // a not a true island
            drawAABB(bounds, color)
        }
    }

    private fun BulletPhysics.drawColliders(pipeline: Pipeline) {
        for ((_, bodyWithScale) in rigidBodies) {
            drawColliders(pipeline, bodyWithScale?.internal ?: continue)
        }
    }

    private fun drawColliders(pipeline: Pipeline, rigidbody: PhysicsBody<*>) {
        val colliders = rigidbody.activeColliders
        for (i in colliders.indices) {
            colliders.getOrNull(i)?.drawShape(pipeline)
        }
    }

    private fun BulletPhysics.drawConstraints(pipeline: Pipeline) {
        for ((_, bodyWithScale) in dynamicRigidBodies) {
            val body = bodyWithScale.internal as? PhysicalBody ?: continue
            drawConstraints(pipeline, body)
        }
    }

    private fun drawConstraints(pipeline: Pipeline, rigidbody: PhysicalBody) {
        val constraints = rigidbody.linkedConstraints
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
        DebugShapes.debugArrows.add(DebugLine(a, b2, color, 0f))
    }

    private fun BulletPhysics.drawAABBs() {

        val tmpTrans = Stack.newTrans()
        val minAabb = Stack.newVec()
        val maxAabb = Stack.newVec()

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
                        tmpTrans.transform(p1)
                        DebugShapes.debugPoints.add(DebugPoint(p1, -1, 0f))
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
        Stack.subVec(2)
    }

    private fun BulletPhysics.drawBodiesWithAxesAndCenter() {
        val bodies = bulletInstance.collisionObjects
        val tmp = JomlPools.vec3d.create()
        val colorX = UIColors.axisXColor
        val colorY = UIColors.axisYColor
        val colorZ = UIColors.axisZColor
        for (i in bodies.indices) {
            val transform = bodies[i].worldTransform
            val center = transform.origin
            val basis = transform.basis
            val scale = 0.1 * center.distance(cameraPosition)

            basis.getColumn(0, tmp).mul(scale).add(center)
            putRelativeLine(center, tmp, colorX)

            basis.getColumn(1, tmp).mul(scale).add(center)
            putRelativeLine(center, tmp, colorY)

            basis.getColumn(2, tmp).mul(scale).add(center)
            putRelativeLine(center, tmp, colorZ)
        }
        JomlPools.vec3d.sub(1)
    }

    private fun BulletPhysics.drawVehicles() {

        val world = bulletInstance
        val vehicles = world.vehicles

        val tmp = Stack.newVec()
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
                putRelativeLine(wheelPosWS, tmp, wheelColor)

                mat.transformDirection(wheel.wheelDirectionCS, tmp)
                tmp.add(wheelPosWS)
                putRelativeLine(wheelPosWS, tmp, wheelColor)

                val contact = wheel.raycastInfo.contactPointWS
                putRelativeLine(wheelPosWS, contact, wheelColor)
            }
        }

        Stack.subVec(1)
        Stack.subTrans(1)

        val actions = world.actions
        for (i in 0 until actions.size) {
            val action = actions[i] ?: break
            action.debugDraw(BulletDebugDraw)
        }
    }
}