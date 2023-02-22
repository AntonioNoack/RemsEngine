package me.anno.ecs.components.shaders.effects

import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.quatRot
import me.anno.ecs.components.shaders.SkyBox
import me.anno.engine.ui.render.Renderers.tonemapGLSL
import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.deferred.DeferredSettingsV2.Companion.singleToVector
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.ReverseDepth.bindDepthToPosition
import me.anno.gpu.shader.ReverseDepth.depthToPosition
import me.anno.gpu.shader.ReverseDepth.depthToPositionList
import me.anno.gpu.shader.ReverseDepth.rawToDepth
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib.noiseFunc
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import org.joml.Matrix4f
import org.joml.Vector4f

// https://lettier.github.io/3d-game-shaders-for-beginners/screen-space-reflection.html
// https://github.com/lettier/3d-game-shaders-for-beginners/blob/master/demonstration/shaders/fragment/screen-space-reflection.frag
object ScreenSpaceReflections {

    private const val testMaxDistanceRatio = 100

    // position + normal + metallic/reflectivity + maybe-roughness + illuminated image -> illuminated + reflections
    fun createShader(sky: SkyBox?): Shader {
        val defaultSkyColor = "vec4 getSkyColor1(vec3 pos){ return skyColor; }\n"
        val variables = arrayListOf(
            Variable(GLSLType.V4F, "fragColor", VariableMode.OUT),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.S2D, "finalColor"),
            Variable(GLSLType.S2D, "finalIlluminated"),
            Variable(GLSLType.S2D, "finalDepth"),
            Variable(GLSLType.S2D, "finalNormal"),
            // reflectivity = metallic * (1-roughness),
            Variable(GLSLType.S2D, "finalMetallic"),
            Variable(GLSLType.S2D, "finalRoughness"),
            Variable(GLSLType.V4F, "metallicMask"),
            Variable(GLSLType.V4F, "roughnessMask"),
            Variable(GLSLType.V1F, "testDistance"),
            Variable(GLSLType.V1F, "maxDistanceSq"),
            Variable(GLSLType.V1F, "resolution"),
            Variable(GLSLType.V1I, "steps"),
            Variable(GLSLType.V1F, "thickness"),
            Variable(GLSLType.V1F, "maskSharpness"),
            Variable(GLSLType.V1F, "strength"),
            Variable(GLSLType.V1B, "applyToneMapping"),
            Variable(GLSLType.V4F, "skyColor"),
        )
        val functions = HashSet<String>()
        if (sky != null) {
            val material = sky.material
            val shader = material.shader as? SkyBox.Companion.SkyShader
            if (shader != null) {
                val overrides = material.shaderOverrides
                variables.addAll(overrides.entries.map { Variable(it.value.type, it.key) })
                val stage = shader.createFragmentStage(isInstanced = false, isAnimated = false, motionVectors = false)
                functions.addAll(stage.functions.map { it.body })
                functions.add("vec4 getSkyColor1(vec3 pos){ return vec4(getSkyColor(pos),1.0);\n }")
            } else functions.add(defaultSkyColor)
        } else functions.add(defaultSkyColor)

        functions.add(noiseFunc)
        functions.add(tonemapGLSL)
        functions.add(quatRot)
        functions.add(rawToDepth)
        functions.add(depthToPosition)
        variables.addAll(depthToPositionList)

