package me.anno.ecs.components.shaders.effects

import me.anno.gpu.GFX.flat01
import me.anno.gpu.RenderState.useFrame
import me.anno.gpu.ShaderLib.simplestVertexShader
import me.anno.gpu.ShaderLib.uvList
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.deferred.DeferredSettingsV2.Companion.singleToVector
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import org.joml.Matrix4f

// https://lettier.github.io/3d-game-shaders-for-beginners/screen-space-reflection.html
// https://github.com/lettier/3d-game-shaders-for-beginners/blob/master/demonstration/shaders/fragment/screen-space-reflection.frag
object ScreenSpaceReflections {

    // position + normal + metallic/reflectivity + maybe-roughness + illuminated image -> illuminated + reflections
    val shader = lazy {
        Shader(
            "ss-reflections", null, simplestVertexShader, uvList, "" +

                    "out vec4 fragColor;\n" +

                    "uniform mat4 transform;\n" +
                    "uniform sampler2D finalColor;\n" +
                    "uniform sampler2D finalIlluminated;\n" +
                    "uniform sampler2D finalPosition;\n" +
                    "uniform sampler2D finalNormal;\n" +
                    "uniform sampler2D finalMetallic;\n" +
                    "uniform vec4 metallicMask;\n" +

                    "uniform float maxDistance;\n" +
                    "uniform float resolution;\n" +
                    "uniform int steps;\n" +
                    "uniform float thickness;\n" +

                    "void main() {\n" +

                    "   vec4 color0 = texture(finalIlluminated, uv);\n" +

                    "   ivec2 texSizeI = textureSize(finalPosition, 0);\n" +
                    "   vec2  texSize  = texSizeI.xy;\n" +

                    "   vec3 positionFrom = texture(finalPosition, uv).xyz;\n" +
                    "   float mask        = dot(texture(finalMetallic, uv), metallicMask);\n" +
                    "   mask = 1;//uv.x > 0.5 ? 1 : 0;\n" + // for debugging

                    "   if (mask <= 0.0) {\n" +
                    "       fragColor = color0;\n" +
                    "       fragColor = fragColor/(1+fragColor);\n" +
                    "       return;\n" +
                    "   }\n" +

                    "   vec3 unitPositionFrom = normalize(positionFrom);\n" +
                    "   vec3 normal           = normalize(texture(finalNormal, uv).xyz*2-1);\n" +
                    "   vec3 pivot            = normalize(reflect(unitPositionFrom, normal));\n" +

                    "   vec4  startView     = vec4(positionFrom, 1.0);\n" +
                    "   float startDistance = length(positionFrom);\n" +
                    "   vec4  endView       = vec4(positionFrom + (pivot * maxDistance), 1.0);\n" +
                    "   float endDistance   = length(endView.xyz);\n" +

                    "   vec4 startFrag     = startView;\n" +
                    "        startFrag     = transform * startFrag;\n" +
                    "        startFrag.xy /= startFrag.w;\n" + // z is never used
                    "        startFrag.xy  = startFrag.xy * 0.5 + 0.5;\n" +
                    "        startFrag.xy *= texSize;\n" +

                    "   vec4 endFrag     = endView;\n" +
                    "        endFrag     = transform * endFrag;\n" +
                    "        endFrag.xy /= endFrag.w;\n" + // z is never used
                    "        endFrag.xy  = endFrag.xy * 0.5 + 0.5;\n" +
                    "        endFrag.xy *= texSize;\n" +

                    "   vec2 frag  = startFrag.xy;\n" +
                    "   vec2 dstUV = frag / texSize;\n" +

                    "   vec2  deltaXY   = endFrag.xy - startFrag.xy;\n" +
                    "   bool  useX      = abs(deltaXY.x) >= abs(deltaXY.y);\n" +
                    "   float delta     = (useX ? abs(deltaXY.x) : abs(deltaXY.y)) * resolution;\n" +
                    "   vec2  increment = deltaXY / max(delta, 0.001);\n" +

                    "   float search0 = 0;\n" +
                    "   float search1 = 0;\n" +

                    "   int hit0 = 0;\n" +
                    "   int hit1 = 0;\n" +

                    "   float depth = thickness, viewDistance;\n" +
                    "   vec3 positionTo;\n" +

                    "   int maxLinearSteps = min(int(delta), min(texSizeI.x, texSizeI.y));\n" +
                    "   for (int i = 0; i < maxLinearSteps; i++) {\n" +
                    "       frag      += increment;\n" +
                    "       dstUV      = frag / texSize;\n" +
                    "       positionTo = texture(finalPosition, dstUV).xyz;\n" +

                    "       search1 = useX ? (frag.x - startFrag.x) / deltaXY.x : (frag.y - startFrag.y) / deltaXY.y;\n" +
                    "       search1 = clamp(search1, 0.0, 1.0);\n" +

                    "       viewDistance = (startDistance * endDistance) / mix(endDistance, startDistance, search1);\n" +
                    "       depth        = viewDistance - length(positionTo);\n" +

                    "       if (depth > 0 && depth < thickness) {\n" +
                    "           hit0 = 1;\n" +
                    "           break;\n" +
                    "       } else {\n" +
                    "           search0 = search1;\n" +
                    "       }\n" +
                    "   }\n" +

                    "   if(hit0 == 0){\n" +
                    "       fragColor = color0;\n" +
                    "       fragColor = fragColor/(1+fragColor);\n" +
                    // debug color
                    // "       fragColor = vec4(1,0,1,1);\n" +
                    "       return;\n" +
                    "   }\n" +

                    "   search1 = (search0 + search1) * 0.5;\n" +

                    "   for (int i = 0; i < steps; i++) {\n" +
                    "       frag       = mix(startFrag.xy, endFrag.xy, search1);\n" +
                    "       dstUV      = frag / texSize;\n" +
                    "       positionTo = texture(finalPosition, dstUV).xyz;\n" +

                    "       viewDistance = (startDistance * endDistance) / mix(endDistance, startDistance, search1);\n" +
                    "       depth        = viewDistance - length(positionTo);\n" +

                    // todo why is this soo noisy for regions under the thing?
                    "       if (depth > 0 && depth < thickness) {\n" +
                    "           hit1 = 1;\n" +
                    "           search1 = (search0 + search1) * 0.5;\n" +
                    "       } else {\n" +
                    "           float temp = search1;\n" +
                    "           search1 += (search1 - search0) * 0.5;\n" + // why?? mmh...
                    "           search0 = temp;\n" +
                    "       }\n" +
                    "   }\n" +
                    "" +
                    "   if(hit1 == 0){\n" +
                    "       fragColor = color0;\n" +
                    "       fragColor = fragColor/(1+fragColor);\n" +
                    // debug color
                    // "       fragColor = vec4(0,1,1,1);\n" +
                    "       return;\n" +
                    "   }\n" +

                    "   float visibility = hit1 * mask\n" +
                    "       * (1 + min(dot(unitPositionFrom, pivot), 0))\n" + // actually [0,1], I think
                    "       * (1 - clamp(depth / thickness, 0, 1))\n" +
                    "       * (1 - clamp(length(positionTo - positionFrom) / maxDistance, 0, 1))\n" +
                    "       * (dstUV.x < 0 || dstUV.x > 1 ? 0 : 1)\n" +
                    "       * (dstUV.y < 0 || dstUV.y > 1 ? 0 : 1);\n" +

                    // reflected position * base color of mirror (for golden reflections)
                    "   vec4 color1 = texture(finalIlluminated, dstUV) * texture(finalColor, uv);\n" +
                    "   fragColor = mix(color0, color1, visibility);\n" +
                    "   fragColor = fragColor/(1+fragColor);\n" +
                    // "   fragColor = vec4(dstUV, visibility, 1);\n" +
                    "}"
        ).apply {
            setTextureIndices(listOf("finalColor", "finalPosition", "finalNormal", "finalMetallic", "finalIlluminated"))
        }
    }

