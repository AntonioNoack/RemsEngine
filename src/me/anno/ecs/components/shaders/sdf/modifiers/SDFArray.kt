package me.anno.ecs.components.shaders.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.clamp
import org.joml.Vector3f
import kotlin.math.round

class SDFArray : SDFModifier() {

    // todo hex grid array and triangle grid array
    // todo better functions that allow for exactly N instances, no matter if odd or even

    /**
     * limit repetition count to 2*repLimit+1
     * */
    var repLimit = Vector3f(Float.POSITIVE_INFINITY)
        set(value) {
            field.set(value)
        }

    /**
     * m.x > 0 ? mod(pos.x, m.x) : pos.x for all xyz
     * */
    var repetition = Vector3f()
        set(value) {
            field.set(value)
        }

    var dynamicX = false
    var dynamicY = false
    var dynamicZ = false

    /**
     * whether dynamic components are allowed to be zero
     * */
    var dynamicNull = false

    override fun createTransform(
        builder: StringBuilder,
        posIndex: Int,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        functions.add("vec3 mod2(vec3 p, vec3 c, vec3 l){ return p-c*clamp(round(p/c),-l,l); }\n")
        functions.add("float mod2(float p, float c, float l){ return p-c*clamp(round(p/c),-l,l); }\n")
        val rep = repetition
        val lim = repLimit
        if (dynamicX || dynamicY || dynamicZ) {
            val repUniform = defineUniform(uniforms, rep)
            val limUniform = defineUniform(uniforms, lim)
            if (dynamicX) repeat(builder, posIndex, repUniform, limUniform, 'x')
            else if (rep.x > 0f) repeat(builder, posIndex, rep.x, lim.x, 'x')
            if (dynamicY) repeat(builder, posIndex, repUniform, limUniform, 'y')
            else if (rep.y > 0f) repeat(builder, posIndex, rep.y, lim.y, 'y')
            if (dynamicZ) repeat(builder, posIndex, repUniform, limUniform, 'z')
            else if (rep.z > 0f) repeat(builder, posIndex, rep.z, lim.z, 'z')
        } else {
            if (rep.x > 0f) repeat(builder, posIndex, rep.x, lim.x, 'x')
            if (rep.y > 0f) repeat(builder, posIndex, rep.y, lim.y, 'y')
            if (rep.z > 0f) repeat(builder, posIndex, rep.z, lim.z, 'z')
        }
    }

    private fun apply(p: Float, c: Float, l: Float): Float {
        return p - c * clamp(round(p / c), -l, l)
    }

    override fun applyTransform(pos: Vector3f) {
        val rep = repetition
        val lim = repLimit
        if (rep.x > 0f) pos.x = apply(pos.x, rep.x, lim.x)
        if (rep.y > 0f) pos.y = apply(pos.y, rep.y, lim.y)
        if (rep.z > 0f) pos.z = apply(pos.z, rep.z, lim.z)
    }

    fun repeat(
        builder: StringBuilder,
        posIndex: Int,
        value: Float,
        limit: Float,
        component: Char,
    ) {
        builder.append("pos").append(posIndex).append(".").append(component)
        builder.append("=")
        builder.append("mod2(pos").append(posIndex).append(".").append(component)
        builder.append(",")
        builder.append(value)
        builder.append(",")
        builder.append(limit)
        builder.append(");\n")
    }

    fun repeat(
        builder: StringBuilder,
        posIndex: Int,
        rep: String,
        lim: String,
        component: Char
    ) {
        if (dynamicNull) {
            builder.append("if(")
            builder.append(rep).append(".").append(component)
            builder.append(">0.0)")
        }
        builder.append("pos").append(posIndex).append(".").append(component)
        builder.append("=")
        builder.append("mod2(pos").append(posIndex).append(".").append(component)
        builder.append(",")
        builder.append(rep).append(".").append(component)
        builder.append(",")
        builder.append(lim).append(".").append(component)
        builder.append(");\n")
    }

    override fun clone(): SDFArray {
        val clone = SDFArray()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFArray
        clone.dynamicNull = dynamicNull
        clone.dynamicX = dynamicX
        clone.dynamicY = dynamicY
        clone.dynamicZ = dynamicZ
        clone.repLimit = repLimit
        clone.repetition = repetition
    }

    override val className: String = "SDFArray"

}