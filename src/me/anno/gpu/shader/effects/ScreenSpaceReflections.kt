package me.anno.gpu.shader.effects

import me.anno.engine.ui.render.Renderers
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.ITexture2D
import me.anno.utils.structures.lists.LazyList
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.iff
import org.joml.Matrix4f

// https://lettier.github.io/3d-game-shaders-for-beginners/screen-space-reflection.html
// https://github.com/lettier/3d-game-shaders-for-beginners/blob/master/demonstration/shaders/fragment/screen-space-reflection.frag
// todo these are broken for orthographic rendering
// todo these are too sharp, make them blurry when the material is rough
object ScreenSpaceReflections {

    // position + normal + metallic/reflectivity + maybe-roughness + illuminated image -> illuminated + reflections
    fun createShader(inPlace: Boolean, depthMaskI: Int, roughnessMaskI: Int, metallicMaskI: Int): Shader {
        val variables = arrayListOf(
            Variable(GLSLType.V4F, "result", VariableMode.OUT),
            Variable(GLSLType.M4x4, "cameraMatrix"),
            Variable(GLSLType.S2D, "finalColor"),
            Variable(GLSLType.S2D, "finalIlluminated"),
            Variable(GLSLType.S2D, "finalDepth"),
            Variable(GLSLType.S2D, "finalNormal"),
            // reflectivity = metallic * (1-roughness),
            Variable(GLSLType.S2D, "finalMetallic"),
            Variable(GLSLType.S2D, "finalRoughness"),
            Variable(GLSLType.V1F, "resolution"),
            Variable(GLSLType.V1I, "steps"),
            Variable(GLSLType.V1F, "thickness"),
            Variable(GLSLType.V1F, "maskSharpness"),
            Variable(GLSLType.V1F, "strength"),
            Variable(GLSLType.V1B, "normalZW"),
        )

        val functions = HashSet<String>()
        functions.add(ShaderFuncLib.randomGLSL)
        functions.add(Renderers.tonemapGLSL)
        functions.add(ShaderLib.quatRot)
        functions.add(DepthTransforms.rawToDepth)
        functions.add(DepthTransforms.depthToPosition)
        functions.add(ShaderLib.octNormalPacking)
        variables.addAll(DepthTransforms.depthVars)

        val returnColor0 =
            if (inPlace) "result = vec4(color0, 1.0);\nreturn;\n"
            else "result = vec4(0.0);\nreturn;\n"

        val depthMask = "xyzw"[depthMaskI]
        val roughnessMask = "xyzw"[roughnessMaskI]
        val metallicMask = "xyzw"[metallicMaskI]

        return Shader(
            "ss-reflections", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList, variables, "" +

                    functions.joinToString("\n") +

                    "void main() {\n" +

                    "   vec3 color0 = texture(finalIlluminated, uv).rgb;\n".iff(inPlace) +

                    "   float metallic = texture(finalMetallic, uv).$metallicMask;\n" +
                    "   float roughness = texture(finalRoughness, uv).$roughnessMask;\n" +
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

                    "   vec3 positionFrom = rawDepthToPosition(uv,texelFetch(finalDepth,uvi,0).$depthMask);\n" +

                    "   vec4 normalData = texture(finalNormal, uv);\n" +
                    "   vec3 normal     = UnpackNormal(normalZW ? normalData.zw : normalData.xy);\n" +
                    "   vec3 pivot      = normalize(reflect(positionFrom, normal));\n" +

                    "   float startDistance = length(positionFrom);\n" +
                    "   vec3 endView; vec4 endUV0; vec2 endUV;\n" +

                    // guess a distance to the border
                    "   float dist = startDistance;\n" +
                    "   endView = positionFrom + pivot * dist;\n" +
                    "   endUV0 = matMul(cameraMatrix, vec4(endView, 0.0));\n" +
                    "   endUV = endUV0.xy / endUV0.w * 0.5 + 0.5;\n" +
                    // correct that distance
                    "   dist *= min(\n" +
                    "       (endUV.x<uv.x?uv.x:1.0-uv.x)/abs(endUV.x-uv.x),\n" +
                    "       (endUV.y<uv.y?uv.y:1.0-uv.y)/abs(endUV.y-uv.y)\n" +
                    "   );\n" +

                    // now we're on the border :)
                    "   endView = positionFrom + pivot * dist;\n" +
                    "   endUV0 = matMul(cameraMatrix, vec4(endView, 0.0));\n" +
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
                    "       if(dstUV.x <= 0.0 || dstUV.y <= 0.0 || dstUV.x >= 1.0 || dstUV.y >= 1.0) break;\n" +
                    "       float depthTo = texelFetch(finalDepth,ivec2(dstUV*texSize),0).$depthMask;\n" +
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
                    "       float depthTo = texelFetch(finalDepth,ivec2(dstUV*texSize),0).$depthMask;\n" +
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
                    "   if(bestUV.x <= 0.0 || bestUV.x >= 1.0 || bestUV.y <= 0.0 || bestUV.y >= 1.0){\n" +
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
                    "   float mixFactor = min(visibility * reflectivity * strength, 1.0);\n" +
                    (if (inPlace) "result = vec4(mix(color0, color1, mixFactor), 1.0);\n"
                    else "result = vec4(color1, mixFactor);\n") +
                    "}\n"
        )
    }

    val shader = LazyList(2 * 4 * 4 * 4) {
        createShader(
            it.hasFlag(1),
            (it shr 1).and(3),
            (it shr 3).and(3),
            (it shr 5).and(3),
        )
    }

    /**
     * computes screen space reflections from metallic, roughness, normal, position and color buffers
     * */
    fun compute(
        depth: ITexture2D,
        // depth may be on r, g, b, or a
        depthMask: Int,
        normal: ITexture2D,
        normalZW: Boolean,
        color: ITexture2D,
        metallic: ITexture2D,
        // metallic may be on r, g, b, or a
        metallicMask: Int,
        roughness: ITexture2D,
        // roughness may be on r, g, b, or a
        roughnessMask: Int,
        illuminated: ITexture2D,
        cameraMatrix: Matrix4f,
        strength: Float, // 1f
        maskSharpness: Float, // 1f
        wallThickness: Float, // 0.2f
        fineSteps: Int, // 10 are enough, if there are only rough surfaces
        inPlace: Boolean,
        dst: IFramebuffer
    ) {
        GFXState.useFrame(dst, Renderer.copyRenderer) {
            val shader = shader[inPlace.toInt() +
                    depthMask.shl(1) +
                    roughnessMask.shl(3) +
                    metallicMask.shl(5)]
            shader.use()
            shader.v1f("resolution", 1f / fineSteps)
            shader.v1i("steps", fineSteps)
            shader.v1f("maskSharpness", maskSharpness)
            shader.v1f("thickness", wallThickness) // thickness, when we are under something
            shader.v1f("strength", strength)
            shader.m4x4("cameraMatrix", cameraMatrix)
            shader.v1b("normalZW", normalZW)
            DepthTransforms.bindDepthUniforms(shader)
            illuminated.bindTrulyLinear(shader, "finalIlluminated")
            roughness.bindTrulyLinear(shader, "finalRoughness")
            metallic.bindTrulyLinear(shader, "finalMetallic")
            normal.bindTrulyLinear(shader, "finalNormal")
            depth.bindTrulyLinear(shader, "finalDepth")
            color.bindTrulyLinear(shader, "finalColor")
            SimpleBuffer.flat01.draw(shader)
        }
    }
}