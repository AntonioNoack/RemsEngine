package me.anno.maths.bvh

import me.anno.ecs.components.mesh.sdf.shapes.SDFBoundingBox.Companion.boundingBoxSDF
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.texture.Texture2D
import me.anno.maths.bvh.RayTracing.coloring
import me.anno.maths.bvh.RayTracing.glslBLASIntersection
import me.anno.maths.bvh.RayTracing.glslComputeDefines
import me.anno.maths.bvh.RayTracing.glslGraphicsDefines
import me.anno.maths.bvh.RayTracing.glslIntersections
import me.anno.maths.bvh.RayTracing.glslRandomGen
import me.anno.maths.bvh.RayTracing.glslTLASIntersection
import me.anno.maths.bvh.RayTracing.loadMat4x3
import me.anno.maths.bvh.RayTracing2.bufferLayouts
import me.anno.maths.bvh.RayTracing2.bufferStructs
import me.anno.maths.bvh.RayTracing2.glslBLASIntersection2
import me.anno.maths.bvh.RayTracing2.glslTLASIntersection2
import me.anno.utils.LOGGER
import me.anno.utils.structures.tuples.Quad
import org.joml.Vector2i

val shading = "" +
        "   float distance = Infinity;\n" +
        "   vec3 pos = worldPos, dir = worldDir;\n" +
        "   vec3 normal = vec3(0.0), normal0 = worldDir;\n" +
        "   vec3 color = vec3(1.0);\n" +
        "   uint tlasCtr=0u,blasCtr=0u,trisCtr=0u;\n" +
        "if(drawMode==${DrawMode.GLOBAL_ILLUMINATION.id}){\n" +
        "   float roughness = 0.1;\n" +
        "   for(int i=ZERO,limit=3;i<limit;i++){\n" +
        "       distance = Infinity;\n" +
        "#ifndef TLAS_DEPTH\n" +
        "       intersectBLAS(0u,pos,dir,1.0/dir,distance,normal,blasCtr,trisCtr);\n" +
        "#else\n" +
        "       intersectTLAS(pos,dir,distance,normal,tlasCtr,blasCtr,trisCtr);\n" +
        "#endif\n" +
        "       if(distance < Infinity){\n" +
        // todo textures, different materials, ...
        // mix color with hit color (at the moment: light gray)
        "           color *= 0.95;\n" +
        // continue raytracing by readjusting pos & normal
        // find new ray direction depending on BRDF
        "           pos = pos + dir * distance;\n" +
        // diffuse:
        "           dir = normalize(normal + nextRandS3(seed));\n" +
        // metallic:
        // "           dir = normalize(reflect(dir, normalize(normal)) + roughness * nextRandS3(seed));\n" +
        // add small offset, so we don't have shadow acne
        "           pos += dir * 0.01;\n" +
        "           if(i==limit-1) color = vec3(0.0);\n" +
        "           normal0 = normal;\n" +
        "       } else {\n" +
        // we hit the sky -> query the sky color
        "           normal = normalize(normal0);\n" +
        "           color *= 20.0 * mix(sky0,sky1,normal.y*.5+.5);\n" +
        "           break;\n" +
        "       }\n" +
        "   }\n" +
        "} else {\n" +
        "#ifndef TLAS_DEPTH\n" +
        "       intersectBLAS(0u,pos,dir,1.0/dir,distance,normal,blasCtr,trisCtr);\n" +
        "#else\n" +
        "       intersectTLAS(pos,dir,distance,normal,tlasCtr,blasCtr,trisCtr);\n" +
        "#endif\n" +
        "   if(drawMode==${DrawMode.NORMAL.id}) {\n" +
        // simple coloring
        "      if(dot(normal,normal)>0.0){\n" +
        "           color = normalize(normal)*.5+.5;\n" +
        "      } else {\n" +
        // compute sky color
        "           color = mix(sky0,sky1,dir.y*.5+.5);\n" +
        "      }\n" +
        "   } else if(drawMode==${DrawMode.TLAS_DEPTH.id}){\n" +
        "      color = coloring(float(tlasCtr)*0.1);\n" +
        "   } else if(drawMode==${DrawMode.BLAS_DEPTH.id}){\n" +
        "      color = coloring(float(blasCtr)*0.1);\n" +
        "   } else if(drawMode==${DrawMode.TRIS_DEPTH.id}){\n" +
        "      color = coloring(float(trisCtr)*${0.1 / 9});\n" +
        "   } else if(drawMode==${DrawMode.SIMPLE_SHADOW.id}){\n" +
        "      if(dot(normal,normal)>0.0){\n" +
        "           pos += dir * distance * 0.999;\n" +
        "           dir = normalize(vec3(5,9,3) + nextRandS3(seed));\n" +
        "           distance = Infinity;\n" +
        "           color = vec3(dot(normalize(normal),dir)*.4+.6);\n" +
        "#ifndef TLAS_DEPTH\n" +
        "       intersectBLAS(0u,pos,dir,1.0/dir,distance,normal,blasCtr,trisCtr);\n" +
        "#else\n" +
        "       intersectTLAS(pos,dir,distance,normal,tlasCtr,blasCtr,trisCtr);\n" +
        "#endif\n" +
        "           if(distance < 1e6) color *= 0.2;\n" + // shadow
        "      } else {\n" +
        // compute sky color
        "          color = mix(sky0,sky1,dir.y*.5+.5);\n" +
        "      }\n" +
        "   }\n" +
        "}\n"

