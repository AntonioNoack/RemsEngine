package me.anno.ecs.components.mesh

import me.anno.ecs.Component
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.RenderState
import me.anno.maths.Maths.clamp
import me.anno.utils.pooling.JomlPools
import org.joml.Vector3d
import kotlin.math.abs

@Docs("rotates and scales the entity parallel to the camera; only works well with a single local player")
open class BillboardTransformer : Component() {

    @Docs("minimum scale; set this to maximum scale to disable scaling in screen space")
    var minSize = 0.0

    @Docs("maximum scale at a distance of maxSizeDistance")
    var maxSize = 0.1

    @Docs("base distance for scale calculations")
    var maxSizeDistance = 1000.0

    var tiltX = true
    var tiltY = true
    var tiltZ = true

    @Docs("needs to be enabled, if the base entity of this may rotate or scale; disabled is faster")
    var useGlobalTransform = true

    val dir = Vector3d()

    override fun onUpdate(): Int {
        super.onUpdate()

        val transform = transform ?: return 16

        dir.set(transform.globalPosition).sub(RenderState.cameraPosition)

        val distance = abs(dir.dot(RenderState.cameraDirection))
        // remove non-forward components, so the billboard is parallel to the camera instead of looking at the camera
        dir.set(RenderState.cameraDirection).mul(distance)

        var size = maxSize * maxSizeDistance / distance
        size = clamp(size, minSize, maxSize)

        val scale = size * distance
        val rotation =
            if (tiltX && tiltY && tiltZ) { // easy path: objects look towards +z, camera towards -z, so just copy the rotation
                RenderState.cameraRotation
            } else {

                if (!tiltX) dir.x = 0.0
                if (!tiltY) dir.y = 0.0
                if (!tiltZ) dir.z = 0.0

                val length = dir.length()

                if (length > 0.0) {
                    dir.div(length)
                    JomlPools.quat4d.create().identity()
                        .lookAlong(dir, RenderState.cameraDirectionUp).conjugate()
                } else null
            }

        if (rotation != null) {
            if (useGlobalTransform) {
                transform.globalRotation = rotation
            } else {
                transform.localRotation = rotation
            }
        }

        if (useGlobalTransform) {
            transform.globalScale = transform.globalScale.set(scale)
        } else {
            transform.localScale = transform.localScale.set(scale)
        }
        transform.teleportUpdate()
        invalidateAABB()

        return 1
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as BillboardTransformer
        dst.minSize = minSize
        dst.maxSize = maxSize
        dst.maxSizeDistance = maxSizeDistance
        dst.tiltX = tiltX
        dst.tiltY = tiltY
        dst.tiltZ = tiltZ
    }

}