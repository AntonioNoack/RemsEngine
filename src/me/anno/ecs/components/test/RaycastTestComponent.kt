package me.anno.ecs.components.test

import me.anno.ecs.Component
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes
import me.anno.engine.raycast.Raycast
import org.joml.Vector3d

class RaycastTestComponent : Component() {

    var colliderMask = -1

    var maxLength = 1e3

    override fun onVisibleUpdate() {
        // throw ray cast, and draw the result
        val transform = transform!!.globalTransform
        val start = transform.transformPosition(Vector3d())
        val direction = transform.transformDirection(Vector3d(0.0, 0.0, 1.0)).normalize()
        val hit = Raycast.raycast(entity!!, start, direction, maxLength, Raycast.TypeMask.BOTH, colliderMask)
        if (hit != null) {
            DebugShapes.debugLines.add(DebugLine(start, hit.positionWS, -1))
        } else {
            DebugShapes.debugLines.add(DebugLine(start, Vector3d(direction).add(start), 0xff0000))
        }
    }

    override val className: String = javaClass.simpleName

}