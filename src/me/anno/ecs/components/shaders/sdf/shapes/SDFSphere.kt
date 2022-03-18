package me.anno.ecs.components.shaders.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.length
import org.joml.Vector4f

class SDFSphere : SDFShape() {

    var radius = 1f

    override fun buildShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: VariableCounter,
        dstName: String,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions)
        functions.add(sdSphere)
        smartMinBegin(builder, dstName)
        builder.append("sdSphere(pos")
        builder.append(trans.posIndex)
        builder.append(',')
        if (dynamicSize) builder.append(defineUniform(uniforms, GLSLType.V1F, { radius }))
        else builder.append(radius)
        builder.append(')')
        smartMinEnd(builder, dstName, nextVariableId, uniforms, functions, trans)
    }

    override fun computeSDFBase(pos: Vector4f): Float {
        applyTransform(pos)
        return length(pos.x, pos.y, pos.z) - radius + pos.w
    }

    override fun clone(): SDFSphere {
        val clone = SDFSphere()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFSphere
        clone.radius = radius
    }

    override val className = "SDFSphere"

    companion object {
        // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
        const val sdSphere = "" +
                "float sdSphere(vec3 p, float s){ return length(p)-s; }\n"
    }

}