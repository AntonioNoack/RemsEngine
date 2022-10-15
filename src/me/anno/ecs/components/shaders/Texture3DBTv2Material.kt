package me.anno.ecs.components.shaders

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture3D
import me.anno.gpu.texture.TextureLib.whiteTex3d
import me.anno.maths.Maths.max
import org.joml.Vector3i

/**
 * texture 3d - block traced material; with many-color and transparency support
 * */
@Suppress("unused")
class Texture3DBTv2Material : Material() {

    val size = Vector3i(1)

    var blocks: Texture3D? = null
        set(value) {
            field = value
            if (value != null) {
                size.set(value.w, value.h, value.d)
            }
        }

    init {
        shader = Texture3DBTv2Shader
    }

    override fun bind(shader: Shader) {
        super.bind(shader)
        val ti = shader.getTextureIndex("blocksTexture")
        val blocks = blocks
        if (ti >= 0) {
            if (blocks != null) blocks.bind(ti, GPUFiltering.TRULY_NEAREST)
            else whiteTex3d.bind(ti, GPUFiltering.TRULY_NEAREST)
        }
        shader.v3i("bounds", size)
        // max amount of blocks that can be traversed
        val maxSteps = max(1, size.x + size.y + size.z)
        shader.v1i("maxSteps", maxSteps)
        me.anno.utils.LOGGER.warn(shader.fragmentSource)
    }

    override fun clone(): Texture3DBTv2Material {
        val clone = Texture3DBTv2Material()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Texture3DBTv2Material
        clone.size.set(size)
        // texture cannot be simply copied
    }

    override val className = "Texture3DBTv2Material"

}