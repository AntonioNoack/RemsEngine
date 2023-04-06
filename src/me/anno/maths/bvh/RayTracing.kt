package me.anno.maths.bvh

import me.anno.maths.bvh.BLASNode.Companion.PIXELS_PER_BLAS_NODE
import me.anno.maths.bvh.BLASNode.Companion.PIXELS_PER_TRIANGLE
import me.anno.maths.bvh.BLASNode.Companion.PIXELS_PER_VERTEX
import org.joml.*
import kotlin.math.max
import kotlin.math.min

@Suppress("unused")
object RayTracing {

    val glslIntersections = "" +
            // https://stackoverflow.com/questions/59257678/intersect-a-ray-with-a-triangle-in-glsl-c
            "float pointInOrOn(vec3 p1, vec3 p2, vec3 a, vec3 b){\n" +
            "    vec3 ba  = b-a;\n" +
            "    vec3 cp1 = cross(ba, p1 - a);\n" +
            "    vec3 cp2 = cross(ba, p2 - a);\n" +
            "    return step(0.0, dot(cp1, cp2));\n" +
            "}\n" +
            "void intersectTriangle(vec3 pos, vec3 dir, vec3 p0, vec3 p1, vec3 p2, inout vec3 normal, inout float bestDistance){\n" +
            "   vec3 N = cross(p1-p0, p2-p0);\n" +
            "   float dnn = dot(dir, N);\n" +
            "   float distance = dot(p0-pos, N) / dnn;\n" +
            // hit point
            "   vec3 px = pos + dir * distance;\n" +
            // large, branchless and-concatenation
            "   bool hit = \n" +
            "       step(0.0, -dnn) *\n" + // is front face
            "       pointInOrOn(px, p0, p1, p2) *\n" +
            "       pointInOrOn(px, p1, p2, p0) *\n" +
            "       pointInOrOn(px, p2, p0, p1) *\n" +
            "       step(0.0, distance) *\n" +
            "       step(0.0, bestDistance - distance) > 0.0;\n" +
            "   bestDistance = hit ? distance : bestDistance;\n" +
            "   normal = hit ? N : normal;\n" +
            "}\n" +
            "void intersectTriangle(\n" +
            "      vec3 pos, vec3 dir, vec3 p0, vec3 p1, vec3 p2,\n" +
            "      vec3 n0, vec3 n1, vec3 n2, inout vec3 normal, inout float bestDistance\n" +
            "){\n" +
            "   vec3 N = cross(p1-p0, p2-p0);\n" +
            "   float dnn = dot(dir, N);\n" +
            "   float distance = dot(p0-pos, N) / dnn;\n" +
            "   vec3 px = pos + dir * distance;\n" +
            // https://gamedev.stackexchange.com/questions/23743/whats-the-most-efficient-way-to-find-barycentric-coordinates
            "   vec3 v0 = p1-p0, v1 = p2-p0, v2 = px-p0;\n" +
            "   float d00=dot(v0,v0),d01=dot(v0,v1),d11=dot(v1,v1),d20=dot(v2,v0),d21=dot(v2,v1);\n" +
            "   float d = 1.0/(d00*d11-d01*d01);\n" +
            "   float v = (d11 * d20 - d01 * d21) * d;\n" +
            "   float w = (d00 * d21 - d01 * d20) * d;\n" +
            "   float u = 1.0 - v - w;\n" +
            // large, branchless and-concatenation
            "   bool hit = \n" +
            "       step(0.0, -dnn) *\n" + // is front face
            "       step(0.0,u)*step(0.0,v)*step(0.0,w) *\n" +
            "       step(0.0, distance) *\n" +
            "       step(0.0, bestDistance - distance) > 0.0;\n" +
            "   bestDistance = hit ? distance : bestDistance;\n" +
            "   normal = hit ? (u*n0+v*n1+w*n2) : normal;\n" +
            "}\n" +
            "float minComp(vec3 v){ return min(v.x,min(v.y,v.z)); }\n" +
            "float maxComp(vec3 v){ return max(v.x,max(v.y,v.z)); }\n" +
            "bool intersectAABB(vec3 pos, vec3 invDir, vec3 bMin, vec3 bMax, float maxDistance){\n" +
            "   bvec3 neg   = lessThan(invDir, vec3(0.0));\n" +
            "   vec3  close = mix(bMin,bMax,neg);\n" +
            "   vec3  far   = mix(bMax,bMin,neg);\n" +
            "   float tMin  = maxComp((close-pos)*invDir);\n" +
            "   float tMax  = minComp((far-pos)*invDir);\n" +
            "   return max(tMin, 0.0) <= min(tMax, maxDistance);\n" +
            "}\n"

