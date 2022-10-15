package me.anno.ecs.components.shaders

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.GFX
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

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
    override fun createDepthShader(isInstanced: Boolean, isAnimated: Boolean, limitedTransform: Boolean): Shader {
        val builder = createBuilder()
        builder.addVertex(
            createVertexStage(
                isInstanced,
                isAnimated,
                colors = false,
                motionVectors = false,
                limitedTransform
            )
        )
        builder.addFragment(createFragmentStage(isInstanced, isAnimated, motionVectors = false))
        GFX.check()
        val shader = builder.create()
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

    override fun createFragmentVariables(
        isInstanced: Boolean,
        isAnimated: Boolean,
        motionVectors: Boolean
    ): ArrayList<Variable> {
        val list = super.createFragmentVariables(isInstanced, isAnimated, motionVectors)
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

    override fun createFragmentStage(isInstanced: Boolean, isAnimated: Boolean, motionVectors: Boolean): ShaderStage {
        return ShaderStage(
            // todo this flickering randomly, even without discard, why??
            "material", createFragmentVariables(isInstanced, isAnimated, motionVectors), "" +
                    // step by step define all material properties
                    "if(!gl_FrontFacing) discard;\n" +
                    "vec3 bounds0 = vec3(bounds), halfBounds = bounds0 * 0.5;\n" +
                    "vec3 bounds1 = vec3(bounds-1);\n" +
                    // start our ray on the surface of the cube: we don't need to project the ray onto the box
                    "vec3 dir = normalize(mat3x3(invLocalTransform) * finalPosition);\n" +
                    // prevent divisions by zero
                    "if(abs(dir.x) < 1e-7) dir.x = 1e-7;\n" +
                    "if(abs(dir.y) < 1e-7) dir.y = 1e-7;\n" +
                    "if(abs(dir.z) < 1e-7) dir.z = 1e-7;\n" +
                    // could be a uniform too
                    "vec3 localStart = -(localTransform * vec4(0.0,0.0,0.0,1.0))/worldScale;\n" +
                    // start from camera, and project onto front sides
                    // for proper rendering, we need to use the backsides, and therefore we project the ray from the back onto the front
                    "vec3 dtf3 = (sign(dir) * halfBounds + localStart) / dir;\n" +
                    "float dtf = min(min(dtf3.x, min(dtf3.y, dtf3.z)), 0.0);\n" +
                    "localStart += -dtf * dir + halfBounds;\n" +
                    "vec3 position = localStart;\n" +
                    "vec3 blockPosition = clamp(floor(position), vec3(0.0), bounds1);\n" +
                    "vec3 fractXYZ = position - blockPosition;\n" +
                    "vec3 fractOffset = sign(dir)*.5+.5;\n" +
                    "vec3 s = (fractOffset - fractXYZ)/dir;\n" +
                    "vec3 dn = sign(dir);\n" +
                    "vec3 ds = dn/dir;\n" +
                    "float nextDist, dist = 0.0;\n" +
                    initProperties(isInstanced) +
                    "int lastNormal = dtf3.z == dtf ? 2 : dtf3.y == dtf ? 1 : 0, i;\n" +
                    "bool done = false;\n" +
                    "for(i=0;i<maxSteps;i++){\n" +
                    "   nextDist = min(s.x, min(s.y, s.z));\n" +
                    "   bool continueTracing = false;\n" +
                    "   bool setNormal = true;\n" +
                    processBlock(isInstanced) +
                    "   if(continueTracing){\n" + // continue traversal
                    "       if(nextDist == s.x){\n" +
                    "           blockPosition.x += dn.x; s.x += ds.x; if(setNormal) lastNormal = 0;\n" +
                    "           if(blockPosition.x < 0.0 || blockPosition.x > bounds1.x){ break; }\n" +
                    "       } else if(nextDist == s.y){\n" +
                    "           blockPosition.y += dn.y; s.y += ds.y; if(setNormal) lastNormal = 1;\n" +
                    "           if(blockPosition.y < 0.0 || blockPosition.y > bounds1.y){ break; }\n" +
                    "       } else {\n" +
                    "           blockPosition.z += dn.z; s.z += ds.z; if(setNormal) lastNormal = 2;\n" +
                    "           if(blockPosition.z < 0.0 || blockPosition.z > bounds1.z){ break; }\n" +
                    "       }\n" +
                    "       dist = nextDist;\n" +
                    "   } else break;\n" + // hit something :)
                    "}\n" +
                    onFinish(isInstanced) +
                    // compute normal
                    "vec3 localNormal = vec3(0.0);\n" +
                    "if(lastNormal == 0){ localNormal.x = -dn.x; } else\n" +
                    "if(lastNormal == 1){ localNormal.y = -dn.y; }\n" +
                    "else {               localNormal.z = -dn.z; }\n" +
                    "finalNormal = normalize(mat3x3(localTransform) * localNormal);\n" +
                    "finalTangent = finalBitangent = vec3(0.0);\n" +
                    "mat3x3 tbn = mat3x3(finalTangent,finalBitangent,finalNormal);\n" +
                    // correct depth
                    modifyDepth(isInstanced) +
                    "vec3 localPos = localStart - halfBounds + dir * dist;\n" +
                    "finalPosition = localTransform * vec4(localPos, 1.0);\n" +
                    // must be used for correct mirror rendering
                    discardByCullingPlane +
                    "vec4 newVertex = transform * vec4(finalPosition, 1.0);\n" +
                    "gl_FragDepth = newVertex.z/newVertex.w;\n" +
                    computeMaterialProperties(isInstanced) +
                    reflectionPlaneCalculation +
                    v0 + sheenCalculation +
                    clearCoatCalculation +
                    ""
        )
    }

    // todo we also could implement materials, that are partially translucent, and mix colors within :)

}