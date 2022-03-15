package me.anno.ecs.components.shaders.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.Ptr
import me.anno.maths.Maths.length
import org.joml.Vector2f
import org.joml.Vector4f

@Suppress("unused")
class SDFTorus : SDFShape() {

    private val params = Vector2f(1f, 0.3f)

    var totalRadius
        get() = params.x
        set(value) {
            params.x = value
        }

    var ringRadius
        get() = params.y
        set(value) {
            params.y = value
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: Ptr<Int>,
        dstName: String,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions)
        functions.add(torusSDF)
        smartMinBegin(builder, dstName)
        builder.append("sdTorus(pos")
        builder.append(trans.posIndex)
        builder.append(',')
        if (dynamicSize) builder.append(defineUniform(uniforms, params))
        else writeVec(builder, params)
        builder.append(')')
        smartMinEnd(builder, dstName, nextVariableId, uniforms, functions, trans)
    }

    override fun computeSDFBase(pos: Vector4f): Float {
        applyTransform(pos)
        val t0 = params
        val tx = t0.x - t0.y
        val ty = t0.y
        return length(length(pos.x, pos.z) - tx, pos.y) - ty + pos.w
    }

    override fun clone(): SDFTorus {
        val clone = SDFTorus()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFTorus
        clone.params.set(params)
    }

    override val className = "SDFTorus"

    companion object {
        // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
        private const val torusSDF =
            "float sdTorus(vec3 p, vec2 t){ t.x -= t.y; return length(vec2(length(p.xz)-t.x,p.y))-t.y; }\n"
    }

}