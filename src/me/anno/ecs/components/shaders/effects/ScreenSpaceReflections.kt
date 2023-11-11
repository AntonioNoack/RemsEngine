package me.anno.ecs.components.shaders.effects

import me.anno.engine.ui.render.Renderers.tonemapGLSL
import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.DepthTransforms.bindDepthToPosition
import me.anno.gpu.shader.DepthTransforms.depthToPosition
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib.randomGLSL
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.octNormalPacking
import me.anno.gpu.shader.ShaderLib.quatRot
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
// todo these are too sharp, make them blurry when the material is rough
object ScreenSpaceReflections {

    // position + normal + metallic/reflectivity + maybe-roughness + illuminated image -> illuminated + reflections
    fun createShader(): Shader {
        val variables = arrayListOf(
            Variable(GLSLType.V4F, "result", VariableMode.OUT),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.S2D, "finalColor"),
            Variable(GLSLType.S2D, "finalIlluminated"),
            Variable(GLSLType.S2D, "finalDepth"),
            Variable(GLSLType.V4F, "depthMask"),
            Variable(GLSLType.S2D, "finalNormal"),
            // reflectivity = metallic * (1-roughness),
            Variable(GLSLType.S2D, "finalMetallic"),
            Variable(GLSLType.V4F, "metallicMask"),
            Variable(GLSLType.S2D, "finalRoughness"),
            Variable(GLSLType.V4F, "roughnessMask"),
            Variable(GLSLType.V1F, "resolution"),
            Variable(GLSLType.V1I, "steps"),
            Variable(GLSLType.V1F, "thickness"),
            Variable(GLSLType.V1F, "maskSharpness"),
            Variable(GLSLType.V1F, "strength"),
            Variable(GLSLType.V1B, "applyToneMapping"),
            Variable(GLSLType.V1B, "normalZW"),
        )

        val functions = HashSet<String>()
        functions.add(randomGLSL)
        functions.add(tonemapGLSL)
        functions.add(quatRot)
        functions.add(rawToDepth)
        functions.add(depthToPosition)
        functions.add(octNormalPacking)
        variables.addAll(depthVars)

        val returnColor0 = "" +
                "result = vec4(applyToneMapping ? tonemap(color0) : color0, 1.0);\n" +
                "return;\n"

