package me.anno.maths.bvh.shader

import me.anno.utils.types.Strings.iff

open class BufferRTShaderLib : RTShaderLib() {

    open fun glslTriangleIntersection(): String {
        return "" +
                "Vertex v0 = vertices[index++], v1 = vertices[index++], v2 = vertices[index++];\n" +
                "intersectTriangle(pos, dir, v0.pos, v1.pos, v2.pos, v0.nor, v1.nor, v2.nor, normal, distance);\n"
    }

    override fun glslBLASIntersection(telemetry: Boolean): String {
        return "" +
                "void intersectBLAS(\n" +
                "   uint nodeIndex, vec3 pos, vec3 dir, vec3 invDir, inout float distance, inout vec3 normal\n" +
                "   ,inout uint blasCtr, inout uint trisCtr\n".iff(telemetry) +
                "){\n" +
                "   uint nextNodeStack[BLAS_DEPTH];\n" +
                "   uint stackIndex = 0u;\n" +
                "   uint k=0u;\n" +
                "   while(k++<512u){\n" + // could be k<bvh.count() or true or 2^depth
                // fetch node data
                "       BLASNode node = blasNodes[nodeIndex];\n" +
                "       if(intersectAABB(pos,invDir,node.min,node.max,distance)){\n" + // bounds check
                "           if(node.v1 < 3u){\n" +
                // check closest one first like in https://github.com/mmp/pbrt-v3/blob/master/src/accelerators/bvh.cpp
                "               if(dir[node.v1] > 0.0){\n" + // if !dirIsNeg[axis]
                "                   nextNodeStack[stackIndex++] = node.v0 + nodeIndex;\n" + // mark other child for later
                "                   nodeIndex++;\n" + // search child next
                "               } else {\n" +
                "                   nextNodeStack[stackIndex++] = nodeIndex + 1u;\n" + // mark other child for later
                "                   nodeIndex += node.v0;\n" + // search child next
                "               }\n" +
                "           } else {\n" +
                // this node is a leaf
                // check all triangles for intersections
                "               trisCtr += node.v1;\n".iff(telemetry) +
                "               for(uint index=node.v0,end=index+node.v1;index<end;){\n" + // triangle index -> load triangle data
                glslTriangleIntersection() +
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
                ") {\n" +
                "   uint nodeStack[TLAS_DEPTH];\n" +
                "   for(int i=TLAS_DEPTH-1;i>=0;i--) nodeStack[i]=0u;\n" +
                "   uint nodeIndex = 0u;\n" +
                "   uint stackIndex = 0u;\n" +
                "   worldNormal = vec3(0.0);\n" +
                "   vec3 worldInvDir = 1.0 / worldDir;\n" +
                "   uint k=0u;\n" +
                "   while(k++<512u){\n" + // start of tlas
                // fetch tlas node data
                "       TLASNode0 node = tlasNodes0[nodeIndex];\n" +
                "       if(intersectAABB(worldPos,worldInvDir,node.min,node.max,worldDistance)){\n" +
                "           uvec2 v01 = uvec2(node.v0,node.v1);\n" +
                "           if(v01.y < 3u){\n" + // tlas branch
                // check closest one first like in https://github.com/mmp/pbrt-v3/blob/master/src/accelerators/bvh.cpp
                "               if(worldDir[v01.y] > 0.0){\n" + // if !dirIsNeg[axis]
                "                   nodeStack[stackIndex++] = v01.x + nodeIndex;\n" + // mark the other child for later
                "                   nodeIndex++;\n" + // search child next
                "               } else {\n" +
                "                   nodeStack[stackIndex++] = nodeIndex + 1u;\n" + // mark the other child for later
                "                   nodeIndex += v01.x;\n" + // search child next
                "               }\n" +
                "           } else {\n" + // tlas leaf
                // transform ray into local coordinates
                "               TLASNode1 node1 = tlasNodes1[v01.y-3u];\n" +
                "               mat4x3 worldToLocal = loadMat4x3(node1.w2l0,node1.w2l1,node1.w2l2);\n" +
                "               vec3 localPos = matMul(worldToLocal, vec4(worldPos, 1.0));\n" +
                "               vec3 localDir0 = matMul(mat3x3(worldToLocal), worldDir);\n" +
                "               vec3 localDir = normalize(localDir0);\n" +
                // transform world distance into local coordinates
                "               float localDistance = worldDistance * length(localDir0);\n" +
                "               float localDistanceOld = localDistance;\n" +
                "               vec3 localNormal = vec3(0.0);\n" +
                "               intersectBLAS(\n" +
                "                   node.v0, localPos, localDir, 1.0 / localDir, localDistance, localNormal\n" +
                "                   ,blasCtr, trisCtr\n".iff(telemetry) +
                "               );\n" +
                "               if(localDistance < localDistanceOld){\n" + // we hit something
                // transform result into global coordinates
                // theoretically we could get z-fighting here
                "                   mat4x3 localToWorld = loadMat4x3(node1.l2w0,node1.l2w1,node1.l2w2);\n" +
                "                   float worldDistance1 = localDistance * length(matMul(localToWorld, vec4(localDir, 0.0)));\n" +
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