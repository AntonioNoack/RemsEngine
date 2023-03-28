package me.anno.ecs.components.shaders

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFX
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.gradientXTex
import me.anno.image.ImageCPUCache
import me.anno.utils.pooling.JomlPools
import org.joml.Quaternionf
import org.joml.Vector2d
import org.joml.Vector3d

class AutoTileableMaterial : Material() {

    var anisotropic = true
    var worldPosCenter = Vector3d()
    var scale = Vector2d(1.0)
    var tileDir = Quaternionf()

    init {
        shader = AutoTileableShader
    }

    override fun bind(shader: Shader) {
        super.bind(shader)
        // world scale correction
        val worldScale = RenderState.worldScale
        val pos = RenderState.cameraPosition
        shader.v3f(
            "tileOffset",
            ((worldPosCenter.x - pos.x) * worldScale).toFloat(),
            ((worldPosCenter.y - pos.y) * worldScale).toFloat(),
            ((worldPosCenter.z - pos.z) * worldScale).toFloat()
        )

        val dirU = JomlPools.vec3f.create()
        val dirV = JomlPools.vec3f.create()

        tileDir.transform(dirU.set(1.0, 0.0, 0.0))
        tileDir.transform(dirV.set(0.0, 0.0, 1.0))

        // calculate final scale + aspect ratio correction
        val tex3 = getTex(diffuseMap)
        val scaleX = 1.0 * (if (tex3 != null) tex3.h.toFloat() / tex3.w else 1f) / (scale.x * worldScale)
        val scaleY = 1.0 / (scale.y * worldScale)

        shader.v3f(
            "tileDirU",
            (dirU.x * scaleX).toFloat(),
            (dirU.y * scaleX).toFloat(),
            (dirU.z * scaleX).toFloat()
        )

        shader.v3f(
            "tileDirV",
            (dirV.x * scaleY).toFloat(),
            (dirV.y * scaleY).toFloat(),
            (dirV.z * scaleY).toFloat()
        )

        shader.v1b("anisotropic", anisotropic)

        // could be customizable, but who would use that?
        shader.m2x2("latToWorld", AutoTileableShader.latToWorld)
        shader.m2x2("worldToLat", AutoTileableShader.worldToLat)

        JomlPools.vec3f.sub(2)

        // get cached LUT, bind LUT
        val tex = AutoTileableShader.cache.getFileEntry(diffuseMap, false, 10_000L, true) { it, _ ->
            ImageCPUCache[it, false]?.run {
                val hist = AutoTileableShader.TileMath.buildYHistogram(this)
                val lut = AutoTileableShader.TileMath.buildLUT(hist)
                val tex = Texture2D("auto-tileable-lut", lut.size / 2, 2, 1)
                GFX.addGPUTask("auto-tileable-lut", 1) { tex.createMonochrome(lut, false) }
                tex
            }
        }
        // if LUT is unknown (async), load alternative gradient texture
        val tex2 = if (tex is Texture2D && tex.isCreated) tex else gradientXTex
        tex2.bind(shader, "invLUTTex", GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as AutoTileableMaterial
        dst.anisotropic = anisotropic
        dst.worldPosCenter.set(worldPosCenter)
        dst.scale.set(scale)
        dst.tileDir.set(tileDir)
    }

    override val className get() = "AutoTileableMaterial"

}