        return Shader(
            "ss-reflections", coordsList, coordsUVVertexShader, uvList, variables, "" +

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
                    returnColor0 +
                    "   };\n" +

                    "   ivec2 texSizeI = textureSize(finalDepth, 0);\n" +
                    "   vec2  texSize  = vec2(texSizeI);\n" +
                    "   ivec2 uvi = clamp(ivec2(uv*texSize),ivec2(0,0),texSizeI-1);\n" +

                    "   vec3 positionFrom = rawDepthToPosition(uv,dot(texelFetch(finalDepth,uvi,0),depthMask));\n" +

                    "   vec4 normalData = texture(finalNormal, uv);\n" +
                    "   vec3 normal     = UnpackNormal(normalZW ? normalData.zw : normalData.xy);\n" +
                    "   vec3 pivot      = normalize(reflect(positionFrom, normal));\n" +

                    "   float startDistance = length(positionFrom);\n" +
                    "   vec3 endView; vec4 endUV0; vec2 endUV;\n" +

                    // guess a distance to the border
                    "   float dist = startDistance;\n" +
                    "   endView = positionFrom + pivot * dist;\n" +
                    "   endUV0 = matMul(transform, vec4(endView, 1.0));\n" +
                    "   endUV = endUV0.xy / endUV0.w * 0.5 + 0.5;\n" +
                    // correct that distance
                    "   dist *= min(\n" +
                    "       (endUV.x<uv.x?uv.x:1.0-uv.x)/abs(endUV.x-uv.x),\n" +
                    "       (endUV.y<uv.y?uv.y:1.0-uv.y)/abs(endUV.y-uv.y)\n" +
                    "   );\n" +

                    // now we're on the border :)
                    "   endView = positionFrom + pivot * dist;\n" +
                    "   endUV0 = matMul(transform, vec4(endView, 1.0));\n" +
                    "   endUV = endUV0.xy / endUV0.w * 0.5 + 0.5;\n" +
                    "   float endDistance = length(endView);\n" +

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
                    "   int maxLinearSteps = int(min(100.0, useX ?\n" +
                    "       (deltaXY.x < 0.0 ? uv.x : 1.0 - uv.x) * resolution * texSize.x :\n" +
                    "       (deltaXY.y < 0.0 ? uv.y : 1.0 - uv.y) * resolution * texSize.y\n" +
                    "   ));\n" +
                    "   for (int i = 0; i <= maxLinearSteps; i++){\n" +

                    "       dstUV += increment;\n" +
                    "       if(dstUV.x < 0.0 || dstUV.y < 0.0 || dstUV.x >= 1.0 || dstUV.y >= 1.0) break;\n" +
                    "       float depthTo = dot(texelFetch(finalDepth,ivec2(dstUV*texSize),0),depthMask);\n" +
                    "       positionTo = rawDepthToPosition(dstUV,depthTo);\n" +

                    "       fraction1 = useX ? (dstUV.x - uv.x) / deltaXY.x : (dstUV.y - uv.y) / deltaXY.y;\n" +

                    "       viewDistance = (startDistance * endDistance) / mix(endDistance, startDistance, fraction1);\n" +
                    "       depth = viewDistance - length(positionTo);\n" +

                    "       if (depth > 0.0 && depth < thickness) {\n" +
                    // we found something between fraction0 and fraction1
                    "           hit0 = 1;\n" +
                    "           break;\n" +
                    "       } else {\n" + // last fraction0
                    "           fraction0 = fraction1;\n" +
                    "       }\n" +
                    "   }\n" +

                    "   if(hit0 == 0) {\n" +
                    returnColor0 +
                    "   }\n" +

                    "   vec2  bestUV = dstUV;\n" +
                    "   vec3  bestPositionTo = positionTo;\n" +
                    "   float bestDepth = depth;\n" +
                    "   for (int i = 0; i < steps; i++){\n" +

                    "       float fractionI = mix(fraction0, fraction1, float(i)/float(steps));\n" +

                    "       dstUV      = mix(uv, endUV, fractionI);\n" +
                    "       float depthTo = dot(texelFetch(finalDepth,ivec2(dstUV*texSize),0),depthMask);\n" +
                    "       positionTo = rawDepthToPosition(dstUV,depthTo);\n" +

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
                    "   if(bestUV.x < 0.0 || bestUV.x > 1.0 || bestUV.y < 0.0 || bestUV.y > 1.0){\n" +
                    returnColor0 +
                    "   }\n" +

                    "   float visibility = \n" +
                    "         (1.0 + min(dot(normalize(positionFrom), pivot), 0.0))\n" + // [0,1]
                    "       * (1.0 - min(bestDepth / thickness, 1.0))\n" +
                    "       * min(10.0 * (0.5 - abs(bestUV.x - 0.5)), 1.0)\n" +
                    "       * min(10.0 * (0.5 - abs(bestUV.y - 0.5)), 1.0)\n" +
                    ";\n" +

                    // reflected position * base color of mirror (for golden reflections)
                    "   vec3 diffuseColor = texture(finalColor, uv).rgb;\n" +
                    "   vec3 color1 = diffuseColor * texelFetch(finalIlluminated,ivec2(bestUV*texSize),0).rgb;\n" +
                    "   color0 = mix(color0, color1, min(visibility * reflectivity * strength, 1.0));\n" +
                    returnColor0 +
                    "}\n"
        )
    }

    val shader = createShader()

    /**
     * computes screen space reflections from metallic, roughness, normal, position and color buffers
     * */
    fun compute(
        depth: ITexture2D,
        depthMask: Vector4f,
        normal: ITexture2D,
        normalZW: Boolean,
        color: ITexture2D,
        metallic: ITexture2D,
        metallicMask: Vector4f,
        roughness: ITexture2D,
        roughnessMask: Vector4f,
        illuminated: ITexture2D,
        transform: Matrix4f,
        strength: Float, // 1f
        maskSharpness: Float, // 1f
        wallThickness: Float, // 0.2f
        fineSteps: Int, // 10 are enough, if there are only rough surfaces
        applyToneMapping: Boolean,
        dst: IFramebuffer
    ): IFramebuffer {
        // metallic may be on r, g, b, or a
        useFrame(dst, Renderer.copyRenderer) {
            val shader = shader
            shader.use()
            shader.v1b("applyToneMapping", applyToneMapping)
            shader.v1f("resolution", 1f / fineSteps)
            shader.v1i("steps", fineSteps)
            shader.v1f("maskSharpness", maskSharpness)
            shader.v1f("thickness", wallThickness) // thickness, when we are under something
            shader.v1f("strength", strength)
            shader.m4x4("transform", transform)
            shader.v1b("normalZW", normalZW)
            val n = GPUFiltering.TRULY_LINEAR
            val c = Clamping.CLAMP
            shader.v4f("depthMask", depthMask)
            shader.v4f("metallicMask", metallicMask)
            shader.v4f("roughnessMask", roughnessMask)
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