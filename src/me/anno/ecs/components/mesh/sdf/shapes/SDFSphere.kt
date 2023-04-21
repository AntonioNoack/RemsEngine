package me.anno.ecs.components.mesh.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.maths.Maths.length
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector4f

class SDFSphere : SDFShape() {

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
        functions.add(sdSphere)
        smartMinBegin(builder, dstIndex)
        builder.append("sdSphere(pos")
        builder.append(trans.posIndex)
        builder.append(",1.0)")
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        return length(pos.x, pos.y, pos.z) - 1f + pos.w
    }

    override val className: String get() = "SDFSphere"

    companion object {
        // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
        const val sdSphere = "" +
                "float sdSphere(vec3 p, float s){ return length(p)-s; }\n"
    }

}