package me.anno.ecs.components.shaders.effects

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
import me.anno.gpu.shader.BaseShader.Companion.NEEDS_COLORS
import me.anno.gpu.shader.DepthTransforms.bindDepthToPosition
import me.anno.gpu.shader.DepthTransforms.depthToPosition
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib.noiseFunc
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.octNormalPacking
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.whiteCube
import org.joml.Matrix4f
import org.joml.Vector4f

// https://lettier.github.io/3d-game-shaders-for-beginners/screen-space-reflection.html
// https://github.com/lettier/3d-game-shaders-for-beginners/blob/master/demonstration/shaders/fragment/screen-space-reflection.frag
object ScreenSpaceReflections {

    private const val testMaxDistanceRatio = 100

    // position + normal + metallic/reflectivity + maybe-roughness + illuminated image -> illuminated + reflections
    fun createShader(sky: SkyBox?): Shader {
        val variables = arrayListOf(
            Variable(GLSLType.V4F, "fragColor", VariableMode.OUT),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.S2D, "finalColor"),
            Variable(GLSLType.S2D, "finalEmissive"),
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
            Variable(GLSLType.SCube, "skyCubeMap"),
            Variable(GLSLType.V1B, "normalZW"),
        )
        val functions = HashSet<String>()
        val defaultSkyColor = "vec4 getSkyColor1(vec3 dir, float roughness) {\n" +
                "   return skyColor * textureLod(skyCubeMap, -dir, roughness * 10.0);" +
                "}\n"
        if (sky != null) {
            val material = sky.material
            val shader = material.shader as? SkyBox.Companion.SkyShader
            if (shader != null) {
                val overrides = material.shaderOverrides
                variables.addAll(overrides.entries.map { Variable(it.value.type, it.key) })
                val stages = shader.createFragmentStages(NEEDS_COLORS)
                for (stage in stages) functions.addAll(stage.functions.map { it.body })
                functions.add("vec4 getSkyColor1(vec3 pos, float roughness){ return vec4(getSkyColor(pos),1.0);\n }")
            } else functions.add(defaultSkyColor)
        } else functions.add(defaultSkyColor)

        functions.add(noiseFunc)
        functions.add(tonemapGLSL)
        functions.add(quatRot)
        functions.add(rawToDepth)
        functions.add(depthToPosition)
        functions.add(octNormalPacking)
        variables.addAll(depthVars)

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
                    "       return;\n" +
                    "   } else reflectivity = sqrt(reflectivity);\n" +

                    "   ivec2 texSizeI = textureSize(finalDepth, 0);\n" +
                    "   vec2  texSize  = vec2(texSizeI);\n" +

                    "   vec3 positionFrom     = rawDepthToPosition(uv,texture(finalDepth,uv).r);\n" +

                    "   vec4 normalData = texture(finalNormal, uv);\n" +
                    "   vec3 normal           = UnpackNormal(normalZW ? normalData.zw : normalData.xy);\n" +
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
                    "       positionTo = rawDepthToPosition(dstUV,texture(finalDepth, dstUV).r);\n" +

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

