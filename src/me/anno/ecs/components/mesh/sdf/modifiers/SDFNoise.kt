package me.anno.ecs.components.mesh.sdf.modifiers

import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.appendUniform
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.fract
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

@Suppress("unused")
class SDFNoise : DistanceMapper() {

    enum class Type(val id: Int, val glslName: String) {
        PERLIN(0, "perlinNoise"),
        VORONOI(1, "voronoiNoise")
    }

    var type = Type.PERLIN
        set(value) {
            if (field != value) {
                field = value
                invalidateShader()
            }
        }

    private val params = Vector4f(8f, 0.5f, 1.8f, 0.5f)
    private val voronoiParams = Vector2f(2f, 1f)

    @Range(0.0, 16.0)
    var octaves
        get() = params.x.toInt()
        set(value) {
            params.x = max(value, 0).toFloat()
        }

    @Range(0.0, 1.0)
    var amplitudeDecrease
        get() = params.y
        set(value) {
            params.y = value
        }

    @Range(1.0, 1e9)
    var frequencyIncrease
        get() = params.z
        set(value) {
            params.z = value
        }

    var offset
        get() = params.w
        set(value) {
            params.w = value
            invalidateBounds()
        }

    var amplitude = 0.1f
        set(value) {
            if (field != value) {
                field = value
                invalidateBounds()
            }
        }

    var frequency = Vector3f(1f)
        set(value) {
            field.set(value)
        }

    val notVoronoi get() = type != Type.VORONOI

    @HideInInspector("notVoronoi")
    @Range(0.0, 4.0)
    var voronoiLevel
        get() = voronoiParams.x
        set(value) {
            voronoiParams.x = value
        }

