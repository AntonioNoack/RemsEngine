package me.anno.ecs.components.mesh.sdf.shapes

import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable

abstract class SDFSmoothShape : SDFShape() {

    var dynamicSmoothness = false
        set(value) {
            if (field != value) {
                field = value
                invalidateShader()
            }
        }

    @Range(0.0, 1e38)
    var smoothness = 0f
        set(value) {
            if (field != value) {
                if (!dynamicSmoothness) invalidateShader()
                // bounds are not influenced by smoothness (in 99% of cases)
                field = value
            }
        }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFSmoothShape
        clone.smoothness = smoothness
        clone.dynamicSmoothness = dynamicSmoothness
    }

}