package me.anno.ecs.components.shaders.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.sdf.SDFComposer.dot2
import me.anno.ecs.components.shaders.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.sq
import org.joml.Vector2f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

class SDFTunnel : SDF2DShape() {

    private var params = Vector2f(1f, 2f)
        set(value) {
            field.set(value)
        }

    var width
        get() = params.x
        set(value) {
            params.x = value
        }

    var height
        get() = params.y
        set(value) {
            params.y = value
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: VariableCounter,
        dstName: String,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions)
        functions.add(dot2)
        functions.add(tunnelSDF)
        smartMinBegin(builder, dstName)
        builder.append("sdTunnel(")
        writeFuncInput(builder, trans.posIndex)
        builder.append(',')
        if (dynamicSize) builder.append(defineUniform(uniforms, params))
        else writeVec(builder, params)
        builder.append(')')
        smartMinEnd(builder, dstName, nextVariableId, uniforms, functions, trans)
    }

    override fun computeSDFBase(pos: Vector4f): Float {
        applyTransform(pos)
        val w = width
        val h = height
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

    override fun clone(): SDFTunnel {
        val clone = SDFTunnel()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFTunnel
        clone.params.set(params)
    }

    override val className = "SDFTunnel"

    companion object {
        // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
        private const val tunnelSDF = "" +
                "float sdTunnel(in vec2 p, in vec2 wh){\n" +
                "   p.x = abs(p.x);" +
                "   p.y = -p.y;\n" +
                "   vec2 q = p - wh;\n" +
                "\n" +
                "   float d1 = dot2(vec2(max(q.x,0.0),q.y));\n" +
                "   q.x = (p.y>0.0) ? q.x : length(p)-wh.x;\n" +
                "   float d2 = dot2(vec2(q.x,max(q.y,0.0)));\n" +
                "   float d = sqrt(min(d1,d2));\n" +
                "\n" +
                "   return sign(max(q.x,q.y)) * d;\n" +
                "}\n"
    }

}