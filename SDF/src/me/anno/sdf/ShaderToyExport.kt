package me.anno.sdf

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MaterialCache
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.SkyShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.matMul
import me.anno.sdf.shapes.SDFShape
import me.anno.utils.structures.arrays.BooleanArrayList
import org.joml.Planef
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4f
import org.joml.Vector4i
import kotlin.math.max

object ShaderToyExport {
    fun createScript(tree: SDFComponent): String {

        // todo make everything as static (non-dynamic) as possible

        val stateDesc = "/* Place this into 'Buffer A' (plus icon on the left to 'Image')\n" +
                " * iChannel0 must be 'Buffer A'\n" +
                " * iChannel1 must be 'Keyboard' */\n"

        val runDesc = "\n\n/* Place this in the 'Image' slot; iChannel0 must be 'Buffer A' */\n"

        val stateScript = "" +
                // adjusted from https://www.shadertoy.com/view/ttVfDc
                // iChannel0 of buffer A must be itself
                // iChannel1 of buffer A must be the keyboard
                // todo calculate pos and dir
                "vec4 rotateYX(float angleY, float angleX) {\n" +
                "   float sx = sin(angleX * 0.5);\n" +
                "   float cx = cos(angleX * 0.5);\n" +
                "   float sy = sin(angleY * 0.5);\n" +
                "   float cy = cos(angleY * 0.5);\n" +
                "   float yx = cy * sx;\n" +
                "   float yy = sy * cx;\n" +
                "   float yz = sy * sx;\n" +
                "   float yw = cy * cx;\n" +
                "   return vec4(yx, yy, -yz, yw);\n" +
                "}\n" +
                "void mainImage(out vec4 col, in vec2 uv) {\n" +
                "   if(uv.y > 1.0 || uv.x > 4.0) discard;\n" +
                "   col = vec4(0.0);\n" +
                "   vec4 iMouseLast      = texelFetch(iChannel0, ivec2(0, 0), 0);\n" +
                "   vec4 iMouseAccuLast  = texelFetch(iChannel0, ivec2(1, 0), 0);\n" +
                "   float kW = texelFetch(iChannel1, ivec2(0x57, 0), 0).x;\n" +
                "   float kA = texelFetch(iChannel1, ivec2(0x41, 0), 0).x;\n" +
                "   float kS = texelFetch(iChannel1, ivec2(0x53, 0), 0).x;\n" +
                "   float kD = texelFetch(iChannel1, ivec2(0x44, 0), 0).x;\n" +
                "   float kQ = texelFetch(iChannel1, ivec2(0x51, 0), 0).x;\n" +
                "   float kE = texelFetch(iChannel1, ivec2(0x45, 0), 0).x;\n" +
                "   vec2 mouseDelta = 0.005 * (iMouse.z > 0.0 && iMouseLast.z > 0.0 ? iMouse.xy - iMouseLast.xy : vec2(0.0));\n" +
                "   vec2 newMouseDelta = iMouseAccuLast.xy + mouseDelta;\n" +
                "   newMouseDelta.y = clamp(newMouseDelta.y, -1.57, +1.57);\n" +
                "   vec4 rot = rotateYX(newMouseDelta.x, -newMouseDelta.y);\n" +
                // *iDeltaTime is somehow unknown :/
                "   vec3 pos = texelFetch(iChannel0,ivec2(2,0),0).xyz + 0.3 * quatRot(vec3(kD-kA,kE-kQ,kS-kW),rot);\n" +
                "   if(uv.x == 0.5) col = iMouse;\n" +
                "   if(uv.x == 1.5) col = vec4(newMouseDelta,0,1);\n" +
                "   if(uv.x == 2.5) col = vec4(pos,1.0);\n" +
                "   if(uv.x == 3.5) col = rot;\n" +
                "}\n\n"

        // done compute approximate bounds on cpu, so we can save computations
        // done traverse with larger step size on normals? normals don't use traversal
        // done traverse over tree to assign material ids? no, the programmer does that to reduce cases
        val functions = LinkedHashSet<String>()
        val uniforms = HashMap<String, TypeValue>()
        val shapeDependentShader = StringBuilder()
        val seeds = ArrayList<String>()
        tree.buildShader(shapeDependentShader, 0, VariableCounter(1), 0, uniforms, functions, seeds)

        uniforms["sdfReliability"] = TypeValue(GLSLType.V1F, tree.globalReliability)
        uniforms["sdfNormalEpsilon"] = TypeValue(GLSLType.V1F, tree.normalEpsilon)
        uniforms["sdfMaxRelativeError"] = TypeValue(GLSLType.V1F, tree.maxRelativeError)
        uniforms["maxSteps"] = TypeValue(GLSLType.V1I, tree.maxSteps)
        uniforms["distanceBounds"] = TypeValue(GLSLType.V2F, Vector2f(0.01f, 1000f))

        val materials = tree.sdfMaterials.map { MaterialCache[it] }
        val builder = StringBuilder(max(1, materials.size) * 128)

        val needsSwitch = materials.size > 1
        if (needsSwitch) builder
            .append("switch(clamp(int(ray.y),0,")
            .append(materials.lastIndex)
            .append(")){\n")

        // register all materials that use textures
        val materialsUsingTextures = BooleanArrayList(materials.size)
        if (materials.isNotEmpty()) {
            tree.simpleTraversal(false) {
                if (it is SDFComponent && it.positionMappers.any { pm -> pm is me.anno.sdf.random.SDFRandomUV }) {
                    it.simpleTraversal(false) { c ->
                        if (c is SDFShape) {
                            if (c.materialId < materialsUsingTextures.size)
                                materialsUsingTextures.set(c.materialId)
                        }
                        false
                    }
                }
                false
            }
        }

        for (index in 0 until max(materials.size, 1)) {
            if (needsSwitch) builder.append("case ").append(index).append(":\n")
            val material = materials.getOrNull(index) ?: Material.defaultMaterial
            builder.append("finalColor = vec3(${material.diffuseBase.x},${material.diffuseBase.y},${material.diffuseBase.z});\n")
            builder.append("finalAlpha = ${material.diffuseBase.w};\n")
            builder.append("finalMetallic = ${material.metallicMinMax.y};\n")
            builder.append("finalRoughness = ${material.roughnessMinMax.y};\n")
            builder.append("finalEmissive = vec3(${material.emissiveBase.x},${material.emissiveBase.y},${material.emissiveBase.z});\n")
            if (needsSwitch) builder.append("break;\n")
        }
        if (needsSwitch) builder.append("}\n")

        val defines = uniforms.entries.joinToString("\n") {
            "#define ${it.key} ${
                when (val v = it.value.value) {
                    is Vector2f -> "vec2(${v.x},${v.y})"
                    is Vector3f -> "vec3(${v.x},${v.y},${v.z})"
                    is Vector4f -> "vec4(${v.x},${v.y},${v.z},${v.w})"
                    is Quaternionf -> "vec4(${v.x},${v.y},${v.z},${v.w})"
                    is Planef -> "vec4(${v.dirX},${v.dirY},${v.dirZ},${v.distance})"
                    is Vector2i -> "ivec2(${v.x},${v.y})"
                    is Vector3i -> "ivec3(${v.x},${v.y},${v.z})"
                    is Vector4i -> "ivec4(${v.x},${v.y},${v.z},${v.w})"
                    else -> v.toString()
                }
            }"
        }

