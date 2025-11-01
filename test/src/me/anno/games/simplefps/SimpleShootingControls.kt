package me.anno.games.simplefps

import me.anno.bullet.bodies.DynamicBody
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.interfaces.CustomEditMode
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes.showDebugArrow
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.render.RenderView
import me.anno.input.Key
import me.anno.utils.Color.black
import org.joml.Vector3d

class SimpleShootingControls : Component(), CustomEditMode {

    var force = 25.0

    override fun onEditClick(button: Key, long: Boolean): Boolean {
        if (button == Key.BUTTON_LEFT) {

            val rv = RenderView.currentInstance
            val scene = rv?.getWorld() as? Entity ?: return false

            // apply impulse onto scene
            val query = rv.rayQuery()
            if (Raycast.raycast(scene, query)) {
                val hitComponent = query.result.component
                val hitEntity = hitComponent?.entity
                val hitRigidbody = hitEntity?.getComponent(DynamicBody::class)
                if (hitRigidbody != null) {
                    val relativePos = query.result.positionWS.sub(hitEntity.transform.globalPosition, Vector3d())
                    val impulse = Vector3d(query.direction).mul(force)
                    hitRigidbody.applyImpulse(relativePos, impulse)
                } else {
                    showDebugArrow(
                        DebugLine(
                            Vector3d(query.start),
                            Vector3d(query.result.positionWS),
                            0x3377ff or black
                        )
                    )
                }
            }

            return true
        } else return false
    }
}
