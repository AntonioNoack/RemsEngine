package me.anno.ecs.components.mesh.material

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.material.shaders.TriplanarShader
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GPUShader
import org.joml.Vector4f
import kotlin.math.min

class TriplanarMaterial : Material() {

    @Range(0.0, 1.0)
    var sharpness: Float = 0.7f

    @Range(0.0, 1.0)
    var blendPreferY: Float = 0.675f

    var primaryTiling: Vector4f = Vector4f(1f, 1f, 0f, 0f)
        set(value) {
            field.set(value)
        }

    init {
        shader = TriplanarShader
    }

    override fun bind(shader: GPUShader) {
        super.bind(shader)
        shader.v1f("sharpness", if (blendPreferY > 0f) sharpness else min(sharpness, 0.999f))
        shader.v1f("blendPreferY", blendPreferY)
        shader.v4f("primaryTiling", primaryTiling)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is TriplanarMaterial) return
        dst.sharpness = sharpness
        dst.blendPreferY = blendPreferY
        dst.primaryTiling.set(primaryTiling)
    }
}