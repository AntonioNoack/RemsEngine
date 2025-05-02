package me.anno.ecs.components.mesh

import me.anno.ecs.Component
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnBeforeDraw
import me.anno.engine.ui.render.RenderState
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.utils.pooling.JomlPools
import org.joml.AABBd
import org.joml.Matrix4x3
import kotlin.math.abs

@Docs("rotates and scales the entity parallel to the camera; only works well with a single local player")
open class LookAtComponent : Component(), OnBeforeDraw {

    @Docs("Minimum scale; set this to maximum scale to disable scaling in screen space")
    var minSize = 0.0

    @Docs("Maximum scale at a distance of maxSizeDistance")
    var maxSize = 0.1

    @Docs("Base distance for scale calculations")
    var maxSizeDistance = 1000.0

    var tiltX = true
    var tiltY = true
    var tiltZ = true

    @Docs("Needs to be enabled, if the base entity of this may rotate or scale; disabled is faster")
    var useGlobalTransform = true

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd): Boolean {
        dstUnion.all()
        return true
    }

    override fun onBeforeDraw() {
        val transform = transform ?: return
        transform.validate()

        val dir = JomlPools.vec3f.create()
        transform.globalPosition.sub(RenderState.cameraPosition, dir)

        val minDistance = 1e-38f
        val distance = max(minDistance, abs(dir.dot(RenderState.cameraDirection)))
        // remove non-forward components, so the billboard is parallel to the camera instead of looking at the camera
        dir.set(RenderState.cameraDirection).normalize(distance)

        var size = maxSize * maxSizeDistance / distance
        size = clamp(size, minSize, maxSize)

        val scale = size * distance
        val rotation =
            if (tiltX && tiltY && tiltZ) { // easy path: objects look towards +z, camera towards -z, so just copy the rotation
                RenderState.cameraRotation
            } else {

                if (!tiltX) dir.x = 0f
                if (!tiltY) dir.y = 0f
                if (!tiltZ) dir.z = 0f

                val length = dir.length()

                if (length > 0.0) {
                    dir.div(length)
                    JomlPools.quat4f.create().identity()
                        .lookAlong(dir, RenderState.cameraDirectionUp).conjugate()
                } else null
            }

        JomlPools.vec3f.sub(1)

        if (rotation != null) {
            if (useGlobalTransform) transform.globalRotation = rotation
            else transform.localRotation = rotation
        }

        if (useGlobalTransform) transform.globalScale = transform.globalScale.set(scale)
        else transform.localScale = transform.localScale.set(scale)

        transform.validate()
        invalidateAABB()
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is LookAtComponent) return
        dst.minSize = minSize
        dst.maxSize = maxSize
        dst.maxSizeDistance = maxSizeDistance
        dst.tiltX = tiltX
        dst.tiltY = tiltY
        dst.tiltZ = tiltZ
    }
}