        val trace = "vec4 trace(vec3 localPos, vec3 localDir,\n" +
                "out vec3 finalColor, out float finalAlpha, out vec3 finalNormal,\n" +
                "out float finalMetallic, out float finalRoughness, out vec3 finalEmissive) {\n" +
                "   int steps; finalAlpha = 0.0;\n" +
                "   vec4 ray = raycast(localPos, localDir, steps);\n" +
                "   finalColor = vec3(0.0);\n" +
                "   if(ray.y >= 0.0) {\n" +
                "      vec3 localHit = localPos + ray.x * localDir;\n" +
                "      finalNormal = calcNormal(localPos, localDir, localHit, ray.x * sdfNormalEpsilon, ray.x);\n" +
                // step by step define all material properties
                "      vec2 uv = ray.zw;\n" +
                builder.toString() +
                "       return ray;\n" +
                "   }\n" +
                "   return ray;\n" +
                "}\n"

        val res = StringBuilder()
        res.append(stateDesc)
        res.append(ShaderLib.quatRot)
        res.append(stateScript)

        res.append(runDesc)
        res.append("#define ZERO min(int(iFrame),0)\n")
        res.append(defines).append("\n")
        res.append(SDFComposer.sdfConstants)
        res.append(matMul)
        functions.add(ShaderLib.quatRot)
        functions.add(SkyShader.funcHash)
        functions.add(SkyShader.funcNoise)
        functions.add(SkyShader.funcFBM)
        for (func in functions) res.append(func)
        res.append("vec4 map(vec3 ro, vec3 rd, vec3 pos0, float t){\n")
        res.append("   vec4 res0; vec3 dir0=rd; float sca0=1.0/t; vec2 uv=vec2(0.0);\n")
        res.append(shapeDependentShader)
        res.append("   return res0;\n}\n")
        res.append(SDFComposer.raycasting)
        res.append(SDFComposer.normal)

