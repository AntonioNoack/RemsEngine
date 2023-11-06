package me.anno.ecs.components.shaders

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.GFX
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.maths.Maths.hasFlag

/**
 * a material, that is defined by blocks (which may be empty);
 * it is ray-traced internally;
 * the mesh needs to be back-faced to work correctly, when you're inside the mesh;
 * the mesh typically would be a cuboid;
 * the dimensions should be integers;
 *
 * if you want a textured cuboid only, use a different material!
 * (this one is recursive = expensive)
 * */
abstract class BlockTracedShader(name: String) : ECSMeshShader(name) {

    // needs to be adjusted as well for accurate shadows
    // I hope this gets optimized well, because no material data is actually required...
    override fun createDepthShader(key: ShaderKey): Shader {
        val builder = createBuilder()
        builder.addVertex(createVertexStages(key))
        builder.addFragment(createFragmentStages(key))
        GFX.check()
        val shader = builder.create("depth")
        shader.glslVersion = glslVersion
        GFX.check()
        return shader
    }

    open fun initProperties(instanced: Boolean): String = ""
    open fun processBlock(instanced: Boolean): String = ""
    open fun modifyDepth(instanced: Boolean): String = ""
    open fun onFinish(instanced: Boolean): String = ""

    open fun computeMaterialProperties(instanced: Boolean): String {
        return "" +
                "finalColor = vec3(1.0);\n" +
                "finalAlpha = 1.0;\n" +
                "finalEmissive = vec3(0.0);\n" +
                "finalMetallic = 0.0;\n" +
                "finalRoughness = 0.5;\n"
    }