        return Shader(
            "ss-reflections", coordsList, coordsVShader, uvList, variables, "" +

                    functions.joinToString("\n") +

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

                    "   ivec2 texSizeI = textureSize(finalDepth, 0);\n" +
                    "   vec2  texSize  = vec2(texSizeI);\n" +

                    "   vec3 positionFrom     = depthToPosition(texture(finalDepth,uv).r);\n" +

                    "   vec3 normal           = normalize(texture(finalNormal, uv).xyz * 2.0 - 1.0);\n" +
                    "   vec3 pivot            = normalize(reflect(positionFrom, normal));\n" +

                    "   float startDistance = length(positionFrom);\n" +

                    "   vec3  endView       = positionFrom + pivot * testDistance;\n" +
                    "   float endDistance   = length(endView);\n" +

                    "   vec4 endUV0    = transform * vec4(endView, 1.0);\n" +
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
                    "   int maxLinearSteps = int(min(delta * $testMaxDistanceRatio.0, useX ?\n" +
                    "       (deltaXY.x < 0.0 ? uv.x : 1.0 - uv.x) * resolution * texSize.x :\n" +
                    "       (deltaXY.y < 0.0 ? uv.y : 1.0 - uv.y) * resolution * texSize.y\n" +
                    "   ));\n" +
                    "   for (int i = 0; i <= maxLinearSteps; i++){\n" +

                    "       dstUV     += increment;\n" +
                    "       positionTo = depthToPosition(texture(finalDepth, dstUV).r);\n" +

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
                    "       vec4 color2 = getSkyColor1(pivot) * vec4(texture(finalColor, uv).rgb, 1.0);\n" +
                    "       color0 = mix(color0, color2.rgb, min(reflectivity * color2.a * strength, 1.0));\n" +
                    "       fragColor = vec4(applyToneMapping ? tonemap(color0) : color0, 1.0);\n" +
                    "       return;\n" +
                    "   }\n" +

                    // "   fraction1 = (fraction0 + fraction1) * 0.5;\n" +

                    "   vec2  bestUV = dstUV;\n" +
                    "   vec3  bestPositionTo = positionTo;\n" +
                    "   float bestDepth = depth;\n" +
                    "   for (int i = 0; i < steps; i++){\n" +

                    "       float fractionI = mix(fraction0, fraction1, float(i)/float(steps));\n" +

                    "       dstUV      = mix(uv, endUV, fractionI);\n" +
                    "       positionTo = depthToPosition(texture(finalDepth,dstUV).r);\n" +

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

                    "   float visibility = \n" +
                    "         (1.0 + min(dot(normalize(positionFrom), pivot), 0.0))\n" + // [0,1]
                    "       * (1.0 - min(bestDepth / thickness, 1.0))\n" +
                    "       * (1.0 - sqrt(distanceSq / maxDistanceSq))\n" +
                    "       * min(10.0 * (0.5 - abs(bestUV.x - 0.5)), 1.0)\n" +
                    "       * min(10.0 * (0.5 - abs(bestUV.y - 0.5)), 1.0);\n" +

                    // reflected position * base color of mirror (for golden reflections)
                    "   vec3 color1 = texture(finalIlluminated, bestUV).rgb;\n" +
                    "   vec4 sky = getSkyColor1(pivot);\n" +
                    "   vec4 color2 = mix(sky, vec4(color1,1.0), visibility) * vec4(texture(finalColor, uv).rgb, 1.0);\n" +
                    "   color0 = mix(color0, color2.rgb, min(reflectivity * color2.a * strength, 1.0));\n" +
                    "   fragColor = vec4(applyToneMapping ? tonemap(color0) : color0, 1.0);\n" +
                    // "   fragColor = vec4(0,0,visibility,1);\n" +
                    // "   fragColor = vec4(bestUV, visibility, 1);\n" +
                    "}"
        )
    }

    val shaders = HashMap<SkyBox.Companion.SkyShader?, Shader>()

    fun compute(
        buffer: IFramebuffer,
        illuminated: ITexture2D,
        deferred: DeferredSettingsV2,
        transform: Matrix4f,
        skyBox: SkyBox?,
        skyColor: Vector4f,
        applyToneMapping: Boolean,
        dst: Framebuffer = FBStack["ss-reflections", buffer.w, buffer.h, 4, true, 1, false]
    ) = compute(
        buffer, illuminated, deferred, transform,
        skyBox, skyColor,
        1f, 2f, 0.2f, 10, 8f,
        applyToneMapping, dst
    )

    /**
     * computes screen space reflections from metallic, roughness, normal, position and color buffers
     * */
    fun compute(
        buffer: IFramebuffer,
        illuminated: ITexture2D,
        deferred: DeferredSettingsV2,
        transform: Matrix4f,
        skyBox: SkyBox?,
        skyColor: Vector4f,
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
        val colorTexture = deferred.findTexture(buffer, DeferredLayerType.COLOR) ?: return null
        return compute(
            buffer.depthTexture!!,
            normalTexture,
            colorTexture,
            deferred.findTexture(buffer, metallicLayer),
            metallicMask,
            deferred.findTexture(buffer, roughnessLayer),
            roughnessMask,
            illuminated,
            transform,
            skyBox, skyColor, strength,
            maskSharpness, wallThickness, fineSteps, maxDistance, applyToneMapping,
            dst
        ).getTexture0()
    }

    /**
     * computes screen space reflections from metallic, roughness, normal, position and color buffers
     * */
    fun compute(
        depth: ITexture2D,
        normal: ITexture2D,
        color: ITexture2D,
        metallic: ITexture2D,
        metallicMask: String,
        roughness: ITexture2D,
        roughnessMask: String,
        illuminated: ITexture2D,
        transform: Matrix4f,
        skyBox: SkyBox?,
        skyColor: Vector4f,
        strength: Float = 1f,
        maskSharpness: Float = 2f,
        wallThickness: Float = 0.2f,
        fineSteps: Int = 10, // 10 are enough, if there are only rough surfaces
        maxDistance: Float = 8f,
        applyToneMapping: Boolean,
        dst: IFramebuffer = FBStack["ss-reflections", depth.w, depth.h, 4, true, 1, false]
    ): IFramebuffer {
        // metallic may be on r, g, b, or a
        useFrame(dst, Renderer.copyRenderer) {
            val skyShader = skyBox?.material?.shader as? SkyBox.Companion.SkyShader
            val shader = shaders.getOrPut(skyShader) { createShader(skyBox) }
            shader.use()
            shader.v4f("skyColor", skyColor)
            if (skyShader != null) {
                for ((k, v) in skyBox.material.shaderOverrides) {
                    v.bind(shader, k)
                }
            }
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
            bindDepthToPosition(shader)
            illuminated.bind(shader, "finalIlluminated", n, c)
            roughness.bind(shader, "finalRoughness", n, c)
            metallic.bind(shader, "finalMetallic", n, c)
            normal.bind(shader, "finalNormal", n, c)
            depth.bind(shader, "finalDepth", n, c)
            color.bind(shader, "finalColor", n, c)
            flat01.draw(shader)
        }
        return dst
    }

}