package me.anno.ecs.components.mesh.material

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Group
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.material.shaders.ParallaxShader
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureLib.grayTexture
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import org.joml.Vector4f
import kotlin.math.max

class ParallaxMaterial : Material() {

    init {
        shader = ParallaxShader
    }

    @Group("Parallax")
    @SerializedProperty
    var parallaxMap: FileReference = InvalidRef

    @Docs("How big the parallax effect is in meters")
    @Group("Parallax")
    @SerializedProperty
    var parallaxScale = 0.05f

    @Range(-3.0, 3.0)
    @Group("Parallax")
    @Docs("Which color on the heightmap means in-plane")
    @SerializedProperty
    var parallaxBias = 0.5f

    @Group("Parallax")
    @Range(1.0, 512.0)
    @SerializedProperty
    var parallaxMinSteps = 8

    @Group("Parallax")
    @Range(1.0, 512.0)
    @SerializedProperty
    var parallaxMaxSteps = 25

    @Type("Tiling")
    @Group("PBR")
    @Docs("uv = uv * tiling.xy + tiling.zw")
    var parallaxTiling = Vector4f(1f, 1f, 0f, 0f)

    override fun listTextures(): List<FileReference> {
        return super.listTextures() + parallaxMap
    }

    override fun listTiling(): List<Vector4f> {
        return super.listTiling() + parallaxTiling
    }

    override fun bind(shader: GPUShader) {
        super.bind(shader)

        val f = if (linearFiltering) Filtering.LINEAR else Filtering.NEAREST
        val c = clamping
        val bound = bindTexture(shader, "parallaxMap", parallaxMap, grayTexture, f, c)
        if (bound != null) {
            shader.v1f("parallaxScale", parallaxScale)
            shader.v1i("minParallaxSteps", max(parallaxMinSteps, 1))
            shader.v1i("maxParallaxSteps", max(parallaxMaxSteps, 512))
        } else {
            // no texture is defined, so set all parallax parameters to zero
            shader.v1f("parallaxScale", 0f)
            shader.v1i("minParallaxSteps", 0)
            shader.v1i("maxParallaxSteps", 0)
        }

        shader.v1f("parallaxBias", parallaxBias)
        shader.v4f("parallaxTiling", parallaxTiling)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is ParallaxMaterial) return

        dst.parallaxMap = parallaxMap
        dst.parallaxBias = parallaxBias
        dst.parallaxScale = parallaxScale
        dst.parallaxMinSteps = parallaxMinSteps
        dst.parallaxMaxSteps = parallaxMaxSteps
    }

    override fun equalProperties(other: MaterialBase): Boolean {
        return other is ParallaxMaterial &&
                other.parallaxMap == parallaxMap &&
                other.parallaxBias == parallaxBias &&
                other.parallaxScale == parallaxScale &&
                other.parallaxMinSteps == parallaxMinSteps &&
                other.parallaxMaxSteps == parallaxMaxSteps
    }
}