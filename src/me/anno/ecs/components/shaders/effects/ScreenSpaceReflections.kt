package me.anno.ecs.components.shaders.effects

import me.anno.engine.ui.render.Renderers.tonemapGLSL
import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.deferred.DeferredSettingsV2.Companion.singleToVector
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib.noiseFunc
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import org.joml.Matrix4f

// https://lettier.github.io/3d-game-shaders-for-beginners/screen-space-reflection.html
// https://github.com/lettier/3d-game-shaders-for-beginners/blob/master/demonstration/shaders/fragment/screen-space-reflection.frag
object ScreenSpaceReflections {

    private const val testMaxDistanceRatio = 100

    // position + normal + metallic/reflectivity + maybe-roughness + illuminated image -> illuminated + reflections
    val shader = lazy {
        Shader(
            "ss-reflections", coordsList, coordsVShader, uvList, emptyList(), "" +

                    "out vec4 fragColor;\n" +

                    "uniform mat4 transform;\n" +
                    "uniform sampler2D finalColor;\n" +
                    "uniform sampler2D finalIlluminated;\n" +
                    "uniform sampler2D finalPosition;\n" +
                    "uniform sampler2D finalNormal;\n" +
                    // reflectivity = metallic * (1-roughness)
                    "uniform sampler2D finalMetallic;\n" +
                    "uniform sampler2D finalRoughness;\n" +
                    "uniform vec4 metallicMask, roughnessMask;\n" +

                    "uniform float testDistance;\n" +
                    "uniform float maxDistanceSq;\n" +
                    "uniform float resolution;\n" +
                    "uniform int steps;\n" +
                    "uniform float thickness;\n" +
                    "uniform float maskSharpness;\n" +
                    "uniform float strength;\n" +

                    "uniform bool applyToneMapping;\n" +

                    noiseFunc +
                    tonemapGLSL +

                    "void main() {\n" +

                    "   vec3 color0 = texture(finalIlluminated, uv).rgb;\n" +

                    "   float metallic = dot(texture(finalMetallic, uv), metallicMask);\n" +
                    "   float roughness = dot(texture(finalRoughness, uv), roughnessMask);\n" +
                    "   float reflectivity = metallic * (1.0 - roughness);\n" +
                    "   reflectivity = (reflectivity - 1.0) * maskSharpness + 1.0;\n" +
                    // "   reflectivity = 1;//uv.x > 0.5 ? 1 : 0;\n" + // for debugging
                    // skip, if not reflective
                    "   if (reflectivity <= 0.0) {\n" +
                    "       fragColor = vec4(applyToneMapping ? tonemap(color0) : color0, 1.0);\n" +
                    // "       fragColor = vec4(1,0,0,1);\n" +
                    "       return;\n" +
                    "   }\n" +

                    "   ivec2 texSizeI = textureSize(finalPosition, 0);\n" +
                    "   vec2  texSize  = vec2(texSizeI);\n" +

                    "   vec3 positionFrom     = texture(finalPosition, uv).xyz;\n" +
                    "   vec3 normal           = normalize(texture(finalNormal, uv).xyz * 2.0 - 1.0);\n" +
                    "   vec3 pivot            = normalize(reflect(positionFrom, normal));\n" +

                    "   vec4  startView     = vec4(positionFrom, 1.0);\n" +
                    "   float startDistance = length(positionFrom);\n" +

                    "   vec3  endView       = positionFrom + pivot * testDistance;\n" +
                    "   float endDistance   = length(endView);\n" +

                    "   vec4 endUV0    = transform * vec4(endView,1);\n" +
                    "   vec2 endUV     = endUV0.xy / endUV0.w * 0.5 + 0.5;\n" +

                    "   vec2 dstUV = uv;\n" +

                    "   vec2  deltaXY   = endUV - uv;\n" +
                    "   vec2  absDelta  = abs(deltaXY * texSize);\n" +
                    "   bool  useX      = absDelta.x >= absDelta.y;\n" +
                    "   float delta     = (useX ? absDelta.x : absDelta.y) * resolution;\n" + // number of pixels / resolution
                    "   vec2  increment = deltaXY / delta;\n" +

                    "   float fraction0 = 0.0;\n" +
                    "   float fraction1 = 0.0;\n" +

                    "   int hit0 = 0;\n" +

                    "   float depth = thickness, viewDistance;\n" +
                    "   vec3 positionTo = vec3(0.0);\n" +

                    // calculate the number of pixels to the edge of the screen
                    "   int maxLinearSteps = int(min(delta * $testMaxDistanceRatio.0, useX ? " +
                    "       (deltaXY.x < 0.0 ? uv.x : 1.0 - uv.x) * resolution * texSize.x : " +
                    "       (deltaXY.y < 0.0 ? uv.y : 1.0 - uv.y) * resolution * texSize.y" +
                    "   ));\n" +
                    "   for (int i = 0; i <= maxLinearSteps; i++){\n" +

                    "       dstUV     += increment;\n" +
                    "       positionTo = texture(finalPosition, dstUV).xyz;\n" +

                    "       fraction1 = useX ? (dstUV.x - uv.x) / deltaXY.x : (dstUV.y - uv.y) / deltaXY.y;\n" +

                    "       viewDistance = (startDistance * endDistance) / mix(endDistance, startDistance, fraction1);\n" +
                    "       depth        = viewDistance - length(positionTo);\n" +

                    "       if (depth > 0.0 && depth < thickness) {\n" +
                    // we found something between fraction0 and fraction1
                    "           hit0 = 1;\n" +
                    "           break;\n" +
                    "       } else {\n" + // last fraction0
                    "           fraction0 = fraction1;\n" +
                    "       }\n" +
                    "   }\n" +

                    "   if(hit0 == 0){\n" +
                    "       fragColor = vec4(applyToneMapping ? tonemap(color0) : color0, 1.0);\n" +
                    // debug color
                    // "       fragColor = vec4(1,0,1,1);\n" +
                    "       return;\n" +
                    "   }\n" +

                    // "   fraction1 = (fraction0 + fraction1) * 0.5;\n" +

                    "   vec2  bestUV = dstUV;\n" +
                    "   vec3  bestPositionTo = positionTo;\n" +
                    "   float bestDepth = depth;\n" +
                    "   for (int i = 0; i < steps; i++){\n" +

                    "       float fractionI = mix(fraction0, fraction1, float(i)/float(steps));\n" +

                    "       dstUV      = mix(uv, endUV, fractionI);\n" +
                    "       positionTo = texture(finalPosition, dstUV).xyz;\n" +

                    "       viewDistance = (startDistance * endDistance) / mix(endDistance, startDistance, fractionI);\n" +
                    "       depth        = viewDistance - length(positionTo);\n" +

                    "       if (depth > 0.0 && depth < bestDepth) {\n" +
                    "           bestDepth = depth;\n" +
                    "           bestUV = dstUV;\n" +
                    "           bestPositionTo = positionTo;\n" +
                    "           break;\n" +
                    "       }\n" +
                    "   }\n" +

                    "   vec3 distanceDelta = bestPositionTo - positionFrom;\n" +
                    "   float distanceSq = dot(distanceDelta, distanceDelta);\n" +
                    "   if(distanceSq >= maxDistanceSq || bestUV.x < 0.0 || bestUV.x > 1.0 || bestUV.y < 0.0 || bestUV.y > 1.0){\n" +
                    "       fragColor = vec4(applyToneMapping ? tonemap(color0) : color0, 1.0);\n" +
                    // debug color
                    // "       fragColor = vec4(0,0,0,1);\n" +
                    "       return;\n" +
                    "   }\n" +

                    "   float visibility = reflectivity\n" +
                    "       * (1.0 + min(dot(normalize(positionFrom), pivot), 0.0))\n" + // [0,1]
                    "       * (1.0 - min(bestDepth / thickness, 1.0))\n" +
                    "       * (1.0 - sqrt(distanceSq / maxDistanceSq))\n" +
                    "       * min(10.0 * (0.5 - abs(bestUV.x - 0.5)), 1.0)\n" +
                    "       * min(10.0 * (0.5 - abs(bestUV.y - 0.5)), 1.0);\n" +

                    // reflected position * base color of mirror (for golden reflections)
                    "   vec3 color1 = texture(finalIlluminated, bestUV).rgb * texture(finalColor, uv).rgb;\n" +
                    "   color0 = mix(color0, color1, min(visibility * strength, 1.0));\n" +
                    "   fragColor = vec4(applyToneMapping ? tonemap(color0) : color0, 1.0);\n" +
                    // "   fragColor = vec4(0,0,visibility,1);\n" +
                    // "   fragColor = vec4(bestUV, visibility, 1);\n" +
                    "}"
        ).apply {
            setTextureIndices(
                listOf(
                    "finalColor", "finalPosition", "finalNormal",
                    "finalMetallic", "finalRoughness", "finalIlluminated"
                )
            )
        }
    }

