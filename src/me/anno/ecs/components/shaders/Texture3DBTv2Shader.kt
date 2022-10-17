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
        base += Variable(GLSLType.V1B, "useSDF")
        return base
    }

    override fun initProperties(instanced: Boolean): String {
        return "vec4 totalColor = vec4(0.0);\n" +
                "float skippingFactor = 147.2;\n" // 255/sqrt3
    }

    override fun processBlock(instanced: Boolean): String {
        return "" +
                "vec4 blockColor = texture(blocksTexture, (blockPosition+0.5)/bounds0);\n" +
                "float effect = min(blockColor.a, 1.0-totalColor.a);\n" +
                "totalColor += vec4(blockColor.rgb * effect, effect);\n" +
                "continueTracing = (totalColor.a < 0.99);\n" +
                "setNormal = totalColor.a == 0.0;\n" +
                "if(useSDF && totalColor.a <= 0.0){\n" +
                "   skippingDist = blockColor.x * skippingFactor - 1.0;\n" +
                "}\n"
    }

    override fun onFinish(instanced: Boolean): String {
        return "if(totalColor.a <= 0.0) discard;\n"
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