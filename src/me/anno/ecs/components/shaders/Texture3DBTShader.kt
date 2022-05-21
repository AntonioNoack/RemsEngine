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
 * texture 3d - block traced shader
 * @see Texture3DBTMaterial
 * */
@Suppress("unused")
object Texture3DBTShader : BlockTracedShader("3dTex-rt") {

    override fun createFragmentVariables(isInstanced: Boolean, isAnimated: Boolean): ArrayList<Variable> {
        val base = super.createFragmentVariables(isInstanced, isAnimated)
        base += Variable(GLSLType.S3D, "blocksTexture")
        base += Variable(GLSLType.V3F, "color0")
        base += Variable(GLSLType.V3F, "color1")
        return base
    }

    override fun initProperties(instanced: Boolean): String {
        return "float block = 0.0;\n"
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