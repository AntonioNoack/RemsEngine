package me.anno.ecs.components.shaders

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Texture3D
import me.anno.gpu.texture.TextureLib.whiteTex3d
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths.max
import org.joml.Vector3i

/**
 * texture 3d - block traced material; with many-color and transparency support
 * */
@Suppress("unused")
class Texture3DBTv2Material : Material() {

    val size = Vector3i(1)

    var useSDF = false

    @NotSerializedProperty
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
        if (ti >= 0) (blocks ?: whiteTex3d).bindTrulyNearest(ti)
        shader.v3i("bounds", size)
        // max amount of blocks that can be traversed
        val maxSteps = max(1, size.x + size.y + size.z)
        shader.v1i("maxSteps", maxSteps)
        shader.v1b("useSDF", useSDF)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Texture3DBTv2Material
        dst.size.set(size)
        dst.useSDF = useSDF
        // texture cannot be simply copied
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }

    override fun equals(other: Any?): Boolean {
        return other === this
    }

    override val className: String get() = "Texture3DBTv2Material"

}