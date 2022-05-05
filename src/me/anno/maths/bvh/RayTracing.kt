package me.anno.maths.bvh

import me.anno.maths.Maths
import me.anno.maths.bvh.BLASNode.Companion.PIXELS_PER_BLAS_NODE
import me.anno.maths.bvh.BLASNode.Companion.PIXELS_PER_TRIANGLE
import org.joml.*
import kotlin.math.max

// to do gpu accelerated BVH traversal
// to do using compute shaders / glsl, how much performance can we get? :)
// to do ray-aabb-intersection:
// to do (x01 - rayOrigin) / rayDir, r0=min(v0,v1) and r1=max(v0,v1)
// to do hit if min(r1x,r1y,r1z) > max(r1x,r1y,r1z)
@Suppress("unused")
object RayTracing {

    const val glslIntersections = "" +
            // https://stackoverflow.com/questions/59257678/intersect-a-ray-with-a-triangle-in-glsl-c
            "float pointInOrOn(vec3 p1, vec3 p2, vec3 a, vec3 b){\n" +
            "    vec3 ba  = b-a;\n" +
            "    vec3 cp1 = cross(ba, p1 - a);\n" +
            "    vec3 cp2 = cross(ba, p2 - a);\n" +
            "    return step(0.0, dot(cp1, cp2));\n" +
            "}\n" +
            "void intersectTriangle(vec3 pos, vec3 dir, vec3 p0, vec3 p1, vec3 p2, out vec3 normal, inout float bestDistance){\n" +
            "   vec3 N = cross(p1-p0, p2-p0);\n" +
            "   float dnn = dot(dir, N);\n" +
            "   float distance = dot(p0-pos, N) / dnn;\n" +
            "   vec3 px = pos + dir * distance;\n" +
            // large, branchless and-concatenation
            "   bool hit = \n" +
            "       step(0.0, -dnn) *\n" + // is front face
            "       pointInOrOn(px, p0, p1, p2) *\n" +
            "       pointInOrOn(px, p1, p2, p0) *\n" +
            "       pointInOrOn(px, p2, p0, p1) *\n" +
            "       step(0.0, bestDistance - distance) > 0.0;\n" +
            "   bestDistance = hit ? distance : bestDistance;\n" +
            "   normal = hit ? N : normal;\n" +
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

    val glslBLASIntersection = "" +
            "void intersectBLAS(\n" +
            "       uint nodeIndex, vec3 pos, vec3 dir, vec3 invDir,\n" +
            "       inout vec3 normal, inout float distance, inout uint nodeCtr\n" +
            "){\n" +
            "   ivec2 nodeTexSize = imageSize(nodes);\n" +
            "   ivec2 triTexSize = imageSize(triangles);\n" +
            "   uint nextNodeStack[BLAS_DEPTH];\n" +
            "   uint stackIndex = 0;\n" +
            "   uint k=nodeCtr+1024;\n" +
            "   while(nodeCtr++<k){\n" + // could be k<bvh.count() or true or 2^depth
            // fetch node data
            "       uint pixelIndex = nodeIndex * $PIXELS_PER_BLAS_NODE;\n" +
            "       uint nodeX = pixelIndex % nodeTexSize.x;\n" +
            "       uint nodeY = pixelIndex / nodeTexSize.x;\n" +
            "       vec4 d0 = imageLoad(nodes, ivec2(nodeX,  nodeY));\n" +
            "       vec4 d1 = imageLoad(nodes, ivec2(nodeX+1,nodeY));\n" +
            "       if(intersectAABB(pos,invDir,d0.xyz,d1.xyz,distance)){\n" + // bounds check
            "           uvec2 v01 = floatBitsToUint(vec2(d0.a,d1.a));\n" +
            "           if(v01.y == 0){\n" +
            // to do: check closest one first like in https://github.com/mmp/pbrt-v3/blob/master/src/accelerators/bvh.h ?
            "               nextNodeStack[stackIndex++] = v01.x + nodeIndex;\n" + // mark other child for later
            "               nodeIndex++;\n" + // search child next
            "          } else {\n" +
            // this node is a leaf
            // check all triangles for intersections
            "               uint index = v01.x * $PIXELS_PER_TRIANGLE, end = index + v01.y * $PIXELS_PER_TRIANGLE;\n" +
            "               uint triX = index % triTexSize.x;\n" +
            "               uint triY = index / triTexSize.x;\n" +
            "               for(;index<end;index += $PIXELS_PER_TRIANGLE){\n" + // triangle index -> load triangle data
            "                   vec3 p0 = imageLoad(triangles, ivec2(triX,  triY)).rgb;\n" +
            "                   vec3 p1 = imageLoad(triangles, ivec2(triX+1,triY)).rgb;\n" +
            "                   vec3 p2 = imageLoad(triangles, ivec2(triX+2,triY)).rgb;\n" +
            "                   intersectTriangle(pos, dir, p0, p1, p2, normal, distance);\n" +
            "                   triX += $PIXELS_PER_TRIANGLE;\n" +
            "                   if(triX >= triTexSize.x){\n" + // switch to next row of data if needed
            "                       triX=0;triY++;\n" +
            "                   }\n" +
            "               }\n" + // next node
            "               if(stackIndex < 1) break;\n" +
            "              nodeIndex = nextNodeStack[--stackIndex];\n" +
            "          }\n" +
            "       } else {\n" + // next node
            "           if(stackIndex < 1) break;\n" +
            "           nodeIndex = nextNodeStack[--stackIndex];\n" +
            "       }\n" +
            "   }\n" +
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