        val skyColor = "" +
                "#define sunDir normalize(vec3(0.0, 0.7, 0.7))\n" +
                "#define nadir vec4(0,0,0,1)\n" +
                "#define cirrusOffset vec3(0.005 * iTime, 0, 0)\n" +
                "#define cumulusOffset vec3(0.03 * iTime, 0, 0)\n" +
                "#define sphericalSky false\n" +
                "#define cirrus 0.4\n" +
                "#define cumulus 0.8\n" +
                "vec3 sunColor;\n" +
                SkyShader("").getSkyColor()

        val ambientOcclusion = "" +
                "// ambient occlusion from from https://www.shadertoy.com/view/XlXyD4, mhnewman\n" +
                "#define aoIter 8\n" +
                "#define aoDist 0.07\n" +
                "#define aoPower 2.0\n" +
                "const vec3 aoDir[12] = vec3[12](\n" +
                "   vec3(0.357407, 0.357407, 0.862856),\n" +
                "   vec3(0.357407, 0.862856, 0.357407),\n" +
                "   vec3(0.862856, 0.357407, 0.357407),\n" +
                "   vec3(-0.357407, 0.357407, 0.862856),\n" +
                "   vec3(-0.357407, 0.862856, 0.357407),\n" +
                "   vec3(-0.862856, 0.357407, 0.357407),\n" +
                "   vec3(0.357407, -0.357407, 0.862856),\n" +
                "   vec3(0.357407, -0.862856, 0.357407),\n" +
                "   vec3(0.862856, -0.357407, 0.357407),\n" +
                "   vec3(-0.357407, -0.357407, 0.862856),\n" +
                "   vec3(-0.357407, -0.862856, 0.357407),\n" +
                "   vec3(-0.862856, -0.357407, 0.357407)\n" +
                ");\n" +
                "mat3 alignMatrix(vec3 dir) {\n" +
                "    vec3 f = normalize(dir);\n" +
                "    vec3 s = normalize(cross(f, vec3(0.48, 0.6, 0.64)));\n" +
                "    vec3 u = cross(s, f);\n" +
                "    return mat3(u, s, f);\n" +
                "}\n" +
                "float ao(vec3 p, vec3 n, float dist0) {\n" +
                "    float dist = dist0;\n" +
                "    float occ = 1.0;\n" +
                "    for (int i = ZERO; i < aoIter; i++) {\n" +
                "        occ = min(occ, map(p, n, p + dist * n, dist).x / dist);\n" +
                "        dist *= aoPower;\n" +
                "    }\n" +
                "    return max(occ, 0.0);\n" +
                "}\n" +
                // lightEffectI(1.0,y) - lightEffectI(0.0,y) = 1/(y+1) - 1/(y+2)
                "float lightEffectI(float x, float y) {\n" +
                "   return pow(x,y+1.0) * (1.0/(y+1.0) - x/(y+2.0));\n" +
                "}\n" +
                "float lightEffect(float x, float y) {\n" +
                "   return pow(x,y) / (1.0/(y+1.0) - 1.0/(y+2.0));\n" +
                "}\n" +
                "vec3 calcLight(\n" +
                "   vec3 p, vec3 n, float dist0,\n" +
                "   float metallic, float roughness, vec3 v\n" +
                "){\n" +
                "   mat3 mat = alignMatrix(n);\n" +
                "   vec3 col = vec3(0.0);\n" +
                "   sunColor = vec3(0.0);\n" + // disabled for ambient occlusion
                "   for (int i = ZERO; i < 12; i++) {\n" +
                "       vec3 sampleDir = mix(n, mat * aoDir[i], roughness);\n" +
                "       col += ao(p, sampleDir, dist0) * mix(vec3(1.0), getSkyColor(sampleDir), metallic);\n" +
                "   }\n" +
                // add sun, if we can see it
                "   sunColor = vec3(500.0);\n" +
                "   if(dot(n, sunDir) > 0.0){\n" +
                "       col += ao(p, sunDir, dist0) * sunColor * mix(\n" +
                "           dot(n, sunDir),\n" + // diffuse case
                "           lightEffect(max(dot(sunDir, reflect(v, n)), 0.0), 1.0 + 256.0 * (1.0 - roughness)),\n" + // metallic case
                "           metallic\n" +
                "       );\n" +
                "   }\n" +
                "   return col / 12.0;\n" +
                "}\n"