    @Range(0.0, 100.0)
    var power
        get() = voronoiParams.x
        set(value) {
            voronoiParams.y = value
        }


    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        dstIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        val type = type
        functions.add(generalNoise)
        functions.add(perlinNoise)
        builder.append("res").append(dstIndex).append(".x-=")
        builder.append(type.glslName)
        builder.append("(pos").append(posIndex).append("*")
        builder.appendUniform(uniforms, frequency).append(',')
        builder.appendUniform(uniforms, GLSLType.V1F) { amplitude }.append(',')
        builder.appendUniform(uniforms, params).append(',')
        builder.appendUniform(uniforms, voronoiParams).append(");\n")
    }

    override fun applyTransform(bounds: AABBf) {
        bounds.addMargin(abs(amplitude) + offset)
    }

    override fun calcTransform(pos: Vector4f, distance: Float): Float {
        // hopefully correct
        val params = params
        var delta = params.w
        var b = amplitude
        val py = params.y
        val pz = params.z
        val frequency = frequency
        var fx = pos.x * frequency.x
        var fy = pos.y * frequency.y
        var fz = pos.z * frequency.z
        for (i in 0 until octaves) {
            delta += b * noise(fx, fy, fz)
            b *= py
            fx *= pz
            fy *= pz
            fz *= pz
        }
        return distance - delta
    }

    override fun clone(): PrefabSaveable {
        val clone = SDFNoise()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFNoise
        clone.params.set(params)
        clone.amplitude = amplitude
        clone.frequency = frequency
    }

    override val className get() = "SDFNoise"

    private fun sdfHash(n: Float) = fract(sin(n) * 753.5453123f)

    private fun smoothstep(x: Float) = x * x * (3f - 2f * x)

    private fun noise(x: Float, y: Float, z: Float): Float {
        val px = floor(x)
        val py = floor(y)
        val pz = floor(z)
        val wx = x - px
        val wy = y - py
        val wz = z - pz
        val ux = smoothstep(wx)
        val uy = smoothstep(wy)
        val uz = smoothstep(wz)
        val n = px + py * 157f + pz * 113f
        // points of the cube
        val a = sdfHash(n)
        val b = sdfHash(n + 1f)
        val c = sdfHash(n + 157f)
        val d = sdfHash(n + 158f)
        val e = sdfHash(n + 113f)
        val f = sdfHash(n + 114f)
        val g = sdfHash(n + 270f)
        val h = sdfHash(n + 271f)
        val k1 = b - a
        val k2 = c - a
        val k3 = e - a
        val k4 = a - b - c + d
        val k5 = a - c - e + g
        val k6 = a - b - e + f
        val k7 = -a + b + c - d + e - f - g + h
        return a + k1 * ux + k2 * uy + k3 * uz + k4 * ux * uy + k5 * uy * uz + k6 * uz * ux + k7 * ux * uy * uz
    }

    // from https://www.shadertoy.com/view/XttSz2 by Inigo Quilez
    companion object {

        val generalNoise = "" +
                "float sdfHash(float n) { return fract(sin(n)*753.5453123); }\n" +
                "vec3  sdfHash3(vec3 n) { return fract(sin(n)*vec3(753.5453123,251.3274122,376.9911184)); }\n" +
                "float sdfNoise(vec3 x, vec2 params){\n" +
                "   vec3 index = floor(x);\n" +
                "   vec3 w = x-index;\n" +
                "   vec3 u = w*w*(3.0-2.0*w);\n" +
                "   float n = dot(index, vec3(1.0, 157.0, 113.0));\n" +
                "   float a = sdfHash(n+  0.0);\n" +
                "   float b = sdfHash(n+  1.0);\n" +
                "   float c = sdfHash(n+157.0);\n" +
                "   float d = sdfHash(n+158.0);\n" +
                "   float e = sdfHash(n+113.0);\n" +
                "   float f = sdfHash(n+114.0);\n" +
                "   float g = sdfHash(n+270.0);\n" +
                "   float h = sdfHash(n+271.0);\n" +
                "   float k0 = a;\n" +
                "   float k1 = b - a;\n" +
                "   float k2 = c - a;\n" +
                "   float k3 = e - a;\n" +
                "   float k4 = a - b - c + d;\n" +
                "   float k5 = a - c - e + g;\n" +
                "   float k6 = a - b - e + f;\n" +
                "   float k7 = a - b - c + d - e + f + g - h;\n" +
                "   return pow(k0 + k1*u.x + k2*u.y + k3*u.z + k4*u.x*u.y + k5*u.y*u.z + k6*u.z*u.x - k7*u.x*u.y*u.z, params.y);\n" +
                "}\n" +
                "float sdfVoronoi(vec3 pos, vec2 params){\n" +
                "   vec3 index = floor(pos);\n" +
                "   vec3 fract = pos-index;\n" +
                "   float best = params.x;\n" +
                "   float n = dot(index, vec3(1.0, 157.0, 113.0));\n" +
                // could potentially be simplified to be 0,1 instead of -1,0,1
                "   for(int i=0;i<8;i++){\n" +
                "      vec3 offset = vec3(ivec3(i,i>>1,i>>2)&1);\n" +
                "      float ni = n + dot(offset, vec3(1.0, 157.0, 113.0));\n" +
                "      vec3 random = sdfHash3(vec3(ni,ni*PHI,PI-ni)) - 0.5;\n" +
                "      vec3 delta = fract - offset + random * 0.75;\n" +
                "      float score = dot(delta, delta);\n" +
                "      best = min(score, best);\n" +
                "   }\n" +
                "   return pow(best, params.y);\n" +
                "}\n"

        val perlinNoise = "" +
                "float perlinNoise(vec3 f, float amplitude, vec4 params, vec2 p2){\n" +
                "   float a = params.w;\n" +
                "   float b = amplitude;\n" +
                "   for(int i=0,l=int(params.x); i<l; i++){\n" +
                "       a += b*sdfNoise(f,p2);\n" + // accumulate values
                "       b *= params.y;\n" + // amplitude decrease
                "       f *= params.z;\n" + // frequency increase
                "   }\n" +
                "   return a;\n" +
                "}\n" +
                "float voronoiNoise(vec3 f, float amplitude, vec4 params, vec2 p2){\n" +
                "   float a = params.w;\n" +
                "   float b = amplitude;\n" +
                "   for(int i=0,l=int(params.x); i<l; i++){\n" +
                "       a += b*sdfVoronoi(f,p2);\n" + // accumulate values
                "       b *= params.y;\n" + // amplitude decrease
                "       f *= params.z;\n" + // frequency increase
                "   }\n" +
                "   return a;\n" +
                "}\n"
    }

}