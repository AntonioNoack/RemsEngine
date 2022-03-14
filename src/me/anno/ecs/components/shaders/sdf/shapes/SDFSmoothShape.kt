package me.anno.ecs.components.shaders.sdf.shapes

import me.anno.ecs.prefab.PrefabSaveable

abstract class SDFSmoothShape : SDFShape() {

    var smoothness = 0f
    var dynamicSmoothness = false

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFSmoothShape
        clone.smoothness = smoothness
        clone.dynamicSmoothness = dynamicSmoothness
    }

}