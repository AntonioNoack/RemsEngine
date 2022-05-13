package me.anno.ecs.components.shaders

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.GFX
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import org.joml.Vector3i
import kotlin.math.max

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
abstract class BlockTracedMaterial(name: String) : ECSMeshShader(name) {

    // todo for multiple instances, we should not use multiple shader programs!!!

    val size = Vector3i(1)

    override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        super.bind(shader, renderer, instanced)
        shader.v3i("bounds", size)
        shader.v1i("maxSteps", max(1, size.x + size.y + size.z)) // max amount of blocks that can be traversed
    }

    // needs to be adjusted as well for accurate shadows
    // I hope this gets optimized well, because no material data is actually required...
    override fun createDepthShader(isInstanced: Boolean, isAnimated: Boolean): Shader {
        val builder = createBuilder()
        builder.addVertex(createVertexStage(isInstanced, isAnimated, false))
        builder.addFragment(createFragmentStage(isInstanced, isAnimated))
        GFX.check()
        val shader = builder.create()
        shader.glslVersion = glslVersion
        GFX.check()
        return shader
    }

    open fun initProperties(instanced: Boolean): String = ""
    open fun checkIfIsAir(instanced: Boolean): String = ""
    open fun modifyDepth(instanced: Boolean): String = ""

    open fun computeMaterialProperties(instanced: Boolean): String {
        return "" +
                "finalColor = vec3(1.0);\n" +
                "finalAlpha = 1.0;\n" +
                "finalEmissive = vec3(0.0);\n" +
                "finalMetallic = 0.0;\n" +
                "finalRoughness = 0.5;\n"
    }

    override fun createFragmentVariables(isInstanced: Boolean, isAnimated: Boolean): ArrayList<Variable> {
        return arrayListOf(
            // input varyings
            Variable(GLSLType.V3F, "localPosition"),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.M4x3, "localTransform"),
            Variable(GLSLType.M3x3, "invLocalTransform"),
            Variable(GLSLType.V1I, "maxSteps"),
            Variable(GLSLType.V1F, "worldScale"),
            Variable(GLSLType.V3I, "bounds"),
            // must-have/standard outputs
            Variable(GLSLType.V3F, "finalPosition", VariableMode.INOUT),
            Variable(GLSLType.V3F, "finalNormal", VariableMode.OUT),
            // material outputs
            Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
            Variable(GLSLType.V3F, "finalEmissive", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalMetallic", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalRoughness", VariableMode.OUT),
            // for reflections
            Variable(GLSLType.BOOL, "hasReflectionPlane"),
            Variable(GLSLType.V3F, "reflectionPlaneNormal"),
            Variable(GLSLType.S2D, "reflectionPlane"),
            Variable(GLSLType.V4F, "reflectionCullingPlane"),
        )
    }

    override fun createFragmentStage(isInstanced: Boolean, isAnimated: Boolean): ShaderStage {
        return ShaderStage(
            "material", createFragmentVariables(isInstanced, isAnimated), "" +
                    // step by step define all material properties
                    "vec3 bounds0 = vec3(bounds), halfBounds = bounds0 * 0.5;\n" +
                    "vec3 bounds1 = vec3(bounds-1);\n" +
                    // start our ray on the surface of the cube: we don't need to project the ray onto the box
                    "vec3 dir = normalize(invLocalTransform * finalPosition);\n" +
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
                    "   bool isAir = false;\n" +
                    checkIfIsAir(isInstanced) +
                    "   if(isAir){\n" + // continue traversal
                    "       if(nextDist == s.x){\n" +
                    "           blockPosition.x += dn.x; s.x += ds.x; lastNormal = 0;\n" +
                    "           if(blockPosition.x < 0.0 || blockPosition.x > bounds1.x){ discard; }\n" +
                    "       } else if(nextDist == s.y){\n" +
                    "           blockPosition.y += dn.y; s.y += ds.y; lastNormal = 1;\n" +
                    "           if(blockPosition.y < 0.0 || blockPosition.y > bounds1.y){ discard; }\n" +
                    "       } else {\n" +
                    "           blockPosition.z += dn.z; s.z += ds.z; lastNormal = 2;\n" +
                    "           if(blockPosition.z < 0.0 || blockPosition.z > bounds1.z){ discard; }\n" +
                    "       }\n" +
                    "       dist = nextDist;\n" +
                    "   } else break;\n" + // hit something :)
                    "}\n" +
                    // compute normal
                    "vec3 localNormal = vec3(0.0);\n" +
                    "if(lastNormal == 0){ localNormal.x = -dn.x; } else\n" +
                    "if(lastNormal == 1){ localNormal.y = -dn.y; }\n" +
                    "else {               localNormal.z = -dn.z; }\n" +
                    "finalNormal = normalize((localTransform * vec4(localNormal, 0.0)).xyz);\n" +
                    // correct depth
                    modifyDepth(isInstanced) +
                    "vec3 localPos = localStart - halfBounds + dir * dist;\n" +
                    "finalPosition = localTransform * vec4(localPos, 1.0);\n" +
                    // must be used for correct mirror rendering
                    "if(dot(vec4(finalPosition, 1.0), reflectionCullingPlane) < 0.0) discard;\n" +
                    "vec4 newVertex = transform * vec4(finalPosition, 1.0);\n" +
                    "gl_FragDepth = newVertex.z/newVertex.w;\n" +
                    // todo add reflections from reflection planes back in
                    // todo add other stuff back in maybe, like clear coat & stuff
                    computeMaterialProperties(isInstanced)
        )
    }

    // todo we also could implement materials, that are partially translucent, and mix colors within :)

}