    fun isRayIntersectingAABB(rayOrigin: Vector3f, invRayDirection: Vector3f, aabb: AABBf): Boolean {
        val rx = rayOrigin.x
        val ry = rayOrigin.y
        val rz = rayOrigin.z
        val rdx = invRayDirection.x
        val rdy = invRayDirection.y
        val rdz = invRayDirection.z
        val sx0 = (aabb.minX - rx) * rdx
        val sy0 = (aabb.minY - ry) * rdy
        val sz0 = (aabb.minZ - rz) * rdz
        val sx1 = (aabb.maxX - rx) * rdx
        val sy1 = (aabb.maxY - ry) * rdy
        val sz1 = (aabb.maxZ - rz) * rdz
        val nearX = Maths.min(sx0, sx1)
        val farX = max(sx0, sx1)
        val nearY = Maths.min(sy0, sy1)
        val farY = max(sy0, sy1)
        val nearZ = Maths.min(sz0, sz1)
        val farZ = max(sz0, sz1)
        val far = Maths.min(farX, Maths.min(farY, farZ))
        val near = max(max(nearX, max(nearY, nearZ)), 0f)
        return far >= near
    }

    fun isRayIntersectingAABB(
        rayOrigin: Vector3f,
        invRayDirection: Vector3f,
        aabb: AABBf,
        maxDistance: Float
    ): Boolean {
        val rx = rayOrigin.x
        val ry = rayOrigin.y
        val rz = rayOrigin.z
        val rdx = invRayDirection.x
        val rdy = invRayDirection.y
        val rdz = invRayDirection.z
        val sx0 = (aabb.minX - rx) * rdx
        val sy0 = (aabb.minY - ry) * rdy
        val sz0 = (aabb.minZ - rz) * rdz
        val sx1 = (aabb.maxX - rx) * rdx
        val sy1 = (aabb.maxY - ry) * rdy
        val sz1 = (aabb.maxZ - rz) * rdz
        val nearX = Maths.min(sx0, sx1)
        val farX = max(sx0, sx1)
        val nearY = Maths.min(sy0, sy1)
        val farY = max(sy0, sy1)
        val nearZ = Maths.min(sz0, sz1)
        val farZ = max(sz0, sz1)
        val far = Maths.min(farX, Maths.min(farY, farZ))
        val near = max(max(nearX, max(nearY, nearZ)), 0f)
        return far >= near && near < maxDistance
    }

