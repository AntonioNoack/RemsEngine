package me.anno.ecs.components.mesh.material

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Group
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.material.shaders.ParallaxShader
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureLib.grayTexture
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import kotlin.math.max

class ParallaxMaterial : Material() {

    init {
        shader = ParallaxShader
    }

    @Group("Parallax")
    @SerializedProperty
    var heightMap: FileReference = InvalidRef

    @Docs("How big the parallax effect is in meters")
    @Group("Parallax")
    @SerializedProperty
    var parallaxScale = 0.05f

    @Group("Parallax")
    @Docs("Which color on the heightmap means in-plane")
    @SerializedProperty
    var parallaxBias = 0.5f

    @Group("Parallax")
    @Range(1.0, 512.0)
    @SerializedProperty
    var minParallaxSteps = 8

    @Group("Parallax")
    @Range(1.0, 512.0)
    @SerializedProperty
    var maxParallaxSteps = 25

    override fun bind(shader: GPUShader) {
        super.bind(shader)

        val f = if (linearFiltering) Filtering.LINEAR else Filtering.NEAREST
        val c = clamping
        val bound = bindTexture(shader, "heightMap", heightMap, grayTexture, f, c)
        if (bound != null) {
            shader.v1f("parallaxScale", parallaxScale)
            shader.v1i("minParallaxSteps", max(minParallaxSteps, 1))
            shader.v1i("maxParallaxSteps", max(maxParallaxSteps, 512))
        } else {
            // no texture is defined, so set all parallax parameters to zero
            shader.v1f("parallaxScale", 0f)
            shader.v1i("minParallaxSteps", 0)
            shader.v1i("maxParallaxSteps", 0)
        }

        shader.v1f("parallaxBias", parallaxBias)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if(dst !is ParallaxMaterial) return

        dst.heightMap = heightMap
        dst.parallaxBias = parallaxBias
        dst.parallaxScale = parallaxScale
        dst.minParallaxSteps = minParallaxSteps
        dst.maxParallaxSteps = maxParallaxSteps
    }
}