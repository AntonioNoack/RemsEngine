package me.anno.ecs.components.test

import me.anno.ecs.Component
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes
import me.anno.engine.raycast.Raycast
import me.anno.io.serialization.SerializedProperty
import org.joml.Vector3d

class RaycastTestComponent : Component() {

    @Docs("Only colliders with matching flags will be tested")
    @SerializedProperty
    var colliderMask = -1

    @Docs("Which kinds of colliders will be tested; flags; 1 = triangles, 2 = colliders, 4 = sdfs, see Raycast.kt")
    @SerializedProperty
    var typeMask = -1

    @SerializedProperty
    var maxDistance = 1e3

    @Docs("Thickness of the ray-cone at the start of the ray")
    @SerializedProperty
    var radiusAtOrigin = 0.0

    @Docs("Thickness delta with every unit along the ray")
    @SerializedProperty
    var radiusPerUnit = 0.0

    override fun onVisibleUpdate(): Boolean {
        // throw ray cast, and draw the result
        val entity = entity!!
        val transform = entity.transform.globalTransform
        val start = transform.transformPosition(Vector3d())
        val direction = transform.transformDirection(Vector3d(0.0, 0.0, 1.0)).normalize()
        val hit = Raycast.raycast(
            entity, start, direction, radiusAtOrigin, radiusPerUnit,
            maxDistance, typeMask, colliderMask
        )
        if (hit != null) {
            DebugShapes.debugLines.add(DebugLine(start, hit.positionWS, -1))
        } else {
            DebugShapes.debugLines.add(DebugLine(start, Vector3d(direction).add(start), 0xff0000))
        }
        return true
    }

    override fun clone(): RaycastTestComponent {
        val clone = RaycastTestComponent()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as RaycastTestComponent
        clone.colliderMask = colliderMask
        clone.maxDistance = maxDistance
    }

    override val className get() = "RaycastTest"

}