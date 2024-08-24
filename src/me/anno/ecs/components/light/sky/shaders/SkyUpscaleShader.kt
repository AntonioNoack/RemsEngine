package me.anno.ecs.components.light.sky.shaders

import me.anno.gpu.GFXState
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.bicubicInterpolation
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureLib

object SkyUpscaleShader : SkyShaderBase("SkyboxUpscaler") {

    var source: ITexture2D = TextureLib.whiteTexture

    override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        super.bind(shader, renderer, instanced)
        source.bindTrulyLinear(shader, "skyTex")
        val target = GFXState.currentBuffer
        shader.v2f("invResolution", 1f / target.width, 1f / target.height)
    }

    override fun createFragmentVariables(key: ShaderKey): ArrayList<Variable> {
        val list = super.createFragmentVariables(key)
        list.add(Variable(GLSLType.S2D, "skyTex"))
        list.add(Variable(GLSLType.V2F, "invResolution"))
        return list
    }

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        val stage = ShaderStage(
            "skyBase-upscale", createFragmentVariables(key), concatDefines(key).toString() +
                    // sky no longer properly defined for y > 0
                    "finalNormal = normalize(-normal);\n" +
                    // sky color can be quite expensive to compute, so only do so if we need it
                    "#ifdef COLORS\n" +
                    "   finalColor = vec3(0.0);\n" +
                    "   finalEmissive = getSkyColor(gl_FragCoord.xy * invResolution);\n" +
                    "#endif\n" +
                    "finalPosition = finalNormal * 1e20;\n" +
                    finalMotionCalculation
        )
        stage.add(getSkyColor())
        return listOf(stage)
    }

    override fun getSkyColor(): String {
        return  bicubicInterpolation +
                "vec3 getSkyColor(vec2 uv){\n" +
                "   return bicubicInterpolation(skyTex,uv,vec2(dFdx(uv.x),dFdy(uv.y))).rgb;\n" +
                "}\n"
    }
}