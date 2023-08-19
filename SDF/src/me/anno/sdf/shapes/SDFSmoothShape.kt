package me.anno.sdf.shapes

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType

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

    fun appendSmoothnessParameter(builder: StringBuilder, uniforms: HashMap<String, TypeValue>) {
        val dynamicSmoothness = dynamicSmoothness || globalDynamic
        if (dynamicSmoothness || smoothness > 0f) {
            builder.append(',')
            if (dynamicSmoothness) builder.appendUniform(uniforms, GLSLType.V1F) { smoothness }
            else builder.append(smoothness)
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFSmoothShape
        dst.smoothness = smoothness
        dst.dynamicSmoothness = dynamicSmoothness
    }
}