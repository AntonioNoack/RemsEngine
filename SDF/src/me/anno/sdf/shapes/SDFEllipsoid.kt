package me.anno.sdf.shapes

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.length
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f

open class SDFEllipsoid : SDFShape() {

    var halfAxes = Vector3f(1f)
        set(value) {
            if (!dynamicSize && !globalDynamic) invalidateShader()
            else invalidateBounds()
            field.set(value)
        }

    override fun calculateBaseBounds(dst: AABBf) {
        val r = halfAxes
        dst.setMin(-r.x, -r.y, -r.z)
        dst.setMax(+r.x, +r.y, +r.z)
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
        functions.add(sdEllipsoid)
        smartMinBegin(builder, dstIndex)
        builder.append("sdEllipsoid(pos").append(trans.posIndex).append(',')
        val dynamicSize = dynamicSize || globalDynamic
        if (dynamicSize) builder.appendUniform(uniforms, halfAxes)
        else builder.appendVec(halfAxes)
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        val r = halfAxes
        val k0 = length(pos.x / r.x, pos.y / r.y, pos.z / r.z)
        val k1 = length(pos.x / (r.x * r.x), pos.y / (r.y * r.y), pos.z / (r.z * r.z))
        return k0 * (k0 - 1f) / k1
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFEllipsoid
        dst.halfAxes = halfAxes
    }

    override val className: String get() = "SDFEllipsoid"

    companion object {
        // from https://iquilezles.org/www/articles/distfunctions/distfunctions.htm, Inigo Quilez
        // not exact
        const val sdEllipsoid = "" +
                "float sdEllipsoid(vec3 p, vec3 r) {\n" +
                "   float k0 = length(p/r);\n" +
                "   float k1 = length(p/(r*r));\n" +
                "   return k0*(k0-1.0)/k1;\n" +
                "}"

    }

}