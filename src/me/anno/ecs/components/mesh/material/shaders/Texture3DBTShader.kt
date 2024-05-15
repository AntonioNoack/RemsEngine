package me.anno.ecs.components.mesh.material.shaders

import me.anno.ecs.components.mesh.material.Texture3DBTMaterial
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.Variable

/**
 * texture 3d - block traced shader
 * @see Texture3DBTMaterial
 * */
object Texture3DBTShader : BlockTracedShader("3dTex-rt") {

    override fun createFragmentVariables(key: ShaderKey): ArrayList<Variable> {
        val base = super.createFragmentVariables(key)
        base += Variable(GLSLType.S3D, "blocksTexture")
        base += Variable(GLSLType.V3F, "color0")
        base += Variable(GLSLType.V3F, "color1")
        return base
    }

    override fun initProperties(instanced: Boolean): String {
        return "float block = 0.0;\n"
    }

    override fun processBlock(instanced: Boolean): String {
        return "" +
                "block = texture(blocksTexture, blockPosition/bounds1).x;\n" +
                "continueTracing = (block == 0.0);\n"
    }

    override fun onFinish(instanced: Boolean): String {
        return "if(block == 0.0) discard;\n"
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