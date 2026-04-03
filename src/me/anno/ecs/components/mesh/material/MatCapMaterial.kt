package me.anno.ecs.components.mesh.material

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.material.shaders.MatCapShader
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib
import me.anno.io.files.FileReference
import me.anno.utils.OS.res

/**
 * MatCap material: a cheap-to-calculate, pre-illuminated material that only looks plausible on non-trivial geometry.
 *
 * Continue reading: https://learn.foundry.com/modo/content/help/pages/shading_lighting/shader_items/matcap.html
 * Get textures for this material type: https://github.com/nidorx/matcaps
 * For testing, you can also use the bundled examples in res://textures/matcap
 * */
class MatCapMaterial : MaterialBase() {

    init {
        shader = MatCapShader
    }

    @Docs("MatCap Texture")
    @Type("Texture/Reference")
    var matCapMap: FileReference = defaultTexture

    override fun bind(shader: GPUShader) {
        super.bind(shader)

        val texture = TextureCache[matCapMap].value?.createdOrNull()
            ?: TextureLib.whiteTexture
        texture.bind(shader, "diffuseMap", Filtering.LINEAR, Clamping.CLAMP)
    }

    override fun hashCode(): Int {
        // only hash common properties?
        var result = super.hashCode()
        result = 31 * result + matCapMap.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        // if you have a customized Material class, you must implement your own equals function
        if (this::class != MatCapMaterial::class || other !is MatCapMaterial ||
            other::class != MatCapMaterial::class
        ) return false

        return equalProperties(other)
    }

    override fun equalProperties(other: MaterialBase): Boolean {
        if (!super.equalProperties(other)) return false
        if (other !is MatCapMaterial) return false
        if (matCapMap != other.matCapMap) return false

        return true
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is MatCapMaterial) return
        dst.matCapMap = matCapMap
    }

    companion object {
        private val defaultTexture = res.getChild("textures/matcap/Ceramic.webp")
    }

}