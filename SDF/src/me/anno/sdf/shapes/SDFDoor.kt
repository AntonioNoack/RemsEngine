package me.anno.sdf.shapes

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.sdf.SDFComposer.dot2
import me.anno.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.sq
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

class SDFDoor : SDF2DShape() {

    private var params: Vector2f = Vector2f(1f, 2f)
        set(value) {
            field.set(value)
        }

    @Range(0.0, 1e38)
    var bottomExtends: Float
        get() = params.y
        set(value) {
            if (params.y != value) {
                if (dynamicSize || globalDynamic) invalidateBounds()
                else invalidateShader()
                params.y = value
            }
        }

    override fun calculateBaseBounds2d(dst: AABBf) {
        val w = 1f
        val h = bottomExtends
        dst.setMin(-w, -h, 0f)
        dst.setMax(+w, +w, 0f)
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
        functions.add(dot2)
        functions.add(doorSDF)
        smartMinBegin(builder, dstIndex)
        builder.append("sdDoor(")
        writeFuncInput(builder, trans.posIndex)
        builder.append(',')
        val dynamicSize = dynamicSize || globalDynamic
        if (dynamicSize) builder.appendUniform(uniforms, params)
        else builder.appendVec(params)
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        val params = params
        val w = params.x
        val h = params.y
        val px = abs(pos.x)
        val py = -pos.y
        var qx = px - w
        val qy = py - h
        val d1 = sq(max(qx, 0f), qy)
        if (py <= 0f) qx = length(px, py) - w
        val d2 = sq(qx, max(qy, 0f))
        val d = sqrt(min(d1, d2))
        return d * sign(max(qx, qy))
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SDFDoor) return
        dst.params.set(params)
    }

    companion object {
        // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
        private const val doorSDF = "" +
                "float sdDoor(in vec2 p, in vec2 wh){\n" +
                "   p.x = abs(p.x);" +
                "   p.y = -p.y;\n" +
                "   vec2 q = p - wh;\n" +
                "   float d1 = dot2(vec2(max(q.x,0.0),q.y));\n" +
                "   q.x = (p.y>0.0) ? q.x : length(p)-wh.x;\n" +
                "   float d2 = dot2(vec2(q.x,max(q.y,0.0)));\n" +
                "   float d = sqrt(min(d1,d2));\n" +
                "   return sign(max(q.x,q.y)) * d;\n" +
                "}\n"
    }

}