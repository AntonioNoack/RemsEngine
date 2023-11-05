package me.anno.ecs.components.shaders

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.Variable

/**
 * texture 3d - block traced shader; with many-color and transparency support
 * @see Texture3DBTv2Material
 * */
@Suppress("unused")
object Texture3DBTv2Shader : BlockTracedShader("3dTex-rt") {

    override fun createFragmentVariables(key: ShaderKey): ArrayList<Variable> {
        val base = super.createFragmentVariables(key)
        base += Variable(GLSLType.S3D, "blocksTexture")
        base += Variable(GLSLType.V1B, "useSDF")
        return base
    }

    override fun initProperties(instanced: Boolean): String {
        return "vec4 totalColor = vec4(0.0);\n" +
                "finalTranslucency = 0.0;\n" +
                "float skippingFactor = 147.2;\n" // 255/sqrt3
    }

    override fun processBlock(instanced: Boolean): String {
        // todo transparent things look weird, if you're inside of them
        // todo transparency is implemented here awkwardly: multiply color again and again to achieve tinting :)
        return "" +
                "vec4 blockColor = texture(blocksTexture, (blockPosition+0.5)/bounds0);\n" +
                "float maxEffect = 1.0-totalColor.a;\n" +
                "float effect = blockColor.a <= 0.0 ? 0.0 : blockColor.a >= 1.0 ? maxEffect :\n" +
                "   (1.0 - exp(-abs(nextDist-dist)*10.0*blockColor.a*blockColor.a)) * maxEffect;\n" + // beer's law
                // this line has no effect :/, why?
                // "if(totalColor.a == 0.0) finalTranslucency = blockColor.a < 1.0 ? 1.0 : 0.0;\n" +
                "totalColor += vec4(blockColor.rgb * blockColor.rgb * effect, effect);\n" +
                "continueTracing = (totalColor.a < 0.997);\n" +
                "setNormal = totalColor.a == 0.0;\n" +
                "if(useSDF && setNormal){\n" +
                "   skippingDist = blockColor.x * skippingFactor - 1.0;\n" +
                "}\n"
    }

    override fun onFinish(instanced: Boolean): String {
        return "if(totalColor.a <= 0.0) discard;\n"
    }

    override fun computeMaterialProperties(instanced: Boolean): String {
        return "" +
                "finalColor = sqrt(totalColor.rgb / totalColor.a);\n" +
                "finalAlpha = totalColor.a;\n" +
                "finalEmissive = vec3(0.0);\n" +
                "finalMetallic = metallicMinMax.x;\n" +
                "finalRoughness = roughnessMinMax.x;\n"
    }
}