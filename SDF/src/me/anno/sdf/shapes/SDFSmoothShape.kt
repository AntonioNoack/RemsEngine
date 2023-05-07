package me.anno.sdf.shapes

import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable

abstract class SDFSmoothShape : SDFShape() {

    var dynamicSmoothness = false
        set(value) {
            if (field != value) {
                field = value
                if (!globalDynamic) invalidateShader()
            }
        }

    @Range(0.0, 1e38)
    var smoothness = 0f
        set(value) {
            if (field != value) {
                if (!(dynamicSmoothness || globalDynamic)) invalidateShader()
                // bounds are not influenced by smoothness (in 99% of cases)
                field = value
            }
        }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFSmoothShape
        dst.smoothness = smoothness
        dst.dynamicSmoothness = dynamicSmoothness
    }

}