package me.anno.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.sq
import me.anno.sdf.SDFComposer.dot2
import me.anno.sdf.VariableCounter
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Triangles.crossDot
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt

open class SDFTriangle : SDFShape() {

    var a: Vector3f = Vector3f(1f, 0f, 0f)
        set(value) {
            if (dynamicSize || globalDynamic) invalidateBounds()
            else invalidateShader()
            field.set(value)
        }

    var b: Vector3f = Vector3f(0f, 1f, 0f)
        set(value) {
            if (dynamicSize || globalDynamic) invalidateBounds()
            else invalidateShader()
            field.set(value)
        }

    var c: Vector3f = Vector3f(0f, 0f, 1f)
        set(value) {
            if (dynamicSize || globalDynamic) invalidateBounds()
            else invalidateShader()
            field.set(value)
        }

    override fun calculateBaseBounds(dst: AABBf) {
        dst.minX = min(a.x, min(b.x, c.x))
        dst.maxX = max(a.x, max(b.x, c.x))
        dst.minY = min(a.y, min(b.y, c.y))
        dst.maxY = max(a.y, max(b.y, c.y))
        dst.minZ = min(a.z, min(b.z, c.z))
        dst.maxZ = max(a.z, max(b.z, c.z))
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
        functions.add(dot2)
        functions.add(udTriangle)
        val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions, seeds)
        smartMinBegin(builder, dstIndex)
        builder.append("udTriangle(pos")
        builder.append(trans.posIndex)
        builder.append(',')
        val dynamicSize = dynamicSize || globalDynamic
        if (dynamicSize) {
            builder.appendUniform(uniforms, a)
            builder.append(',')
            builder.appendUniform(uniforms, b)
            builder.append(',')
            builder.appendUniform(uniforms, c)
        } else {
            builder.appendVec(a)
            builder.append(',')
            builder.appendVec(b)
            builder.append(',')
            builder.appendVec(c)
        }
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        val term = calculateDistSq(a, b, c, pos, false)
        return sqrt(term) + pos.w
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFTriangle
        dst.a.set(a)
        dst.b.set(b)
        dst.c.set(c)
    }

    override val className: String get() = "SDFTriangle"

    companion object {

        fun calculateDistSq(
            a: Vector3f, b: Vector3f, c: Vector3f,
            ba: Vector3f, cb: Vector3f, ac: Vector3f,
            pa: Vector3f, pb: Vector3f, pc: Vector3f,
            pos: Vector4f, n: Vector3f,
        ): Float {

            b.sub(a, ba)
            c.sub(b, cb)
            a.sub(c, ac)

            pa.set(pos.x, pos.y, pos.z).sub(a)
            pb.set(pos.x, pos.y, pos.z).sub(b)
            pc.set(pos.x, pos.y, pos.z).sub(c)

            ba.cross(ac, n)

            return if (
                sign(crossDot(ba, n, pa)) +
                sign(crossDot(cb, n, pb)) +
                sign(crossDot(ac, n, pc)) < 2f
            ) min(
                min(
                    dot2Clamp(ba, pa),
                    dot2Clamp(cb, pb)
                ),
                dot2Clamp(ac, pc)
            ) else {
                sq(n.dot(pa)) / n.lengthSquared()
            }
        }

        fun calculateDistSq(a: Vector3f, b: Vector3f, c: Vector3f, pos: Vector4f, signed: Boolean): Float {
            val pool = JomlPools.vec3f
            val cb = pool.create()
            val ba = pool.create()
            val ac = pool.create()
            val pa = pool.create()
            val pb = pool.create()
            val pc = pool.create()
            val n = pool.create()
            var dist = calculateDistSq(a, b, c, ba, cb, ac, pa, pb, pc, pos, n)
            if (signed && n.dot(pos.x, pos.y, pos.z) > 0f) dist = -dist
            pool.sub(7)
            return dist
        }

        // dot2(ba*clamp(dot(ba,pa)/dot2(ba),0.0,1.0)-pa)
        fun dot2Clamp(ba: Vector3f, pa: Vector3f): Float {
            val clamp = clamp(ba.dot(pa) / ba.lengthSquared(), 0f, 1f)
            val fx = ba.x * clamp - pa.x
            val fy = ba.y * clamp - pa.y
            val fz = ba.z * clamp - pa.z
            return fx * fx + fy * fy + fz * fz
        }

        // https://iquilezles.org/www/articles/distfunctions/distfunctions.htm
        private const val udTriangle = "" +
                "float udTriangle(vec3 p, vec3 a, vec3 b, vec3 c){\n" +
                "  vec3 ba = b - a; vec3 pa = p - a;\n" +
                "  vec3 cb = c - b; vec3 pb = p - b;\n" +
                "  vec3 ac = a - c; vec3 pc = p - c;\n" +
                "  vec3 nor = cross(ba, ac);\n" +
                "  return sqrt(\n" +
                "    (sign(dot(cross(ba,nor),pa)) +\n" +
                "     sign(dot(cross(cb,nor),pb)) +\n" +
                "     sign(dot(cross(ac,nor),pc))<2.0)\n" +
                "     ?\n" +
                "     min(min(\n" +
                "     dot2(ba*clamp(dot(ba,pa)/dot2(ba),0.0,1.0)-pa),\n" +
                "     dot2(cb*clamp(dot(cb,pb)/dot2(cb),0.0,1.0)-pb)),\n" +
                "     dot2(ac*clamp(dot(ac,pc)/dot2(ac),0.0,1.0)-pc))\n" +
                "     :\n" +
                "     dot(nor,pa)*dot(nor,pa)/dot2(nor));" +
                "}\n"
    }

}