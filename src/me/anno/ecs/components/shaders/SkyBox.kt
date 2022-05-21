package me.anno.ecs.components.shaders

import me.anno.ecs.Component
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Function
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.mesh.Shapes
import me.anno.utils.types.AABBs.all
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3f

class SkyBox : MeshComponentBase() {

    // todo override raytracing for clicking: if ray goes far enough, let it click us

    @SerializedProperty
    var shader
        get() = material.shader
        set(value) {
            material.shader = value
        }

    @NotSerializedProperty
    val material = Material()

    init {
        material.shader = defaultShader
        material.shaderOverrides["cloudTime"] = TypeValue(GLSLType.V1F, 1f)
        material.shaderOverrides["cirrus"] = TypeValue(GLSLType.V1F, 0.4f)
        material.shaderOverrides["cumulus"] = TypeValue(GLSLType.V1F, 0.8f)
        material.shaderOverrides["sunDir"] = TypeValue(GLSLType.V3F, Vector3f(0.7f, 0.7f, 0f))
        materials = listOf(material.ref)
    }

    override fun getMesh() = mesh

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        aabb.all() // skybox is visible everywhere
        return true
    }

    override fun clone(): Component {
        val clone = SkyBox()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SkyBox
        clone.shader = shader
    }

    override val className = "SkyBox"

    companion object {

        val mesh = Shapes.smoothCube.back

        // https://github.com/shff/opengl_sky/blob/master/main.c
        val defaultShader = object : ECSMeshShader("sky") {

            override fun createVertexStage(isInstanced: Boolean, isAnimated: Boolean, colors: Boolean): ShaderStage {
                val defines = if (colors) "#define COLORS\n" else ""
                return ShaderStage(
                    "vertex",
                    createVertexVariables(isInstanced, isAnimated, colors),
                    "" +
                            defines +
                            "localPosition = 1e15 * sign(coords);\n" +
                            "finalPosition = localPosition;\n" +
                            "#ifdef COLORS\n" +
                            "   normal = normals;\n" +
                            "#endif\n" +
                            "gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                            ShaderLib.positionPostProcessing
                )
            }

            override fun createFragmentStage(isInstanced: Boolean, isAnimated: Boolean): ShaderStage {

                val funcNoise = "" +
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
                        "float fbm(vec3 p){\n" +
                        "  float f = 0.0;\n" +
                        "  f += noise(p) / 2;  p = fbmM * p;\n" +
                        "  f += noise(p) / 4;  p = fbmM * (p * 1.1);\n" +
                        "  f += noise(p) / 6;  p = fbmM * (p * 1.2);\n" +
                        "  f += noise(p) / 12; p = fbmM * (p * 1.3);\n" +
                        "  f += noise(p) / 24;\n" +
                        "  return f;\n" +
                        "}\n"

                val stage = ShaderStage(
                    "sky", listOf(
                        Variable(GLSLType.V3F, "normal", VariableMode.IN),
                        Variable(GLSLType.V3F, "finalNormal", VariableMode.OUT),
                        Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT),
                        Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
                        Variable(GLSLType.V3F, "finalEmissive", VariableMode.OUT),
                        Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
                        Variable(GLSLType.V1F, "finalRoughness", VariableMode.OUT),
                        Variable(GLSLType.V1F, "finalMetallic", VariableMode.OUT),
                        Variable(GLSLType.V3F, "sunDir"),
                        Variable(GLSLType.V1F, "cloudTime"),
                        Variable(GLSLType.V1F, "cirrus"), // 0.4
                        Variable(GLSLType.V1F, "cumulus"), // 0.8
                    ), "" +
                            "const float Br = 0.0025;\n" +
                            "const float Bm = 0.0003;\n" +
                            "const float g =  0.9800;\n" +
                            "const vec3 nitrogen = vec3(0.650, 0.570, 0.475);\n" +
                            "const vec3 Kr = Br / pow(nitrogen, vec3(4.0));\n" +
                            "const vec3 Km = Bm / pow(nitrogen, vec3(0.84));\n" +

                            // sky no longer properly defined for y > 0
                            "finalNormal = normalize(-normal);\n" +
                            "if(finalNormal.y < -0.2) discard;\n" +

                            "vec3 pos = finalNormal, color;\n" +
                            // todo override depth
                            // todo disable lighting somehow

                            // Atmospheric Scattering
                            "float mu = dot(finalNormal, sunDir);\n" +
                            "float rayleigh = 3.0 / (8.0 * 3.1416) * (1.0 + mu * mu);\n" +
                            "vec3 mie = (Kr + Km * (1.0 - g * g) / (2.0 + g * g) / pow(1.0 + g * g - 2.0 * g * mu, 1.5)) / (Br + Bm);\n" +

                            "vec3 day_extinction = exp(-exp(-((pos.y + sunDir.y * 4.0) * (exp(-pos.y * 16.0) + 0.1) / 80.0) / Br)" +
                            " * (exp(-pos.y * 16.0) + 0.1) * Kr / Br) * exp(-pos.y * exp(-pos.y * 8.0 ) * 4.0) * exp(-pos.y * 2.0) * 4.0;\n" +
                            "vec3 night_extinction = vec3(1.0 - exp(sunDir.y)) * 0.2;\n" +
                            "vec3 extinction = mix(day_extinction, night_extinction, -sunDir.y * 0.2 + 0.5);\n" +
                            "color.rgb = rayleigh * mie * extinction;\n" +

                            // Cirrus Clouds
                            "float density = smoothstep(1.0 - cirrus, 1.0, fbm(pos.xyz / pos.y * 2.0 + cloudTime * 0.05)) * 0.3;\n" +
                            "color.rgb = mix(color.rgb, extinction * 4.0, density * max(pos.y, 0.0));\n" +

                            // Cumulus Clouds
                            "for (int i = 0; i < 3; i++){\n" +
                            "  float density = smoothstep(1.0 - cumulus, 1.0, fbm((0.7 + float(i) * 0.01) * pos.xyz / pos.y + cloudTime * 0.3));\n" +
                            "  color.rgb = mix(color.rgb, extinction * density * 5.0, min(density, 1.0) * max(pos.y, 0.0));\n" +
                            "}\n" +

                            "finalColor = vec3(0.0);\n" +
                            "finalAlpha = 1.0;\n" +
                            "finalRoughness = 1.0;\n" +
                            "finalEmissive = clamp(color, 0.0, 1e3);\n" +
                            "finalPosition = finalNormal * 1e20;\n"
                )
                stage.functions.add(Function(funcHash + funcNoise + funcFBM))
                return stage
            }
        }

    }

}