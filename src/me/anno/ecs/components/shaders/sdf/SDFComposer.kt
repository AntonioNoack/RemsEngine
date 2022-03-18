package me.anno.ecs.components.shaders.sdf

import me.anno.ecs.components.mesh.TypeValue
import me.anno.engine.Ptr
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.ShaderLib.simplestVertexShader
import me.anno.gpu.shader.ShaderLib.uvList

object SDFComposer {

    const val dot2 = "" +
            "float dot2(vec2 v){ return dot(v,v); }\n"+
            "float dot2(vec3 v){ return dot(v,v); }\n"

    // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
    private const val raycasting = "" +
            // input: vec3 ray origin, vec3 ray direction
            // output: vec2(distance, materialId)
            "vec2 raycast(in vec3 ro, in vec3 rd){\n" +
            // ray marching
            "   vec2 res = vec2(-1.0);\n" +
            "   float tMin = distanceBounds.x, tMax = distanceBounds.y;\n" +
            "   float t = tMin;\n" +
            "   for(int i=0; i<sdfMaxSteps && t<tMax; i++){\n" +
            "     vec2 h = map(ro+rd*t);\n" +
            "     if(abs(h.x)<(sdfMaxRelativeError*t)){\n" + // allowed error grows with distance
            "       res = vec2(t,h.y);\n" +
            "       break;\n" +
            "     }\n" +
            // sdfReliability: sometimes, we need more steps, because the sdf is not reliable
            "     t += sdfReliability * h.x;\n" +
            "   }\n" +
            "   return res;\n" +
            "}\n"

    // http://iquilezles.org/www/articles/normalsSDF/normalsSDF.htm
    private const val normal = "" +
            "uniform int iFrame;\n" +
            "#define ZERO (min(iFrame,0))\n" +
            "vec3 calcNormal(in vec3 pos, float epsilon) {\n" +
            // inspired by tdhooper and klems - a way to prevent the compiler from inlining map() 4 times
            "  vec3 n = vec3(0.0);\n" +
            "  for(int i=ZERO; i<4; i++) {\n" +
            "      vec3 e = 0.5773*(2.0*vec3((((i+3)>>1)&1),((i>>1)&1),(i&1))-1.0);\n" +
            "      n += e*map(pos+e*epsilon).x;\n" +
            "  }\n" +
            "  return normalize(n);\n" +
            "}\n"

    fun createShader(tree: SDFComponent): Pair<HashMap<String, TypeValue>, BaseShader> {
        // todo compute approximate bounds on cpu, so we can save computations
        // todo traverse with larger step size on normals? :)
        // todo traverse over tree to assign material ids
        val functions = LinkedHashSet<String>()
        val uniforms = HashMap<String, TypeValue>()
        val shapeDependentShader = StringBuilder()
        tree.buildShader(shapeDependentShader, 0, VariableCounter(1), "res", uniforms, functions)
        return uniforms to BaseShader("raycasting", simplestVertexShader, uvList, "" +
                uniforms.entries.joinToString("") { (k, v) -> "uniform ${v.type.glslName} $k;\n" } +
                "uniform mat3 camMatrix;\n" +
                "uniform vec2 camScale;\n" +
                "uniform vec3 camPosition;\n" +
                "uniform vec2 distanceBounds;\n" +
                "uniform vec3 sunDir;\n" +
                "uniform int sdfMaxSteps;\n" +
                "uniform float sdfReliability, sdfNormalEpsilon, sdfMaxRelativeError;\n" + // [0,1.5], can be 1.0 in most cases; higher = faster convergence
                "uniform vec3 depthParams;\n" + // near, far, reversedZ
                "#define Infinity 1e20\n" +
                functions.joinToString("") +
                "vec2 map(in vec3 pos0){\n" +
                "   vec2 res = vec2(1e20,-1.0);\n" +
                // here comes the shape dependant shader
                shapeDependentShader.toString() +
                "   return res;\n" +
                "}\n" +
                raycasting +
                normal +
                // todo after that, we need the material-assignment functions
                "void main(){\n" +
                // define ray origin and ray direction from projection matrix
                "   vec3 dir = vec3((uv*2.0-1.0)*camScale, -1.0);\n" +
                "   dir = normalize(camMatrix * dir);\n" +
                "   vec2 ray = raycast(camPosition, dir);\n" +
                "   if(ray.y < 0.0) discard;\n" +
                "   vec3 hit = camPosition + ray.x * dir;\n" +
                "   vec3 normal = calcNormal(hit, sdfNormalEpsilon);\n" +
                // "   gl_FragColor = vec4(normal * .5+.5, 1.0);\n" +
                "   gl_FragColor = vec4(vec3(dot(normal,sunDir)*.4+.8), 1.0);\n" +
                // todo support non-reversed depth
                "   gl_FragDepth = depthParams.x / ray.x;\n" +
                "}")
    }

}