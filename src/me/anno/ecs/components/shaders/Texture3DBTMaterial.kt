package me.anno.ecs.components.shaders

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture3D
import me.anno.gpu.texture.TextureLib.whiteTex3d
import org.joml.Vector3f

/**
 * texture 3d - block traced material
 * */
class Texture3DBTMaterial : BlockTracedMaterial("3dTex-rt") {

    var blocks: Texture3D? = null
    var color0 = Vector3f()
    var color1 = Vector3f()

    override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        super.bind(shader, renderer, instanced)
        val ti = shader.getTextureIndex("blocksTexture")
        val blocks = blocks
        if (ti >= 0) {
            if (blocks != null) blocks.bind(ti, GPUFiltering.TRULY_NEAREST)
            else whiteTex3d.bind(ti, GPUFiltering.TRULY_NEAREST)
        }
        shader.v3f("color0", color0)
        shader.v3f("color1", color1)
    }

    override fun createFragmentVariables(instanced: Boolean): List<Variable> {
        return super.createFragmentVariables(instanced) +
                listOf(
                    Variable(GLSLType.S3D, "blocksTexture"),
                    Variable(GLSLType.V3F, "color0"),
                    Variable(GLSLType.V3F, "color1"),
                )
    }

    override fun initProperties(instanced: Boolean): String {
        return "float block;\n"
    }

    override fun checkIfIsAir(instanced: Boolean): String {
        return "" +
                "block = texture(blocksTexture, blockPosition/bounds1).x;\n" +
                "isAir = (block == 0.0);\n"
    }

    override fun computeMaterialProperties(instanced: Boolean): String {
        return "" +
                "finalColor = mix(color0, color1, block);\n" +
                "finalAlpha = 1.0;\n" +
                "finalEmissive = vec3(0.0);\n" +
                "finalMetallic = 0.0;\n" +
                "finalRoughness = 0.5;\n"
    }
}