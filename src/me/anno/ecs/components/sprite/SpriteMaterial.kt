package me.anno.ecs.components.sprite

import me.anno.ecs.components.mesh.material.Material
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib.blackTex2da
import me.anno.gpu.texture.TextureLib.normalTex2da
import me.anno.gpu.texture.TextureLib.whiteTex2da
import me.anno.io.files.FileReference
import org.joml.Vector2i

/**
 * Material, that uses a texture atlas instead of a single texture.
 * Needs specialized mesh-data, that sets V1I,"spriteIndex" for the fragment shader.
 * */
class SpriteMaterial : Material() {

    var numTiles = Vector2i(16, 16)
        set(value) {
            field.set(value)
        }

    init {
        shader = SpriteShader
        linearFiltering = false
        clamping = Clamping.CLAMP
    }

    override fun bind(shader: GPUShader) {
        super.bind(shader)
        val filtering = if (linearFiltering) Filtering.LINEAR else Filtering.NEAREST
        bindTextureArray(shader, "diffuseMapArray", diffuseMap, filtering, whiteTex2da)
        bindTextureArray(shader, "emissiveMapArray", emissiveMap, filtering, blackTex2da)
        bindTextureArray(shader, "normalMapArray", normalMap, filtering, normalTex2da)
        bindTextureArray(shader, "occlusionMapArray", occlusionMap, filtering, whiteTex2da)
    }

    private fun bindTextureArray(
        shader: GPUShader, dstName: String, source: FileReference, filtering: Filtering,
        default: Texture2DArray
    ) {
        val texIdx = shader.getTextureIndex(dstName)
        if (texIdx < 0) return
        val texture = TextureCache.getTextureArray(source, numTiles, true) ?: default
        texture.bind(texIdx, filtering, clamping)
    }
}