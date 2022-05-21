package me.anno.ecs.components.shaders

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture3D
import me.anno.gpu.texture.TextureLib.whiteTex3d
import me.anno.maths.Maths.max
import me.anno.maths.Maths.unmix
import me.anno.utils.pooling.JomlPools
import org.joml.Vector3f
import org.joml.Vector3i

/**
 * texture 3d - block traced material
 * */
@Suppress("unused")
class Texture3DBTMaterial : Material() {

    val size = Vector3i(1)

    var blocks: Texture3D? = null
        set(value) {
            field = value
            if (value != null) {
                size.set(value.w, value.h, value.d)
            }
        }

    var color0 = Vector3f()
    var color1 = Vector3f()

    init {
        shader = Texture3DBTShader
    }

    fun limitColors(count: Int) {
        val div = max(1, count - 1)
        val f0 = 1f + 1f / div
        val f1 = 1f - 255f / div
        val tmp = JomlPools.vec3f.borrow().set(color0)
        color0.lerp(color1, 1f - f0)
        color1.lerp(tmp, f1)
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
        shader.v3f("color0", color0)
        shader.v3f("color1", color1)
    }

    override fun clone(): Texture3DBTMaterial {
        val clone = Texture3DBTMaterial()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Texture3DBTMaterial
        clone.color0.set(color0)
        clone.color1.set(color1)
        clone.size.set(size)
        // texture cannot be simply copied
    }

    override val className = "Texture3DBTMaterial"

}