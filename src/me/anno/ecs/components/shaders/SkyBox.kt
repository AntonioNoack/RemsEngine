package me.anno.ecs.components.shaders

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Group
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.TypeValueV3
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.mesh.Shapes
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.toRadians
import me.anno.utils.types.Vectors.safeNormalize
import org.joml.*

class SkyBox : MeshComponentBase() {

    // todo make this a light, such that all things can be lighted from it

    // todo override raytracing for clicking: if ray goes far enough, let it click us

    @SerializedProperty
    var shader
        get() = material.shader
        set(value) {
            material.shader = value
        }

    @NotSerializedProperty
    val material = Material()

    @SerializedProperty
    var sunRotation = Quaternionf()
        .rotateX(45f.toRadians()) // 45° from zenith
        .rotateZ(90f.toRadians()) // 90° from sunset, so noon
        set(value) {
            field.set(value)
                .normalize()
        }

    @Docs("Property for automatic daylight cycle; set the z-euler property, when sunRotation has an x-euler value and vice-versa")
    @SerializedProperty
    var sunSpeed = Quaternionf()

    @Range(0.0, 1.0)
    @Group("Cirrus")
    @SerializedProperty
    var cirrus = 0.4f

    @Group("Cirrus")
    @SerializedProperty
    var cirrusOffset = Vector2f()
        set(value) {
            field.set(value)
        }

    @Group("Cirrus")
    @SerializedProperty
    var cirrusSpeed = Vector2f(0.005f, 0f)
        set(value) {
            field.set(value)
        }

    @Range(0.0, 1.0)
    @Group("Cumulus")
    @SerializedProperty
    var cumulus = 0.8f

    @Group("Cumulus")
    @SerializedProperty
    var cumulusOffset = Vector2f()
        set(value) {
            field.set(value)
        }

    @Group("Cumulus")
    @SerializedProperty
    var cumulusSpeed = Vector2f(0.03f, 0f)
        set(value) {
            field.set(value)
        }

    @Type("Color3HDR")
    @SerializedProperty
    var nadirColor = Vector3f()
        set(value) {
            field.set(value)
            nadir.set(value.x, value.y, value.z, nadirSharpness)
        }

    @Range(0.0, 1e9)
    @SerializedProperty
    var nadirSharpness
        get() = nadir.w
        set(value) {
            nadir.w = value
        }

    @NotSerializedProperty
    private var nadir = Vector4f(0f, 0f, 0f, 1f)
        set(value) {
            field.set(value)
            nadirColor.set(value.x, value.y, value.z)
        }

    @SerializedProperty
    var sunBaseDir = Vector3f(1f, 0f, 0f)
        set(value) {
            field.set(value).safeNormalize()
        }

    init {
        material.shader = defaultShader
        material.shaderOverrides["cirrus"] = TypeValue(GLSLType.V1F) { cirrus }
        material.shaderOverrides["cumulus"] = TypeValue(GLSLType.V1F) { cumulus }
        material.shaderOverrides["nadir"] = TypeValue(GLSLType.V4F, nadir)
        material.shaderOverrides["cirrusOffset"] = TypeValue(GLSLType.V2F, cirrusOffset)
        material.shaderOverrides["cumulusOffset"] = TypeValue(GLSLType.V2F, cumulusOffset)
        material.shaderOverrides["sunDir"] = TypeValueV3(GLSLType.V3F, Vector3f()) {
            it.set(sunBaseDir).rotate(sunRotation)
        }
        materials = listOf(material.ref)
    }

    override fun onUpdate(): Int {
        val dt = Engine.deltaTime
        cirrusSpeed.mulAdd(dt, cirrusOffset, cirrusOffset)
        cumulusSpeed.mulAdd(dt, cumulusOffset, cumulusOffset)
        sunRotation.mul(JomlPools.quat4f.borrow().identity().slerp(sunSpeed, dt))
        return 1
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
        clone.sunRotation = sunRotation
        clone.sunBaseDir = sunBaseDir
        clone.cirrus = cirrus
        clone.cumulus = cumulus
        clone.cumulusSpeed = cumulusSpeed
        clone.cumulusOffset = cumulusOffset
        clone.cirrusSpeed = cirrusSpeed
        clone.cirrusOffset = cirrusOffset
        clone.nadir = nadir
        clone.sunSpeed.set(sunSpeed)
    }

    override val className = "SkyBox"