                    "   vec4 skyAtPivot = getSkyColor1(pivot, roughness);\n" +
                    "   vec4 baseEmission = vec4(texture(finalEmissive, uv).rgb, 0.0);\n" +
                    "   vec4 baseColor = vec4(texture(finalColor, uv).rgb, 1.0);\n" +
                    "   if(hit0 == 0) {\n" +
                    "       vec4 skyColor = skyAtPivot * baseColor + baseEmission;\n" +
                    "       color0 = mix(color0, skyColor.rgb, min(reflectivity * skyColor.a * strength, 1.0));\n" +
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
                    "       positionTo = rawDepthToPosition(dstUV,texture(finalDepth,dstUV).r);\n" +

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
                    "       vec4 skyColor = skyAtPivot * baseColor + baseEmission;\n" +
                    "       color0 = mix(color0, skyColor.rgb, min(reflectivity * skyColor.a * strength, 1.0));\n" +
                    "       fragColor = vec4(applyToneMapping ? tonemap(color0) : color0, 1.0);\n" +
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
                    "   vec4 skyColor = mix(skyAtPivot, vec4(color1,1.0), visibility) * baseColor + baseEmission;\n" +
                    "   color0 = mix(color0, skyColor.rgb, min(reflectivity * skyColor.a * strength, 1.0));\n" +
                    "   fragColor = vec4(applyToneMapping ? tonemap(color0) : color0, 1.0);\n" +
                    "}\n"
        )
    }

    val shaders = HashMap<SkyBox.Companion.SkyShader?, Shader>()

    fun compute(
        buffer: IFramebuffer,
        illuminated: ITexture2D,
        deferred: DeferredSettingsV2,
        transform: Matrix4f,
        skyBox: SkyBox?,
        skyCubeMap: CubemapTexture?,
        skyColor: Vector4f,
        applyToneMapping: Boolean,
        dst: Framebuffer = FBStack["ss-reflections", buffer.w, buffer.h, 4, true, 1, false]
    ) = compute(
        buffer, illuminated, deferred, transform,
        skyBox, skyCubeMap, skyColor,
        1f, 1f, 0.2f, 10, 8f,
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
        skyCubeMap: CubemapTexture?,
        skyColor: Vector4f,
        strength: Float = 1f,
        maskSharpness: Float = 1f,
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
        val emissiveTexture = deferred.findTexture(buffer, DeferredLayerType.EMISSIVE) ?: blackTexture
        val colorTexture = deferred.findTexture(buffer, DeferredLayerType.COLOR) ?: return null
        val metallic = deferred.findTexture(buffer, metallicLayer)
        val roughness = deferred.findTexture(buffer, roughnessLayer)
        return compute(
            buffer.depthTexture!!,
            normalTexture, deferred.zw(DeferredLayerType.NORMAL),
            colorTexture, emissiveTexture,
            metallic, singleToVector[metallicMask]!!,
            roughness, singleToVector[roughnessMask]!!,
            illuminated, transform,
            skyBox, skyCubeMap, skyColor, strength,
            maskSharpness, wallThickness, fineSteps,
            maxDistance, applyToneMapping, dst
        ).getTexture0()
    }

    /**
     * computes screen space reflections from metallic, roughness, normal, position and color buffers
     * */
    fun compute(
        depth: ITexture2D,
        normal: ITexture2D,
        normalZW: Boolean,
        color: ITexture2D,
        emissive: ITexture2D,
        metallic: ITexture2D,
        metallicMask: Vector4f,
        roughness: ITexture2D,
        roughnessMask: Vector4f,
        illuminated: ITexture2D,
        transform: Matrix4f,
        skyBox: SkyBox?,
        skyCubeMap: CubemapTexture?,
        skyColor: Vector4f,
        strength: Float = 1f,
        maskSharpness: Float = 1f,
        wallThickness: Float = 0.2f,
        fineSteps: Int = 10, // 10 are enough, if there are only rough surfaces
        maxDistance: Float = 8f,
        applyToneMapping: Boolean,
        dst: IFramebuffer = FBStack["ss-reflections", depth.w, depth.h, 4, true, 1, false]
    ): IFramebuffer {
        val skyBox1 = if (skyCubeMap != null && skyBox != null) null else skyBox
        // metallic may be on r, g, b, or a
        useFrame(dst, Renderer.copyRenderer) {
            val skyShader = skyBox1?.material?.shader as? SkyBox.Companion.SkyShader
            val shader = shaders.getOrPut(skyShader) { createShader(skyBox1) }
            shader.use()
            shader.v4f("skyColor", skyColor)
            if (skyShader != null) for ((k, v) in skyBox1.material.shaderOverrides) {
                v.bind(shader, k)
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
            shader.v1b("normalZW", normalZW)
            val n = GPUFiltering.TRULY_LINEAR
            val c = Clamping.CLAMP
            shader.v4f("metallicMask", metallicMask)
            shader.v4f("roughnessMask", roughnessMask)
            bindDepthToPosition(shader)
            illuminated.bind(shader, "finalIlluminated", n, c)
            roughness.bind(shader, "finalRoughness", n, c)
            emissive.bind(shader, "finalEmissive", n, c)
            metallic.bind(shader, "finalMetallic", n, c)
            normal.bind(shader, "finalNormal", n, c)
            depth.bind(shader, "finalDepth", n, c)
            color.bind(shader, "finalColor", n, c)
            (skyCubeMap ?: whiteCube).bind(shader, "skyCubeMap", GPUFiltering.LINEAR, c)
            flat01.draw(shader)
        }
        return dst
    }

}