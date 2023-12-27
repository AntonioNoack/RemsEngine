package me.anno.ecs.components.shaders

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.gradientXTex
import me.anno.image.ImageCache
import me.anno.io.files.FileReference

class AutoTileableMaterial : PlanarMaterialBase() {

    companion object {
        fun lookUp(diffuseMap: FileReference): ITexture2D {
            // get cached LUT, bind LUT
            val tex = AutoTileableShader.cache.getFileEntry(diffuseMap, false, 10_000L, true) { it, _ ->
                ImageCache[it, false]?.run {
                    val hist = AutoTileableShader.TileMath.buildYHistogram(this)
                    val lut = AutoTileableShader.TileMath.buildLUT(hist)
                    val tex = Texture2D("auto-tileable-lut", lut.size / 2, 2, 1)
                    GFX.addGPUTask("auto-tileable-lut", 1) { tex.createMonochrome(lut, false) }
                    tex
                }
            }
            // if LUT is unknown (async), load alternative gradient texture
            return if (tex is Texture2D && tex.isCreated) tex else gradientXTex
        }
    }

    var anisotropic = true

    init {
        shader = AutoTileableShader
    }

    override fun bind(shader: GPUShader) {
        super.bind(shader)
        shader.v1b("anisotropic", anisotropic)
        // could be customizable, but who would use that?
        shader.m2x2("latToWorld", AutoTileableShader.latToWorld)
        shader.m2x2("worldToLat", AutoTileableShader.worldToLat)
        lookUp(diffuseMap).bind(shader, "invLUTTex", Filtering.TRULY_LINEAR, Clamping.CLAMP)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as AutoTileableMaterial
        dst.anisotropic = anisotropic
    }

    override val className: String get() = "AutoTileableMaterial"
}