    companion object {

        val mesh = Shapes.smoothCube.back

        // https://github.com/shff/opengl_sky/blob/master/main.c
        val defaultShader = object : ECSMeshShader("sky") {

            override fun createVertexStage(
                isInstanced: Boolean,
                isAnimated: Boolean,
                colors: Boolean,
                motionVectors: Boolean,
                limitedTransform: Boolean
            ): ShaderStage {
                val defines = if (colors) "#define COLORS\n" else ""
                return ShaderStage(
                    "vertex",
                    createVertexVariables(isInstanced, isAnimated, colors, motionVectors, limitedTransform),
                    "" +
                            defines +
                            "localPosition = 1e15 * sign(coords);\n" +
                            "finalPosition = localPosition;\n" +
                            "#ifdef COLORS\n" +
                            "   normal = -sign(coords);\n" +
                            "#endif\n" +
                            "gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                            ShaderLib.positionPostProcessing
                )
            }

            override fun createFragmentStage(
                isInstanced: Boolean,
                isAnimated: Boolean,
                motionVectors: Boolean
            ): ShaderStage {

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
                        "  f += noise(p) / 2;  p = fbmM * p;\n" +
                        "  f += noise(p) / 4;  p = fbmM * (p * 1.1);\n" +
                        "  f += noise(p) / 6;  p = fbmM * (p * 1.2);\n" +
                        "  f += noise(p) / 12; p = fbmM * (p * 1.3);\n" +
                        "  f += noise(p) / 24;\n" +
                        "  return f;\n" +
                        "}\n" +
                        "float fbm(vec2 p){ return fbm(vec3(p, 0.0)); }\n"

                // todo the red clouds in the night sky are a bit awkward
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
                        Variable(GLSLType.V1F, "cirrus"), // 0.4
                        Variable(GLSLType.V1F, "cumulus"), // 0.8
                        Variable(GLSLType.V2F, "cirrusOffset"), // 0.05
                        Variable(GLSLType.V2F, "cumulusOffset"), // 0.3
                        Variable(GLSLType.V4F, "nadir"),
                    ), "" +
                            "const float Br = 0.0025;\n" +
                            "const float Bm = 0.0003;\n" +
                            "const float g =  0.9800;\n" +
                            "const vec3 nitrogen = vec3(0.650, 0.570, 0.475);\n" +
                            "const vec3 Kr = Br / pow(nitrogen, vec3(4.0));\n" +
                            "const vec3 Km = Bm / pow(nitrogen, vec3(0.84));\n" +

                            // sky no longer properly defined for y > 0
                            "finalNormal = normalize(-normal);\n" +

                            "vec3 pos = finalNormal;\n" +
                            "pos.y = max(pos.y, 0.0);\n" +
                            // todo override depth
                            // todo disable lighting

                            // Atmospheric Scattering
                            "float mu = max(dot(pos, sunDir), 0.0);\n" +
                            "float rayleigh = 3.0 / (8.0 * 3.1416) * (1.0 + mu * mu);\n" +
                            "vec3 mie = (Kr + Km * (1.0 - g * g) / (2.0 + g * g) / pow(1.0 + g * g - 2.0 * g * mu, 1.5)) / (Br + Bm);\n" +

                            "vec3 day_extinction = exp(-exp(-((pos.y + sunDir.y * 4.0) * (exp(-pos.y * 16.0) + 0.1) / 80.0) / Br)" +
                            " * (exp(-pos.y * 16.0) + 0.1) * Kr / Br) * exp(-pos.y * exp(-pos.y * 8.0 ) * 4.0) * exp(-pos.y * 2.0) * 4.0;\n" +
                            "vec3 night_extinction = vec3(0.2 - exp(max(sunDir.y, 0.0)) * 0.2);\n" +
                            "vec3 extinction = mix(clamp(day_extinction, 0.0, 1.0), night_extinction, -sunDir.y * 0.2 + 0.5);\n" +
                            "vec3 color = rayleigh * mie * extinction;\n" +

                            // Cirrus Clouds
                            "vec2 pxz = pos.xz / max(pos.y, 0.001);\n" +
                            "float density = smoothstep(1.0 - cirrus, 1.0, fbm(pxz * 2.0 + cirrusOffset)) * 0.3;\n" +
                            "color = mix(color, extinction * 4.0, density * max(pos.y, 0.0));\n" +

                            // Cumulus Clouds
                            "for (int i = 0; i < 3; i++){\n" +
                            "  float density = smoothstep(1.0 - cumulus, 1.0, fbm((0.7 + float(i) * 0.01) * pxz + cumulusOffset));\n" +
                            "  color = mix(color, extinction * density * 5.0, min(density, 1.0) * max(pos.y, 0.0));\n" +
                            "}\n" +

                            // falloff towards downwards
                            "if(finalNormal.y < 0.0){\n" +
                            "   color = mix(nadir.rgb, color, exp(finalNormal.y * nadir.w));\n" +
                            "}\n" +

                            "finalNormal = -finalNormal;\n" +
                            "finalColor = vec3(0.0);\n" +
                            "finalAlpha = 1.0;\n" +
                            "finalRoughness = 1.0;\n" +
                            "finalEmissive = color;\n" +
                            "finalPosition = finalNormal * 1e20;\n"
                )
                stage.add(funcHash)
                stage.add(funcNoise)
                stage.add(funcFBM)
                return stage
            }
        }

    }

}