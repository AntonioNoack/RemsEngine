package me.anno.openxr.ecs

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponentsInChildrenAndBounds
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.LineRenderer
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.render.RenderView
import me.anno.input.Input
import me.anno.input.Key
import me.anno.input.controller.Controller
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.pow
import me.anno.maths.Maths.sq
import me.anno.openxr.OpenXRActions.Companion.AXIS_THUMBSTICK_Y
import me.anno.openxr.OpenXRController.Companion.xrControllers
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Vectors.normalToQuaternionY
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.min
import kotlin.math.sqrt

// controller pose at the moment:
//  minus z = up
//  plus x = right
//  minus y = forward

class VRHandController : Component(), OnUpdate {

    var isRightHand = true
    var teleportCircleMesh: Entity? = null
    var showIfDisconnected = false

    var inHandItem: VRHandPickup? = null
    var inHandDistance = 1.0

    var playerHeight = 1.8

    private var wasPointingAtFloor = false

    override fun onUpdate() {

        // todo fade out rumble?

        val controller = xrControllers[isRightHand.toInt()]
        if (!controller.isConnected) {
            lastWarning = "Controller isn't connected"
            updateVisibility(showIfDisconnected)
            return
        }

        updateVisibility(true)
        lastWarning = null

        teleportToController(controller)

        val query = RayQuery(
            controller.position,
            controller.rotation.transform(Vector3d(0.0, -1.0, -0.4).normalize()),
            1e9
        )

        val entity = entity
        val root = entity?.getRoot(Entity::class)
        query.ignored = setOfNotNull(entity, teleportCircleMesh, inHandItem?.entity)
        val hitSth = root != null && Raycast.raycastClosestHit(root, query)

        query.result.geometryNormalWS.safeNormalize()
        val hitDistance = if (hitSth) query.result.distance else 1.0
        val hitPosition = query.direction.mulAdd(hitDistance, query.start, Vector3d())

        // todo check that it is a teleportArea
        // todo snap to teleport anchors
        // disable teleport when holding sth, because key is used already
        val isHoldingSth = inHandItem != null
        val teleportKey = Key.CONTROLLER_RIGHT_THUMBSTICK_UP
        val mayTeleport = hitSth && !isHoldingSth && isRightHand && hitDistance > 1.0 &&
                (Input.isKeyDown(teleportKey) || Input.wasKeyReleased(teleportKey)) &&
                query.result.geometryNormalWS.y > 0.8 // floor must be flat

        val rv = RenderView.currentInstance
        if (mayTeleport && Input.wasKeyReleased(teleportKey) && rv != null) {
            // actually teleport there
            // todo better API for that? another camera than editorCamera won't respect this
            rv.orbitCenter.set(hitPosition)
                .add(0.0, playerHeight, 0.0)
        }

        updateLineRenderer(controller, hitPosition, hitDistance, mayTeleport, isHoldingSth)

        updateTeleportCircleMesh(hitPosition, query.result.shadingNormalWS, hitDistance, mayTeleport)
        updateInHandItem(root, controller, hitPosition, hitDistance)
        updateInHandTransform(controller, hitDistance)

        // todo when looking at UI,
        //  then allow scrolling, clicking, hovering and such
        //  -> additional, virtual mice?
        // todo draw ray towards UI/items
    }

    fun updateVisibility(visible: Boolean) {
        val scale = if (visible) 1.0 else 1e-16
        entity?.setScale(scale)
        teleportCircleMesh?.setScale(scale)
    }

    private fun teleportToController(controller: Controller) {
        val transform = transform ?: return
        transform.setGlobal(
            transform.globalTransform
                .identity()
                .translationRotate(controller.position, controller.rotation)
        )
    }

    private fun updateTeleportCircleMesh(
        hitPosition: Vector3d, hitNormal: Vector3d, hitDistance: Double,
        mayTeleport: Boolean
    ) {
        val hitTarget = teleportCircleMesh
        if (hitTarget != null) {
            hitNormal.safeNormalize()
            hitTarget.transform.setGlobal(
                hitTarget.transform.globalTransform.identity()
                    .translate(hitPosition)
                    .translate(0.0, hitDistance * 0.01, 0.0)
                    .rotate(hitNormal.normalToQuaternionY())
                    .scale(if (mayTeleport) 0.5 else 1e-16)
            )
            hitTarget.transform.teleportUpdate()
            teleportCircleMesh?.invalidateAABBsCompletely()
        }
    }

