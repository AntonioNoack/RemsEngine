package me.anno.ecs.components.mesh.material

import me.anno.ecs.components.mesh.material.shaders.Texture3DBTShader
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Texture3D
import me.anno.gpu.texture.TextureLib
import me.anno.maths.Maths
import me.anno.utils.pooling.JomlPools
import org.joml.Vector3f

/**
 * [Texture3D] - block traced material.
 * This [Material] lerps between two colors.
 * You can modify the materials to be more complex by writing your own [Texture3DBTShader]-like [Shader].
 * */
@Suppress("unused")
open class Texture3DBTMaterial : Material() {

    @NotSerializedProperty
    var blocks: Texture3D? = null

    var color0 = Vector3f()
        set(value) {
            field.set(value)
        }

    var color1 = Vector3f()
        set(value) {
            field.set(value)
        }

    init {
        shader = Texture3DBTShader
    }

    fun limitColors(count: Int) {
        val div = Maths.max(1, count - 1)
        val f0 = 1f + 1f / div
        val f1 = 1f - 255f / div
        val tmp = JomlPools.vec3f.borrow().set(color0)
        color0.mix(color1, 1f - f0)
        color1.mix(tmp, f1)
    }

    override fun bind(shader: GPUShader) {
        super.bind(shader)
        val ti = shader.getTextureIndex("blocksTexture")
        val blocks = blocks ?: TextureLib.whiteTex3d
        if (ti >= 0) blocks.bindTrulyNearest(ti)
        shader.v3i("bounds", blocks.width, blocks.height, blocks.depth)
        // max amount of blocks that can be traversed
        val maxSteps = Maths.max(1, blocks.width + blocks.height + blocks.depth)
        shader.v1i("maxSteps", maxSteps)
        shader.v3f("color0", color0)
        shader.v3f("color1", color1)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is Texture3DBTMaterial) return
        dst.color0 = color0
        dst.color1 = color1
        // texture cannot be simply copied
    }
}