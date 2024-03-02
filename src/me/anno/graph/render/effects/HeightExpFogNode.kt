package me.anno.graph.render.effects

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.DepthTransforms.bindDepthUniforms
import me.anno.gpu.shader.DepthTransforms.depthToPosition
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.CubemapTexture.Companion.cubemapsAreLeftHanded
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.missingTexture
import me.anno.gpu.texture.TextureLib.whiteCube
import me.anno.graph.render.Texture
import me.anno.graph.render.scene.RenderViewNode
import me.anno.graph.types.flow.FlowGraphNodeUtils.getBoolInput
import me.anno.graph.types.flow.FlowGraphNodeUtils.getFloatInput
import me.anno.maths.Maths.max
import me.anno.maths.Maths.pow
import org.joml.Vector3f

class HeightExpFogNode : RenderViewNode(
    "Height+Exp Fog",
    listOf(
        "Float", "Exp Fog Distance", // exp fog distance
        "Float", "Height Fog Strength", // maximum density of height fog
        "Float", "Height Fog Sharpness", // how quickly the fog begins (low = slow)
        "Float", "Height Fog Level", // where the height fog starts
        "Vector3f", "Height Fog Color",
        "Boolean", "Cheap Mixing",
        "Texture", "Illuminated",
        "Texture", "Depth",
    ), listOf("Texture", "Illuminated")
) {

    init {
        // exponential fog
        setInput(1, 1000f) // relative distance
        // height fog
        setInput(2, 0.3f) // strength
        setInput(3, 1f) // sharpness
        setInput(4, 0f) // offset
        setInput(5, Vector3f(0.375f, 0.491f, 0.697f))
    }

    override fun executeAction() {
        val color0 = getInput(7) as? Texture
        val color = (color0?.tex as? Texture2D)
        val depth = ((getInput(8) as? Texture)?.tex as? Texture2D)
        val relativeDistance = max(getFloatInput(1), 0f)
        val fogStrength = max(getFloatInput(2), 0f)
        if (color == null || depth == null || (relativeDistance.isFinite() && fogStrength == 0f)) {
            setOutput(1, color0 ?: Texture(missingTexture))
        } else {
            val fogSharpness = max(getFloatInput(3), 0f)
            val fogOffset = getFloatInput(4)
            val fogColor = getInput(5) as Vector3f
            val cheapMixing = getBoolInput(6)
            val result = FBStack[name, color.width, color.height, 3, true, 1, DepthBufferType.NONE]
            useFrame(result, copyRenderer) {
                val shader = shader
                shader.use()
                shader.v1f("fogStrength", fogStrength)
                shader.v1f("fogSharpness", fogSharpness)
                shader.v1f("fogOffset", fogOffset)
                val fx = max(fogColor.x, 0f)
                val fy = max(fogColor.y, 0f)
                val fz = max(fogColor.z, 0f)
                shader.v3f(
                    "fogColor",
                    if (cheapMixing) fx else pow(fx, 2.2f),
                    if (cheapMixing) fy else pow(fy, 2.2f),
                    if (cheapMixing) fz else pow(fz, 2.2f),
                )
                shader.v1f("invExpDistance", 1f / relativeDistance)
                shader.v3f("cameraPosition", RenderState.cameraPosition)
                shader.v1f("worldScale", RenderState.worldScale)
                shader.v1b("cheapMixing", cheapMixing)
                color.bindTrulyNearest(shader, "colorTex")
                depth.bindTrulyNearest(shader, "depthTex")
                (renderView.pipeline.bakedSkybox?.getTexture0() ?: whiteCube)
                    .bind(shader, "skyTex", Filtering.LINEAR, Clamping.CLAMP)
                bindDepthUniforms(shader)
                flat01.draw(shader)
            }
            setOutput(1, Texture(result.getTexture0()))
        }
    }

    companion object {
        val shader = Shader(
            "height+exp-fog", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1F, "fogStrength"),
                Variable(GLSLType.V1F, "fogSharpness"),
                Variable(GLSLType.V1F, "fogOffset"),
                Variable(GLSLType.V1F, "invExpDistance"),
                Variable(GLSLType.V3F, "cameraPosition"),
                Variable(GLSLType.V1F, "worldScale"),
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
                    "   float startY = start.y / worldScale + cameraPosition.y, endY = end.y / worldScale + cameraPosition.y;\n" +
                    "   float integralOverHeight = abs((heightFogIntegral(endY) - heightFogIntegral(startY)) / normalize(dir).y);\n" +
                    "   float heightFogAbsorption = exp(-integralOverHeight);\n" +
                    "   vec3 srcColor = texture(colorTex,uv).xyz;\n" +
                    "   vec3 newColor = cheapMixing ?\n" +
                    "       mix(fogColor, srcColor, heightFogAbsorption) :\n" +
                    "       mix(fogColor, pow(srcColor,vec3(2.2)), heightFogAbsorption);\n" +
                    "   if(isFinite){\n" + // don't apply distance-based fog onto sky
                    "       float integralOverDistance = distance * invExpDistance / worldScale;\n" +
                    "       float expFogAbsorption = exp(-integralOverDistance);\n" +
                    // todo better filtering... cubic?
                    "       vec3 skyColor = textureLod(skyTex,-$cubemapsAreLeftHanded * dir,10.0).xyz;\n" +
                    "       newColor = cheapMixing ?\n" +
                    "           mix(skyColor, newColor, expFogAbsorption) :\n" +
                    "           mix(pow(skyColor,vec3(2.2)), newColor, expFogAbsorption);\n" +
                    "   }\n" +
                    "   newColor = cheapMixing ?\n" +
                    "       newColor :\n" +
                    "       pow(newColor, vec3(1.0/2.2));\n" +
                    "   result = vec4(newColor, 1.0);\n" +
                    "}\n"
        )
    }
}