    fun compute(
        buffer: IFramebuffer,
        illuminated: ITexture2D,
        deferred: DeferredSettingsV2,
        transform: Matrix4f,
        applyToneMapping: Boolean,
        dst: Framebuffer = FBStack["ss-reflections", buffer.w, buffer.h, 4, true, 1, false]
    ): ITexture2D? {
        return compute(
            buffer, illuminated, deferred, transform,
            1f, 2f, 0.2f, 10, 8f,
            applyToneMapping, dst
        )
    }

    /**
     * computes screen space reflections from metallic, roughness, normal, position and color buffers
     * */
    fun compute(
        buffer: IFramebuffer,
        illuminated: ITexture2D,
        deferred: DeferredSettingsV2,
        transform: Matrix4f,
        strength: Float = 1f,
        maskSharpness: Float = 2f,
        wallThickness: Float = 0.2f,
        fineSteps: Int = 10, // 10 are enough, if there are only rough surfaces
        maxDistance: Float = 8f,
        applyToneMapping: Boolean,
        dst: Framebuffer = FBStack["ss-reflections", buffer.w, buffer.h, 4, true, 1, false]
    ): ITexture2D? {
        // metallic may be on r, g, b, or a
        val metallicLayer = deferred.findLayer(DeferredLayerType.METALLIC) ?: return null
        val metallicMask = metallicLayer.mapping
        val roughnessLayer = deferred.findLayer(DeferredLayerType.ROUGHNESS) ?: return null
        val roughnessMask = roughnessLayer.mapping
        val normalTexture = deferred.findTexture(buffer, DeferredLayerType.NORMAL) ?: return null
        val positionTexture = deferred.findTexture(buffer, DeferredLayerType.POSITION) ?: return null
        val colorTexture = deferred.findTexture(buffer, DeferredLayerType.COLOR) ?: return null
        return compute(
            positionTexture,
            normalTexture,
            colorTexture,
            deferred.findTexture(buffer, metallicLayer),
            metallicMask,
            deferred.findTexture(buffer, roughnessLayer),
            roughnessMask,
            illuminated,
            transform,
            strength, maskSharpness, wallThickness, fineSteps, maxDistance,
            applyToneMapping, dst
        ).getTexture0()
    }

