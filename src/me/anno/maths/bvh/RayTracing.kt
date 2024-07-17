package me.anno.maths.bvh

object RayTracing {

    const val intersectAABB = "" +
            "float minComp(vec3 v){ return min(v.x,min(v.y,v.z)); }\n" +
            "float maxComp(vec3 v){ return max(v.x,max(v.y,v.z)); }\n" +
            // taken from JOML
            "bool intersectAABB(vec3 pos, vec3 invDir, vec3 bMin, vec3 bMax, float maxDistance){\n" +
            "   bvec3 neg   = lessThan(invDir, vec3(0.0));\n" +
            "   vec3  close = mix(bMin,bMax,neg);\n" +
            "   vec3  far   = mix(bMax,bMin,neg);\n" +
            "   float tMin  = maxComp((close-pos)*invDir);\n" +
            "   float tMax  = minComp((far-pos)*invDir);\n" +
            "   return max(tMin, 0.0) <= min(tMax, maxDistance);\n" +
            "}\n" +
            "bool intersectAABB(vec3 pos, vec3 invDir, vec3 bMin, vec3 bMax, float s, float maxDistance){\n" +
            "   return intersectAABB(pos, invDir, bMin-s, bMax+s, maxDistance);\n" +
            "}\n"

    /**
     * https://stackoverflow.com/questions/59257678/intersect-a-ray-with-a-triangle-in-glsl-c
     * */
    const val intersectTriangle = "" +
            "float pointInOrOn(vec3 p1, vec3 p2, vec3 a, vec3 b){\n" +
            "    vec3 ba  = b-a;\n" +
            "    vec3 cp1 = cross(ba, p1 - a);\n" +
            "    vec3 cp2 = cross(ba, p2 - a);\n" +
            "    return step(0.0, dot(cp1, cp2));\n" +
            "}\n" +
            "void intersectTriangle(\n" +
            "   vec3 pos, vec3 dir, vec3 p0, vec3 p1, vec3 p2,\n" +
            "   inout vec3 normal, inout float bestDistance\n" +
            ") {\n" +
            "   vec3 N = cross(p1-p0, p2-p0);\n" +
            "   float dnn = dot(dir, N);\n" +
            "   float distance = dot(p0-pos, N) / dnn;\n" +
            // hit point
            "   vec3 px = pos + dir * distance;\n" +
            // large, branchless and-concatenation
            "   bool hit = \n" +
            "       step(dnn,0.0) *\n" + // is front face
            "       pointInOrOn(px, p0, p1, p2) *\n" +
            "       pointInOrOn(px, p1, p2, p0) *\n" +
            "       pointInOrOn(px, p2, p0, p1) *\n" +
            "       step(0.0, distance) *\n" +
            "       step(0.0, bestDistance - distance) > 0.0;\n" +
            "   bestDistance = hit ? distance : bestDistance;\n" +
            "   normal = hit ? N : normal;\n" +
            "}\n" +
            "vec4 intersectTriangle(\n" +
            "      vec3 pos, vec3 dir, vec3 p0, vec3 p1, vec3 p2,\n" +
            "      vec3 n0, vec3 n1, vec3 n2, inout vec3 normal, inout float bestDistance\n" +
            ") {\n" +
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
            "       step(dnn,0.0) *\n" + // is front face
            "       step(0.0,u)*step(0.0,v)*step(0.0,w) *\n" +
            "       step(0.0,distance) *\n" +
            "       step(distance,bestDistance) > 0.0;\n" +
            "   bestDistance = hit ? distance : bestDistance;\n" +
            "   normal = hit ? (u*n0+v*n1+w*n2) : normal;\n" +
            "   return vec4(u,v,w,hit?1.0:0.0);\n" +
            "}\n"

    const val glslIntersections = "" +
            intersectTriangle +
            intersectAABB

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

    const val glslRandomGen = "" +
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
}