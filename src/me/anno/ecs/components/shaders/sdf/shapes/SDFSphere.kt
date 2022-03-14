package me.anno.ecs.components.shaders.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.Ptr
import me.anno.gpu.shader.GLSLType
import org.joml.Vector3f

class SDFSphere : SDFShape() {

    var radius = 1f

    override fun createSDFShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextIndex: Ptr<Int>,
        dstName: String,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        val (posIndex, scaleName) = createTransformShader(builder, posIndex0, nextIndex, uniforms, functions)
        functions.add(sdSphere)
        smartMinBegin(builder, dstName)
        builder.append("sdSphere(pos")
        builder.append(posIndex)
        builder.append(',')
        if (dynamicSize) builder.append(defineUniform(uniforms, GLSLType.V1F, { radius }))
        else builder.append(radius)
        builder.append(')')
        smartMinEnd(builder, scaleName)
    }

    override fun computeSDF(pos: Vector3f): Float {
        applyTransform(pos)
        return pos.length() - radius
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