package me.anno.ecs.components.shaders

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable

class CubemapSkyboxShader(name: String) : SkyShaderBase(name) {

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        val stages = super.createFragmentStages(key)
        stages.last().variables += listOf(
            Variable(GLSLType.S2D, "skyTexture"),
            Variable(GLSLType.V1B, "applyInverseToneMapping"),
            Variable(GLSLType.V2F, "uv")
        )
        return stages
    }

    override fun getSkyColor(): String {
        return "vec3 getSkyColor(vec3 pos) {\n" +
                "   vec3 color = texture(skyTexture,uv).rgb;\n" +
                "   if(applyInverseToneMapping) color = 1.0/(1.03-min(color,1.0))-1.0;\n" +
                "   return color * skyColor;\n" +
                "}\n"
    }
}