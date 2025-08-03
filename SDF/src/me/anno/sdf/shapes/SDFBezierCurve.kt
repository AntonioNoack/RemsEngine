package me.anno.sdf.shapes

import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.sq
import me.anno.sdf.VariableCounter
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.structures.maps.LazyMap
import org.joml.AABBf
import org.joml.Vector4f
import kotlin.math.min

class SDFBezierCurve : SDFShape() {

    @Type("List<Vector4f>")
    var points = arrayListOf(
        Vector4f(0f, 0f, 0f, 0.1f),
        Vector4f(0f, 1f, 0f, 0.1f),
        Vector4f(0f, 1f, 1f, 0.1f)
    )
        set(value) {
            if (field === value && dynamicSize) invalidateShaderBounds()
            else invalidateShader()
            field = value
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: VariableCounter,
        dstIndex: Int,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ) {
        val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions, seeds)
        val points = points
        functions.add(sdBezier)
        functions.add(Companion.functions[points.size])
        smartMinBegin(builder, dstIndex)
        builder.append("sdBezier(pos").append(trans.posIndex).append(',')
        if (dynamicSize) {
            for (i in points.indices) {
                if (i > 0) builder.append(',')
                builder.appendUniform(uniforms, points[i])
            }
        } else {
            for (i in points.indices) {
                if (i > 0) builder.append(',')
                builder.appendVec(points[i])
            }
        }
        builder.append(")")
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun calculateBaseBounds(dst: AABBf) {
        val points = points
        val p0 = points.first()
        dst.setMin(p0.x - p0.w, p0.y - p0.w, p0.z - p0.w)
        dst.setMax(p0.x + p0.w, p0.y + p0.w, p0.z + p0.w)
        for (i in 1 until points.size) {
            val p = points[i]
            dst.union(p.x - p.w, p.y - p.w, p.z - p.w)
            dst.union(p.x + p.w, p.y + p.w, p.z + p.w)
        }
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        val points = points
        return when (points.size) {
            0 -> Float.POSITIVE_INFINITY // impossible
            1 -> { // a single point
                val p0 = points[0]
                pos.distance(p0.x, p0.y, p0.z, pos.w) - p0.w + pos.w
            }
            2 -> { // a line
                val p0 = points[0]
                val p1 = points[1]
                val tmp = JomlPools.vec4f.create()
                tmp.set(p0).add(p1).mul(0.5f)
                val r = sdBezier(pos, p0, tmp, p1)
                JomlPools.vec4f.sub(1)
                return r + pos.w
            }
            else -> {
                // proper Bézier curve, approximation through quadratic curves
                var a = points[0]
                var b = points[1]
                var c = points[2]
                var best = sdBezier(pos, a, b, c)
                for (i in 3 until points.size) {
                    a = b
                    b = c
                    c = points[i]
                    best = min(best, sdBezier(pos, a, b, c))
                }
                best + pos.w
            }
        }
    }

