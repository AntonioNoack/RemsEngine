package me.anno.ecs.components.shaders

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Texture3D
import me.anno.gpu.texture.TextureLib.whiteTex3d
import me.anno.maths.Maths.max
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
        if (ti >= 0) (blocks ?: whiteTex3d).bindTrulyNearest(ti)
        shader.v3i("bounds", size)
        // max amount of blocks that can be traversed
        val maxSteps = max(1, size.x + size.y + size.z)
        shader.v1i("maxSteps", maxSteps)
        shader.v3f("color0", color0)
        shader.v3f("color1", color1)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Texture3DBTMaterial
        dst.color0.set(color0)
        dst.color1.set(color1)
        dst.size.set(size)
        // texture cannot be simply copied
    }

    override val className get() = "Texture3DBTMaterial"

}