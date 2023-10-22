package me.anno.graph.render.effects

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.DepthTransforms.bindDepthToPosition
import me.anno.gpu.shader.DepthTransforms.depthToPosition
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer.Companion.copyRenderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.CubemapTexture.Companion.cubemapsAreLeftHanded
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.missingTexture
import me.anno.gpu.texture.TextureLib.whiteCube
import me.anno.graph.render.Texture
import me.anno.graph.render.scene.RenderSceneNode0
import me.anno.maths.Maths.pow
import org.joml.Vector3f

class HeightExpFogNode : RenderSceneNode0(
    "Height+Exp Fog",
    listOf(
        "Float", "Exp Fog Distance", // exp fog distance
        "Float", "Height Fog Strength", // maximum density of height fog
        "Float", "Height Fog Sharpness", // how quickly the fog begins (low = slow)
        "Float", "Height Fog Level", // where the height fog starts
        "Vector3f", "Height Fog Color",
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
        val color0 = getInput(6) as? Texture
        val color = (color0?.tex as? Texture2D)
        val depth = ((getInput(7) as? Texture)?.tex as? Texture2D)
        if (color == null || depth == null) {
            setOutput(1, color0 ?: Texture(missingTexture))
        } else {
            val relativeDistance = getInput(1) as Float
            val fogStrength = getInput(2) as Float
            val fogSharpness = getInput(3) as Float
            val fogOffset = getInput(4) as Float
            val fogColor = getInput(5) as Vector3f
            val result = FBStack[name, color.width, color.height, 3, true, 1, DepthBufferType.NONE]
            useFrame(result, copyRenderer) {
                val shader = shader
                shader.use()
                shader.v1f("fogStrength", fogStrength)
                shader.v1f("fogSharpness", fogSharpness)
                shader.v1f("fogOffset", fogOffset)
                shader.v3f(
                    "fogColor",
                    pow(fogColor.x, 2.2f),
                    pow(fogColor.y, 2.2f),
                    pow(fogColor.z, 2.2f)
                )
                shader.v1f("invExpDistance", 1f / relativeDistance)
                shader.v3f("cameraPosition", RenderState.cameraPosition)
                shader.v1f("worldScale", RenderState.worldScale)
                color.bindTrulyNearest(shader, "colorTex")
                depth.bindTrulyNearest(shader, "depthTex")
                (renderView.pipeline.bakedSkybox?.getTexture0() ?: whiteCube)
                    .bind(shader, "skyTex", GPUFiltering.LINEAR, Clamping.CLAMP)
                bindDepthToPosition(shader)
                flat01.draw(shader)
            }
            setOutput(1, Texture(result.getTexture0()))
        }
    }

    companion object {
        val shader = Shader(
            "height+exp-fog", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1F, "fogStrength"),
                Variable(GLSLType.V1F, "fogSharpness"),
                Variable(GLSLType.V1F, "fogOffset"),
                Variable(GLSLType.V1F, "invExpDistance"),
                Variable(GLSLType.V3F, "cameraPosition"),
                Variable(GLSLType.V1F, "worldScale"),
                Variable(GLSLType.V3F, "fogColor"),
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
                    "   vec3 srcColor = pow(texture(colorTex,uv).xyz,vec3(2.2));\n" +
                    "   vec3 newColor = mix(fogColor, srcColor, heightFogAbsorption);\n" +
                    "   if(isFinite){\n" + // don't apply distance-based fog onto sky
                    "       float integralOverDistance = distance * invExpDistance / worldScale;\n" +
                    "       float expFogAbsorption = exp(-integralOverDistance);\n" +
                    // todo make this srgb/linear-correction optional for weak devices like on Android
                    // for proper mixing, we have to convert them to linear, and then back
                    // todo better filtering... cubic?
                    "       vec3 skyColor = pow(textureLod(skyTex,-$cubemapsAreLeftHanded * dir,10.0).xyz,vec3(2.2));\n" +
                    "       newColor = mix(skyColor, newColor, expFogAbsorption);\n" +
                    "   }\n" +
                    "   newColor = pow(newColor, vec3(1.0/2.2));\n" +
                    "   result = vec4(newColor, 1.0);\n" +
                    "}\n"
        )
    }
}