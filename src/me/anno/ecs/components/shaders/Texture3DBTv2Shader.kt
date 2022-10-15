package me.anno.ecs.components.shaders

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.Variable

/**
 * texture 3d - block traced shader; with many-color and transparency support
 * @see Texture3DBTv2Material
 * */
@Suppress("unused")
object Texture3DBTv2Shader : BlockTracedShader("3dTex-rt") {

    override fun createFragmentVariables(
        isInstanced: Boolean,
        isAnimated: Boolean,
        motionVectors: Boolean
    ): ArrayList<Variable> {
        val base = super.createFragmentVariables(isInstanced, isAnimated, motionVectors)
        base += Variable(GLSLType.S3D, "blocksTexture")
        return base
    }

    override fun initProperties(instanced: Boolean): String {
        return "vec4 totalColor = vec4(1.0);\n"
    }

    override fun processBlock(instanced: Boolean): String {
        return "" +
                "vec4 blockColor = texture(blocksTexture, blockPosition/bounds1);\n" +
                "float effect = min(blockColor.a, 1.0-totalColor.a);\n" +
                "totalColor += vec4(blockColor.rgb * effect, effect);\n" +
                "continueTracing = (totalColor.a < 0.99);\n" +
                "setNormal = totalColor.a == 0.0;\n"
    }

    override fun onFinish(instanced: Boolean): String {
        return "//if(totalColor.a <= 0.0) discard;\n"
    }

    override fun computeMaterialProperties(instanced: Boolean): String {
        return "" +
                "finalColor = totalColor.rgb / totalColor.a;\n" +
                "finalAlpha = totalColor.a;\n" +
                "finalEmissive = vec3(0.0);\n" +
                "finalMetallic = 0.0;\n" +
                "finalRoughness = 0.5;\n"
    }
}