package me.anno.ecs.components.mesh.material

import me.anno.ecs.components.mesh.material.shaders.Texture3DBTv2Shader
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.texture.Texture3D
import me.anno.gpu.texture.TextureLib
import me.anno.utils.Logging
import org.joml.Vector3i
import kotlin.math.max

/**
 * texture 3d - block traced material; with many-color and transparency support
 * */
@Suppress("unused")
open class Texture3DBTv2Material : Material() {

    val size = Vector3i(1)

    var useSDF = false

    @NotSerializedProperty
    var blocks: Texture3D? = null
        set(value) {
            field = value
            if (value != null) {
                size.set(value.width, value.height, value.depth)
            }
        }

    init {
        shader = Texture3DBTv2Shader
    }

    override fun bind(shader: GPUShader) {
        super.bind(shader)
        val ti = shader.getTextureIndex("blocksTexture")
        val blocks = blocks
        if (ti >= 0) (blocks ?: TextureLib.whiteTex3d).bindTrulyNearest(ti)
        shader.v3i("bounds", size)
        // max amount of blocks that can be traversed
        val maxSteps = max(1, size.x + size.y + size.z)
        shader.v1i("maxSteps", maxSteps)
        shader.v1b("useSDF", useSDF)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is Texture3DBTv2Material) return
        dst.size.set(size)
        dst.useSDF = useSDF
        // texture cannot be simply copied
    }

    override fun hashCode(): Int {
        return Logging.hash32raw(this)
    }

    override fun equals(other: Any?): Boolean {
        return other === this
    }
}