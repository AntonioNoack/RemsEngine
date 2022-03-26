package me.anno.ecs.components.mesh.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComposer.dot2
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.max
import kotlin.math.min

open class SDFTriangle : SDFShape() {

    var a = Vector3f(1f, 0f, 0f)
        set(value) {
            if(dynamicSize) invalidateBounds()
            else invalidateShader()
            field.set(value)
        }

    var b = Vector3f(0f, 1f, 0f)
        set(value) {
            if(dynamicSize) invalidateBounds()
            else invalidateShader()
            field.set(value)
        }

    var c = Vector3f(0f, 0f, 1f)
        set(value) {
            if(dynamicSize) invalidateBounds()
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

    override fun computeSDFBase(pos: Vector4f): Float {
        applyTransform(pos)
        return pos.y
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