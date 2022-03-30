package me.anno.ecs.components.mesh.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComposer.dot2
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.sq
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Triangles.subCross
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt

open class SDFTriangle : SDFShape() {

    var a = Vector3f(1f, 0f, 0f)
        set(value) {
            if (dynamicSize) invalidateBounds()
            else invalidateShader()
            field.set(value)
        }

    var b = Vector3f(0f, 1f, 0f)
        set(value) {
            if (dynamicSize) invalidateBounds()
            else invalidateShader()
            field.set(value)
        }

    var c = Vector3f(0f, 0f, 1f)
        set(value) {
            if (dynamicSize) invalidateBounds()
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
        dstName: String,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        functions.add(dot2)
        functions.add(udTriangle)
        val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions)
        smartMinBegin(builder, dstName)
        builder.append("udTriangle(pos")
        builder.append(trans.posIndex)
        builder.append(',')
        if (dynamicSize) {
            builder.appendUniform(uniforms, a)
            builder.append(',')
            builder.appendUniform(uniforms, b)
            builder.append(',')
            builder.appendUniform(uniforms, c)
        } else {
            writeVec(builder, a)
            builder.append(',')
            writeVec(builder, b)
            builder.append(',')
            writeVec(builder, c)
        }
        builder.append(')')
        smartMinEnd(builder, dstName, nextVariableId, uniforms, functions, trans)
    }

    // dot2(ba*clamp(dot(ba,pa)/dot2(ba),0.0,1.0)-pa)
    private fun dot2Clamp(a: Vector3f, b: Vector3f, p: Vector4f): Float {
        val dot2ba = b.lengthSquared()
        val bax = b.x - a.x
        val bay = b.y - a.y
        val baz = b.z - a.z
        val pax = p.x - a.x
        val pay = p.y - a.y
        val paz = p.z - a.z
        val clamp = clamp((bax * pax + bay * pay + baz * paz) / dot2ba)
        val fx = bax * clamp - pax
        val fy = bay * clamp - pay
        val fz = baz * clamp - paz
        return sq(fx, fy, fz)
    }

    // dot(cross(ba,nor),pa)
    private fun subCrossDot(a: Vector3f, b: Vector3f, n: Vector3f, p: Vector4f): Float {
        val bax = b.x - a.x
        val bay = b.y - a.y
        val baz = b.z - a.z
        val pax = p.x - a.x
        val pay = p.y - a.y
        val paz = p.z - a.z
        val nx = n.x
        val ny = n.y
        val nz = n.z
        // 23 32 = yz zy
        // 31 13 = zx xz
        // 12 21 = xy yx
        val cx = bay * nz - baz * ny
        val cy = baz * nx - bax * nz
        val cz = bax * ny - bay * nx
        return pax * cx + pay * cy + paz * cz
    }

    override fun computeSDFBase(pos: Vector4f): Float {
        // this is kind of working, but still incorrect :/
        // (raycast is working fine, but normal is incorrect)
        val a = a
        val b = b
        val c = c
        val n = JomlPools.vec3f.create()
        subCross(a, b, c, n)
        n.mul(-1f)
        val term = if (
            sign(subCrossDot(a, b, n, pos)) +
            sign(subCrossDot(a, b, n, pos)) +
            sign(subCrossDot(a, b, n, pos)) < 2f
        ) min(
            min(
                dot2Clamp(a, b, pos),
                dot2Clamp(b, c, pos)
            ),
            dot2Clamp(c, a, pos)
        ) else sq(n.dot(pos.x - a.x, pos.y - a.y, pos.z - a.z)) / n.lengthSquared()
        JomlPools.vec3f.sub(1)
        return sqrt(term) + pos.w
    }

    override fun clone(): SDFTriangle {
        val clone = SDFTriangle()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFTriangle
        clone.a.set(a)
        clone.b.set(b)
        clone.c.set(c)
    }

    override val className = "SDFTriangle"

    companion object {
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