    fun isRayIntersectingAABB(rayOrigin: Vector3d, invRayDirection: Vector3d, aabb: AABBd): Boolean {
        val rx = rayOrigin.x
        val ry = rayOrigin.y
        val rz = rayOrigin.z
        val rdx = invRayDirection.x
        val rdy = invRayDirection.y
        val rdz = invRayDirection.z
        val sx0 = (aabb.minX - rx) * rdx
        val sy0 = (aabb.minY - ry) * rdy
        val sz0 = (aabb.minZ - rz) * rdz
        val sx1 = (aabb.maxX - rx) * rdx
        val sy1 = (aabb.maxY - ry) * rdy
        val sz1 = (aabb.maxZ - rz) * rdz
        val nearX = Maths.min(sx0, sx1)
        val farX = max(sx0, sx1)
        val nearY = Maths.min(sy0, sy1)
        val farY = max(sy0, sy1)
        val nearZ = Maths.min(sz0, sz1)
        val farZ = max(sz0, sz1)
        val far = Maths.min(farX, Maths.min(farY, farZ))
        val near = max(max(nearX, max(nearY, nearZ)), 0.0)
        return far >= near
    }

    fun isRayIntersectingAABB(
        rayOrigin: Vector3d,
        invRayDirection: Vector3d,
        aabb: AABBd,
        maxDistance: Double
    ): Boolean {
        val rx = rayOrigin.x
        val ry = rayOrigin.y
        val rz = rayOrigin.z
        val rdx = invRayDirection.x
        val rdy = invRayDirection.y
        val rdz = invRayDirection.z
        val sx0 = (aabb.minX - rx) * rdx
        val sy0 = (aabb.minY - ry) * rdy
        val sz0 = (aabb.minZ - rz) * rdz
        val sx1 = (aabb.maxX - rx) * rdx
        val sy1 = (aabb.maxY - ry) * rdy
        val sz1 = (aabb.maxZ - rz) * rdz
        val nearX = Maths.min(sx0, sx1)
        val farX = max(sx0, sx1)
        val nearY = Maths.min(sy0, sy1)
        val farY = max(sy0, sy1)
        val nearZ = Maths.min(sz0, sz1)
        val farZ = max(sz0, sz1)
        val far = Maths.min(farX, Maths.min(farY, farZ))
        val near = max(max(nearX, max(nearY, nearZ)), 0.0)
        return far >= near && near < maxDistance
    }

    fun whereIsRayIntersectingAABB(rayOrigin: Vector3f, invRayDirection: Vector3f, aabb: AABBf): Float {
        val rx = rayOrigin.x
        val ry = rayOrigin.y
        val rz = rayOrigin.z
        val rdx = invRayDirection.x
        val rdy = invRayDirection.y
        val rdz = invRayDirection.z
        val sx0 = (aabb.minX - rx) * rdx
        val sy0 = (aabb.minY - ry) * rdy
        val sz0 = (aabb.minZ - rz) * rdz
        val sx1 = (aabb.maxX - rx) * rdx
        val sy1 = (aabb.maxY - ry) * rdy
        val sz1 = (aabb.maxZ - rz) * rdz
        val nearX = Maths.min(sx0, sx1)
        val farX = max(sx0, sx1)
        val nearY = Maths.min(sy0, sy1)
        val farY = max(sy0, sy1)
        val nearZ = Maths.min(sz0, sz1)
        val farZ = max(sz0, sz1)
        val far = Maths.min(farX, Maths.min(farY, farZ))
        val near = max(max(nearX, max(nearY, nearZ)), 0f)
        return if (far >= near) near else Float.POSITIVE_INFINITY
    }

    fun whereIsRayIntersectingAABB(rayOrigin: Vector3d, invRayDirection: Vector3d, aabb: AABBd): Double {
        val rx = rayOrigin.x
        val ry = rayOrigin.y
        val rz = rayOrigin.z
        val rdx = invRayDirection.x
        val rdy = invRayDirection.y
        val rdz = invRayDirection.z
        val sx0 = (aabb.minX - rx) * rdx
        val sy0 = (aabb.minY - ry) * rdy
        val sz0 = (aabb.minZ - rz) * rdz
        val sx1 = (aabb.maxX - rx) * rdx
        val sy1 = (aabb.maxY - ry) * rdy
        val sz1 = (aabb.maxZ - rz) * rdz
        val nearX = Maths.min(sx0, sx1)
        val farX = max(sx0, sx1)
        val nearY = Maths.min(sy0, sy1)
        val farY = max(sy0, sy1)
        val nearZ = Maths.min(sz0, sz1)
        val farZ = max(sz0, sz1)
        val far = Maths.min(farX, Maths.min(farY, farZ))
        val near = max(max(nearX, max(nearY, nearZ)), 0.0)
        return if (far >= near) near else Double.POSITIVE_INFINITY
    }

}