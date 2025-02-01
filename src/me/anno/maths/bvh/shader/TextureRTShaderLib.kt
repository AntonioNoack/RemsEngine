package me.anno.maths.bvh.shader

import me.anno.maths.bvh.BLASTexture.PIXELS_PER_BLAS_NODE
import me.anno.maths.bvh.TLASTexture.PIXELS_PER_TLAS_NODE
import me.anno.maths.bvh.TriangleTexture.PIXELS_PER_VERTEX
import me.anno.utils.types.Strings.iff

open class TextureRTShaderLib(
    val pixelsPerVertex: Int = PIXELS_PER_VERTEX,
    val pixelsPerTLASNode: Int = PIXELS_PER_TLAS_NODE
) : RTShaderLib() {

    open fun glslTriangleIntersection(): String {
        return if (pixelsPerVertex >= 2) {
            "" +
                    "vec4 p0 = LOAD_PIXEL(triangles, ivec2(triX, triY));\n" +
                    "vec4 n0 = LOAD_PIXEL(triangles, ivec2(triX+1u, triY));\n" +
                    "vec4 p1 = LOAD_PIXEL(triangles, ivec2(triX+${pixelsPerVertex}u,triY));\n" +
                    "vec4 n1 = LOAD_PIXEL(triangles, ivec2(triX+${pixelsPerVertex + 1}u,triY));\n" +
                    "vec4 p2 = LOAD_PIXEL(triangles, ivec2(triX+${pixelsPerVertex * 2}u,triY));\n" +
                    "vec4 n2 = LOAD_PIXEL(triangles, ivec2(triX+${pixelsPerVertex * 2 + 1}u,triY));\n" +
                    "intersectTriangle(pos, dir, p0.rgb, p1.rgb, p2.rgb, n0.rgb, n1.rgb, n2.rgb, normal, distance);\n"
        } else {
            "" +
                    "vec4 p0 = LOAD_PIXEL(triangles, ivec2(triX, triY));\n" +
                    "vec4 p1 = LOAD_PIXEL(triangles, ivec2(triX+${pixelsPerVertex}u,triY));\n" +
                    "vec4 p2 = LOAD_PIXEL(triangles, ivec2(triX+${pixelsPerVertex * 2}u,triY));\n" +
                    "intersectTriangle(pos, dir, p0.rgb, p1.rgb, p2.rgb, normal, distance);\n"
        }
    }

    override fun glslBLASIntersection(telemetry: Boolean): String {
        return "" +
                "void intersectBLAS(\n" +
                "   uint nodeIndex, vec3 pos, vec3 dir, vec3 invDir, inout float distance, inout vec3 normal\n" +
                "   ,inout uint blasCtr, inout uint trisCtr\n".iff(telemetry) +
                "){\n" +
                "   uvec2 nodeTexSize = TEXTURE_SIZE(nodes);\n" +
                "   uvec2 triTexSize  = TEXTURE_SIZE(triangles);\n" +
                "   uint nextNodeStack[BLAS_DEPTH];\n" +
                "   uint stackIndex = 0u;\n" +
                "   uint k=0u;\n" +
                "   while(k++<512u){\n" + // could be k<bvh.count() or true or 2^depth
                // fetch node data
                "       uint pixelIndex = nodeIndex * ${PIXELS_PER_BLAS_NODE}u;\n" +
                "       uint nodeX = pixelIndex % nodeTexSize.x;\n" +
                "       uint nodeY = pixelIndex / nodeTexSize.x;\n" +
                "       vec4 d0 = LOAD_PIXEL(nodes, ivec2(nodeX,   nodeY));\n" +
                "       vec4 d1 = LOAD_PIXEL(nodes, ivec2(nodeX+1u,nodeY));\n" +
                "       if(intersectAABB(pos,invDir,d0.xyz,d1.xyz,distance)){\n" + // bounds check
                "           uvec2 v01 = floatBitsToUint(vec2(d0.a,d1.a));\n" +
                "           if(v01.y < 3u) {\n" +
                // check closest one first like in https://github.com/mmp/pbrt-v3/blob/master/src/accelerators/bvh.cpp
                "               if(dir[v01.y] > 0.0){\n" + // if !dirIsNeg[axis]
                "                   nextNodeStack[stackIndex++] = v01.x + nodeIndex;\n" + // mark other child for later
                "                   nodeIndex++;\n" + // search child next
                "               } else {\n" +
                "                   nextNodeStack[stackIndex++] = nodeIndex + 1u;\n" + // mark other child for later
                "                   nodeIndex += v01.x;\n" + // search child next
                "               }\n" +
                "           } else {\n" +
                // this node is a leaf
                // check all triangles for intersections
                "               trisCtr += v01.y;\n".iff(telemetry) +
                "               uint index = v01.x, end = index + v01.y;\n" +
                "               uint triX = index % triTexSize.x;\n" +
                "               uint triY = index / triTexSize.x;\n" +
                "               for(;index<end;index += ${pixelsPerVertex * 3}u){\n" + // triangle index -> load triangle data
                glslTriangleIntersection() +
                "                   triX += ${pixelsPerVertex * 3}u;\n" +
                "                   if(triX >= triTexSize.x){\n" + // switch to next row of data if needed
                "                       triX=0u;triY++;\n" +
                "                   }\n" +
                "               }\n" + // next node
                "               if(stackIndex < 1u) break;\n" +
                "               nodeIndex = nextNodeStack[--stackIndex];\n" +
                "          }\n" +
                "       } else {\n" + // next node
                "           if(stackIndex < 1u) break;\n" +
                "           nodeIndex = nextNodeStack[--stackIndex];\n" +
                "       }\n" +
                "   }\n" +
                "   blasCtr += k;\n".iff(telemetry) +
                "}\n"
    }

    override fun glslTLASIntersection(telemetry: Boolean): String {
        return "" +
                "void intersectTLAS(\n" +
                "   vec3 worldPos, vec3 worldDir, inout float worldDistance, out vec3 worldNormal\n" +
                "   ,inout uint tlasCtr, inout uint blasCtr, inout uint trisCtr\n".iff(telemetry) +
                "){\n" +
                "   uint nodeStack[TLAS_DEPTH];\n" +
                "   for(int i=TLAS_DEPTH-1;i>=0;i--) nodeStack[i]=0u;\n" +
                "   uint nodeIndex = 0u;\n" +
                "   uint stackIndex = 0u;\n" +
                "   worldNormal = vec3(0.0);\n" +
                "   uvec2 tlasTexSize = TEXTURE_SIZE(tlasNodes);\n" +
                "   vec3 worldInvDir = 1.0 / worldDir;\n" +
                "   uint k=0u;\n" +
                "   while(k++<512u){\n" + // start of tlas
                // fetch tlas node data
                "       uint pixelIndex = nodeIndex * ${pixelsPerTLASNode}u;\n" +
                "       uint nodeX = pixelIndex % tlasTexSize.x;\n" +
                "       uint nodeY = pixelIndex / tlasTexSize.x;\n" +
                "       vec4 d0 = LOAD_PIXEL(tlasNodes, ivec2(nodeX,   nodeY));\n" +
                "       vec4 d1 = LOAD_PIXEL(tlasNodes, ivec2(nodeX+1u,nodeY));\n" + // tlas bounds check
                "       if(intersectAABB(worldPos,worldInvDir,d0.xyz,d1.xyz,worldDistance)){\n" +
                "           uvec2 v01 = floatBitsToUint(vec2(d0.a,d1.a));\n" +
                "           if(v01.y < 3u){\n" + // tlas branch
                // check closest one first like in https://github.com/mmp/pbrt-v3/blob/master/src/accelerators/bvh.cpp
                "               if(worldDir[v01.y] > 0.0){\n" + // if !dirIsNeg[axis]
                "                   nodeStack[stackIndex++] = v01.x + nodeIndex;\n" + // mark other child for later
                "                   nodeIndex++;\n" + // search child next
                "               } else {\n" +
                "                   nodeStack[stackIndex++] = nodeIndex + 1u;\n" + // mark other child for later
                "                   nodeIndex += v01.x;\n" + // search child next
                "               }\n" +
                "           } else {\n" + // tlas leaf
                // load more data and then transform ray into local coordinates
                "               vec4 d10 = LOAD_PIXEL(tlasNodes, ivec2(nodeX+2u,nodeY));\n" +
                "               vec4 d11 = LOAD_PIXEL(tlasNodes, ivec2(nodeX+3u,nodeY));\n" +
                "               vec4 d12 = LOAD_PIXEL(tlasNodes, ivec2(nodeX+4u,nodeY));\n" +
                "               mat4x3 worldToLocal = loadMat4x3(d10,d11,d12);\n" +
                // transform ray into local coordinates
                "               vec3 localPos = matMul(worldToLocal, vec4(worldPos, 1.0));\n" +
                "               vec3 localDir0 = matMul(worldToLocal, vec4(worldDir, 0.0));\n" +
                "               vec3 localDir = normalize(localDir0);\n" +
                // transform world distance into local coordinates
                "               float localDistance = worldDistance * length(localDir0);\n" +
                "               float localDistanceOld = localDistance;\n" +
                "               vec3 localNormal = vec3(0.0);\n" +
                "               intersectBLAS(\n" +
                "                   v01.x, localPos, localDir, 1.0 / localDir, localDistance, localNormal\n" +
                "                   ,blasCtr, trisCtr\n".iff(telemetry) +
                "               );\n" +
                "               if(localDistance < localDistanceOld){\n" + // we hit something
                "                   vec4 d20 = LOAD_PIXEL(tlasNodes, ivec2(nodeX+5u,nodeY));\n" +
                "                   vec4 d21 = LOAD_PIXEL(tlasNodes, ivec2(nodeX+6u,nodeY));\n" +
                "                   vec4 d22 = LOAD_PIXEL(tlasNodes, ivec2(nodeX+7u,nodeY));\n" +
                "                   mat4x3 localToWorld = loadMat4x3(d20,d21,d22);\n" +
                // transform result into global coordinates
                // theoretically we could get z-fighting here
                "                   float worldDistance1 = localDistance * length(matMul(localToWorld, vec4(localDir,0.0)));\n" +
                "                   if(worldDistance1 < worldDistance){\n" + // could be false by numerical errors
                // transform hit normal into world coordinates
                "                       worldDistance = worldDistance1;\n" +
                "                       worldNormal = matMul(localToWorld, vec4(localNormal,0.0));\n" +
                "                   }\n" +
                "               }\n" + // end of blas; get next tlas node
                "               if(stackIndex < 1u) break;\n" +
                "               nodeIndex = nodeStack[--stackIndex];\n" +
                "           }\n" +
                "       } else {\n" + // next tlas node
                "           if(stackIndex < 1u) break;\n" +
                "           nodeIndex = nodeStack[--stackIndex];\n" +
                "       }\n" +
                "   }\n" + // end of tlas
                "   tlasCtr += k;\n".iff(telemetry) +
                "}\n"
    }
}