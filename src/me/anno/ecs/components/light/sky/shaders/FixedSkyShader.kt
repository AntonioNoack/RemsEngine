package me.anno.ecs.components.light.sky.shaders

import me.anno.ecs.components.light.sky.shaders.SkyShader.Companion.approxExponents
import me.anno.ecs.components.light.sky.shaders.SkyShader.Companion.funcFBM
import me.anno.ecs.components.light.sky.shaders.SkyShader.Companion.funcHash
import me.anno.ecs.components.light.sky.shaders.SkyShader.Companion.funcNoise
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import org.joml.Vector3d
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

object FixedSkyShader : SkyShaderBase("FixedSky") {

    val fixedSkyCode = run {

        // https://github.com/shff/opengl_sky/blob/master/main.c
        val Br = 0.0025
        val Bm = 0.0003
        val g = 0.9800
        val nitrogen = Vector3d(0.650, 0.570, 0.475)
        val Kr = pow(nitrogen, -4.0).mul(Br)
        val Km = pow(nitrogen, -0.84).mul(Bm)
        val rayleighFactor = 3.0 / (8.0 * 3.1416)
        val invBrBm = 1.0 / (Br + Bm)
        val ggp2 = g * g + 2.0
        val mieConst = Kr * invBrBm
        val mieLinear = Km * ((1.0 - g * g) * invBrBm)
        val KrBr = Kr / Br
        val inv80Br = -1.0 / (80.0 * Br)

        val sunDir = Vector3d(-0.479f, 0.738f, 0.474f)
        "" +
                "float fbm(vec3); float fbm(vec2);\n" + // imports
                approxExponents +
                "vec3 getSkyColor(vec3 pos){\n" +
                "vec3 pos0 = pos;\n" +
                "pos.y = max(pos.y, 0.0);\n" +

                // Atmospheric Scattering
                "float mu = max(dot(pos, ${sunDir.v}), 0.0);\n" +
                "float rayleigh = $rayleighFactor * (1.0 + mu * mu);\n" +
                "vec3 mie = ${mieConst.v} + ${mieLinear.v} / ($ggp2 * pow(1.0 + $g * ($g - 2.0 * mu), 1.5));\n" +

                "float negPosY = -pos.y;\n" +
                "float deTerm0 = pos.y + ${sunDir.y * 4.0};\n" +
                "float deTerm3 = approxExp16(negPosY) + 0.1;\n" +
                "float deTerm1 = deTerm3 * $inv80Br;\n" +
                "vec3 deTerm2 = deTerm3 * ${KrBr.v};\n" +
                "vec3 day_extinction = exp(-exp(deTerm0 * deTerm1) * deTerm2) * approx1(negPosY) * approxExp2x4(negPosY);\n" +
                "day_extinction = clamp(day_extinction, 0.0, 1.0);\n" +
                "vec3 night_extinction = vec3(${0.2 - exp(max(sunDir.y, 0.0)) * 0.2});\n" +
                "vec3 extinction = mix(day_extinction, night_extinction, ${-sunDir.y * 0.2 + 0.5});\n" +

                "vec3 color = rayleigh * mie * extinction;\n" +
                // falloff towards downwards
                "if(pos0.y <= 0.0){\n" +
                "   color *= exp(pos0.y);\n" +
                "} else {\n" +
                "   vec3 pxz = vec3(pos.xz / max(pos.y, 0.001), 0.0);\n" +
                // Cirrus Clouds
                "   float density = smoothstep(0.6, 1.0, fbm(pxz * 2.0)) * 0.3;\n" +
                "   color = mix(color, extinction * 4.0, density * max(pos.y, 0.0));\n" +

                // Cumulus Clouds
                "   for (int i = 0; i < 3; i++){\n" +
                "       float density = smoothstep(0.2, 1.0, fbm((0.7 + float(i) * 0.01) * pxz));\n" +
                "       color = mix(color, extinction * density * 5.0, min(density, 1.0) * max(pos.y, 0.0));\n" +
                "   }\n" +
                "}\n" +
                "return max(color, vec3(0.0));\n" +
                "}\n"
    }

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        val stage = ShaderStage(
            "sky", listOf(
                Variable(GLSLType.V3F, "normal"),
                Variable(GLSLType.V3F, "finalNormal", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
                Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalEmissive", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalMotion", VariableMode.OUT),
                Variable(GLSLType.V1F, "finalOcclusion"),
                Variable(GLSLType.V4F, "currPosition"),
                Variable(GLSLType.V4F, "prevPosition"),
                Variable(GLSLType.V4F, "worldRot"),
            ), concatDefines(key).toString() +
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

    private fun pow(v: Vector3d, e: Double): Vector3d {
        return Vector3d(v.x.pow(e), v.y.pow(e), v.z.pow(e))
    }

    private val Vector3d.v: String
        get() = "vec3($x,$y,$z)"

    override fun getSkyColor(): String = fixedSkyCode
}
