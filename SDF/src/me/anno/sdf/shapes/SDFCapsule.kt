package me.anno.sdf.shapes

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import me.anno.maths.Maths.sq
import me.anno.sdf.VariableCounter
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f

// todo this is broken visually :(
open class SDFCapsule : SDFShape() {

    var p0 = Vector3f(0f, 0f, 0f)
        set(value) {
            field.set(value)
            update()
        }

    var p1 = Vector3f(0f, 1f, 0f)
        set(value) {
            field.set(value)
            update()
        }

    var radius = 1f
        set(value) {
            if (field != value) {
                update()
                field = value
            }
        }

    private fun update() {
        if (dynamicSize || globalDynamic) invalidateBounds()
        else invalidateShader()
    }

    override fun calculateBaseBounds(dst: AABBf) {
        dst.union(p0)
        dst.union(p1)
        dst.addMargin(radius)
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
        functions.add(sdCapsule)
        smartMinBegin(builder, dstIndex)
        builder.append("sdCapsule(pos").append(trans.posIndex).append(',')
        val dynamicSize = dynamicSize || globalDynamic
        if (dynamicSize) builder.appendUniform(uniforms, p0)
        else builder.appendVec(p0)
        builder.append(',')
        if (dynamicSize) builder.appendUniform(uniforms, p1)
        else builder.appendVec(p1)
        builder.append(',')
        if (dynamicSize) builder.appendUniform(uniforms) { radius }
        else builder.append(radius)
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        val a = p0
        val b = p1
        val pax = pos.x - a.x
        val pay = pos.y - a.y
        val paz = pos.z - a.z
        val bax = b.x - a.x
        val bay = b.y - a.y
        val baz = b.z - a.z
        val h = clamp((pax * bax + pay * bay + paz * baz) / sq(bax, bay, baz))
        return length(pax - bax * h, pay - bay * h, paz - baz * h) - radius + pos.w
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFCapsule
        dst.p0 = p0
        dst.p1 = p1
        dst.radius = radius
    }

    companion object {
        // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
        const val sdCapsule = "" +
                "float sdCapsule(vec3 p, vec3 a, vec3 b, float r) {\n" +
                "   vec3 pa = p-a, ba = b-a;\n" +
                "   float h = clamp(dot(pa,ba)/dot(ba,ba), 0.0, 1.0);\n" +
                "   return length(pa - ba*h) - r;\n" +
                "}"
    }
}