package me.anno.ecs.components.mesh.material

import me.anno.cache.CacheSection
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.ecs.annotations.Docs
import me.anno.ecs.components.mesh.material.shaders.AutoTileableShader
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib
import me.anno.image.ImageCache
import me.anno.io.files.FileKey
import me.anno.io.files.FileReference

class AutoTileableMaterial : PlanarMaterialBase() {

    companion object {

        val cache = CacheSection<FileKey, Texture2D>("auto-tileable")

        fun lookUp(diffuseMap: FileReference): ITexture2D {
            // get cached LUT, bind LUT
            val texFromCache = cache.getFileEntry(diffuseMap, false, 10_000L) { srcFile, result ->
                ImageCache[srcFile.file].onSuccess(result) { image->
                    val hist = AutoTileableShader.TileMath.buildYHistogram(image)
                    val lut = AutoTileableShader.TileMath.buildLUT(hist)
                    val tex = Texture2D("auto-tileable-lut", lut.size / 2, 2, 1)
                    addGPUTask("auto-tileable-lut", 1) {
                        tex.createMonochrome(lut, false)
                        result.value = tex
                    }
                }
            }.value
            // if LUT is unknown (async), load alternative gradient texture
            return texFromCache?.createdOrNull() as? Texture2D ?: TextureLib.gradientXTex
        }
    }

    @Docs("More much computationally expensive, but also visually more stable")
    var useAnisotropicFiltering = true

    init {
        shader = AutoTileableShader
    }

    override fun bind(shader: GPUShader) {
        super.bind(shader)
        shader.v1b("anisotropic", useAnisotropicFiltering)
        // could be customizable, but who would use that?
        shader.m2x2("latToWorld", AutoTileableShader.latToWorld)
        shader.m2x2("worldToLat", AutoTileableShader.worldToLat)
        lookUp(diffuseMap).bind(shader, "invLUTTex", Filtering.TRULY_LINEAR, Clamping.CLAMP)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is AutoTileableMaterial) return
        dst.useAnisotropicFiltering = useAnisotropicFiltering
    }
}