val commonUniforms = "" +
        "uniform ivec2 size;\n" +
        "uniform vec3 worldPos;\n" +
        "uniform vec4 worldRot;\n" +
        "uniform vec3 cameraOffset;\n" +
        "uniform vec3 sky0, sky1;\n" +
        "uniform int drawMode;\n" +
        "#define Infinity 1e15\n" +
        "uniform int frameIndex;\n" +
        "uniform int ZERO;\n" +
        "uniform float alpha;\n"

val core = "" +
        "uint pixelId = uint(uv.x + uv.y * size.x);\n" +
        "uint seed = initRand(pixelId, uint(frameIndex));\n" +
        "vec3 worldDir = vec3(vec2(uv)-cameraOffset.xy, cameraOffset.z);\n" +
        "worldDir = normalize(quatRot(worldDir, worldRot));\n" +
        shading

val imageStore = "" +
        "vec3 oldColor = imageLoad(dst, uv).rgb;\n" +
        "color = mix(oldColor, color, alpha);\n" +
        "imageStore(dst, uv, vec4(color, 1.0));\n"

val commonFunctions = glslIntersections + quatRot + loadMat4x3 + glslRandomGen + boundingBoxSDF + coloring

fun createBLASTextureComputeShader(maxDepth: Int): ComputeShader {
    return ComputeShader(
        "bvh-traversal", Vector2i(16), "" +
                "layout(rgba32f, binding = 0) uniform image2D triangles;\n" +
                "layout(rgba32f, binding = 1) uniform image2D nodes;\n" +
                "layout(rgba32f, binding = 3) uniform image2D dst;\n" +
                commonUniforms +
                commonFunctions +
                "#define BLAS_DEPTH $maxDepth\n" +
                glslComputeDefines +
                glslBLASIntersection +
                "void main(){\n" +
                "   ivec2 uv = ivec2(gl_GlobalInvocationID.xy);\n" +
                "   if(all(lessThan(uv,size))){\n" +
                core + imageStore +
                "   }\n" +
                "}\n"
    )
}

fun createBLASBufferComputeShader(maxDepth: Int): ComputeShader {
    return ComputeShader(
        "bvh-traversal", Vector2i(16), "" +
                bufferStructs +
                bufferLayouts +
                commonUniforms +
                commonFunctions +
                "#define BLAS_DEPTH $maxDepth\n" +
                glslComputeDefines +
                glslBLASIntersection2 +
                "void main(){\n" +
                "   ivec2 uv = ivec2(gl_GlobalInvocationID.xy);\n" +
                "   if(all(lessThan(uv,size))){\n" +
                core + imageStore +
                "   }\n" +
                "}\n"
    )
}

fun createBLASTextureGraphicsShader(bvh: BLASNode): Shader {

    val maxBLASDepth = bvh.maxDepth()

    return Shader(
        "bvh-traversal", coordsList, coordsVShader, uvList, listOf(), "" +
                "out vec4 dst;\n" +
                "uniform sampler2D triangles, blasNodes, tlasNodes;\n" +
                commonUniforms +
                commonFunctions +
                "#define nodes blasNodes\n" +
                "#define BLAS_DEPTH $maxBLASDepth\n" +
                glslGraphicsDefines +
                glslBLASIntersection +
                "void main(){\n" +
                "   ivec2 uv = ivec2(gl_FragCoord.xy);\n" +
                core +
                "   dst = vec4(color, alpha);\n" +
                "}\n"
    ).apply {
        glslVersion = 330 // for floatBitsToUint
        setTextureIndices("triangles", "blasNodes", "tlasNodes")
    }

}