    private fun updateInHandItem(root: Entity?, controller: Controller, hitPosition: Vector3d, hitDistance: Double) {

        val grabKey = if (isRightHand) Key.CONTROLLER_RIGHT_SQUEEZE_PRESS
        else Key.CONTROLLER_LEFT_SQUEEZE_PRESS

        var inHandItem = inHandItem
        if (root != null && inHandItem == null && Input.wasKeyPressed(grabKey)) {
            // find inHandItem in scene by distance to hand/hit
            val searchedBounds = AABBd().set(hitPosition).addMargin(1.0) // good? probably :)
            var bestTarget: VRHandPickup? = null
            var bestDistanceSq = Double.POSITIVE_INFINITY
            root.forAllComponentsInChildrenAndBounds(VRHandPickup::class, searchedBounds, false) {
                val itEntity = it.transform!!
                val distSq = itEntity.globalPosition.distanceSquared(hitPosition)
                if (distSq < bestDistanceSq && distSq < sq(it.maxPickupDistance)) {
                    bestTarget = it
                    bestDistanceSq = distSq
                }
            }
            controller.rumble += if (bestTarget != null) 0.5f else 0.2f
            // to do disable physics if present?
            inHandItem = bestTarget
            inHandDistance = sqrt(bestDistanceSq)
        }

        if (inHandItem != null && Input.wasKeyReleased(grabKey)) {
            controller.rumble += 0.5f
            val inHandTransform = inHandItem.transform
            if (root != null && inHandTransform != null) {
                // placing stuff into sockets
                var bestTarget: Transform? = null
                var bestDistanceSq = 0.2 + hitDistance * 0.03 // good like that?
                val searchedBounds = AABBd().set(hitPosition).addMargin(sqrt(bestDistanceSq))
                root.forAllComponentsInChildrenAndBounds(VRSocket::class, searchedBounds, false) {
                    val itEntity = it.transform!!
                    val distSq = itEntity.globalPosition.distanceSquared(hitPosition)
                    if (distSq < bestDistanceSq) {
                        bestTarget = itEntity
                        bestDistanceSq = distSq
                    }
                }
                val bestTargetI = bestTarget
                if (bestTargetI != null) {
                    // place object there
                    inHandTransform.setGlobal( // good like that? should be :)
                        inHandTransform.globalTransform
                            .translationRotate(bestTargetI.globalPosition, bestTargetI.globalRotation)
                    )
                    inHandTransform.teleportUpdate()
                    // todo create/define some events, so we know about this happening
                }
            }
            inHandItem = null // released
            // to do enable physics, if present?
        }
        this.inHandItem = inHandItem
    }

    private fun updateInHandTransform(controller: Controller, hitDistance: Double) {
        val inHandItem = inHandItem
        val inHandTransform = inHandItem?.transform
        // todo if is hitting floor (hitDistance ~ inHandDistance), rotate item, too?
        if (inHandItem != null && inHandTransform != null) {
            if (inHandItem.shouldBeLockedInHand) {
                inHandTransform.globalPosition = controller.position
                inHandTransform.globalRotation = controller.rotation
            } else {
                // allow zooming it out/in
                inHandDistance *= pow(2.0, controller.getAxis(AXIS_THUMBSTICK_Y) * Time.deltaTime)
                if (inHandDistance > hitDistance) { // smooth-clamp distance to hit
                    inHandDistance = mix(inHandDistance, hitDistance, dtTo01(Time.deltaTime))
                }
                // update in-hand transform
                val shownDistance = min(inHandDistance, hitDistance) // hard-clamp distance here, too
                inHandTransform.globalPosition = inHandTransform.globalPosition
                    .set(0.0, 0.0, shownDistance).rotate(controller.rotation)
                    .add(controller.position)
                inHandTransform.globalRotation = controller.rotation
            }
            inHandTransform.smoothUpdate()
        }
    }

    private fun updateLineRenderer(
        controller: Controller, hitPosition: Vector3d, distance: Double,
        mayTeleport: Boolean, isHoldingSth: Boolean
    ) {
        // line renderer like in Unity to show potential teleport opportunities
        // todo change line color/thickness based on proposed action (?)
        // make this a round stick???
        val lineRenderer = getComponent(LineRenderer::class)
        if (lineRenderer != null) {
            if (isHoldingSth) {
                // hide line, when holding sth
                lineRenderer.points = emptyList()
            } else {

                if (mayTeleport != wasPointingAtFloor) {
                    wasPointingAtFloor = mayTeleport
                    controller.rumble += 0.5f
                }

                val numPoints = 20
                if (lineRenderer.points.size != numPoints) {
                    lineRenderer.points = createArrayList(numPoints) { Vector3f() }
                }

                // invTransform is known to be [controller.position, controller.rotation]^-1, so this could be simplified
                val invTransform = lineRenderer.transform!!.globalTransform
                    .invert(Matrix4x3d())

                val tmp = Vector3d()
                val extraHeight = if (mayTeleport) 0.05 * distance else 0.0
                for (i in lineRenderer.points.indices) {
                    val f = i / (lineRenderer.points.size - 1.0)
                    controller.position.lerp(hitPosition, f, tmp)
                    tmp.y += extraHeight * (1.0 - sq(2.0 * f - 1.0))
                    invTransform.transformPosition(tmp) // global -> local
                    lineRenderer.points[i].set(tmp)
                }

                tmp.set(0.0, 1.0, 0.0)
                if (mayTeleport) {
                    invTransform.transformDirection(tmp)
                }
                lineRenderer.up.set(tmp.normalize())
                lineRenderer.thickness = min(distance.toFloat(), 0.3f) * 0.1f
            }
        }
    }
}