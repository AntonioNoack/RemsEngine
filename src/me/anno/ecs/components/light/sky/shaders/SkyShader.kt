package me.anno.ecs.components.light.sky.shaders

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

open class SkyShader(name: String) : SkyShaderBase(name) {
    companion object {
        val funcNoise = "" +
                "float hash(float);\n" +
                "float noise(vec3 x){\n" +
                "  vec3 f = fract(x);\n" +
                "  float n = dot(floor(x), vec3(1.0, 157.0, 113.0));\n" +
                "  return mix(mix(mix(hash(n +   0.0), hash(n +   1.0), f.x),\n" +
                "                 mix(hash(n + 157.0), hash(n + 158.0), f.x), f.y),\n" +
                "             mix(mix(hash(n + 113.0), hash(n + 114.0), f.x),\n" +
                "                 mix(hash(n + 270.0), hash(n + 271.0), f.x), f.y), f.z);\n" +
                "}\n"
        val funcHash = "" +
                "float hash(float n){\n" +
                "  return fract(sin(n) * 43758.5453123);\n" +
                "}\n"
        val funcFBM = "" +
                "const mat3 fbmM = mat3(0.0, 1.75,  1.3, -1.8, 0.8, -1.1, -1.3, -1.1, 1.4);\n" +
                "float noise(vec3);" +
                "float fbm(vec3 p){\n" +
                "  float f = 0.0;\n" +
                "  f += noise(p) * 0.50; p = matMul(fbmM, p);\n" +
                "  f += noise(p) * 0.25; p = matMul(fbmM, (p * 1.1));\n" +
                "  f += noise(p) * 0.16; p = matMul(fbmM, (p * 1.2));\n" +
                "  f += noise(p) * 0.08; p = matMul(fbmM, (p * 1.3));\n" +
                "  f += noise(p) * 0.04;\n" +
                "  return f;\n" +
                "}\n" +
                "float fbm(vec2 p){ return fbm(vec3(p, 0.0)); }\n"
    }
    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {

        // todo the red clouds in the night sky are a bit awkward
        val stage = ShaderStage(
            "sky", listOf(
                Variable(GLSLType.V3F, "normal"),
                Variable(GLSLType.V3F, "finalNormal", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
                Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalEmissive", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalMotion", VariableMode.OUT),
                Variable(GLSLType.V4F, "currPosition"),
                Variable(GLSLType.V4F, "prevPosition"),
                Variable(GLSLType.V3F, "sunDir"),
                Variable(GLSLType.V1F, "cirrus"), // 0.4
                Variable(GLSLType.V1F, "cumulus"), // 0.8
                Variable(GLSLType.V3F, "cirrusOffset"), // 0.05
                Variable(GLSLType.V3F, "cumulusOffset"), // 0.3
                Variable(GLSLType.V4F, "nadir"),
                Variable(GLSLType.V4F, "worldRot"),
                Variable(GLSLType.V1B, "sphericalSky"),
                Variable(GLSLType.V3F, "sunColor"),
            ), concatDefines(key).toString() +
                    // sky no longer properly defined for y > 0
                    "finalNormal = normalize(-normal);\n" +
                    "#ifdef COLORS\n" +
                    "   finalColor = vec3(0.0);\n" +
                    "   finalEmissive = getSkyColor(quatRot(finalNormal, worldRot));\n" +
                    "#endif\n" +
                    "finalNormal = -finalNormal;\n" +
                    "finalPosition = finalNormal * 1e20;\n" +
                    finalMotionCalculation
        )
        stage.add(quatRot)
        stage.add(funcHash)
        stage.add(funcNoise)
        stage.add(funcFBM)
        stage.add(getSkyColor())
        return listOf(stage)
    }

    override fun getSkyColor(): String {
        // todo define night stars somehow...
        //  how do we get a natural looking star distribution?
        //  -> make sky rendering into multiple parts, so we can use particle systems? :)
        // https://github.com/shff/opengl_sky/blob/master/main.c
        return "" +
                "float fbm(vec3); float fbm(vec2);\n" + // imports
                "vec3 getSkyColor(vec3 pos){\n" +
                "vec3 pos0 = pos;\n" +

                "const float Br = 0.0025;\n" +
                "const float Bm = 0.0003;\n" +
                "const float g =  0.9800;\n" +
                "const vec3 nitrogen = vec3(0.650, 0.570, 0.475);\n" +
                "const vec3 Kr = Br / pow(nitrogen, vec3(4.0));\n" +
                "const vec3 Km = Bm / pow(nitrogen, vec3(0.84));\n" +
                "pos.y = max(pos.y, 0.0);\n" +

                // Atmospheric Scattering
                "float mu = max(dot(pos, sunDir), 0.0);\n" +
                "float rayleigh = 3.0 / (8.0 * 3.1416) * (1.0 + mu * mu);\n" +
                "vec3 mie = (Kr + Km * (1.0 - g * g) / ((2.0 + g * g) * pow(1.0 + g * (g - 2.0 * mu), 1.5))) / (Br + Bm);\n" +

                "vec3 day_extinction = exp(-exp(-((pos.y + sunDir.y * 4.0) * \n" +
                "   (exp(-pos.y * 16.0) + 0.1) / 80.0) / Br) * \n" +
                "   (exp(-pos.y * 16.0) + 0.1) * Kr / Br) * \n" +
                "   exp(-pos.y * exp(-pos.y * 8.0 ) * 4.0) * \n" +
                "   exp(-pos.y * 2.0) * 4.0;\n" +
                "vec3 night_extinction = vec3(0.2 - exp(max(sunDir.y, 0.0)) * 0.2);\n" +
                "vec3 extinction = mix(clamp(day_extinction, 0.0, 1.0), night_extinction, -sunDir.y * 0.2 + 0.5);\n" +

                "vec3 color = rayleigh * mie * extinction;\n" +

                // falloff towards downwards
                "if(pos0.y < 0.0){\n" +
                "   color = mix(nadir.rgb, color, exp(pos0.y * nadir.w));\n" +
                "} else if(pos.y > 0.0){\n" +
                // Cirrus Clouds
                "   vec3 pxz = sphericalSky ? pos0 : vec3(pos.xz / max(pos.y, 0.001), 0.0);\n" +
                "   float density = smoothstep(1.0 - cirrus, 1.0, fbm(pxz * 2.0 + cirrusOffset)) * 0.3;\n" +
                "   color = mix(color, extinction * 4.0, density * max(pos.y, 0.0));\n" +

                // Cumulus Clouds
                "   for (int i = 0; i < 3; i++){\n" +
                "     float density = smoothstep(1.0 - cumulus, 1.0, fbm((0.7 + float(i) * 0.01) * pxz + cumulusOffset));\n" +
                "     color = mix(color, extinction * density * 5.0, min(density, 1.0) * max(pos.y, 0.0));\n" +
                "   }\n" +
                "}\n" +
                "return max(color, vec3(0.0));\n" +
                "}"
    }
}