    const val loadMat4x3 = "" +
            "mat4x3 loadMat4x3(vec4 a, vec4 b, vec4 c){\n" +
            "   return mat4x3(\n" +
            "       a.xyz,\n" +
            "       vec3(a.w, b.xy),\n" +
            "       vec3(b.zw, c.x),\n" +
            "       c.yzw\n" +
            "   );\n" +
            "}\n"

    const val coloring = "" +
            "vec3 coloring(float t){\n" +
            "   float base = fract(t)*.8+.2;\n" +
            "   vec3 a = vec3(97.0,130.0,234.0)/255.0;\n" +
            "   vec3 b = vec3(220.0,94.0,75.0)/255.0;\n" +
            "   vec3 c = vec3(221.0,220.0,219.0)/255.0;\n" +
            "   t = clamp(t*0.05,0.0,2.0);\n" +
            "   return base * (t < 1.0 ? mix(a,b,t) : mix(b,c,t-1.0));\n" +
            "}\n"

    // compatibility defines for compute & graphics
    const val glslGraphicsDefines = "" +
            "#define TEXTURE_SIZE(name) uvec2(textureSize(name,0))\n" +
            "#define LOAD_PIXEL(name,uv) texelFetch(name,uv,0)\n"

    const val glslComputeDefines = "" +
            "#define TEXTURE_SIZE(name) imageSize(name)\n" +
            "#define LOAD_PIXEL(name,uv) imageLoad(name,uv)\n"

    val glslRandomGen = "" +
            // from https://github.com/SlightlyMad/SimpleDxrPathTracer
            // http://reedbeta.com/blog/quick-and-easy-gpu-random-numbers-in-d3d11/
            // https://github.com/nvpro-samples/optix_prime_baking/blob/master/random.h
            "uint initRand(uint seed){\n" +
            "   seed = ((seed ^ 61u) ^ (seed >> 16u)) * 9u;\n" +
            "   seed = (seed ^ (seed >> 4)) * 0x27d4eb2du;\n" +
            "   return seed ^ (seed >> 15);\n" +
            "}\n" +
            "uint initRand(uint seed1, uint seed2){\n" +
            "   uint seed = 0u;\n" +
            "   for(uint i = 0u; i < 16u; i++){\n" +
            "       seed  += 0x9e3779b9u;\n" +
            "       seed1 += ((seed2 << 4) + 0xa341316cu) ^ (seed2 + seed) ^ ((seed2 >> 5) + 0xc8013ea4u);\n" +
            "       seed2 += ((seed1 << 4) + 0xad90777du) ^ (seed1 + seed) ^ ((seed1 >> 5) + 0x7e95761eu);\n" +
            "   }\n" +
            "   return seed1;\n" +
            "}\n" +
            "float nextRand(inout uint seed){\n" +
            "   seed = 1664525u * seed + 1013904223u;\n" +
            "   return float(seed & 0xffffffu) / float(0x01000000);\n" +
            "}\n" +
            "vec2 nextRand2(inout uint seed){\n" +
            "   return vec2(nextRand(seed),nextRand(seed));\n" +
            "}\n" +
            "vec3 nextRand3(inout uint seed){\n" +
            "   return vec3(nextRand(seed),nextRand(seed),nextRand(seed));\n" +
            "}\n" +
            "vec3 nextRandS3(inout uint seed){\n" +
            "   for(int i=0;i<10;i++){\n" +
            "       vec3 v = vec3(nextRand(seed),nextRand(seed),nextRand(seed));\n" +
            "       if(dot(v,v) <= 1.0) return v;\n" +
            "   }\n" +
            "   return vec3(1.0,0.0,0.0);\n" +
            "}\n"