    /**
     * computes screen space reflections from metallic, roughness, normal, position and color buffers
     * */
    fun compute(
        position: ITexture2D,
        normal: ITexture2D,
        color: ITexture2D,
        metallic: ITexture2D,
        metallicMask: String,
        roughness: ITexture2D,
        roughnessMask: String,
        illuminated: ITexture2D,
        transform: Matrix4f,
        strength: Float = 1f,
        maskSharpness: Float = 2f,
        wallThickness: Float = 0.2f,
        fineSteps: Int = 10, // 10 are enough, if there are only rough surfaces
        maxDistance: Float = 8f,
        applyToneMapping: Boolean,
        dst: IFramebuffer = FBStack["ss-reflections", position.w, position.h, 4, true, 1, false]
    ): IFramebuffer {
        // metallic may be on r, g, b, or a
        useFrame(dst, Renderer.copyRenderer) {
            val shader = shader.value
            shader.use()
            shader.v1b("applyToneMapping", applyToneMapping)
            shader.v1f("testDistance", maxDistance / testMaxDistanceRatio)
            shader.v1f("maxDistanceSq", maxDistance * maxDistance)
            shader.v1f("resolution", 1f / fineSteps)
            shader.v1i("steps", fineSteps)
            shader.v1f("maskSharpness", maskSharpness)
            shader.v1f("thickness", wallThickness) // thickness, when we are under something
            shader.v1f("strength", strength)
            shader.m4x4("transform", transform)
            val n = GPUFiltering.TRULY_LINEAR
            val c = Clamping.CLAMP
            shader.v4f("metallicMask", singleToVector[metallicMask]!!)
            shader.v4f("roughnessMask", singleToVector[roughnessMask]!!)
            illuminated.bind(5, n, c)
            roughness.bind(4, n, c)
            metallic.bind(3, n, c)
            normal.bind(2, n, c)
            position.bind(1, n, c)
            color.bind(0, n, c)
            flat01.draw(shader)
        }
        return dst
    }

}