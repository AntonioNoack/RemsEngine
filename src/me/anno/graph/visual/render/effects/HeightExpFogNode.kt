package me.anno.graph.visual.render.effects

import me.anno.ecs.systems.GlobalSettings
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.DepthTransforms.bindDepthUniforms
import me.anno.gpu.shader.DepthTransforms.depthToPosition
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.gamma
import me.anno.gpu.shader.ShaderLib.gammaInv
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.CubemapTexture.Companion.cubemapsAreLeftHanded
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureLib.whiteCube
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.graph.visual.render.effects.TimedRenderingNode.Companion.finish
import me.anno.graph.visual.render.scene.RenderViewNode
import me.anno.maths.Maths.pow
import me.anno.maths.MinMax.max
import me.anno.utils.GFXFeatures

/**
 * Adds height fog and exponential distance fog to your scene.
 *
 * Add a HeightExpFogSettings instance to your scene to configure this.
 * */
class HeightExpFogNode : RenderViewNode(
    "Height+Exp Fog",
    listOf(
        "Boolean", "Cheap Mixing",
        "Texture", "Illuminated",
        "Texture", "Depth",
    ), listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, GFXFeatures.hasWeakGPU) // cheap mixing
    }

    override fun executeAction() {
        val color0 = getInput(2) as? Texture
        val color = color0?.texOrNull ?: return finish()
        val depth = getTextureInput(3) ?: return finish(color0)

        val settings = GlobalSettings[HeightExpFogSettings::class]

        val relativeDistance = max(settings.expFogDistance, 0f)
        val fogStrength = settings.heightFogStrength
        if (!relativeDistance.isFinite() && fogStrength <= 0f) {
            return finish(color0)
        }

        timeRendering(name, timer) {
            renderFog(color, depth, settings)
        }
    }

    private fun renderFog(color: ITexture2D, depth: ITexture2D, settings: HeightExpFogSettings) {
        val cheapMixing = getBoolInput(1)
        val result = FBStack[name, color.width, color.height, 3, true, 1, DepthBufferType.NONE]
        useFrame(result, copyRenderer) {
            val shader = shader
            shader.use()
            shader.v1f("fogStrength", settings.heightFogStrength)
            shader.v1f("fogSharpness", settings.heightFogSharpness)
            shader.v1f("fogOffset", settings.heightFogLevel)
            val fogColor = settings.heightFogColor
            val fx = max(fogColor.x, 0f)
            val fy = max(fogColor.y, 0f)
            val fz = max(fogColor.z, 0f)
            val gamma = gamma.toFloat()
            shader.v3f(
                "fogColor",
                if (cheapMixing) fx else pow(fx, gamma),
                if (cheapMixing) fy else pow(fy, gamma),
                if (cheapMixing) fz else pow(fz, gamma),
            )
            shader.v1f("invExpDistance", 1f / settings.expFogDistance)
            shader.v3f("cameraPosition", RenderState.cameraPosition)
            shader.v1b("cheapMixing", cheapMixing)
            color.bindTrulyNearest(shader, "colorTex")
            depth.bindTrulyNearest(shader, "depthTex")
            (renderView.pipeline.bakedSkybox?.getTexture0() ?: whiteCube)
                .bind(shader, "skyTex", Filtering.LINEAR, Clamping.CLAMP)
            bindDepthUniforms(shader)
            flat01.draw(shader)
        }
        finish(result.getTexture0())
    }

    companion object {
        val shader = Shader(
            "height+exp-fog", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1F, "fogStrength"),
                Variable(GLSLType.V1F, "fogSharpness"),
                Variable(GLSLType.V1F, "fogOffset"),
                Variable(GLSLType.V1F, "invExpDistance"),
                Variable(GLSLType.V3F, "cameraPosition"),
                Variable(GLSLType.V3F, "fogColor"),
                Variable(GLSLType.V1B, "cheapMixing"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.SCube, "skyTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ) + depthVars, quatRot + rawToDepth + depthToPosition +
                    "float heightFog(float y) {\n" +
                    "   y = (fogOffset - y) * fogSharpness;\n" +
                    "   return fogStrength/(exp(-y)+1.0);\n" +
                    "}\n" +
                    "float heightFogIntegral(float y) {\n" +
                    "   y = (fogOffset - y) * fogSharpness;\n" +
                    "   float scale = fogStrength / fogSharpness;\n" +
                    "   return scale * (y < -10.0 ? 0.0 : y > 10.0 ? y : log(exp(y)+1.0));\n" +
                    "}\n" +
                    "void main(){\n" +
                    // find all camera parameters like in SSAO, just in world space
                    "   vec3 start = getLocalCameraPosition(uv);\n" +
                    "   vec3 dir = rawCameraDirection(uv);\n" +
                    "   float distance = rawToDepth(texture(depthTex,uv).x);\n" +
                    "   vec3 end = start + dir * distance;\n" +
                    "   bool isFinite = distance < 1e38;\n" +
                    "   float startY = start.y + cameraPosition.y, endY = end.y + cameraPosition.y;\n" +
                    "   float integralOverHeight = abs((heightFogIntegral(endY) - heightFogIntegral(startY)) / normalize(dir).y);\n" +
                    "   float heightFogAbsorption = exp(-integralOverHeight);\n" +
                    "   vec3 srcColor = texture(colorTex,uv).xyz;\n" +
                    "   vec3 newColor = cheapMixing ?\n" +
                    "       mix(fogColor, srcColor, heightFogAbsorption) :\n" +
                    "       mix(fogColor, pow(srcColor,vec3($gamma)), heightFogAbsorption);\n" +
                    "   if(isFinite){\n" + // don't apply distance-based fog onto sky
                    "       float integralOverDistance = distance * invExpDistance;\n" +
                    "       float expFogAbsorption = exp(-integralOverDistance);\n" +
                    // todo better filtering... cubic?
                    "       vec3 skyColor = textureLod(skyTex,-$cubemapsAreLeftHanded * dir,10.0).xyz;\n" +
                    "       newColor = cheapMixing ?\n" +
                    "           mix(skyColor, newColor, expFogAbsorption) :\n" +
                    "           mix(pow(skyColor,vec3($gamma)), newColor, expFogAbsorption);\n" +
                    "   }\n" +
                    "   newColor = cheapMixing ?\n" +
                    "       newColor :\n" +
                    "       pow(newColor, vec3($gammaInv));\n" +
                    "   result = vec4(newColor, 1.0);\n" +
                    "}\n"
        )
    }
}