        // todo test ambient occlusion
        // todo add bloom (?)
        //  https://www.shadertoy.com/view/lsBfRc

        // trace a ray :)
        val mainScript = "" +
                "void mainImage(out vec4 col, in vec2 uv){\n" +
                "   uv = (uv-iResolution.xy*0.5) / iResolution.y;" +
                "   vec3 color,normal,emissive;\n" +
                "   float alpha,metallic,roughness;\n" +
                "   vec3 pos = texelFetch(iChannel0,ivec2(2,0),0).xyz;\n" +
                "   vec4 rot = texelFetch(iChannel0,ivec2(3,0),0);\n" +
                "   vec3 dir = quatRot(normalize(vec3(uv,-1.0)),rot);\n" +
                "   vec4 ray = trace(pos,dir,color,alpha,normal,metallic,roughness,emissive);\n" +
                // shading
                "   if(ray.x > 0.0){\n" + // hit something
                // todo if metallic=1,roughness=0, only use a single sample, and path-trace further
                "       vec3 reflectedDir = reflect(dir,normal);\n" +
                "       vec3 light = calcLight(pos + dir * ray.x, normal, aoDist * ray.x, metallic, roughness, dir);\n" +
                "       vec3 color = pow(color * light,vec3(2.2)) + pow(emissive,vec3(2.2));\n" +
                "       color = pow(color,vec3(1.0/2.2));\n" +
                "       col = vec4(color, 1.0);\n" + // hit something
                "   } else {\n" + // sky
                "       sunColor = vec3(500.0);\n" +
                "       col = vec4(getSkyColor(dir),1.0);\n" +
                "   }\n" +
                "   col.rgb *= 5.0;\n" +
                "   col.rgb /= 1.0 + max(col.r,max(col.g,col.b));\n" + // hdr -> sdr
                "}\n"

        res.append(trace)
        res.append(skyColor)
        res.append(ambientOcclusion)
        res.append(mainScript)

        return res.toString()
    }
}