    private fun sdBezier(p: Vector4f, b0: Vector4f, b1: Vector4f, b2: Vector4f): Float {
        val b0x = b0.x - p.x
        val b0y = b0.y - p.y
        val b0z = b0.z - p.z
        val b1x = b1.x - p.x
        val b1y = b1.y - p.y
        val b1z = b1.z - p.z
        val b2x = b2.x - p.x
        val b2y = b2.y - p.y
        val b2z = b2.z - p.z
        val b01x = b0y * b1z - b0z * b1y
        val b01y = b0z * b1x - b0x * b1z
        val b01z = b0x * b1y - b0y * b1x
        val b12x = b1y * b2z - b1z * b2y
        val b12y = b1z * b2x - b1x * b2z
        val b12z = b1x * b2y - b1y * b2x
        val b20x = b2y * b0z - b2z * b0y
        val b20y = b2z * b0x - b2x * b0z
        val b20z = b2x * b0y - b2y * b0x
        val nx = b01x + b12x + b20x
        val ny = b01y + b12y + b20y
        val nz = b01z + b12z + b20z
        val a = -(nx * b20x + ny * b20y + nz * b20z)
        val b = -(nx * b01x + ny * b01y + nz * b01z)
        val d = -(nx * b12x + ny * b12y + nz * b12z)
        val m = -sq(nx, ny, nz)
        val db = d - b
        val ba05 = b + a * 0.5f
        val da05 = d + a * 0.5f
        val gx = db * b1x + ba05 * b2x - da05 * b0x
        val gy = db * b1y + ba05 * b2y - da05 * b0y
        val gz = db * b1z + ba05 * b2z - da05 * b0z
        val f = a * a * 0.25f - b * d
        val kx = b0x - 2f * b1x + b2x
        val ky = b0y - 2f * b1y + b2y
        val kz = b0z - 2f * b1z + b2z
        val t = clamp((a * 0.5f + b - 0.5f * f * (gx * kx + gy * ky + gz * kz) / sq(gx, gy, gz)) / m)
        // could be optimized by directly computing the coefficients
        return length(
            mix(mix(b0x, b1x, t), mix(b1x, b2x, t), t),
            mix(mix(b0y, b1y, t), mix(b1y, b2y, t), t),
            mix(mix(b0z, b1z, t), mix(b1z, b2z, t), t)
        ) - mix(mix(b0.w, b1.w, t), mix(b1.w, b2.w, t), t)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SDFBezierCurve) return
        val points = points
        dst.points = createArrayList(points.size) { Vector4f(points[it]) }
    }

    companion object {

        val functions = LazyMap { n: Int ->
            when (n) {
                0 -> "float sdBezier(vec3 p){ return Infinity; }\n"
                1 -> "float sdBezier(vec3 p, vec4 b0){ return length(p-b0.xyz)-b0.w; }\n"
                2 -> "float sdBezier(vec3 p, vec4 b0, vec4 b1){ return sdBezier(p,b0,(b0+b1)*0.5,b1); }\n"
                3 -> ""
                else -> {
                    // create function
                    val builder = StringBuilder()
                    builder.append("float sdBezier(vec3 p")
                    for (i in 0 until n) builder.append(", vec4 b").append(i)
                    builder.append(
                        "){\n" +
                                "vec4 a=b0,b=b1,c=b2,na,nb,nc;\n" +
                                "float dist=sdBezier(p,a,b,c);\n"
                    )
                    for (i in 3 until n) {
                        builder.append("na=c;nb=c+c-b;c=b").append(i).append(";\n")
                        builder.append("a=na;b=nb;dist=min(dist,sdBezier(p,a,b,c));\n")
                    }
                    builder.append("return dist;\n}\n")
                    builder.toString()
                }
            }
        }

        // from https://www.shadertoy.com/view/ldj3Wh, Inigo Quilez
        const val sdBezier = "" +
                // x: distance, y: relative progress
                "vec2 sdBezier(vec3 p, vec3 b0, vec3 b1, vec3 b2){\n" +
                "   b0 -= p;\n" +
                "   b1 -= p;\n" +
                "   b2 -= p;\n" +
                "   vec3 b01 = cross(b0,b1);\n" +
                "   vec3 b12 = cross(b1,b2);\n" +
                "   vec3 b20 = cross(b2,b0);\n" +
                "   vec3 n =  b01+b12+b20;\n" +
                "   float a = -dot(b20,n);\n" +
                "   float b = -dot(b01,n);\n" +
                "   float d = -dot(b12,n);\n" +
                "   float m = -dot(n,n);\n" +
                "   vec3  g = (d-b)*b1 + (b+a*0.5)*b2 + (-d-a*0.5)*b0;\n" +
                "   float f = a*a*0.25-b*d;\n" +
                "   vec3  k = b0-2.0*b1+b2;\n" +
                "   float t = clamp((a*0.5+b-0.5*f*dot(g,k)/dot(g,g))/m, 0.0, 1.0);\n" +
                "   return vec2(length(mix(mix(b0,b1,t), mix(b1,b2,t),t)),t);\n" +
                "}\n" +
                // x,y,z,thickness
                "float sdBezier(vec3 p, vec4 b0, vec4 b1, vec4 b2){\n" +
                "   vec2 bez = sdBezier(p,b0.xyz,b1.xyz,b2.xyz);\n" +
                "   float thickness = mix(mix(b0.w,b1.w,bez.y),mix(b1.w,b2.w,bez.y),bez.y);\n" +
                "   return bez.x-thickness;\n" +
                "}\n"
    }
}