package me.anno.ecs.components.mesh.sdf.shapes

import me.anno.ecs.prefab.PrefabSaveable

abstract class SDFSmoothShape : SDFShape() {

    var dynamicSmoothness = false
        set(value) {
            if (field != value) {
                field = value
                invalidateShader()
            }
        }

    var smoothness = 0f
        set(value) {
            if (field != value && !dynamicSmoothness) invalidateShader()
            field = value
        }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFSmoothShape
        clone.smoothness = smoothness
        clone.dynamicSmoothness = dynamicSmoothness
    }

}