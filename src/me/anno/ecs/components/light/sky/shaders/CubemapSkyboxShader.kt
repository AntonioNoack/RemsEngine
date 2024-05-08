package me.anno.ecs.components.light.sky.shaders

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable

object CubemapSkyboxShader : SkyShaderBase("cubemap-skybox") {

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        val stages = super.createFragmentStages(key)
        stages.last().addVariables(
            listOf(
                Variable(GLSLType.S2D, "skyTexture"),
                Variable(GLSLType.V1B, "applyInverseToneMapping"),
                Variable(GLSLType.V1F, "maxBrightness"),
                Variable(GLSLType.V2F, "uv")
            )
        )
        return stages
    }

    override fun getSkyColor(): String {
        return "vec3 getSkyColor(vec3 pos) {\n" +
                "   vec3 color = texture(skyTexture,uv).rgb;\n" +
                colorMapping +
                "   return color;\n" +
                "}\n"
    }
}