    fun compute(
        buffer: Framebuffer,
        illuminated: Framebuffer,
        settingsV2: DeferredSettingsV2,
        transform: Matrix4f,
        dst: Framebuffer = FBStack["ss-reflections", buffer.w, buffer.h, 4, true, 1, false]
    ): Texture2D {
        useFrame(dst, Renderer.copyRenderer) {
            val shader = shader.value
            shader.use()
            shader.v1("maxDistance", 8f)
            shader.v1("resolution", 0.3f) // [0,1]
            shader.v1("steps", 10)
            shader.v1("thickness", 0.2f) // thickness, when we are under something
            shader.m4x4("transform", transform)
            // metallic may be on r, g, b, or a
            val metallicLayer = settingsV2.findLayer(DeferredLayerType.METALLIC)!!
            val metallicName = metallicLayer.mapping
            illuminated.bindTexture0(4, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
            shader.v4("metallicMask", singleToVector[metallicName]!!)
            settingsV2.findTexture(buffer, DeferredLayerType.METALLIC)!!
                .bind(3, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
            settingsV2.findTexture(buffer, DeferredLayerType.NORMAL)!!
                .bind(2, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
            settingsV2.findTexture(buffer, DeferredLayerType.POSITION)!!
                .bind(1, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
            settingsV2.findTexture(buffer, DeferredLayerType.COLOR)!!
                .bind(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
            flat01.draw(shader)
        }
        return dst.getColor0()
    }

}