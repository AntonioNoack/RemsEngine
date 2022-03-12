package me.anno.ecs.components.shaders.sdf

import me.anno.utils.types.AABBs.clear
import org.joml.AABBd
import org.joml.Quaternionf
import org.joml.Vector3f

open class SDFComponent {

    // input: 3d position
    // output: float distance, int material index

    // todo list parameters with their types
    // todo somehow replace them, so multiple can be used

    // local transform
    var position = Vector3f()
    var rotation = Quaternionf()
    var scale = Vector3f()

    var dynamicPosition = false
    var dynamicRotation = false
    var dynamicScale = false

    open fun unionBounds(aabb: AABBd) {
        aabb.clear()
    }

}