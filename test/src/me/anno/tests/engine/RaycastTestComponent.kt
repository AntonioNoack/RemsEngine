package me.anno.tests.engine

import me.anno.ecs.Component
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.serialization.SerializedProperty
import org.joml.Vector3d
import org.joml.Vector3f

class RaycastTestComponent : Component(), OnUpdate {

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

    override fun onUpdate() {
        // throw ray cast, and draw the result
        val entity = entity!!
        val transform = entity.transform.globalTransform
        val start = transform.transformPosition(Vector3d())
        val direction = transform.transformDirection(Vector3f(0f, 0f, 1f)).normalize()

        val query = RayQuery(
            start, direction, maxDistance, radiusAtOrigin, radiusPerUnit,
            typeMask, colliderMask, false, emptySet(),
        )
        if (Raycast.raycast(entity, query)) {
            DebugShapes.debugLines.add(DebugLine(start, query.result.positionWS, -1))
        } else {
            DebugShapes.debugLines.add(DebugLine(start, Vector3d(direction).add(start), 0xff0000))
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is RaycastTestComponent) return
        dst.colliderMask = colliderMask
        dst.maxDistance = maxDistance
    }
}