    val glslBLASIntersection = "" +
            "void intersectBLAS(\n" +
            "    uint nodeIndex, vec3 pos, vec3 dir, vec3 invDir,\n" +
            "    inout vec3 normal, inout float distance, inout uint nodeCtr\n" +
            "){\n" +
            "   uvec2 nodeTexSize = TEXTURE_SIZE(nodes);\n" +
            "   uvec2 triTexSize  = TEXTURE_SIZE(triangles);\n" +
            "   uint nextNodeStack[BLAS_DEPTH];\n" +
            "   uint stackIndex = 0u;\n" +
            "   uint k=nodeCtr + 512u;\n" +
            "   while(nodeCtr++<k){\n" + // could be k<bvh.count() or true or 2^depth
            // fetch node data
            "       uint pixelIndex = nodeIndex * ${PIXELS_PER_BLAS_NODE}u;\n" +
            "       uint nodeX = pixelIndex % nodeTexSize.x;\n" +
            "       uint nodeY = pixelIndex / nodeTexSize.x;\n" +
            "       vec4 d0 = LOAD_PIXEL(nodes, ivec2(nodeX,   nodeY));\n" +
            "       vec4 d1 = LOAD_PIXEL(nodes, ivec2(nodeX+1u,nodeY));\n" +
            "       if(intersectAABB(pos,invDir,d0.xyz,d1.xyz,distance)){\n" + // bounds check
            "           uvec2 v01 = floatBitsToUint(vec2(d0.a,d1.a));\n" +
            "           if(v01.y < 3u){\n" +
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
            "               uint index = v01.x, end = index + v01.y;\n" +
            "               uint triX = index % triTexSize.x;\n" +
            "               uint triY = index / triTexSize.x;\n" +
            "               for(;index<end;index += ${PIXELS_PER_TRIANGLE}u){\n" + // triangle index -> load triangle data
            "#if $PIXELS_PER_VERTEX >= 2\n" +
            "                   vec3 p0 = LOAD_PIXEL(triangles, ivec2(triX, triY)).rgb;\n" +
            "                   vec3 n0 = LOAD_PIXEL(triangles, ivec2(triX+1u, triY)).rgb;\n" +
            "                   vec3 p1 = LOAD_PIXEL(triangles, ivec2(triX+${PIXELS_PER_VERTEX}u,triY)).rgb;\n" +
            "                   vec3 n1 = LOAD_PIXEL(triangles, ivec2(triX+${PIXELS_PER_VERTEX + 1}u,triY)).rgb;\n" +
            "                   vec3 p2 = LOAD_PIXEL(triangles, ivec2(triX+${PIXELS_PER_VERTEX * 2}u,triY)).rgb;\n" +
            "                   vec3 n2 = LOAD_PIXEL(triangles, ivec2(triX+${PIXELS_PER_VERTEX * 2 + 1}u,triY)).rgb;\n" +
            "                   intersectTriangle(pos, dir, p0, p1, p2, n0, n1, n2, normal, distance);\n" +
            "#else\n" +
            "                   vec3 p0 = LOAD_PIXEL(triangles, ivec2(triX, triY)).rgb;\n" +
            "                   vec3 p1 = LOAD_PIXEL(triangles, ivec2(triX+${PIXELS_PER_VERTEX}u,triY)).rgb;\n" +
            "                   vec3 p2 = LOAD_PIXEL(triangles, ivec2(triX+${PIXELS_PER_VERTEX * 2}u,triY)).rgb;\n" +
            "                   intersectTriangle(pos, dir, p0, p1, p2, normal, distance);\n" +
            "#endif\n" +
            "                   triX += ${PIXELS_PER_TRIANGLE}u;\n" +
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
            "}\n"

