package me.anno.ecs.components.shaders

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable

object UVSkyboxShader : SkyShaderBase("uv-skybox") {

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        val stages = super.createFragmentStages(key)
        stages.last().variables += listOf(
            Variable(GLSLType.S2D, "skyTexture"),
            Variable(GLSLType.V1B, "applyInverseToneMapping")
        )
        return stages
    }

    override fun getSkyColor(): String {
        return "vec3 getSkyColor(vec3 pos) {\n" +
                "   float u = atan(pos.z,pos.x)*${0.5 / Math.PI}+0.5;\n" +
                "   float v = atan(pos.y,length(pos.xz))*${1.0 / Math.PI}+.5;\n" +
                // this fixes the seam; it is caused by sampling the 2x2 field with (0,v) and (1,v) as neighbors
                // -> anisotropic filtering wants to fix that, and the lowest-res LOD would be sampled
                // -> we sample LOD 0 for these pixels instead. Subsampling patterns shouldn't appear,
                // because it's only a line (of natural colors, high-frequency checker would still look wrong)
                "   vec2 du = vec2(dFdx(u),dFdy(u));\n" +
                "   vec3 color = dot(du,du) > 0.1 ? textureLod(skyTexture,vec2(0.0,v),0.0).rgb : texture(skyTexture,vec2(u,v)).rgb;\n" +
                "   if(applyInverseToneMapping) color = 1.0/(1.03-min(color,1.0))-1.0;\n" +
                "   return color * skyColor;\n" +
                "}\n"
    }
}