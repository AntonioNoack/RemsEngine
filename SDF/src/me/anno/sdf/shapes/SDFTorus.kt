package me.anno.sdf.shapes

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.length
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector4f

@Suppress("unused")
class SDFTorus : SDFShape() {

    private val params = Vector2f(1f, 0.3f)

    var totalRadius: Float
        get() = params.x
        set(value) {
            if (params.x != value) {
                if (dynamicSize || globalDynamic) invalidateBounds()
                else invalidateShader()
                params.x = value
            }
        }

    var ringRadius: Float
        get() = params.y
        set(value) {
            if (params.y != value) {
                if (dynamicSize || globalDynamic) invalidateBounds()
                else invalidateShader()
                params.y = value
            }
        }

    override fun calculateBaseBounds(dst: AABBf) {
        val r = totalRadius
        val h = ringRadius
        dst.setMin(-r, -h, -r)
        dst.setMax(+r, +h, +r)
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
        functions.add(torusSDF)
        smartMinBegin(builder, dstIndex)
        builder.append("sdTorus(pos")
        builder.append(trans.posIndex)
        builder.append(',')
        val dynamicSize = dynamicSize || globalDynamic
        if (dynamicSize) builder.appendUniform(uniforms, params)
        else builder.appendVec(params)
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        val t0 = params
        val tx = t0.x - t0.y
        val ty = t0.y
        return length(length(pos.x, pos.z) - tx, pos.y) - ty + pos.w
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFTorus
        dst.params.set(params)
    }

    companion object {
        // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
        private const val torusSDF = "" +
                "float sdTorus(vec3 p, vec2 t){\n" +
                "   t.x -= t.y;\n" +
                "   return length(vec2(length(p.xz)-t.x,p.y))-t.y;\n" +
                "}\n"
    }

}