    val glslTLASIntersection = "" +
            "uint intersectTLAS(vec3 worldPos, vec3 worldDir, inout float worldDistance, out vec3 worldNormal){\n" +
            "   uint nodeStack[TLAS_DEPTH];\n" +
            "   for(int i=TLAS_DEPTH-1;i>=0;i--) nodeStack[i]=0u;\n" +
            "   uint nodeIndex = 0u;\n" +
            "   uint stackIndex = 0u;\n" +
            "   worldNormal = vec3(0.0);\n" +
            "   uvec2 tlasTexSize = TEXTURE_SIZE(tlasNodes);\n" +
            "   vec3 worldInvDir = 1.0 / worldDir;\n" +
            "   uint k=512u,nodeCtr=0u;\n" +
            "   uint numIntersections=0u;\n" +
            "   while(k-- > 0u){\n" + // start of tlas
            // fetch tlas node data
            "       uint pixelIndex = nodeIndex * ${TLASNode.PIXELS_PER_TLAS_NODE}u;\n" +
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
            "           numIntersections++;\n" +
            // load more data and then transform ray into local coordinates
            "               vec4 d10 = LOAD_PIXEL(tlasNodes, ivec2(nodeX+2u,nodeY));\n" +
            "               vec4 d11 = LOAD_PIXEL(tlasNodes, ivec2(nodeX+3u,nodeY));\n" +
            "               vec4 d12 = LOAD_PIXEL(tlasNodes, ivec2(nodeX+4u,nodeY));\n" +
            "               mat4x3 worldToLocal = loadMat4x3(d10,d11,d12);\n" +
            // transform ray into local coordinates
            "               vec3 localPos = worldToLocal * vec4(worldPos, 1.0);\n" +
            "               vec3 localDir0 = mat3x3(worldToLocal) * worldDir;\n" +
            "               vec3 localDir = normalize(localDir0);\n" +
            "               vec3 localInvDir = 1.0 / localDir;\n" +
            // transform world distance into local coordinates
            "               float localDistance = worldDistance * length(localDir0);\n" +
            "               float localDistanceOld = localDistance;\n" +
            "               vec3 localNormal = vec3(0.0);\n" +
            "               intersectBLAS(v01.x, localPos, localDir, localInvDir, localNormal, localDistance, nodeCtr);\n" +
            "               if(localDistance < localDistanceOld){\n" + // we hit something
            "                   vec4 d20 = LOAD_PIXEL(tlasNodes, ivec2(nodeX+5u,nodeY));\n" +
            "                   vec4 d21 = LOAD_PIXEL(tlasNodes, ivec2(nodeX+6u,nodeY));\n" +
            "                   vec4 d22 = LOAD_PIXEL(tlasNodes, ivec2(nodeX+7u,nodeY));\n" +
            "                   mat4x3 localToWorld = loadMat4x3(d20,d21,d22);\n" +
            // transform result into global coordinates
            // theoretically we could get z-fighting here
            "                   float worldDistance1 = localDistance * length(mat3x3(localToWorld) * localDir);\n" +
            "                   if(worldDistance1 < worldDistance){\n" + // could be false by numerical errors
            // transform hit normal into world coordinates
            "                       worldDistance = worldDistance1;\n" +
            "                       worldNormal = mat3x3(localToWorld) * localNormal;\n" +
            "                   }\n" +
            "               }\n" + // end of blas; get next tlas node*/
            "               if(stackIndex < 1u) break;\n" +
            "               nodeIndex = nodeStack[--stackIndex];\n" +
            "           }\n" +
            "       } else {\n" + // next tlas node
            "           if(stackIndex < 1u) break;\n" +
            "           nodeIndex = nodeStack[--stackIndex];\n" +
            "       }\n" +
            "   }\n" + // end of tlas
            "   return numIntersections;\n" +
            "}\n"

    fun Vector4f.dot(v: Vector3f, w: Float) = dot(v.x, v.y, v.z, w)

    /*fun rayTriangleIntersection(
        rayOrigin: Vector3f,
        rayDirection: Vector3f,
        planeAB: Vector4f, // normal ( = (A+B)/2 towards (A+B+C)/3), -dot(position ( = (A+B)/2 ), normal)
        planeBC: Vector4f,
        planeCA: Vector4f,
        planeTri: Vector4f
    ): Boolean {
        // to do project ray onto plane
        val hitPoint = rayOrigin + rayDirection * (planeTri.dot(rayDirection) - planeTri.dot(rayOrigin))
        return planeAB.dot(hitPoint) >= 0f && planeBC.dot(hitPoint) >= 0f && planeCA.dot(hitPoint) >= 0f
    }*/

}