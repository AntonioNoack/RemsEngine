package me.anno.openxr.ecs

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.mesh.LineRenderer
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths.sq
import me.anno.openxr.OpenXRController.Companion.xrControllers
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Booleans.toInt
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.min

// controller pose at the moment:
//  minus z = up
//  plus x = right
//  minus y = forward

class VRHandController : Component(), OnUpdate {

    var isRightHand = true
    var teleportCircleMesh: Entity? = null
    var showIfDisconnected = false

    var inHandItem: VRHandPickup? = null

    fun updateVisibility(visible: Boolean) {
        val scale = if (visible) 1.0 else 1e-16
        entity?.setScale(scale)
        teleportCircleMesh?.setScale(scale)
    }

    private var wasPointingAtFloor = false

    override fun onUpdate() {
        val controller = xrControllers[isRightHand.toInt()]
        if (!controller.isConnected) {
            lastWarning = "Controller isn't connected"
            updateVisibility(showIfDisconnected)
            return
        } else {
            updateVisibility(true)
            lastWarning = null
        }

        val transform = transform
        transform?.setGlobal(
            transform.globalTransform
                .identity()
                .translationRotate(controller.position, controller.rotation)
        )

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
        val distance = if (hitSth) query.result.distance else 1.0
        val hitPosition = query.direction.mulAdd(distance, query.start, Vector3d())

        // todo check that it is a teleportArea
        // todo snap to teleport anchors
        val pointingAtFloor = hitSth && isRightHand && distance > 1.0 &&
                Input.isKeyDown(Key.CONTROLLER_RIGHT_THUMBSTICK_UP) &&
                query.result.geometryNormalWS.y > 0.8 // floor must be flat

        // line renderer like in Unity to show potential teleport opportunities
        // todo change line color based on proposed action (?)
        // make this a round stick???
        val lineRenderer = getComponent(LineRenderer::class)
        if (lineRenderer != null) {

            if (pointingAtFloor != wasPointingAtFloor) {
                wasPointingAtFloor = pointingAtFloor
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
            val extraHeight = if (pointingAtFloor) 0.05 * distance else 0.0
            for (i in lineRenderer.points.indices) {
                val f = i / (lineRenderer.points.size - 1.0)
                controller.position.lerp(hitPosition, f, tmp)
                tmp.y += extraHeight * (1.0 - sq(2.0 * f - 1.0))
                invTransform.transformPosition(tmp) // global -> local
                lineRenderer.points[i].set(tmp)
            }

            tmp.set(0.0, 1.0, 0.0)
            if (pointingAtFloor) {
                invTransform.transformDirection(tmp)
            }
            lineRenderer.up.set(tmp.normalize())
            lineRenderer.thickness = min(distance.toFloat(), 0.3f) * 0.1f
        }

        val hitTarget = teleportCircleMesh
        if (hitTarget != null) {
            hitTarget.transform.setGlobal(
                hitTarget.transform.globalTransform.identity()
                    .translate(hitPosition)
                    .translate(0.0, distance * 0.01, 0.0) // todo rotate according to normal
                    .scale(if (pointingAtFloor) 0.5 else 1e-16)
            )
            hitTarget.transform.teleportUpdate()
            teleportCircleMesh?.invalidateAABBsCompletely()
        }

        val grabKey = if (isRightHand) Key.CONTROLLER_RIGHT_SQUEEZE_PRESS
        else Key.CONTROLLER_LEFT_SQUEEZE_PRESS

        var inHandItem = inHandItem
        if (Input.wasKeyReleased(grabKey) && inHandItem != null) {
            controller.rumble += 0.5f
            inHandItem = null // released
            // todo enable physics, if present
        }

        if (Input.wasKeyPressed(grabKey)) {
            controller.rumble += 0.5f
            // todo find inHandItem in scene by distance to hand/hit
            // todo disable physics if present
        }
        this.inHandItem = inHandItem

        val inHandEntity = inHandItem?.entity
        if (inHandItem != null && inHandEntity != null) {
            if (inHandItem.shouldBeLockedInHand) {
                inHandEntity.transform.setGlobal(
                    inHandEntity.transform.globalTransform
                        .translationRotate(controller.position, controller.rotation)
                )
            } else {
                // todo clamp distance to hit (?)
                // todo allow zooming it out/in
                inHandEntity.transform.setGlobal(
                    inHandEntity.transform.globalTransform
                        .translationRotate(controller.position, controller.rotation)
                        .translate(0.0, 0.0, 1.0)
                )
            }
        }

        // todo
        //  - teleporting to areas
        //  - teleporting to anchors
        //  - picking stuff up
        //  - placing stuff

        // todo when looking at UI,
        //  then allow scrolling, clicking, hovering and such
        //  -> additional, virtual mice?
        // todo draw ray towards UI/items
    }
}