fun createTLASTextureGraphicsShader(bvh: TLASNode): Pair<Shader, List<BLASNode>> {

    val meshes = HashSet<BLASNode>(bvh.countTLASLeaves())
    bvh.collectMeshes(meshes)

    val maxTLASDepth = bvh.maxDepth()
    val maxBLASDepth = meshes.maxOf { it.maxDepth() }

    return Shader(
        "bvh-traversal", coordsList, coordsVShader, uvList, listOf(), "" +
                "out vec4 dst;\n" +
                "uniform sampler2D triangles, blasNodes, tlasNodes;\n" +
                commonUniforms +
                commonFunctions +
                "#define nodes blasNodes\n" +
                "#define BLAS_DEPTH $maxBLASDepth\n" +
                "#define TLAS_DEPTH $maxTLASDepth\n" +
                glslGraphicsDefines +
                glslBLASIntersection +
                glslTLASIntersection +
                "void main(){\n" +
                "   ivec2 uv = ivec2(gl_FragCoord.xy);\n" +
                core +
                "   dst = vec4(color, alpha);\n" +
                "}\n"
    ).apply {
        glslVersion = 330 // for floatBitsToUint
        setTextureIndices("triangles", "blasNodes", "tlasNodes")
    } to meshes.toList()

}

fun createTLASTextureComputeShader(bvh: TLASNode): Quad<ComputeShader, Texture2D, Texture2D, Texture2D> {

    val uniqueMeshes = HashSet<BLASNode>(bvh.countTLASLeaves())
    bvh.collectMeshes(uniqueMeshes)

    val meshes = uniqueMeshes
        .sortedByDescending { it.countNodes() } // complex meshes first for testing and consistency
        .toList()

    val triangles = BLASNode.createTriangleTexture(meshes)
    val blasNodes = BLASNode.createBLASTexture(meshes)
    val tlasNodes = bvh.createTLASTexture() // needs to be created after blas nodes

    // triangles.write(desktop.getChild("bvh/sponza-tri.png"), false, withAlpha = false)
    // blasNodes.write(desktop.getChild("bvh/sponza-blas.png"), false, withAlpha = false)
    // tlasNodes.write(desktop.getChild("bvh/sponza-tlas.png"), false, withAlpha = false)

    val maxTLASDepth = bvh.maxDepth()
    val maxBLASDepth = meshes.maxOf { it.maxDepth() }

    LOGGER.debug("Max TLAS depth: $maxTLASDepth, max BLAS depth: $maxBLASDepth")

    return Quad(
        ComputeShader(
            "bvh-traversal", Vector2i(16), "" +
                    "layout(rgba32f, binding = 0) uniform image2D triangles;\n" +
                    "layout(rgba32f, binding = 1) uniform image2D blasNodes;\n" +
                    "layout(rgba32f, binding = 2) uniform image2D tlasNodes;\n" +
                    "layout(rgba32f, binding = 3) uniform image2D dst;\n" +
                    commonUniforms +
                    commonFunctions +
                    "#define nodes blasNodes\n" +
                    "#define BLAS_DEPTH $maxBLASDepth\n" +
                    "#define TLAS_DEPTH $maxTLASDepth\n" +
                    glslComputeDefines +
                    glslBLASIntersection +
                    glslTLASIntersection +
                    "void main(){\n" +
                    "   ivec2 uv = ivec2(gl_GlobalInvocationID.xy);\n" +
                    "   if(all(lessThan(uv,size))){\n" +
                    core + imageStore +
                    "   }\n" +
                    "}\n"
        ), triangles, blasNodes, tlasNodes
    )
}

fun createTLASBufferComputeShader(tlas: TLASNode): Pair<ComputeShader, Array<ComputeBuffer>> {

    val uniqueMeshes = HashSet<BLASNode>(tlas.countTLASLeaves())
    tlas.collectMeshes(uniqueMeshes)

    val meshes = uniqueMeshes
        .sortedByDescending { it.countNodes() } // complex meshes first for testing and consistency
        .toList()

    val triangles = BLASNode.createTriangleBuffer(meshes)
    val blasNodes = BLASNode.createBLASBuffer(meshes)
    val tlasNodes = tlas.createTLASBuffer() // needs to be created after blas nodes

    val maxTLASDepth = tlas.maxDepth()
    val maxBLASDepth = meshes.maxOf { it.maxDepth() }

    LOGGER.debug("Max TLAS depth: $maxTLASDepth, max BLAS depth: $maxBLASDepth")

    val shader = ComputeShader(
        "bvh-traversal", Vector2i(16), "" +
                bufferStructs +
                bufferLayouts +
                commonUniforms +
                commonFunctions +
                "#define BLAS_DEPTH $maxBLASDepth\n" +
                "#define TLAS_DEPTH $maxTLASDepth\n" +
                // glslComputeDefines +
                glslBLASIntersection2 +
                glslTLASIntersection2 +
                "void main(){\n" +
                "   ivec2 uv = ivec2(gl_GlobalInvocationID.xy);\n" +
                "   if(all(lessThan(uv,size))){\n" +
                core + imageStore +
                "   }\n" +
                "}\n"
    )

    return Pair(shader, arrayOf(triangles, blasNodes, tlasNodes.first, tlasNodes.second))
}
