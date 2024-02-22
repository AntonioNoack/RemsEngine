package me.anno.sdf.shapes

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.maths.Maths.sq
import me.anno.sdf.SDFComposer.dot2
import me.anno.sdf.VariableCounter
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sign
import kotlin.math.sqrt

class SDFStairs : SDF2DShape() {

    @NotSerializedProperty
    private var params: Vector3f = Vector3f(0.2f, 0.2f, 5f)
        set(value) {
            if (dynamicSize || globalDynamic) invalidateBounds()
            else invalidateShader()
            field.set(value)
        }

    @Range(0.0, 1e38)
    var stepWidth: Float
        get() = params.x
        set(value) {
            params.x = value
            params = params
        }

    @Range(0.0, 1e38)
    var stepHeight: Float
        get() = params.y
        set(value) {
            params.y = value
            params = params
        }

    @Range(0.0, 1e38)
    var stepCount: Int
        get() = params.z.toInt()
        set(value) {
            params.z = value.toFloat()
            params = params
        }

    override fun calculateBaseBounds2d(dst: AABBf) {
        dst.setMin(0f, 0f, 0f)
        dst.setMax(stepWidth * stepCount, stepHeight * stepCount, 0f)
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
        functions.add(stairsSDF)
        smartMinBegin(builder, dstIndex)
        builder.append("sdStairs(pos")
        builder.append(trans.posIndex)
        builder.append(".").append(axes.glslName).append(",")
        val dynamicSize = dynamicSize || globalDynamic
        if (dynamicSize) builder.appendUniform(uniforms, params)
        else builder.appendVec(params)
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        val whn = params
        val w = whn.x
        val h = whn.y
        val n = whn.z
        val px1 = pos.x
        val py1 = pos.y
        val bax = w * n // total width
        val bay = h * n // total height
        var d = min(
            sq(px1 - clamp(px1, 0f, bax), py1),
            sq(px1 - bax, py1 - clamp(py1, 0f, bay))
        )
        var s = sign(max(-py1, px1 - bax))
        ///////////////////////////////////////////////////////////////
        val dia = sq(w, h)
        // rotate
        val px2 = +px1 * w + py1 * h
        val py2 = -px1 * h + py1 * w
        val id = clamp(round(px2 / dia), 0f, n - 1f)
        val px3 = px2 - id * dia
        // rotate other way around
        var px4 = +px3 * w - py2 * h
        var py4 = +px3 * h + py2 * w
        ///////////////////////////////////////////////////////////////
        val hh = h * 0.5f
        py4 -= hh
        if (py4 > hh * sign(px4)) s = 1f
        if (!(id < 0.5f || px4 > 0f)) {
            px4 = -px4
            py4 = -py4
        }
        d = min(d, sq(px4, py4 - clamp(py4, -hh, hh)))
        d = min(d, sq(px4 - clamp(px4, 0f, w), py4 - hh))
        ///////////////////////////////////////////////////////////////
        return sqrt(d) * s
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFStairs
        dst.params.set(params)
    }

    override val className: String get() = "SDFStairs"

    companion object {
        // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
        private const val stairsSDF = "" +
                "float sdStairs(vec2 p, vec3 whn){\n" +
                // formula only works if w >= h
                "   if(whn.x < whn.y){\n" +
                "       p = vec2(whn.y*whn.z-p.y,whn.x*whn.z-p.x);\n" +
                "       whn.xy = whn.yx;\n" +
                "   }\n" +

                "   vec2 wh = whn.xy;\n" +
                "   float n = whn.z;\n" +
                "   vec2 ba = wh*n;\n" +
                "   float d = min(dot2(p-vec2(clamp(p.x,0.0,ba.x),0.0)), \n" +
                "                 dot2(p-vec2(ba.x,clamp(p.y,0.0,ba.y))));\n" +
                "   float s = sign(max(-p.y,p.x-ba.x) );\n" +

                "   float dia = dot2(wh);\n" +
                "   p = mat2(wh.x,-wh.y,wh.y,wh.x)*p;\n" +
                // here is p.x and p.y correct
                "   float id = clamp(round(p.x/dia),0.0,n-1.0);\n" +
                "   p.x = p.x - id*dia;\n" +
                "   p = mat2(wh.x,wh.y,-wh.y,wh.x)*p/dia;\n" +
                // todo here all shapes seem to be approx. correct
                "   float hh = wh.y/2.0;\n" +
                "   p.y -= hh;\n" +
                "   if(p.y>hh*sign(p.x)) s=1.0;\n" +
                "   p = (id<0.5 || p.x>0.0) ? p : -p;\n" +
                // todo here everything goes wrong...
                // steps
                "   d = min( d, dot2(p-vec2(0.0,clamp(p.y,-hh,hh))) );\n" +
                "   d = min( d, dot2(p-vec2(clamp(p.x,0.0,wh.x),hh)) );\n" +

                "   return sqrt(d)*s;" +
                "}\n"
    }
}