    override fun createFragmentVariables(key: ShaderKey): ArrayList<Variable> {
        val list = super.createFragmentVariables(key)
        list.addAll(
            listOf(
                // input varyings
                Variable(GLSLType.V3F, "localPosition"),
                Variable(GLSLType.M4x4, "transform"),
                Variable(GLSLType.M4x3, "localTransform"),
                Variable(GLSLType.M4x3, "invLocalTransform"),
                Variable(GLSLType.V1I, "maxSteps"),
                Variable(GLSLType.V1F, "worldScale"),
                Variable(GLSLType.V3I, "bounds"),
            )
        )
        return list
    }

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        val flags = key.flags
        return listOf(
            ShaderStage(
                "block-traced shader", createFragmentVariables(key), "" +
                        // step by step define all material properties
                        "vec3 bounds0 = vec3(bounds), halfBounds = bounds0 * 0.5;\n" +
                        "vec3 bounds1 = vec3(bounds-1);\n" +
                        // start our ray on the surface of the cube: we don't need to project the ray onto the box
                        "vec3 dir = normalize(matMul(invLocalTransform, vec4(finalPosition, 0.0)));\n" +
                        // "vec3 dir = normalize(finalPosition);\n" +
                        // prevent divisions by zero
                        "if(abs(dir.x) < 1e-7) dir.x = 1e-7;\n" +
                        "if(abs(dir.y) < 1e-7) dir.y = 1e-7;\n" +
                        "if(abs(dir.z) < 1e-7) dir.z = 1e-7;\n" +
                        // could be a uniform, too (if perspective is projection, not ortho)
                        "vec3 localStart = -matMul(invLocalTransform, vec4(localTransform[3][0],localTransform[3][1],localTransform[3][2],0.0));\n" +
                        // start from camera, and project onto front sides
                        // for proper rendering, we need to use the backsides, and therefore we project the ray from the back onto the front
                        "vec3 dirSign = sign(dir);\n" +
                        "vec3 dtf3 = (dirSign * halfBounds + localStart) / dir;\n" +
                        "float dtf1 = min(dtf3.x, min(dtf3.y, dtf3.z));\n" +
                        "float dtf = min(dtf1, 0.0);\n" +
                        "localStart += -dtf * dir + halfBounds;\n" +
                        "vec3 blockPosition = clamp(floor(localStart), vec3(0.0), bounds1);\n" +
                        "vec3 dist3 = (dirSign*.5+.5 + blockPosition - localStart)/dir;\n" +
                        "vec3 invUStep = dirSign/dir;\n" +
                        "float nextDist, dist = 0.0;\n" +
                        initProperties(flags.hasFlag(IS_INSTANCED)) +
                        "int lastNormal = dtf3.z == dtf1 ? 2 : dtf3.y == dtf1 ? 1 : 0, i;\n" +
                        "bool done = false;\n" +
                        "for(i=0;i<maxSteps;i++){\n" +
                        "   nextDist = min(dist3.x, min(dist3.y, dist3.z));\n" +
                        "   bool continueTracing = false;\n" +
                        "   bool setNormal = true;\n" +
                        "   float skippingDist = 0.0;\n" +
                        processBlock(flags.hasFlag(IS_INSTANCED)) +
                        "   if(skippingDist >= 1.0){\n" +
                        // skip multiple blocks; and then recalculate all necessary stats
                        "       blockPosition = floor(localStart + dir * (dist + skippingDist));\n" +
                        "       if(any(lessThan(blockPosition, vec3(0.0))) || any(greaterThan(blockPosition, bounds1))) break;\n" +
                        "       dist3 = (dirSign*.5+.5 + blockPosition - localStart)/dir;\n" +
                        "       nextDist = min(dist3.x, min(dist3.y, dist3.z));\n" +
                        "       dist = nextDist;\n" +
                        "   } else if(continueTracing){\n" + // continue traversal
                        "       if(nextDist == dist3.x){\n" +
                        "           blockPosition.x += dirSign.x; dist3.x += invUStep.x; if(setNormal) lastNormal = 0;\n" +
                        "           if(blockPosition.x < 0.0 || blockPosition.x > bounds1.x){ break; }\n" +
                        "       } else if(nextDist == dist3.y){\n" +
                        "           blockPosition.y += dirSign.y; dist3.y += invUStep.y; if(setNormal) lastNormal = 1;\n" +
                        "           if(blockPosition.y < 0.0 || blockPosition.y > bounds1.y){ break; }\n" +
                        "       } else {\n" +
                        "           blockPosition.z += dirSign.z; dist3.z += invUStep.z; if(setNormal) lastNormal = 2;\n" +
                        "           if(blockPosition.z < 0.0 || blockPosition.z > bounds1.z){ break; }\n" +
                        "       }\n" +
                        "       dist = nextDist;\n" +
                        "   } else break;\n" + // hit something :)
                        "}\n" +
                        onFinish(flags.hasFlag(IS_INSTANCED)) +
                        // compute normal
                        "vec3 localNormal = vec3(0.0);\n" +
                        "if(lastNormal == 0){ localNormal.x = -dirSign.x; } else\n" +
                        "if(lastNormal == 1){ localNormal.y = -dirSign.y; }\n" +
                        "else {               localNormal.z = -dirSign.z; }\n" +
                        "finalNormal = normalize(matMul(localTransform, vec4(localNormal,0.0)));\n" +
                        "finalTangent = finalBitangent = vec3(0.0);\n" +
                        "mat3x3 tbn = mat3x3(finalTangent,finalBitangent,finalNormal);\n" +
                        // correct depth
                        modifyDepth(flags.hasFlag(IS_INSTANCED)) +
                        "vec3 localPos = localStart - halfBounds + dir * dist;\n" +
                        "finalPosition = matMul(localTransform, vec4(localPos, 1.0));\n" +
                        // must be used for correct mirror rendering
                        discardByCullingPlane +
                        "vec4 newVertex = matMul(transform, vec4(finalPosition, 1.0));\n" +
                        "gl_FragDepth = newVertex.z/newVertex.w;\n" +
                        computeMaterialProperties(flags.hasFlag(IS_INSTANCED)) +
                        v0 + sheenCalculation +
                        clearCoatCalculation +
                        reflectionCalculation +
                        ""
            )
        )
    }

    // todo we also could implement materials, that are partially translucent, and mix colors within :)

}