package me.anno.games.roadcraft

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderFuncLib.costShadingFunc
import me.anno.gpu.shader.ShaderFuncLib.randomGLSL
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.utils.types.Strings.iff

/**
 * Implement looks like streaming down sand particles...
 * */
object FallingSandShader : ECSMeshShader("Falling Sand") {

    // decrease number of layers on y-axis by 4x, we don't need such a high resolution on Y
    val optY = 4f
    val strandSize = 0.1f

    // todo properly set start and end using depth information

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        val showCost = key.renderer.nameDesc.englishName == "Num SDF Steps"
        return listOf(
            ShaderStage(
                "block-traced shader", createFragmentVariables(key) + listOf(
                    // input varyings
                    Variable(GLSLType.V3F, "localPosition"),
                    Variable(GLSLType.V3F, "brightColorSq"),
                    Variable(GLSLType.V3F, "darkColorSq"),
                    Variable(GLSLType.M4x4, "transform"),
                    Variable(GLSLType.M4x3, "localTransform"),
                    Variable(GLSLType.M4x3, "invLocalTransform"),
                    Variable(GLSLType.V1I, "maxSteps"),
                    Variable(GLSLType.V3I, "bounds"),
                    Variable(GLSLType.V1F, "time")
                ), "" +
                        // step by step define all material properties
                        "vec3 bounds0 = vec3(bounds), halfBounds = bounds0 * 0.5;\n" +
                        "vec3 bounds1 = vec3(bounds-1);\n" +
                        // start our ray on the surface of the cube: we don't need to project the ray onto the box
                        // localPosition != matMul(invLocalTransform, vec4(finalPosition, 0.0))
                        "vec3 dir = matMul(invLocalTransform, vec4(finalPosition, 0.0));\n" +
                        // "vec3 dir = normalize(finalPosition);\n" +
                        // prevent divisions by zero
                        "if(abs(dir.x) < 1e-7) dir.x = 1e-7;\n" +
                        "if(abs(dir.y) < 1e-7) dir.y = 1e-7;\n" +
                        "if(abs(dir.z) < 1e-7) dir.z = 1e-7;\n" +
                        // could be a uniform, too (if perspective is projection, not ortho)
                        "vec3 localStart = -matMul(invLocalTransform, vec4(localTransform[3][0],localTransform[3][1],localTransform[3][2],0.0));\n" +

                        "float scaleY = ${1f / optY};\n" +
                        "dir.y *= scaleY;\n" +
                        "localStart.y *= scaleY;\n" +
                        "localStart *= ${1f / strandSize};\n" +
                        "dir = normalize(dir);\n" +

                        // start from camera, and project onto front sides
                        // for proper rendering, we need to use the backsides, and therefore we project the ray from the back onto the front
                        "vec3 dirSign = sign(dir);\n" +
                        "vec3 dtf3 = (dirSign * halfBounds + localStart) / dir;\n" +
                        "float dtf1 = min(dtf3.x, min(dtf3.y, dtf3.z));\n" +
                        "float dtf = min(dtf1, 0.0);\n" +
                        "localStart += -dtf * dir + halfBounds;\n" +

                        "vec3 blockPosition = floor(localStart);\n" +
                        "blockPosition = clamp(blockPosition,vec3(0.0),bounds1);\n" +

                        "vec3 dist3 = (dirSign*.5+.5 + blockPosition - localStart)/dir;\n" +
                        "vec3 invUStep = dirSign / dir;\n" +
                        "float nextDist, dist = 0.0;\n" +
                        // initProperties(flags.hasFlag(IS_INSTANCED)) +
                        "int lastNormal = dtf3.z == dtf1 ? 2 : dtf3.y == dtf1 ? 1 : 0, i;\n" +
                        "float totalAlpha = 0.0;\n" +
                        "vec3 totalColor = vec3(0.0);\n" +
                        "float maxAlpha = 0.0;\n" +
                        "for(i=0;i<maxSteps;i++){\n" +

                        "   nextDist = min(dist3.x, min(dist3.y, dist3.z));\n" +

                        "   float posFalloff = 1.0 - 2.0 * length(blockPosition.xz/bounds1.xz-0.5);\n" +
                        "   if (posFalloff > 0.0) {\n" +
                        "       vec3 strandPos = blockPosition;\n" +
                        "       strandPos.y = localStart.y + dist * dir.y + 100.0 * random(blockPosition.xzz) + time;\n" +
                        "       strandPos.y = round(strandPos.y * 0.1);\n" +
                        "       float deltaDist = clamp(nextDist - dist, 0.0, 1.0);\n" +
                        "       float localAlpha = deltaDist * 0.7 * random(strandPos);\n" +
                        "       localAlpha *= posFalloff;\n" +
                        "       float deltaAlpha = (1.0 - totalAlpha) * localAlpha;\n" +
                        "       totalColor += deltaAlpha * mix(darkColorSq, brightColorSq, 1.0/(1.0 + 5.0 * maxAlpha));\n" +
                        "       totalAlpha += deltaAlpha;\n" +
                        "       maxAlpha += deltaDist * posFalloff;\n" +
                        "   }\n" +

                        "   bool continueTracing = totalAlpha < 0.7;\n" +
                        // processBlock(flags.hasFlag(IS_INSTANCED)) +
                        "   if(continueTracing){\n" + // continue traversal
                        "       if(nextDist == dist3.x){\n" +
                        "           blockPosition.x += dirSign.x; dist3.x += invUStep.x;\n" +
                        "           if(blockPosition.x < 0.0 || blockPosition.x > bounds1.x){ break; }\n" +
                        "       } else if(nextDist == dist3.y){\n" +
                        "           blockPosition.y += dirSign.y; dist3.y += invUStep.y;\n" +
                        "           if(blockPosition.y < 0.0 || blockPosition.y > bounds1.y){ break; }\n" +
                        "       } else {\n" +
                        "           blockPosition.z += dirSign.z; dist3.z += invUStep.z;\n" +
                        "           if(blockPosition.z < 0.0 || blockPosition.z > bounds1.z){ break; }\n" +
                        "       }\n" +
                        "       dist = nextDist;\n" +
                        "   } else break;\n" + // hit something :)
                        "}\n" +
                        "if(totalAlpha < 0.01) discard;\n" +

                        "finalNormal = finalTangent = finalBitangent = vec3(0.0);\n" +
                        "mat3x3 tbn = mat3x3(finalTangent,finalBitangent,finalNormal);\n" +

                        // optional code
                        /*"vec3 localPos = localStart - halfBounds + dir * dist;\n" +
                        "localPos *= vec3($strandSize, ${strandSize * optY}, $strandSize);\n" +
                        "finalPosition = matMul(localTransform, vec4(localPos, 1.0));\n" +
                        // must be used for correct mirror rendering
                        discardByCullingPlane +
                        "vec4 newVertex = matMul(transform, vec4(finalPosition, 1.0));\n" +
                        "#define CUSTOM_DEPTH\n" +
                        "gl_FragDepth = newVertex.z/newVertex.w;\n" +*/

                        // material properties
                        "finalRoughness = 1.0;\n" +
                        "finalMetallic = 0.0;\n" +
                        "finalColor = sqrt(totalColor / totalAlpha);\n" +

                        "#define MODULATE_ALPHA\n" +
                        "finalAlpha = 3.0 * totalAlpha;\n" +

                        ("" +
                                "finalColor = vec3(0.0);\n" +
                                "finalEmissive = costShadingFunc(min(float(i)*0.02,1.0));\n").iff(showCost) +

                        v0
            ).add(getReflectivity).add(costShadingFunc).add(randomGLSL)
        )
    }

}


