package me.anno.sdf.shapes

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.material.utils.TypeValue
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
                else if(boundsInfluencedBySmoothness) invalidateBounds()
                field = value
            }
        }

   open val boundsInfluencedBySmoothness get() = false

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
        if (dst !is SDFSmoothShape) return
        dst.smoothness = smoothness
        dst.dynamicSmoothness = dynamicSmoothness
    }
}